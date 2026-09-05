package com.cryptobank.config;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.domain.enums.UserRole;
import com.cryptobank.repository.UserRepository;
import com.cryptobank.service.*;
import com.cryptobank.web.dto.request.BeneficiaryRequest;
import com.cryptobank.web.dto.request.BillPaymentRequest;
import com.cryptobank.web.dto.request.OpenAccountRequest;
import com.cryptobank.web.dto.request.RegisterRequest;
import com.cryptobank.web.dto.request.SupportTicketRequest;
import com.cryptobank.web.dto.response.AccountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataSeederConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSeederConfig.class);

    @Bean
    CommandLineRunner seedDatabase(UserRepository users,
                                  UserService userService,
                                  AccountService accountService,
                                  BeneficiaryService beneficiaryService,
                                  BillPayService billPayService,
                                  SupportService supportService) {
        return args -> {
            if (users.count() > 0) {
                return;
            }

            log.info("Seeding initial demo data for CryptoBank...");

            // 1. Primary Demo Customer
            UserEntity demo = userService.register(new RegisterRequest(
                    "Demo User",
                    "demo@bank.app",
                    "demo12345",
                    "9876543210",
                    LocalDate.of(1996, 4, 12),
                    "ABCDE1234F",
                    "221B Residency Road, Bengaluru"
            ));

            AccountResponse primary = accountService.open(demo, new OpenAccountRequest(
                    "Demo User", new BigDecimal("50000.00"), "1234"));
            accountService.open(demo, new OpenAccountRequest(
                    "Demo Savings", new BigDecimal("12500.00"), "1234"));

            // 2. Secondary Customer (for transfer demonstration)
            UserEntity other = userService.register(new RegisterRequest(
                    "Priya Shah",
                    "priya@example.com",
                    "password123",
                    "9123456780",
                    LocalDate.of(1998, 9, 3),
                    "PQRSX5678K",
                    "44 Marine Drive, Mumbai"
            ));
            AccountResponse otherAccount = accountService.open(other, new OpenAccountRequest(
                    "Priya Shah", new BigDecimal("8000.00"), "5678"));

            // 3. Saved Beneficiary
            beneficiaryService.add(demo, new BeneficiaryRequest(
                    "Priya Shah", otherAccount.accountNumber(), "Priya"));

            // 4. Initial Bill Payment
            billPayService.pay(demo, new BillPaymentRequest(
                    primary.accountNumber(), "Electricity", "BESCOM · 4471029",
                    new BigDecimal("1180.00"), "1234"));

            // 5. Initial Support Ticket
            supportService.raise(demo, new SupportTicketRequest(
                    "Question about international transfers",
                    "Hi, does CryptoBank support transfers to overseas accounts? Thanks!"));

            // 6. Admin User
            UserEntity admin = userService.register(new RegisterRequest(
                    "Bank Admin",
                    "admin@bank.app",
                    "admin12345",
                    "9988776655",
                    LocalDate.of(1990, 1, 20),
                    "ADMIN1234Z",
                    "CryptoBank Headquarters, Financial District"
            ));
            admin.setRole(UserRole.ROLE_ADMIN);
            users.save(admin);

            log.info("Database seeding complete. Demo accounts ready.");
        };
    }
}
