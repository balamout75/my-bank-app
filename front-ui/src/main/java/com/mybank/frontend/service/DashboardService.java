package com.mybank.frontend.service;

import com.mybank.frontend.client.AccountsClient;
import com.mybank.frontend.client.CashClient;
import com.mybank.frontend.client.TransferClient;
import com.mybank.frontend.mapper.DashboardMapper;
import com.mybank.frontend.viewmodel.FrontendDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;
    private final DashboardMapper mapper;

    public FrontendDTO.MainPageModel buildPage(Authentication authentication) {
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

    public void deposit(Authentication auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        cashClient.deposit(token, null);
    }

    public void withdraw(Authentication auth, FrontendDTO.CashOperationForm form) {
        String token = extractToken(auth);
        cashClient.withdraw(token, null);
    }

    public void transfer(Authentication auth, FrontendDTO.TransferForm form) {
        String token = extractToken(auth);
        transferClient.transfer(token, null);
    }

    private String extractToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is null. User is not authenticated.");
        }
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Unsupported authentication type: " + authentication.getClass().getName());
        }
        return jwtAuth.getToken().getTokenValue();
    }
}
