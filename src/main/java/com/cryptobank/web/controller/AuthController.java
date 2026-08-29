package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.security.TotpService;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.ChangePasswordRequest;
import com.cryptobank.web.dto.request.LoginRequest;
import com.cryptobank.web.dto.request.RegisterRequest;
import com.cryptobank.web.dto.request.TotpCodeRequest;
import com.cryptobank.web.dto.response.LoginResponse;
import com.cryptobank.web.dto.response.UserResponse;
import com.cryptobank.web.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, session login, MFA, profile, and credentials")
public class AuthController {

    private static final String SESSION_MFA_USER_ID = "MFA_PENDING_USER_ID";
    private static final String SESSION_MFA_ATTEMPTS = "MFA_ATTEMPTS";
    private static final int MAX_MFA_ATTEMPTS = 5;

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final TotpService totpService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService, AuthenticationManager authManager, TotpService totpService) {
        this.userService = userService;
        this.authManager = authManager;
        this.totpService = totpService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new customer account with verified KYC details")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.register(req)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login to user session", description = "Validates credentials and establishes authenticated session or requests MFA code")
    public LoginResponse login(@Valid @RequestBody LoginRequest req,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        UserEntity user = userService.byEmail(req.email());

        if (user.isTotpEnabled()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_MFA_USER_ID, user.getId());
            session.setAttribute(SESSION_MFA_ATTEMPTS, 0);
            return LoginResponse.mfaRequired(user.getFullName());
        }

        establishSession(user, request, response);
        return LoginResponse.authenticated(UserResponse.from(user));
    }

    @PostMapping("/login/2fa")
    @Operation(summary = "Verify MFA code", description = "Completes login with 6-digit TOTP code if MFA is required")
    public LoginResponse verifyLoginCode(@Valid @RequestBody TotpCodeRequest req,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(SESSION_MFA_USER_ID);
        if (userId == null) {
            throw ApiException.forbidden("No sign-in in progress -- please sign in again");
        }

        UserEntity user = userService.byId(userId);
        if (!totpService.verify(user.getTotpSecret(), req.code())) {
            int attempts = 1 + (int) session.getAttribute(SESSION_MFA_ATTEMPTS);
            if (attempts >= MAX_MFA_ATTEMPTS) {
                session.invalidate();
                throw ApiException.forbidden("Too many incorrect codes -- please sign in again");
            }
            session.setAttribute(SESSION_MFA_ATTEMPTS, attempts);
            throw ApiException.totpInvalid("Incorrect code -- " + (MAX_MFA_ATTEMPTS - attempts) + " attempts left");
        }

        session.removeAttribute(SESSION_MFA_USER_ID);
        session.removeAttribute(SESSION_MFA_ATTEMPTS);
        establishSession(user, request, response);
        return LoginResponse.authenticated(UserResponse.from(user));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public UserResponse me() {
        return UserResponse.from(userService.currentUser());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Updates user password after verifying current password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(userService.currentUser(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    private void establishSession(UserEntity user, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
