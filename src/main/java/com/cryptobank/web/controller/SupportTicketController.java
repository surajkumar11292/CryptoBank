package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.SupportService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.SupportTicketRequest;
import com.cryptobank.web.dto.response.SupportTicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
@Tag(name = "Support", description = "Customer support tickets and inquiries")
public class SupportTicketController {

    private final SupportService supportService;
    private final UserService userService;

    public SupportTicketController(SupportService supportService, UserService userService) {
        this.supportService = supportService;
        this.userService = userService;
    }

    private UserEntity me() {
        return userService.currentUser();
    }

    @PostMapping
    @Operation(summary = "Raise ticket", description = "Creates a new customer support ticket")
    public ResponseEntity<SupportTicketResponse> raise(@Valid @RequestBody SupportTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.raise(me(), req));
    }

    @GetMapping
    @Operation(summary = "List tickets", description = "Returns customer support tickets created by the authenticated user")
    public List<SupportTicketResponse> mine() {
        return supportService.mine(me());
    }
}
