package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.AccountService;
import com.cryptobank.service.SupportService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.response.AdminTicketResponse;
import com.cryptobank.web.dto.response.AdminUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Admin-only operations: user directory, account freezing, and support ticket triage")
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;
    private final SupportService supportService;

    public AdminController(UserService userService, AccountService accountService, SupportService supportService) {
        this.userService = userService;
        this.accountService = accountService;
        this.supportService = supportService;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users and accounts", description = "Admin overview of all registered customers and accounts")
    public List<AdminUserResponse> users() {
        return userService.allUsers().stream()
                .map((UserEntity u) -> AdminUserResponse.from(u, accountService.myAccounts(u)))
                .toList();
    }

    @PostMapping("/accounts/{accountNumber}/freeze")
    @Operation(summary = "Freeze account", description = "Places an administrative fraud hold on an account")
    public ResponseEntity<Void> freeze(@PathVariable Long accountNumber) {
        accountService.setFrozen(accountNumber, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{accountNumber}/unfreeze")
    @Operation(summary = "Unfreeze account", description = "Lifts an administrative fraud hold on an account")
    public ResponseEntity<Void> unfreeze(@PathVariable Long accountNumber) {
        accountService.setFrozen(accountNumber, false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tickets")
    @Operation(summary = "List all support tickets", description = "Returns all customer support tickets across the platform")
    public List<AdminTicketResponse> tickets() {
        return supportService.allTickets();
    }

    @PostMapping("/tickets/{id}/close")
    @Operation(summary = "Close support ticket", description = "Marks a customer inquiry ticket as closed/resolved")
    public ResponseEntity<Void> closeTicket(@PathVariable Long id) {
        supportService.close(id);
        return ResponseEntity.noContent().build();
    }
}
