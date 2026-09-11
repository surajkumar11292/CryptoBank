package com.cryptobank;

import com.cryptobank.domain.entity.AccountEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.domain.enums.UserRole;
import com.cryptobank.repository.AccountRepository;
import com.cryptobank.repository.UserRepository;
import com.cryptobank.web.dto.request.AiChatRequest;
import com.cryptobank.web.dto.request.LoginRequest;
import com.cryptobank.web.dto.request.TransferRiskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class AiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder encoder;

    private MockHttpSession session;
    private Long testAccountNumber;

    @BeforeEach
    void setup() throws Exception {
        UserEntity user = userRepository.findByEmailIgnoreCase("ai-tester@bank.app").orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setFullName("AI Tester");
            u.setEmail("ai-tester@bank.app");
            u.setPasswordHash(encoder.encode("Password123!"));
            u.setRole(UserRole.ROLE_CUSTOMER);
            u.setDateOfBirth(LocalDate.of(1995, 5, 20));
            u.setPanNumber("ABCDE1234F");
            u.setPhone("+919876543210");
            u.setAddress("Mumbai, Maharashtra");
            return userRepository.save(u);
        });

        AccountEntity account = accountRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream().findFirst().orElseGet(() -> {
                    AccountEntity a = new AccountEntity();
                    a.setOwner(user);
                    a.setHolderName(user.getFullName());
                    a.setBalance(new BigDecimal("50000.00"));
                    a.setPinHash(encoder.encode("1234"));
                    a.setAccountNumber(99887766L);
                    return accountRepository.save(a);
                });
        testAccountNumber = account.getAccountNumber();

        // Perform login to establish authenticated session
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("ai-tester@bank.app", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    @DisplayName("AI Chat endpoint should return intelligent financial advisory response")
    void testAiChatEndpoint() throws Exception {
        AiChatRequest req = new AiChatRequest("Can I afford to purchase a smartphone for ₹15,000?", testAccountNumber);

        mvc.perform(post("/api/ai/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isString())
                .andExpect(jsonPath("$.engine").isString())
                .andExpect(jsonPath("$.quickFollowUps").isArray());
    }

    @Test
    @DisplayName("AI Insights endpoint should return financial health score and budget analysis")
    void testAiInsightsEndpoint() throws Exception {
        mvc.perform(get("/api/ai/insights")
                        .session(session)
                        .param("accountNumber", testAccountNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore").isNumber())
                .andExpect(jsonPath("$.healthGrade").isString())
                .andExpect(jsonPath("$.bulletInsights").isArray())
                .andExpect(jsonPath("$.summary").isString());
    }

    @Test
    @DisplayName("AI Transfer Risk assessment should evaluate anomaly and risk score")
    void testAiTransferRiskAssessment() throws Exception {
        TransferRiskRequest req = new TransferRiskRequest(testAccountNumber, 10002842L, new BigDecimal("75000.00"));

        mvc.perform(post("/api/ai/assess-transfer-risk")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").isString())
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.analysis").isString())
                .andExpect(jsonPath("$.recommendation").isString());
    }
}
