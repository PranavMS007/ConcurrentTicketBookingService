package com.booking.ticketservice.service;

import com.booking.ticketservice.dto.EventDTO;
import com.booking.ticketservice.exception.EventNotFoundException;
import com.booking.ticketservice.exception.NotEnoughTicketsException;
import com.booking.ticketservice.mapper.EventMapper;
import com.booking.ticketservice.model.Event;
import com.booking.ticketservice.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final EventRepository eventRepository;

    @Autowired
    public TicketService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Retrieves all events as DTOs.
     */
    @Transactional(readOnly = true)
    public List<EventDTO> getAllEvents() {
        log.info("Fetching all events");
        List<Event> events = eventRepository.findAll();
        // Map the list of entities to a list of DTOs
        return EventMapper.toDTOList(events);
    }

    /**
     * Gets the details for a single event as a DTO.
     */
    @Transactional(readOnly = true)
    public EventDTO getEventDetails(Long eventId) {
        log.info("Fetching details for event ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", eventId);
                    return new EventNotFoundException("Event not found with ID: " + eventId);
                });

        // Map the single entity to a DTO
        return EventMapper.toDTO(event);
    }

    /**
     * Booking logic remains the same.
     * It does not need to return the entity, so its signature is unchanged.
     */

    @Transactional
    public void bookTickets(Long eventId, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Number of tickets to book must be positive.");
        }

        log.info("Attempting to book {} tickets for event ID: {}", count, eventId);

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found during booking attempt for ID: {}", eventId);
                    return new EventNotFoundException("Event not found with ID: " + eventId);
                });

        log.debug("Found event: {}. Available tickets: {}", event.getEventName(), event.getAvailableTickets());

        if (event.getAvailableTickets() < count) {
            log.warn("Failed to book {} tickets for event ID: {}. Only {} available.",
                    count, eventId, event.getAvailableTickets());
            throw new NotEnoughTicketsException(
                    "Not enough tickets available. Requested: " + count +
                            ", Available: " + event.getAvailableTickets()
            );
        }

        int newTicketCount = event.getAvailableTickets() - count;
        event.setAvailableTickets(newTicketCount);

        eventRepository.save(event);

        log.info("Successfully booked {} tickets for event ID: {}. Remaining: {}",
                count, eventId, newTicketCount);
    }
}
