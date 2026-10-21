package org.example.supportservice.repository;

import org.example.supportservice.model.Ticket;
import org.example.supportservice.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByAuthorId(Long authorId);

    List<Ticket> findByAssigneeId(Long assigneeId);

    List<Ticket> findByStatus(TicketStatus status);
}
