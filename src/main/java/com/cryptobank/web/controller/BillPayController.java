package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.BillPayService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.BillPaymentRequest;
import com.cryptobank.web.dto.response.BillPaymentResponse;
import com.cryptobank.web.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billpay")
@Tag(name = "Bill Pay & Recharge", description = "Utility bill payments and transaction history")
public class BillPayController {

    private final BillPayService billPayService;
    private final UserService userService;

    public BillPayController(BillPayService billPayService, UserService userService) {
        this.billPayService = billPayService;
        this.userService = userService;
    }

    private UserEntity me() {
        return userService.currentUser();
    }

    @PostMapping
    @Operation(summary = "Pay utility bill", description = "Debits owned account and registers utility bill payment")
    public BillPaymentResponse pay(@Valid @RequestBody BillPaymentRequest req) {
        return billPayService.pay(me(), req);
    }

    @GetMapping("/history")
    @Operation(summary = "Bill payment history", description = "Returns paginated list of past bill payments")
    public PagedResponse<BillPaymentResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return billPayService.history(me(), page, size);
    }
}
