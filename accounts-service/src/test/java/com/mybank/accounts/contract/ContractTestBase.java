package com.mybank.accounts.contract;

import com.mybank.accounts.config.TestSecurityItConfig;
import com.mybank.accounts.dto.AccountMeResponse;
import com.mybank.accounts.dto.AccountSummaryResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("contract-test")
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

        // JWT с обеими группами ролей: user + service
        RestAssuredMockMvc.postProcessors(
                jwt().jwt(j -> j
                        .claim("preferred_username", "alice")
                        .claim("client_id", "some-service")
                        .claim("clientRoles", java.util.List.of(
                                "accounts.read", "accounts.write",
                                "balance.write", "balance.transfer"
                        ))
                ).authorities(
                        new SimpleGrantedAuthority("ROLE_accounts.read"),
                        new SimpleGrantedAuthority("ROLE_accounts.write"),
                        new SimpleGrantedAuthority("ROLE_balance.write"),
                        new SimpleGrantedAuthority("ROLE_balance.transfer")
                )
        );

        // --- Service endpoints ---
        doNothing().when(cashService).applyBalance(any(BalanceUpdateRequest.class), anyString());
        doNothing().when(transferService).transfer(any(), anyString());

        // --- User endpoints (для новых контрактов GET /me, GET /all, PUT /me) ---
        when(accountsService.getMe(anyString())).thenReturn(
                AccountMeResponse.builder()
                        .username("alice")
                        .firstName("Alice")
                        .lastName("Smith")
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .balance(new BigDecimal("10000.00"))
                        .build()
        );

        when(accountsService.getAllOthers(anyString())).thenReturn(
                List.of(new AccountSummaryResponse("bob", "Bob Brown"))
        );

        doNothing().when(accountsService).updateMe(anyString(), any());
    }
}
