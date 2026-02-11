package com.mybank.cash.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.cash.config.TestSecurityItConfig;
import com.mybank.cash.template.BaseIntegrationTest;
import com.mybank.cash.client.AccountsClient;
import com.mybank.cash.client.NotificationsClient;
import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.CashOperationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;

import static org.mockito.Mockito.doNothing;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityItConfig.class)
class CashControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AccountsClient accountsClient;
    @MockitoBean NotificationsClient notificationsClient;

    @Test
    void flow_shouldReserveKey_thenOperate() throws Exception {

        // внешние вызовы
        doNothing().when(accountsClient).updateBalance(any());

        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
                .claim("clientRoles", "cash.write")
        ).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));

        // 1) резервируем operationKey
        String keyJson = mockMvc.perform(get("/cash/operation-key").with(auth))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(keyJson);
        long operationId = node.get("operationId").asLong(); // <-- проверь имя поля в OperationKeyResponse

        // 2) выполняем операцию с зарезервированным operationId
        var request = new CashOperationRequest(
                operationId,
                CashOperationType.DEPOSIT,
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/cash/operate")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    void operate_withZeroAmount_shouldReturn400() throws Exception {

        // внешние вызовы
        doNothing().when(accountsClient).updateBalance(any());

        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
                .claim("clientRoles", "cash.write")
        ).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));

        // 1) резервируем operationKey
        String keyJson = mockMvc.perform(get("/cash/operation-key").with(auth))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(keyJson);
        long operationId = node.get("operationId").asLong(); // <-- проверь имя поля в OperationKeyResponse

        // 2) выполняем операцию с зарезервированным operationId
        var request = new CashOperationRequest(
                operationId,
                CashOperationType.DEPOSIT,
                new BigDecimal("0.00")
        );

        mockMvc.perform(post("/cash/operate")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void operate_twice_withSameOperationId_shouldReturn409() throws Exception {

        doNothing().when(accountsClient).updateBalance(any());

        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
                .claim("clientRoles", "cash.write")
        ).authorities(new SimpleGrantedAuthority("ROLE_cash.write"));

        // 1️⃣ Получаем operation key
        String keyJson = mockMvc.perform(get("/cash/operation-key").with(auth))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long operationId = objectMapper.readTree(keyJson).get("operationId").asLong();

        var request = new CashOperationRequest(
                operationId,
                CashOperationType.DEPOSIT,
                new BigDecimal("100.00")
        );

        // 2️⃣ Первый вызов — успешный
        mockMvc.perform(post("/cash/operate")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // 3️⃣ Повторный вызов с тем же ключом — должен упасть
        mockMvc.perform(post("/cash/operate")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())   // 👈 покажет тело ответа
                .andExpect(status().isBadRequest());
    }

    @Test
    void operate_withCashReadOnly_shouldReturn403() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
                .claim("clientRoles", "cash.read")
        ).authorities(new SimpleGrantedAuthority("cash.read"));

        var request = new CashOperationRequest(
                1L,
                CashOperationType.DEPOSIT,
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/cash/operate")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationKey_withCashReadOnly_shouldBeForbiddenOrOk() throws Exception {
        var auth = jwt().jwt(j -> j
                .claim("preferred_username", "alice")
                .claim("clientRoles", "cash.read")
        ).authorities(new SimpleGrantedAuthority("cash.read"));

        mockMvc.perform(get("/cash/operation-key").with(auth))
                .andExpect(status().isForbidden());
    }
}
