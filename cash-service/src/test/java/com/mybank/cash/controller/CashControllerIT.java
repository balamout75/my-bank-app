package com.mybank.cash.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.cash.config.OAuth2TestBeans;
import com.mybank.cash.template.BaseIntegrationTest;
import com.mybank.cash.client.AccountsClient;
import com.mybank.cash.client.NotificationsClient;
import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.CashOperationType;
import com.mybank.cash.template.TestSecurityItConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityItConfig.class, OAuth2TestBeans.class})
class CashControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AccountsClient accountsClient;
    @MockitoBean NotificationsClient notificationsClient;

    @Test
    void operate_shouldReturn200_andPersistOperation() throws Exception {

        doNothing().when(accountsClient).updateBalance(org.mockito.ArgumentMatchers.any());
        doNothing().when(notificationsClient).send(org.mockito.ArgumentMatchers.any());

        var request = new CashOperationRequest(1L,CashOperationType.DEPOSIT,new BigDecimal(100));

        mockMvc.perform(post("/cash/operate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
    }
}
