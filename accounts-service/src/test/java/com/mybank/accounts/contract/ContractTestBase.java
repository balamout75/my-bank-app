package com.mybank.accounts.contract;

import com.mybank.accounts.config.TestSecurityItConfig;
import com.mybank.accounts.dto.BalanceUpdateRequest;
import com.mybank.accounts.outbox.OutboxProcessor;
import com.mybank.accounts.repository.AccountOperationRepository;
import com.mybank.accounts.repository.AccountRepository;
import com.mybank.accounts.repository.ServiceOperationsRepository;
import com.mybank.accounts.service.AccountsService;
import com.mybank.accounts.service.CashService;
import com.mybank.accounts.service.TransferService;
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
    AccountsService accountsService;

    @MockitoBean
    AccountRepository accountRepository;

    @MockitoBean
    AccountOperationRepository accountOperationRepository;

    @MockitoBean
    ServiceOperationsRepository serviceOperationsRepository;

    @MockitoBean
    OutboxProcessor outboxProcessor;

    @MockitoBean
    TransferService transferService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // по умолчанию все contract-запросы идут с сервисным jwt
        RestAssuredMockMvc.postProcessors(
                jwt().jwt(j -> j
                        .claim("client_id", "some-service")
                        .claim("clientRoles", java.util.List.of("balance.write", "balance.transfer"))
                ).authorities(new SimpleGrantedAuthority("ROLE_balance.write"),
                              new SimpleGrantedAuthority("ROLE_balance.transfer")
                )
        );

        doNothing().when(cashService).applyBalance(any(BalanceUpdateRequest.class), anyString());
    }
}
