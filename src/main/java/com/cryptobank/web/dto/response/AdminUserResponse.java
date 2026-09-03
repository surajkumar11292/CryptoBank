package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.UserEntity;

import java.time.Instant;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        Instant createdAt,
        List<AccountResponse> accounts
) {
    public static AdminUserResponse from(UserEntity u, List<AccountResponse> accounts) {
        return new AdminUserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().roleNameWithoutPrefix(),
                u.getCreatedAt(),
                accounts
        );
    }
}
