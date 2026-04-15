package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Ticket;
import com.smartcampus.backend.enums.TicketCategory;
import com.smartcampus.backend.enums.TicketPriority;
import com.smartcampus.backend.enums.TicketStatus;
import com.smartcampus.backend.request.CreateTicketRequest;
import com.smartcampus.backend.request.UpdateTicketRequest;
import com.smartcampus.backend.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                               @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        Ticket ticket = ticketService.createTicket(request, userId);
        return new ResponseEntity<>(ticket, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) Long assignedToId) {
        return ResponseEntity.ok(ticketService.getTicketsWithFilters(status, category, priority, createdById, assignedToId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }


    @GetMapping("/my-tickets")
    public ResponseEntity<List<Ticket>> getMyTickets(@AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id,
                                               @RequestBody UpdateTicketRequest request,
                                               @AuthenticationPrincipal OAuth2User principal) {
        Long updaterId = extractUserId(principal);
        return ResponseEntity.ok(ticketService.updateTicket(id, request, updaterId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id,
                                          @AuthenticationPrincipal OAuth2User principal) {
        Long userId = extractUserId(principal);
        ticketService.deleteTicket(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(OAuth2User principal) {
        if (principal == null) {
            return 1L; // temporary test user id
        }
        return principal.getAttribute("id");
    }
}
