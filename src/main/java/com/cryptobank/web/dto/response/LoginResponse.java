package com.cryptobank.web.dto.response;

public record LoginResponse(
        boolean mfaRequired,
        String fullName,
        UserResponse user
) {
    public static LoginResponse mfaRequired(String fullName) {
        return new LoginResponse(true, fullName, null);
    }

    public static LoginResponse authenticated(UserResponse user) {
        return new LoginResponse(false, null, user);
    }
}
