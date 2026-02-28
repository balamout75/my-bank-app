package com.mybank.frontend.controller;

import com.mybank.frontend.config.TestSecurityConfig;
import com.mybank.frontend.dto.client.CashOperationType;
import com.mybank.frontend.service.DashboardService;
import com.mybank.frontend.viewmodel.FrontendDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "gateway.url=http://localhost:9999"
})
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class FrontControllerIT {

    @Autowired MockMvc mockMvc;

    @MockitoBean DashboardService dashboardService;

    private FrontendDTO.MainPageModel defaultPage() {
        return FrontendDTO.MainPageModel.builder()
                .account(FrontendDTO.AccountInfo.builder()
                        .username("alice")
                        .firstName("Alice")
                        .lastName("Smith")
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .balance(new BigDecimal("10000.00"))
                        .age(36)
                        .build())
                .availableAccounts(List.of(
                        new FrontendDTO.AccountSummary("bob", "Bob Brown")
                ))
                .accountsAvailable(true)
                .accountUpdateForm(new FrontendDTO.AccountUpdateForm("Alice", "Smith", LocalDate.of(1990, 1, 1)))
                .cashOperationForm(new FrontendDTO.CashOperationForm())
                .transferForm(new FrontendDTO.TransferForm())
                .build();
    }

    @BeforeEach
    void setUp() {
        when(dashboardService.buildPage(any())).thenReturn(defaultPage());
    }

    // ======================== SECURITY ========================

    @Test
    @DisplayName("GET / без аутентификации → redirect на OAuth2 login")
    void dashboard_noAuth_shouldRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /css/style.css → 200 (permitAll)")
    void staticResources_shouldBePublic() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
    }

    // ======================== DASHBOARD ========================

    @Test
    @DisplayName("GET / с OAuth2 login → 200, рендерит main.html")
    void dashboard_withAuth_shouldReturn200() throws Exception {
        mockMvc.perform(get("/").with(oauth2Login()
                        .attributes(attrs -> attrs.put("preferred_username", "alice"))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("page"));
    }

    // ======================== ACCOUNT UPDATE ========================

    @Test
    @DisplayName("POST /account/update → redirect:/")
    void updateAccount_shouldRedirect() throws Exception {
        doNothing().when(dashboardService).updateAccount(any(), any());

        mockMvc.perform(post("/account/update")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("firstName", "Alice")
                        .param("lastName", "Johnson")
                        .param("dateOfBirth", "1990-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /account/update с пустым firstName → возвращает main (ошибка валидации)")
    void updateAccount_invalidForm_shouldRenderMain() throws Exception {
        mockMvc.perform(post("/account/update")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "Johnson")
                        .param("dateOfBirth", "1990-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));
    }

    // ======================== CASH DEPOSIT ========================

    @Test
    @DisplayName("POST /cash/deposit → redirect:/")
    void deposit_shouldRedirect() throws Exception {
        doNothing().when(dashboardService).operate(any(), any(), eq(CashOperationType.DEPOSIT));

        mockMvc.perform(post("/cash/deposit")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // ======================== CASH WITHDRAW ========================

    @Test
    @DisplayName("POST /cash/withdraw → redirect:/")
    void withdraw_shouldRedirect() throws Exception {
        doNothing().when(dashboardService).operate(any(), any(), eq(CashOperationType.WITHDRAW));

        mockMvc.perform(post("/cash/withdraw")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // ======================== TRANSFER ========================

    @Test
    @DisplayName("POST /cash/transfer → redirect:/")
    void transfer_shouldRedirect() throws Exception {
        doNothing().when(dashboardService).transfer(any(), any());

        mockMvc.perform(post("/cash/transfer")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("toUsername", "bob")
                        .param("amount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // ======================== ACCOUNTS UNAVAILABLE ========================

    @Test
    @DisplayName("POST /cash/deposit → accounts недоступен → redirect:/ с errorMessage")
    void deposit_accountsDown_shouldRedirectWithError() throws Exception {
        var unavailablePage = defaultPage();
        unavailablePage.setAccountsAvailable(false);
        when(dashboardService.buildPage(any())).thenReturn(unavailablePage);

        mockMvc.perform(post("/cash/deposit")
                        .with(oauth2Login().attributes(a -> a.put("preferred_username", "alice")))
                        .with(csrf())
                        .param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
