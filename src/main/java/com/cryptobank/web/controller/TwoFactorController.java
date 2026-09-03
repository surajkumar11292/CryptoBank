package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.repository.UserRepository;
import com.cryptobank.security.TotpService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.TotpCodeRequest;
import com.cryptobank.web.dto.request.TotpDisableRequest;
import com.cryptobank.web.dto.response.TotpSetupResponse;
import com.cryptobank.web.dto.response.TotpStatusResponse;
import com.cryptobank.web.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/2fa")
@Tag(name = "Two-Factor Authentication", description = "TOTP MFA setup, verification, and deactivation")
public class TwoFactorController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final PasswordEncoder encoder;

    public TwoFactorController(UserService userService, UserRepository userRepository,
                               TotpService totpService, PasswordEncoder encoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.encoder = encoder;
    }

    @GetMapping("/status")
    @Operation(summary = "2FA status", description = "Checks whether TOTP 2FA is active on the authenticated account")
    public TotpStatusResponse status() {
        return new TotpStatusResponse(userService.currentUser().isTotpEnabled());
    }

    @PostMapping("/setup")
    @Transactional
    @Operation(summary = "Initiate 2FA setup", description = "Generates a fresh secret and otpauth URI for authenticator registration")
    public TotpSetupResponse setup() {
        UserEntity user = userService.currentUser();
        if (user.isTotpEnabled()) {
            throw ApiException.badRequest("2FA is already enabled — disable it first to reset the key");
        }
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);
        return new TotpSetupResponse(secret, totpService.otpAuthUri(secret, user.getEmail()));
    }

    @PostMapping("/enable")
    @Transactional
    @Operation(summary = "Confirm & activate 2FA", description = "Validates the authenticator code and activates 2FA on the account")
    public void enable(@Valid @RequestBody TotpCodeRequest req) {
        UserEntity user = userService.currentUser();
        if (user.getTotpSecret() == null) {
            throw ApiException.badRequest("Start setup first");
        }
        if (!totpService.verify(user.getTotpSecret(), req.code())) {
            throw ApiException.totpInvalid("Incorrect code — check your authenticator app and try again");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @PostMapping("/disable")
    @Transactional
    @Operation(summary = "Disable 2FA", description = "Requires current password and valid TOTP code to disable 2FA")
    public void disable(@Valid @RequestBody TotpDisableRequest req) {
        UserEntity user = userService.currentUser();
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.forbidden("Current password is incorrect");
        }
        if (!totpService.verify(user.getTotpSecret(), req.code())) {
            throw ApiException.totpInvalid("Incorrect code");
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }
}
