package com.mybank.frontend.service;

import com.mybank.frontend.client.AccountsClient;
import com.mybank.frontend.client.CashClient;
import com.mybank.frontend.client.TransferClient;
import com.mybank.frontend.mapper.DashboardMapper;
import com.mybank.frontend.viewmodel.FrontendDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;
    private final DashboardMapper mapper;
    private final OAuth2AuthorizedClientService clientService;

    public FrontendDTO.MainPageModel buildPage(OAuth2AuthenticationToken authentication) {
        String token = extractToken(authentication);

        // 1) пользователь
        var meDto = accountsClient.me(token);

        // 2) список аккаунтов (получатели)
        var allDtos = accountsClient.getAll(token);

        // 3) ViewModel
        var accountInfo = mapper.toAccountInfo(meDto);
        List<FrontendDTO.AccountSummary> summaries =
                mapper.toSummaries(allDtos, accountInfo.getUsername());

        return FrontendDTO.MainPageModel.builder()
                .account(accountInfo)
                .availableAccounts(summaries)

                // формы (важно: не null)
                .accountUpdateForm(mapper.defaultUpdateForm(accountInfo))
                .cashOperationForm(new FrontendDTO.CashOperationForm())
                .transferForm(new FrontendDTO.TransferForm())

                // сообщения пока пустые (потом сделаем через RedirectAttributes / Flash)
                .successMessage(null)
                .errorMessage(null)
                .build();
    }

    public void deposit(OAuth2AuthenticationToken auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        cashClient.deposit(token, null);
    }

    public void withdraw(OAuth2AuthenticationToken auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        cashClient.withdraw(token, null);
    }

    public void transfer(OAuth2AuthenticationToken auth, FrontendDTO.TransferForm form) {
        String token = extractToken(auth);
        transferClient.transfer(token, null);
    }

    private String extractToken(OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is null");
        }
        // Получаем principal (пользователя)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser)) {
            throw new IllegalStateException("Principal is not an OidcUser. Type: " +
                    (principal != null ? principal.getClass().getName() : "null"));
        }
        OidcUser oidcUser = (OidcUser) principal;
        // Получаем ID Token (это JWT от Keycloak)
        String tokenValue = oidcUser.getIdToken().getTokenValue();
        if (tokenValue == null || tokenValue.isEmpty()) {
            throw new IllegalStateException("ID Token is null or empty for user: " + authentication.getName());
        }

        System.out.println("Successfully extracted ID token for user: {}" + authentication.getName());
        return tokenValue;
    }
}
