package com.booking.ticketservice.service;

import com.booking.ticketservice.exception.NotEnoughTicketsException;
import com.booking.ticketservice.model.Event;
import com.booking.ticketservice.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * This test is meant to simulate what happens when a multiple users try to book tickets at the same time.
 *
 * Scenario:
 * - We start with an event that has 100 tickets.
 * - Then we spin up 20 threads (think of them as 20 different users).
 * - Each user tries to book 10 tickets.
 *   So in total, there are 200 booking attempts for only 100 tickets.
 *
 * What we expect:
 * - Only 10 users should be able to book successfully (10 x 10 = 100 tickets).
 * - The other 10 should fail with a NotEnoughTicketsException.
 * - In the end, the number of available tickets in the database should be 0.
 * - If the ticket count goes negative, or more than 10 users succeed,
 *   it means our concurrency logic isn’t working correctly.
 */

@SpringBootTest
public class TicketServiceConcurrencyTest {
    private static final Logger log = LoggerFactory.getLogger(TicketServiceConcurrencyTest.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private EventRepository eventRepository;

    private Long testEventId;

    @BeforeEach
    void setUp() {
        // Clean up before each test and create a fresh event
        eventRepository.deleteAll();
        Event testEvent = new Event(null, "Concurrency Test Concert", 100);
        Event savedEvent = eventRepository.save(testEvent);
        testEventId = savedEvent.getId();
        log.info("Setup complete. Created event ID: {} with 100 tickets.", testEventId);
    }

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    void testConcurrentBooking() throws InterruptedException {
        int numThreads = 20;
        int ticketsPerThread = 10;
        int initialTickets = 100;

        int expectedSuccessfulBookings = initialTickets / ticketsPerThread;
        int expectedFailedBookings = numThreads - expectedSuccessfulBookings;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startCountdown = new CountDownLatch(1);
        CountDownLatch endCountDown = new CountDownLatch(numThreads);
        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger failedBookings = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i + 1;
            executor.submit(() -> {
                try {
                    startCountdown.await();

                    log.debug("[Thread {}] starts to book {} tickets...", threadNum, ticketsPerThread);
                    ticketService.bookTickets(testEventId, ticketsPerThread);

                    successfulBookings.incrementAndGet();
                    log.info("[Thread {}] BOOKING SUCCEEDED", threadNum);

                } catch (NotEnoughTicketsException e) {
                    failedBookings.incrementAndGet();
                    log.info("[Thread {}] BOOKING FAILED (Not Enough Tickets)", threadNum);
                } catch (Exception e) {
                    log.error("[Thread {}] UNEXPECTED ERROR: ", threadNum, e);
                } finally {
                    endCountDown.countDown();
                }
            });
        }

        log.info("All threads ready to start simultaneously.");
        Thread.sleep(1000);
        startCountdown.countDown();

        log.info("All threads started. Waiting for completion.");

        boolean allThreadsFinished = endCountDown.await(20, TimeUnit.SECONDS);

        if (!allThreadsFinished) {
            log.warn("Not all threads finished in time.");
        } else {
            log.info(".All threads finished.");
        }

        log.info("Successful bookings: {}", successfulBookings.get());
        log.info("Failed bookings: {}", failedBookings.get());

        assertEquals(expectedSuccessfulBookings, successfulBookings.get(), "Incorrect number of successful bookings.");
        assertEquals(expectedFailedBookings, failedBookings.get(), "Incorrect number of failed bookings.");

        Event finalEvent = eventRepository.findById(testEventId)
                .orElseThrow(() -> new AssertionError("Event not found after test!"));

        int expectedRemainingTickets = 0;
        log.info("Final tickets in DB: {}", finalEvent.getAvailableTickets());

        assertEquals(expectedRemainingTickets, finalEvent.getAvailableTickets(), "Database state is incorrect. Tickets were overbooked or underbooked.");

        executor.shutdown();
    }
}
