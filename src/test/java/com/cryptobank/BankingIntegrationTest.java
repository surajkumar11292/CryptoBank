package com.cryptobank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankingIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    private MockHttpSession authSession(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "fullName", "Test User",
                        "email", email,
                        "password", "password123",
                        "phone", "9876500000",
                        "dateOfBirth", "1997-05-15",
                        "panNumber", "ABCDE1234F",
                        "address", "1 Test Street, Test City"))))
                .andExpect(status().isCreated());

        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) res.getRequest().getSession(false);
    }

    private long openAccount(MockHttpSession session, String balance, String pin) throws Exception {
        MvcResult r = mvc.perform(post("/api/accounts").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("holderName", "Test User", "openingBalance", balance, "pin", pin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", notNullValue()))
                .andReturn();

        JsonNode node = mapper.readTree(r.getResponse().getContentAsString());
        return node.get("accountNumber").asLong();
    }

    @Test
    void unauthenticated_access_is_rejected() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void register_login_and_openAccount() throws Exception {
        MockHttpSession s = authSession("user_a@test.com");
        openAccount(s, "1000.00", "4321");

        mvc.perform(get("/api/accounts").session(s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].balance", is(1000.00)));
    }

    @Test
    void transfer_moves_money_and_writes_ledger() throws Exception {
        MockHttpSession s = authSession("user_b@test.com");
        long from = openAccount(s, "1000.00", "1111");
        long to = openAccount(s, "0.00", "2222");

        mvc.perform(post("/api/accounts/transfer").session(s).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fromAccountNumber", from, "toAccountNumber", to,
                        "amount", "250.00", "pin", "1111"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(750.00)));

        mvc.perform(get("/api/accounts/" + to).session(s))
                .andExpect(jsonPath("$.balance", is(250.00)));

        mvc.perform(get("/api/accounts/" + from + "/history").session(s))
                .andExpect(jsonPath("$.content[0].type", is("DEBIT")))
                .andExpect(jsonPath("$.content[0].amount", is(250.00)));
    }

    @Test
    void transfer_with_insufficient_balance_is_rejected() throws Exception {
        MockHttpSession s = authSession("user_c@test.com");
        long from = openAccount(s, "100.00", "1111");
        long to = openAccount(s, "0.00", "2222");

        mvc.perform(post("/api/accounts/transfer").session(s).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fromAccountNumber", from, "toAccountNumber", to,
                        "amount", "500.00", "pin", "1111"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_with_wrong_pin_is_rejected() throws Exception {
        MockHttpSession s = authSession("user_d@test.com");
        long from = openAccount(s, "1000.00", "1111");
        long to = openAccount(s, "0.00", "2222");

        mvc.perform(post("/api/accounts/transfer").session(s).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fromAccountNumber", from, "toAccountNumber", to,
                        "amount", "100.00", "pin", "9999"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cross_user_access_is_forbidden() throws Exception {
        MockHttpSession s1 = authSession("user_e1@test.com");
        MockHttpSession s2 = authSession("user_e2@test.com");

        long acct1 = openAccount(s1, "500.00", "1234");

        mvc.perform(get("/api/accounts/" + acct1).session(s2))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_endpoint_requires_admin_role() throws Exception {
        MockHttpSession customerSession = authSession("user_cust@test.com");

        mvc.perform(get("/api/admin/users").session(customerSession))
                .andExpect(status().isForbidden());

        // Admin login
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "admin@bank.app", "password", "admin12345"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) res.getRequest().getSession(false);

        mvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk());
    }
}
