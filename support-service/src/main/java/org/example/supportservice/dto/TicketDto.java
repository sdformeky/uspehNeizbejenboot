package org.example.supportservice.dto;

import org.example.supportservice.model.Ticket;
import org.example.supportservice.model.TicketCategory;
import org.example.supportservice.model.TicketPriority;
import org.example.supportservice.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketDto(
        Long id,
        Long authorId,
        Long assigneeId,
        String subject,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {

    public static TicketDto from(Ticket t) {
        return new TicketDto(t.getId(), t.getAuthorId(), t.getAssigneeId(), t.getSubject(), t.getCategory(),
                t.getPriority(), t.getStatus(), t.getCreatedAt(), t.getClosedAt());
    }
}
