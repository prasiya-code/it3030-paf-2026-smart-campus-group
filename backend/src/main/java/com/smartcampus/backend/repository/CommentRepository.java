package com.smartcampus.backend.repository;

import com.smartcampus.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<Comment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
