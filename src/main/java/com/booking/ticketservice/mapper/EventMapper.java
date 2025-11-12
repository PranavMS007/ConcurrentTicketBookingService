package com.booking.ticketservice.mapper;

import com.booking.ticketservice.dto.EventDTO;
import com.booking.ticketservice.model.Event;

import java.util.List;
import java.util.stream.Collectors;
/**
 * Utility class for mapping between Event (Entity) and EventDTO (Data Transfer Object).
 * This is for decoupling internal model from public API.
 */
public class EventMapper {
    /**
     * Converts a single Event entity to an EventDTO.
     */
    public static EventDTO toDTO(Event event) {
        if (event == null) {
            return null;
        }
        return new EventDTO(
                event.getId(),
                event.getEventName(),
                event.getAvailableTickets()
        );
    }

    /**
     * Converts a list of Event entities to a list of EventDTOs.
     */
    public static List<EventDTO> toDTOList(List<Event> events) {
        return events.stream()
                .map(EventMapper::toDTO)
                .collect(Collectors.toList());
    }
}
