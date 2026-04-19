package com.smartcampus.backend.service;

import com.smartcampus.backend.entity.Comment;
import com.smartcampus.backend.entity.Ticket;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.enums.NotificationType;
import com.smartcampus.backend.exception.BadRequestException;
import com.smartcampus.backend.exception.ResourceNotFoundException;
import com.smartcampus.backend.repository.CommentRepository;
import com.smartcampus.backend.repository.TicketRepository;
import com.smartcampus.backend.repository.UserRepository;
import com.smartcampus.backend.request.CreateCommentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Comment createComment(CreateCommentRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setContent(request.getContent());

        Comment saved = commentRepository.save(comment);

        // Notify ticket creator if comment is from someone else
        if (!ticket.getCreatedBy().getId().equals(userId)) {
            try {
                notificationService.createNotification(
                        ticket.getCreatedBy().getId(),
                        "New Comment on Your Ticket",
                        user.getFirstName() + " commented on your ticket: " + ticket.getTicketCode(),
                        NotificationType.TICKET_COMMENT_ADDED,
                        null,
                        ticket.getId()
                );
                System.out.println("Notification created for comment to ticket creator: " + ticket.getId());
            } catch (Exception e) {
                System.err.println("Failed to create notification for comment to ticket creator: " + ticket.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // If assigned technician exists and is not the commenter, notify them too
        if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().getId().equals(userId)) {
            try {
                notificationService.createNotification(
                        ticket.getAssignedTo().getId(),
                        "New Comment on Assigned Ticket",
                        user.getFirstName() + " commented on ticket: " + ticket.getTicketCode(),
                        NotificationType.TICKET_COMMENT_ADDED,
                        null,
                        ticket.getId()
                );
                System.out.println("Notification created for comment to assigned user: " + ticket.getId());
            } catch (Exception e) {
                System.err.println("Failed to create notification for comment to assigned user: " + ticket.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return saved;
    }

    public List<Comment> getCommentsByTicket(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public Comment updateComment(Long commentId, String newContent, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only edit your own comments");
        }

        comment.setContent(newContent);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getId().equals(userId) && !isAdmin) {
            throw new BadRequestException("You can only delete your own comments or must be an admin");
        }

        commentRepository.delete(comment);
    }
}
