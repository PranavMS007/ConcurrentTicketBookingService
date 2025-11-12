package com.booking.ticketservice.controller;

import com.booking.ticketservice.dto.EventDTO;
import com.booking.ticketservice.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private static final Logger log = LoggerFactory.getLogger(TicketController.class);
    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Returns a list of Events.
     * This endpoint was not explicitly in the backend requirements,
     * but it is implied and necessary for the frontend's "Event Listing Component"
     * which needs to "Display all events in a table".
     */
    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        log.info("GET /tickets - Request to fetch all events");
        List<EventDTO> events = ticketService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Returns a single Event.
     * GET /tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventDetails(@PathVariable Long id) {
        log.info("GET /tickets/{} - Request for event details", id);
        EventDTO event = ticketService.getEventDetails(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Endpoint to book tickets.
     * POST /tickets/{id}/book?count=N
     */
    @PostMapping("/{id}/book")
    public ResponseEntity<?> bookTickets(@PathVariable Long id, @RequestParam int count) {

        log.info("POST /tickets/{}/book?count={} - Request to book tickets", id, count);

        try {
            ticketService.bookTickets(id, count);
            Map<String, String> response = Map.of(
                    "message", "Successfully booked " + count + " tickets for event ID " + id
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
