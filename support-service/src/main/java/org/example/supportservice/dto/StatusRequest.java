package org.example.supportservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.example.supportservice.model.TicketStatus;

public record StatusRequest(
        @Schema(example = "RESOLVED")
        @NotNull(message = "Статус обязателен")
        TicketStatus status
) {
}
