package com.booking.ticketservice.dto;
/**
 * Data Transfer Object (DTO) for the Event.
 */
public record EventDTO(
        Long id,
        String eventName,
        int availableTickets
) {
}
