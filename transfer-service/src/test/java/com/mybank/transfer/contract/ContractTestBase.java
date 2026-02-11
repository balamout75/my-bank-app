package com.mybank.transfer.contract;


import com.mybank.transfer.config.TestSecurityItConfig;
import com.mybank.transfer.dto.TransferOperationRequest;
import com.mybank.transfer.dto.OperationKeyResponse;
import com.mybank.transfer.outbox.OutboxProcessor;
import com.mybank.transfer.service.TransferService;
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
    TransferService transferService;

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

        when(transferService.generateOperationKey(anyString()))
                .thenReturn(new OperationKeyResponse(1L));
        doNothing().when(transferService).transfer(anyString(), any(TransferOperationRequest.class));

    }
}
