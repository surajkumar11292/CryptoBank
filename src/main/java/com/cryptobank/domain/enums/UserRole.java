package com.cryptobank.domain.enums;

public enum UserRole {
    ROLE_CUSTOMER,
    ROLE_ADMIN;

    public String roleNameWithoutPrefix() {
        return name().replace("ROLE_", "");
    }
}
