package com.mybank.frontend.service;

import com.mybank.frontend.client.AccountsClient;
import com.mybank.frontend.client.CashClient;
import com.mybank.frontend.client.TransferClient;
import com.mybank.frontend.dto.client.*;
import com.mybank.frontend.mapper.DashboardMapper;
import com.mybank.frontend.viewmodel.FrontendDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock AccountsClient accountsClient;
    @Mock CashClient cashClient;
    @Mock TransferClient transferClient;
    @Mock DashboardMapper mapper;
    @Mock OAuth2AuthorizedClientService clientService;

    @InjectMocks DashboardService dashboardService;

    private OAuth2AuthenticationToken mockAuth() {
        var token = mock(OAuth2AuthenticationToken.class);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        when(token.getName()).thenReturn("alice");

        var authorizedClient = mock(OAuth2AuthorizedClient.class);
        var accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("test-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(clientService.loadAuthorizedClient("keycloak", "alice")).thenReturn(authorizedClient);

        return token;
    }

    // ======================== buildPage ========================

    // buildPage → вызывает getMe, getAll и mapper
    @Test
    void buildPage_shouldCallClientsAndMapper() {
        var auth = mockAuth();
        var meDto = new AccountMeResponse("alice", "Alice", "Smith", LocalDate.of(1990, 1, 1), new BigDecimal("10000"));
        var allDtos = List.of(new AccountSummaryResponse("bob", "Bob Brown"));
        var expectedPage = FrontendDTO.MainPageModel.builder().build();

        when(accountsClient.getMe("test-token")).thenReturn(meDto);
        when(accountsClient.getAll("test-token")).thenReturn(allDtos);
        when(mapper.toPageModel(meDto, allDtos, null, null)).thenReturn(expectedPage);

        FrontendDTO.MainPageModel result = dashboardService.buildPage(auth);

        assertThat(result).isSameAs(expectedPage);
        verify(accountsClient).getMe("test-token");
        verify(accountsClient).getAll("test-token");
    }

    // buildPage → accounts недоступен → mapper получает null и errorMessage
    @Test
    void buildPage_accountsDown_shouldPassErrorToMapper() {
        var auth = mockAuth();
        when(accountsClient.getMe("test-token")).thenThrow(new ResourceAccessException("Connection refused"));
        when(mapper.toPageModel(eq(null), eq(List.of()), isNull(), any(String.class)))
                .thenReturn(FrontendDTO.MainPageModel.builder().accountsAvailable(false).build());

        FrontendDTO.MainPageModel result = dashboardService.buildPage(auth);

        assertThat(result.isAccountsAvailable()).isFalse();
        verify(accountsClient, never()).getAll(any());
    }

    // ======================== operate ========================
    // operate → получает ключ и вызывает cashClient.operate
    @Test
    void operate_shouldGetKeyAndCallOperate() {
        var auth = mockAuth();
        when(cashClient.getOperationKey("test-token")).thenReturn(new OperationKeyResponse(42L));

        var form = new FrontendDTO.CashOperationForm(new BigDecimal("100.00"));
        dashboardService.operate(auth, form, CashOperationType.DEPOSIT);

        ArgumentCaptor<CashOperationRequest> captor = ArgumentCaptor.forClass(CashOperationRequest.class);
        verify(cashClient).operate(eq("test-token"), captor.capture());

        CashOperationRequest req = captor.getValue();
        assertThat(req.operationId()).isEqualTo(42L);
        assertThat(req.cashOperationType()).isEqualTo(CashOperationType.DEPOSIT);
        assertThat(req.amount()).isEqualByComparingTo("100.00");
    }

    // ======================== transfer ========================
    // transfer получает ключ и вызывает transferClient.transfer
    @Test
    void transfer_shouldGetKeyAndCallTransfer() {
        var auth = mockAuth();
        when(transferClient.getOperationKey("test-token")).thenReturn(new OperationKeyResponse(77L));

        var form = new FrontendDTO.TransferForm("bob", new BigDecimal("50.00"));
        dashboardService.transfer(auth, form);

        ArgumentCaptor<TransferOperationRequest> captor = ArgumentCaptor.forClass(TransferOperationRequest.class);
        verify(transferClient).transfer(eq("test-token"), captor.capture());

        TransferOperationRequest req = captor.getValue();
        assertThat(req.operationId()).isEqualTo(77L);
        assertThat(req.recipient()).isEqualTo("bob");
        assertThat(req.amount()).isEqualByComparingTo("50.00");
    }

    // ======================== updateAccount ========================
    // updateAccount вызывает accountsClient.updateMe
    @Test
    void updateAccount_shouldCallAccountsClient() {
        var auth = mockAuth();
        var form = new FrontendDTO.AccountUpdateForm("Alice", "Johnson", LocalDate.of(1990, 5, 15));

        dashboardService.updateAccount(auth, form);

        ArgumentCaptor<AccountUpdateRequest> captor = ArgumentCaptor.forClass(AccountUpdateRequest.class);
        verify(accountsClient).updateMe(captor.capture(), eq("test-token"));

        AccountUpdateRequest req = captor.getValue();
        assertThat(req.firstName()).isEqualTo("Alice");
        assertThat(req.lastName()).isEqualTo("Johnson");
        assertThat(req.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
    }
}