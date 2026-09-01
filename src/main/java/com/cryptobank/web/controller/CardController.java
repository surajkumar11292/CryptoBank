package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.CardService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.CardUpdateRequest;
import com.cryptobank.web.dto.response.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Virtual debit card management and security controls")
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    public CardController(CardService cardService, UserService userService) {
        this.cardService = cardService;
        this.userService = userService;
    }

    private UserEntity me() {
        return userService.currentUser();
    }

    @GetMapping
    @Operation(summary = "List cards", description = "Returns virtual debit cards for all owned accounts")
    public List<CardResponse> mine() {
        return cardService.mine(me());
    }

    @PatchMapping("/{accountNumber}")
    @Operation(summary = "Update card controls", description = "Toggles freeze status, contactless NFC, or online payments")
    public CardResponse update(@PathVariable Long accountNumber, @RequestBody CardUpdateRequest req) {
        return cardService.update(me(), accountNumber, req);
    }

    @PostMapping("/{accountNumber}/request-replacement")
    @Operation(summary = "Request card replacement", description = "Submits a physical card replacement request")
    public CardResponse requestReplacement(@PathVariable Long accountNumber) {
        return cardService.requestReplacement(me(), accountNumber);
    }
}
