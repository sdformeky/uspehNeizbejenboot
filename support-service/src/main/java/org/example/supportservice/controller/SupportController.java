package org.example.supportservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.supportservice.dto.MessageDto;
import org.example.supportservice.dto.MessageRequest;
import org.example.supportservice.dto.StatusRequest;
import org.example.supportservice.dto.TicketDto;
import org.example.supportservice.dto.TicketRequest;
import org.example.supportservice.model.FaqArticle;
import org.example.supportservice.model.TicketCategory;
import org.example.supportservice.service.SupportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
@Tag(name = "Техподдержка")
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/faq")
    @Operation(summary = "Частые вопросы", description = "Все или одной категории (?category=PAYMENT)")
    public List<FaqArticle> getFaq(@RequestParam(required = false) TicketCategory category) {
        return supportService.findFaq(category);
    }

    @GetMapping("/tickets")
    @Operation(summary = "Обращения", description = "Все или одного пользователя (?authorId=)")
    public List<TicketDto> getTickets(@Parameter(example = "1") @RequestParam(required = false) Long authorId) {
        return supportService.findTickets(authorId);
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Обращение по id")
    public TicketDto getTicket(@PathVariable Long id,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return supportService.findTicket(id, currentUserId, roles);
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать обращение", description = "Текст обращения сохраняется первым сообщением")
    public TicketDto createTicket(@Valid @RequestBody TicketRequest request,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return supportService.createTicket(request, currentUserId, roles);
    }

    @GetMapping("/tickets/{id}/messages")
    @Operation(summary = "Переписка по обращению")
    public List<MessageDto> getMessages(@PathVariable Long id,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return supportService.findMessages(id, currentUserId, roles);
    }

    @PostMapping("/tickets/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Написать в обращение", description = "409 — обращение закрыто")
    public MessageDto addMessage(@PathVariable Long id, @Valid @RequestBody MessageRequest request,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return supportService.addMessage(id, request, currentUserId, roles);
    }

    @PatchMapping("/tickets/{id}/status")
    @Operation(summary = "Сменить статус обращения")
    public TicketDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return supportService.changeStatus(id, request.status(), currentUserId, roles);
    }
}
