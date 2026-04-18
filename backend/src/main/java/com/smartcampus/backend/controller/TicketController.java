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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartcampus.backend.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Ticket> createTicket(
            @ModelAttribute @Valid CreateTicketRequest request,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        Ticket ticket = ticketService.createTicket(request, attachments, userId);
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
    public ResponseEntity<List<Ticket>> getMyTickets(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id,
                                               @RequestBody UpdateTicketRequest request,
                                               Authentication authentication) {
        Long updaterId = extractUserId(authentication);
        return ResponseEntity.ok(ticketService.updateTicket(id, request, updaterId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id,
                                             Authentication authentication) {
        Long userId = extractUserId(authentication);
        ticketService.deleteTicket(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return 1L; // fallback for unauthenticated (should be blocked by security)
        }
        
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("id");
        }
        
        // Manual login uses email as username
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(com.smartcampus.backend.entity.User::getId)
                .orElse(1L);
    }
}
