package com.booking.ticketservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.ticketservice.dto.EventDTO;
import com.booking.ticketservice.exception.EventNotFoundException;
import com.booking.ticketservice.exception.NotEnoughTicketsException;
import com.booking.ticketservice.model.Event;
import com.booking.ticketservice.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {TicketService.class})
@DisabledInAotMode
@ExtendWith(SpringExtension.class)
class TicketServiceTest {
    @MockitoBean
    private EventRepository eventRepository;

    @Autowired
    private TicketService ticketService;


    //Get All Events
    @Test
    void testGetAllEventsWhenAvailableTicketsIsOne() {
        Event event = new Event();
        event.setAvailableTickets(1);
        event.setEventName("AWS cloud summit");
        event.setId(1L);

        ArrayList<Event> eventList = new ArrayList<>();
        eventList.add(event);
        when(eventRepository.findAll()).thenReturn(eventList);

        List<EventDTO> actualAllEvents = ticketService.getAllEvents();

        verify(eventRepository).findAll();
        assertEquals(1, actualAllEvents.size());
        EventDTO getResult = actualAllEvents.get(0);
        assertEquals("AWS cloud summit", getResult.eventName());
        assertEquals(1, getResult.availableTickets());
        assertEquals(1L, getResult.id().longValue());
    }

    @Test
    void testGetAllEventsWhenAvailableTicketsIsZero() {
        Event event = new Event();
        event.setAvailableTickets(1);
        event.setEventName("AWS cloud summit");
        event.setId(1L);

        Event event2 = new Event();
        event2.setAvailableTickets(0);
        event2.setEventName("Event Name");
        event2.setId(2L);

        ArrayList<Event> eventList = new ArrayList<>();
        eventList.add(event2);
        eventList.add(event);
        when(eventRepository.findAll()).thenReturn(eventList);

        List<EventDTO> actualAllEvents = ticketService.getAllEvents();

        verify(eventRepository).findAll();
        assertEquals(2, actualAllEvents.size());
        EventDTO getResult = actualAllEvents.get(0);
        assertEquals("Event Name", getResult.eventName());
        EventDTO getResult2 = actualAllEvents.get(1);
        assertEquals("AWS cloud summit", getResult2.eventName());
        assertEquals(0, getResult.availableTickets());
        assertEquals(1, getResult2.availableTickets());
        assertEquals(1L, getResult2.id().longValue());
        assertEquals(2L, getResult.id().longValue());
    }


    @Test
    void testGetAllEventsWhenEmpty() {
        when(eventRepository.findAll()).thenReturn(new ArrayList<>());

        List<EventDTO> actualAllEvents = ticketService.getAllEvents();

        verify(eventRepository).findAll();
        assertTrue(actualAllEvents.isEmpty());
    }


    @Test
    void testGetAllEventsThrowIllegalArgumentException() {
        when(eventRepository.findAll()).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> ticketService.getAllEvents());
        verify(eventRepository).findAll();
    }


    //Get Event Details
    @Test
    void testGetEventDetails() {
        Event event = new Event();
        event.setAvailableTickets(1);
        event.setEventName("Event Name");
        event.setId(1L);
        Optional<Event> ofResult = Optional.of(event);
        when(eventRepository.findById(Mockito.<Long>any())).thenReturn(ofResult);

        EventDTO actualEventDetails = ticketService.getEventDetails(1L);

        verify(eventRepository).findById(1L);
        assertEquals("Event Name", actualEventDetails.eventName());
        assertEquals(1, actualEventDetails.availableTickets());
        assertEquals(1L, actualEventDetails.id().longValue());
    }


    @Test
    void testGetEventDetailsThrowEventNotFoundException() {
        Optional<Event> emptyResult = Optional.empty();
        when(eventRepository.findById(Mockito.<Long>any())).thenReturn(emptyResult);

        assertThrows(EventNotFoundException.class, () -> ticketService.getEventDetails(1L));
        verify(eventRepository).findById(1L);
    }


    @Test
    void testGetEventDetailsThrowIllegalArgumentException() {
        when(eventRepository.findById(Mockito.<Long>any())).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> ticketService.getEventDetails(1L));
        verify(eventRepository).findById(1L);
    }


    //Book Tickets
    @Test
    void testBookTickets() {
        when(eventRepository.findByIdForUpdate(Mockito.<Long>any()))
                .thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> ticketService.bookTickets(1L, 3));
        verify(eventRepository).findByIdForUpdate(1L);
    }


    @Test
    void testBookTicketsEventRepositorySave() {
        Event event = new Event();
        event.setAvailableTickets(3);
        event.setEventName("Event Name");
        event.setId(1L);
        Optional<Event> ofResult = Optional.of(event);

        Event event2 = new Event();
        event2.setAvailableTickets(1);
        event2.setEventName("Event Name");
        event2.setId(1L);
        when(eventRepository.save(Mockito.<Event>any())).thenReturn(event2);
        when(eventRepository.findByIdForUpdate(Mockito.<Long>any())).thenReturn(ofResult);

        ticketService.bookTickets(1L, 3);

        verify(eventRepository).findByIdForUpdate(1L);
        verify(eventRepository).save(isA(Event.class));
    }


    @Test
    void testBookTicketsThrowIllegalArgumentException() {
        Event event = new Event();
        event.setAvailableTickets(3);
        event.setEventName("Event Name");
        event.setId(1L);
        Optional<Event> ofResult = Optional.of(event);
        when(eventRepository.save(Mockito.<Event>any())).thenThrow(new IllegalArgumentException());
        when(eventRepository.findByIdForUpdate(Mockito.<Long>any())).thenReturn(ofResult);

        assertThrows(IllegalArgumentException.class, () -> ticketService.bookTickets(1L, 3));
        verify(eventRepository).findByIdForUpdate(1L);
        verify(eventRepository).save(isA(Event.class));
    }


    @Test
    void testBookTicketsThrowEventNotFoundException() {
        Optional<Event> emptyResult = Optional.empty();
        when(eventRepository.findByIdForUpdate(Mockito.<Long>any())).thenReturn(emptyResult);

        assertThrows(EventNotFoundException.class, () -> ticketService.bookTickets(1L, 3));
        verify(eventRepository).findByIdForUpdate(1L);
    }


    @Test
    void testBookTicketsThrowNotEnoughTicketsException() {
        Event event = new Event();
        event.setAvailableTickets(1);
        event.setEventName("Event Name");
        event.setId(1L);
        Optional<Event> ofResult = Optional.of(event);
        when(eventRepository.findByIdForUpdate(Mockito.<Long>any())).thenReturn(ofResult);

        assertThrows(NotEnoughTicketsException.class, () -> ticketService.bookTickets(1L, 3));
        verify(eventRepository).findByIdForUpdate(1L);
    }
}
