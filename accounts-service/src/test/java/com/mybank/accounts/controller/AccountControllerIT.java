package com.mybank.accounts.controller;

import tools.jackson.databind.ObjectMapper;
import com.mybank.accounts.config.TestSecurityItConfig;
import com.mybank.accounts.dto.*;
import com.mybank.accounts.outbox.OutboxProcessor;
import com.mybank.accounts.template.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"
})
@AutoConfigureMockMvc
@Import(TestSecurityItConfig.class)
class AccountControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OutboxProcessor outboxProcessor;

    // Пользовательские эндпоинты OAUTH2
    // GET /accounts/me ---
    // Валидный запрос

    @Test
    void getMe_withAccountsRead_shouldReturn200() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        mockMvc.perform(get("/accounts/me").with(auth))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    // Нет прав на чтение
    @Test
    void getMe_withoutAccountsRead_shouldReturn403() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.write"));

        mockMvc.perform(get("/accounts/me").with(auth))
                .andExpect(status().isForbidden());
    }

    // Нет прав совсем
    @Test
    void getMe_withNoAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    // --- PUT /accounts/me ---
    // Успешно
    @Test
    void updateMe_withAccountsWrite_shouldReturn204() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.write"));

        var request = new AccountUpdateRequest(null, "Alice", "Johnson", LocalDate.of(1990, 1, 1));

        mockMvc.perform(put("/accounts/me")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // Нет прав на запись
    @Test
    void updateMe_withAccountsReadOnly_shouldReturn403() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        var request = new AccountUpdateRequest(null, "Alice", "Johnson", LocalDate.of(1990, 1, 1));

        mockMvc.perform(put("/accounts/me")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- GET /accounts/all ---
    // Получение списка пользователей
    @Test
    void getAll_withAccountsRead_shouldReturn200() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        mockMvc.perform(get("/accounts/all").with(auth))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Сервисные эндпоинты (Client Credentials)
    // --- POST /accounts/balance (balance.write) ---

    @Test
    void balance_deposit_shouldReturn204() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("client_id", "cash-service")
        ).authorities(new SimpleGrantedAuthority("ROLE_balance.write"));

        var request = new BalanceUpdateRequest("alice", new BigDecimal("100.00"), CashOperationType.DEPOSIT, 9001L);

        mockMvc.perform(post("/accounts/balance")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    //POST /balance × 2 с одним operationId → баланс увеличивается ровно на 50, а не на 100
    @Test
    void balance_twice_sameOperationId_shouldBeIdempotent() throws Exception {
        var serviceAuth = jwt().jwt(j -> j
                .claim("client_id", "cash-service")
        ).authorities(new SimpleGrantedAuthority("ROLE_balance.write"));

        var userAuth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        // Запоминаем баланс до операции
        String before = mockMvc.perform(get("/accounts/me").with(userAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal balanceBefore = new BigDecimal(objectMapper.readTree(before).get("balance").asText());

        var request = new BalanceUpdateRequest("alice", new BigDecimal("50.00"), CashOperationType.DEPOSIT, 9002L);

        // 1️⃣ Первый вызов — применяет
        mockMvc.perform(post("/accounts/balance")
                        .with(serviceAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // 2️⃣ Повторный — идемпотентно
        mockMvc.perform(post("/accounts/balance")
                        .with(serviceAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // ✅ Баланс увеличился ровно на 50, не на 100
        String after = mockMvc.perform(get("/accounts/me").with(userAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal balanceAfter = new BigDecimal(objectMapper.readTree(after).get("balance").asText());

        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.add(new BigDecimal("50.00")));
    }

    @Test
    void balance_withUserRole_shouldReturn403() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.write"));

        var request = new BalanceUpdateRequest("alice", new BigDecimal("100.00"), CashOperationType.DEPOSIT, 9003L);

        mockMvc.perform(post("/accounts/balance")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- POST /accounts/transfer (balance.transfer) ---

    @Test
    void transfer_shouldReturn204() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("client_id", "transfer-service")
        ).authorities(new SimpleGrantedAuthority("ROLE_balance.transfer"));

        var request = new TransferConsumeRequest(9010L, "alice", "bob", new BigDecimal("10.00"));

        mockMvc.perform(post("/accounts/transfer")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    //POST /transfer × 2 с одним operationId → alice -10, bob +10 (не ×2), операция проводится единыжды
    @Test
    void transfer_twice_sameOperationId_shouldBeIdempotent() throws Exception {
        var serviceAuth = jwt().jwt(j -> j
                .claim("client_id", "transfer-service")
        ).authorities(new SimpleGrantedAuthority("ROLE_balance.transfer"));

        var aliceAuth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        var bobAuth = jwt().jwt(j -> j
                .claim("preferred_username", "bob")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        // Запоминаем балансы до операции
        String aliceBefore = mockMvc.perform(get("/accounts/me").with(aliceAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal aliceBalanceBefore = new BigDecimal(objectMapper.readTree(aliceBefore).get("balance").asText());

        String bobBefore = mockMvc.perform(get("/accounts/me").with(bobAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal bobBalanceBefore = new BigDecimal(objectMapper.readTree(bobBefore).get("balance").asText());

        var request = new TransferConsumeRequest(9011L, "alice", "bob", new BigDecimal("10.00"));

        // 1️⃣ Первый вызов — выполняет перевод
        mockMvc.perform(post("/accounts/transfer")
                        .with(serviceAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // 2️⃣ Повторный — идемпотентно
        mockMvc.perform(post("/accounts/transfer")
                        .with(serviceAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // ✅ У alice списалось ровно 10, не 20
        String aliceAfter = mockMvc.perform(get("/accounts/me").with(aliceAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal aliceBalanceAfter = new BigDecimal(objectMapper.readTree(aliceAfter).get("balance").asText());
        assertThat(aliceBalanceAfter).isEqualByComparingTo(aliceBalanceBefore.subtract(new BigDecimal("10.00")));

        // ✅ У bob зачислилось ровно 10, не 20
        String bobAfter = mockMvc.perform(get("/accounts/me").with(bobAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal bobBalanceAfter = new BigDecimal(objectMapper.readTree(bobAfter).get("balance").asText());
        assertThat(bobBalanceAfter).isEqualByComparingTo(bobBalanceBefore.add(new BigDecimal("10.00")));
    }

    @Test
    void transfer_withUserRole_shouldReturn403() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
        ).authorities(new SimpleGrantedAuthority("ROLE_accounts.read"));

        var request = new TransferConsumeRequest(9012L, "alice", "bob", new BigDecimal("10.00"));

        mockMvc.perform(post("/accounts/transfer")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_withBalanceWrite_shouldReturn403() throws Exception {
        // balance.write НЕ даёт доступ к /transfer
        var auth = jwt().jwt(j -> j
                .claim("client_id", "cash-service")
        ).authorities(new SimpleGrantedAuthority("ROLE_balance.write"));

        var request = new TransferConsumeRequest(9013L, "alice", "bob", new BigDecimal("10.00"));

        mockMvc.perform(post("/accounts/transfer")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}