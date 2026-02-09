package com.mybank.cash.contract;


import com.mybank.cash.config.TestSecurityItConfig;
import com.mybank.cash.dto.CashOperationRequest;
import com.mybank.cash.dto.OperationKeyResponse;
import com.mybank.cash.outbox.OutboxProcessor;
import com.mybank.cash.service.CashService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityItConfig.class)
public abstract class ContractTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    CashService cashService;

    @MockitoBean
    OutboxProcessor outboxProcessor;


    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        RestAssuredMockMvc.postProcessors(
                jwt().jwt(j -> j
                        .claim("preferred_username", "alice")
                        .claim("clientRoles", "cash.write")
                ).authorities(new SimpleGrantedAuthority("ROLE_cash.write"))
        );

        when(cashService.generateOperationKey(anyString()))
                .thenReturn(new OperationKeyResponse(1L));
        doNothing().when(cashService).operate(anyString(), any(CashOperationRequest.class));

    }
}
