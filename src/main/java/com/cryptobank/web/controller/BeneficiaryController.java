package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.service.BeneficiaryService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.BeneficiaryRequest;
import com.cryptobank.web.dto.response.BeneficiaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@Tag(name = "Beneficiaries", description = "Saved transfer payees management")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserService userService;

    public BeneficiaryController(BeneficiaryService beneficiaryService, UserService userService) {
        this.beneficiaryService = beneficiaryService;
        this.userService = userService;
    }

    private UserEntity me() {
        return userService.currentUser();
    }

    @GetMapping
    @Operation(summary = "List beneficiaries", description = "Returns saved payees for the authenticated user")
    public List<BeneficiaryResponse> list() {
        return beneficiaryService.mine(me());
    }

    @PostMapping
    @Operation(summary = "Add beneficiary", description = "Saves a new payee linked to an existing account")
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody BeneficiaryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryService.add(me(), req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove beneficiary", description = "Deletes a saved payee")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        beneficiaryService.remove(me(), id);
        return ResponseEntity.noContent().build();
    }
}
