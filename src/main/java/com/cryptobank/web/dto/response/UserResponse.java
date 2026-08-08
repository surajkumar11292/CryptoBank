package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.UserEntity;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String panNumber,
        String address,
        String role,
        boolean totpEnabled
) {
    public static UserResponse from(UserEntity u) {
        return new UserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getDateOfBirth(),
                u.getPanNumber(),
                u.getAddress(),
                u.getRole().roleNameWithoutPrefix(),
                u.isTotpEnabled()
        );
    }
}
