package org.example.supportservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.supportservice.dto.MessageDto;
import org.example.supportservice.dto.MessageRequest;
import org.example.supportservice.dto.TicketDto;
import org.example.supportservice.dto.TicketRequest;
import org.example.supportservice.exception.ConflictException;
import org.example.supportservice.exception.NotFoundException;
import org.example.supportservice.model.FaqArticle;
import org.example.supportservice.model.Ticket;
import org.example.supportservice.model.TicketCategory;
import org.example.supportservice.model.TicketMessage;
import org.example.supportservice.model.TicketPriority;
import org.example.supportservice.model.TicketStatus;
import org.example.supportservice.repository.FaqArticleRepository;
import org.example.supportservice.repository.TicketMessageRepository;
import org.example.supportservice.repository.TicketRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private final FaqArticleRepository faqRepository;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

    public List<FaqArticle> findFaq(TicketCategory category) {
        if (category != null) {
            return faqRepository.findByCategory(category);
        }
        return faqRepository.findAll(Sort.by("id"));
    }

    public List<TicketDto> findTickets(Long authorId) {
        List<Ticket> tickets;
        if (authorId != null) {
            tickets = ticketRepository.findByAuthorId(authorId);
        } else {
            tickets = ticketRepository.findAll(Sort.by("id"));
        }
        return tickets.stream().map(TicketDto::from).toList();
    }

    public TicketDto findTicket(Long id) {
        return TicketDto.from(getTicket(id));
    }

    @Transactional
    public TicketDto createTicket(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setAuthorId(request.authorId());
        ticket.setSubject(request.subject().trim());
        ticket.setCategory(request.category() != null ? request.category() : TicketCategory.OTHER);
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
        ticketRepository.save(ticket);

        // текст обращения — первое сообщение в переписке
        addMessage(ticket, request.authorId(), request.text());
        log.info("Новое обращение {} от пользователя {}: {}", ticket.getId(), ticket.getAuthorId(), ticket.getSubject());
        return TicketDto.from(ticket);
    }

    public List<MessageDto> findMessages(Long ticketId) {
        getTicket(ticketId);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .filter(m -> !m.isInternal())   // внутренние заметки поддержки пользователю не показываем
                .map(MessageDto::from)
                .toList();
    }

    @Transactional
    public MessageDto addMessage(Long ticketId, MessageRequest request) {
        Ticket ticket = getTicket(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Обращение " + ticketId + " закрыто, писать в него нельзя");
        }
        // ответил не автор — значит, поддержка взяла обращение в работу
        if (!ticket.getAuthorId().equals(request.authorId()) && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setAssigneeId(request.authorId());
        }
        return MessageDto.from(addMessage(ticket, request.authorId(), request.text()));
    }

    @Transactional
    public TicketDto changeStatus(Long ticketId, TicketStatus status) {
        Ticket ticket = getTicket(ticketId);
        ticket.setStatus(status);
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        } else {
            ticket.setClosedAt(null);
        }
        log.info("Обращение {}: статус {}", ticketId, status);
        return TicketDto.from(ticket);
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Обращение с id " + id + " не найдено"));
    }

    private TicketMessage addMessage(Ticket ticket, Long authorId, String text) {
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthorId(authorId);
        message.setText(text.trim());
        return messageRepository.save(message);
    }
}
