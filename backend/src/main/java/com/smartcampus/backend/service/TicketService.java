package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Resource;
import com.smartcampus.backend.entity.Ticket;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.entity.Attachment;
import com.smartcampus.backend.enums.NotificationType;
import com.smartcampus.backend.enums.TicketCategory;
import com.smartcampus.backend.enums.TicketPriority;
import com.smartcampus.backend.enums.TicketStatus;
import com.smartcampus.backend.exception.BadRequestException;
import com.smartcampus.backend.exception.ResourceNotFoundException;
import com.smartcampus.backend.repository.ResourceRepository;
import com.smartcampus.backend.repository.TicketRepository;
import com.smartcampus.backend.repository.UserRepository;
import com.smartcampus.backend.request.CreateTicketRequest;
import com.smartcampus.backend.request.UpdateTicketRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, List<MultipartFile> files, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setCreatedBy(user);
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setDescription(request.getDescription());
        ticket.setLocation(request.getLocation());
        ticket.setPreferredContactName(request.getPreferredContactName());
        ticket.setPreferredContactEmail(request.getPreferredContactEmail());
        ticket.setPreferredContactPhone(request.getPreferredContactPhone());
        ticket.setStatus(TicketStatus.OPEN);

        if (request.getResourceId() != null) {
            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
            ticket.setResource(resource);
        }

        if (files != null && !files.isEmpty()) {
            String uploadDir = "uploads/tickets/";
            try {
                Files.createDirectories(Paths.get(uploadDir));
                for (MultipartFile file : files) {
                    if (file.isEmpty()) continue;
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path filePath = Paths.get(uploadDir + fileName);
                    Files.write(filePath, file.getBytes());

                    Attachment attachment = new Attachment();
                    attachment.setTicket(ticket);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFilePath(filePath.toString());
                    attachment.setContentType(file.getContentType());
                    attachment.setFileSize(file.getSize());
                    ticket.getAttachments().add(attachment);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not store files locally", e);
            }
        }

        return ticketRepository.save(ticket);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
    }

    public List<Ticket> getTicketsWithFilters(TicketStatus status, TicketCategory category,
                                               TicketPriority priority, Long createdById, Long assignedToId) {
        return ticketRepository.findTicketsWithFilters(status, category, priority, createdById, assignedToId);
    }

    @Transactional
    public Ticket updateTicket(Long id, UpdateTicketRequest request, Long updaterId) {
        Ticket ticket = getTicketById(id);
        TicketStatus oldStatus = ticket.getStatus();

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }

        if (request.getAssignedToId() != null) {
            User assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            ticket.setAssignedTo(assignedTo);

            notificationService.createNotification(
                    assignedTo.getId(),
                    "Ticket Assigned",
                    "You have been assigned to ticket: " + ticket.getTicketCode(),
                    NotificationType.TICKET_ASSIGNED,
                    null,
                    ticket.getId()
            );
        }

        if (request.getResolutionNotes() != null) {
            ticket.setResolutionNotes(request.getResolutionNotes());
        }

        if (request.getRejectionReason() != null) {
            ticket.setRejectionReason(request.getRejectionReason());
        }

        Ticket updated = ticketRepository.save(ticket);

        if (oldStatus != ticket.getStatus()) {
            notificationService.createNotification(
                    ticket.getCreatedBy().getId(),
                    "Ticket Status Updated",
                    "Your ticket '" + ticket.getTicketCode() + "' status changed from " + oldStatus + " to " + ticket.getStatus(),
                    NotificationType.TICKET_STATUS_CHANGED,
                    null,
                    ticket.getId()
            );
        }

        return updated;
    }

    @Transactional
    public void deleteTicket(Long id, Long userId) {
        Ticket ticket = getTicketById(id);
        User user = userRepository.findById(userId).orElseThrow();
        
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own tickets");
        }

        ticketRepository.delete(ticket);
    }
}