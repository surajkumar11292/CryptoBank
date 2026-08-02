package com.cryptobank.util;

import com.cryptobank.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Long generate() {
        long number;
        do {
            number = 10_000_000L + (long) (RANDOM.nextDouble() * 89_999_999L);
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
