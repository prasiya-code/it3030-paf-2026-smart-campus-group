package com.smartcampus.backend.repository;

import com.smartcampus.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentReportingRepository extends JpaRepository<Ticket, Long> {
}
