package org.example.supportservice.dto;

import org.example.supportservice.model.TicketMessage;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        Long ticketId,
        Long authorId,
        String text,
        LocalDateTime createdAt
) {

    public static MessageDto from(TicketMessage m) {
        return new MessageDto(m.getId(), m.getTicket().getId(), m.getAuthorId(), m.getText(), m.getCreatedAt());
    }
}
