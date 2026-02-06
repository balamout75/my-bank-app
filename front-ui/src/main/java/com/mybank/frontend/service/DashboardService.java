package com.mybank.frontend.service;

import com.mybank.frontend.client.AccountsClient;
import com.mybank.frontend.client.CashClient;
import com.mybank.frontend.client.TransferClient;
import com.mybank.frontend.client.dto.AccountMeResponse;
import com.mybank.frontend.client.dto.AccountSummaryResponse;
import com.mybank.frontend.dto.client.AccountUpdateRequest;
import com.mybank.frontend.dto.client.CashOperationRequest;

import com.mybank.frontend.dto.client.CashOperationType;
import com.mybank.frontend.mapper.DashboardMapper;
import com.mybank.frontend.viewmodel.FrontendDTO;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;
    private final DashboardMapper mapper;
    private final OAuth2AuthorizedClientService clientService;
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    public FrontendDTO.MainPageModel buildPage(OAuth2AuthenticationToken authentication) {
        String token = extractToken(authentication);

        AccountMeResponse meDto = null;
        List<AccountSummaryResponse> allDtos = List.of();

        String errorMessage = null;
        String successMessage = null;

        try {
            meDto = accountsClient.getMe(token);
        } catch (Exception e) {
            errorMessage = accountsUiMessage(e);
            log.warn("accounts getMe failed: {}", e.toString());
        }

        if (meDto != null) {
            try {
                allDtos = accountsClient.getAll(token);
            } catch (Exception e) {
                if (errorMessage == null) {
                    errorMessage = accountsUiMessage(e);
                    log.warn("accounts getAll failed: {}", e.toString());
                }
            }
        }

        return mapper.toPageModel(meDto, allDtos, successMessage, errorMessage);
    }

    public void deposit(OAuth2AuthenticationToken auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        Long opId = cashClient.getOperationKey(token).operationId();
        try {
            cashClient.operate(token, new CashOperationRequest(opId, CashOperationType.DEPOSIT, form.getAmount()));
        } catch (HttpStatusCodeException e) {

        }
    }

    public void withdraw(OAuth2AuthenticationToken auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        Long opId = cashClient.getOperationKey(token).operationId();
        cashClient.operate(token, new CashOperationRequest(opId, CashOperationType.WITHDRAW, form.getAmount()));
    }

    public void transfer(OAuth2AuthenticationToken auth, FrontendDTO.TransferForm form) {
        String token = extractToken(auth);
        transferClient.transfer(token, null);
    }

    private String extractToken(OAuth2AuthenticationToken authentication) {
        var client = clientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new AuthenticationCredentialsNotFoundException("Access token not found");
        }

        return client.getAccessToken().getTokenValue();
    }

    public void updateAccount(OAuth2AuthenticationToken authentication, FrontendDTO.AccountUpdateForm form) {
        String token = extractToken(authentication);
        if (form == null) throw new IllegalArgumentException("AccountUpdateForm is null");

        var req = new AccountUpdateRequest(
                form.getFirstName(),
                form.getLastName(),
                form.getDateOfBirth()
        );
        accountsClient.updateMe(req, token);
    }

    private String accountsUiMessage(Throwable e) {

        // 1) Circuit breaker открыт
        if (e instanceof CallNotPermittedException) {
            return "Сервис аккаунтов временно перегружен, повторите позже.";
        }

        // 2) Сетевые проблемы (чаще всего RestClient заворачивает их в ResourceAccessException)
        if (e instanceof ResourceAccessException rae) {
            Throwable c = rae.getCause();

            if (c instanceof ConnectException) {
                return "Сервис аккаунтов недоступен (нет соединения).";
            }
            if (c instanceof SocketTimeoutException) {
                return "Сервис аккаунтов не отвечает (таймаут).";
            }
            return "Ошибка связи с сервисом аккаунтов.";
        }

        // 3) Ответ сервера с кодом (5xx/4xx)
        if (e instanceof HttpStatusCodeException hsce) {
            int code = hsce.getStatusCode().value();
            if (code >= 500) return "Сервис аккаунтов временно недоступен (ошибка сервера).";
            if (code == 401 || code == 403) return "Нет доступа к сервису аккаунтов (требуется авторизация).";
            return "Ошибка при обращении к сервису аккаунтов (HTTP " + code + ").";
        }

        // 4) На всякий
        return "Сервис аккаунтов временно недоступен.";
    }

}
