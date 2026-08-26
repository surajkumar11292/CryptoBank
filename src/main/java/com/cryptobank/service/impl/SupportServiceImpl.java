package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.SupportTicketEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.domain.enums.TicketStatus;
import com.cryptobank.repository.SupportTicketRepository;
import com.cryptobank.service.SupportService;
import com.cryptobank.web.dto.request.SupportTicketRequest;
import com.cryptobank.web.dto.response.AdminTicketResponse;
import com.cryptobank.web.dto.response.SupportTicketResponse;
import com.cryptobank.web.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportServiceImpl implements SupportService {

    private final SupportTicketRepository tickets;

    public SupportServiceImpl(SupportTicketRepository tickets) {
        this.tickets = tickets;
    }

    @Override
    @Transactional
    public SupportTicketResponse raise(UserEntity owner, SupportTicketRequest req) {
        SupportTicketEntity t = new SupportTicketEntity();
        t.setOwner(owner);
        t.setSubject(req.subject());
        t.setMessage(req.message());
        t.setStatus(TicketStatus.OPEN);
        return SupportTicketResponse.from(tickets.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> mine(UserEntity owner) {
        return tickets.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(SupportTicketResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminTicketResponse> allTickets() {
        return tickets.findAllByOrderByCreatedAtDesc().stream().map(AdminTicketResponse::from).toList();
    }

    @Override
    @Transactional
    public void close(Long ticketId) {
        SupportTicketEntity t = tickets.findById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Ticket #" + ticketId + " not found"));
        t.setStatus(TicketStatus.CLOSED);
    }
}
