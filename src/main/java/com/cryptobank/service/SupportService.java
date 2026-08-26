package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.SupportTicketRequest;
import com.cryptobank.web.dto.response.AdminTicketResponse;
import com.cryptobank.web.dto.response.SupportTicketResponse;

import java.util.List;

public interface SupportService {
    SupportTicketResponse raise(UserEntity owner, SupportTicketRequest req);
    List<SupportTicketResponse> mine(UserEntity owner);
    List<AdminTicketResponse> allTickets();
    void close(Long ticketId);
}
