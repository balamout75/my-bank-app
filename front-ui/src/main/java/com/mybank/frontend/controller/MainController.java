package com.mybank.frontend.controller;

import com.mybank.frontend.dto.FrontendDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Главный контроллер фронтенда
 * Обрабатывает все действия пользователя на веб-странице
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final RestClient gatewayClient;

    /**
     * Главная страница
     * Отображает информацию об аккаунте, форму операций и форму перевода
     */
    @GetMapping("/")
    public String mainPage(Model model, @AuthenticationPrincipal Object principal) {
        try {
            // Получаем username
            // В режиме заглушки (form login): principal это UserDetails
            // В режиме OAuth2: principal это OAuth2User
            String username;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                username = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("sub");
            } else {
                username = "alice"; // Fallback
            }
            
            log.info("Loading main page for user: {}", username);

            // Получаем данные аккаунта
            FrontendDTO.AccountInfo account;
            try {
                account = getMyAccount();
            } catch (Exception e) {
                log.warn("Backend services unavailable, using mock data");
                // Заглушка когда бэкенд недоступен
                account = FrontendDTO.AccountInfo.builder()
                    .id(1L)
                    .username(username)
                    .firstName("Alice")
                    .lastName("Smith")
                    .email("alice@mybank.com")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .balance(new BigDecimal("10000.00"))
                    .build();
            }
            
            // Получаем список других аккаунтов для переводов
            List<FrontendDTO.AccountSummary> accounts;
            try {
                accounts = getAllAccounts();
            } catch (Exception e) {
                log.warn("Backend services unavailable, using empty list");
                accounts = List.of(); // Пустой список если бэкенд недоступен
            }

            // Создаем модель для страницы
            FrontendDTO.MainPageModel pageModel = FrontendDTO.MainPageModel.builder()
                    .account(account)
                    .availableAccounts(accounts)
                    .build();

            model.addAttribute("pageModel", pageModel);
            model.addAttribute("updateForm", new FrontendDTO.AccountUpdateForm());
            model.addAttribute("cashForm", new FrontendDTO.CashOperationForm());
            model.addAttribute("transferForm", new FrontendDTO.TransferForm());

            return "main";

        } catch (Exception e) {
            log.error("Error loading main page", e);
            model.addAttribute("errorMessage", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Обновление данных аккаунта
     */
    @PostMapping("/account/update")
    public String updateAccount(@Valid @ModelAttribute("updateForm") FrontendDTO.AccountUpdateForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка валидации: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }

        try {
            log.info("Updating account: {} {}", form.getFirstName(), form.getLastName());

            gatewayClient.put()
                    .uri("/api/accounts/me")
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            redirectAttributes.addFlashAttribute("successMessage", 
                "Данные успешно обновлены!");
            
        } catch (RestClientResponseException e) {
            log.error("Error updating account", e);
            String errorMsg = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка обновления: " + errorMsg);
        }

        return "redirect:/";
    }

    /**
     * Пополнение счета
     */
    @PostMapping("/cash/deposit")
    public String deposit(@Valid @ModelAttribute("cashForm") FrontendDTO.CashOperationForm form,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }

        try {
            log.info("Depositing: {}", form.getAmount());

            Map<String, BigDecimal> request = Map.of("amount", form.getAmount());

            gatewayClient.post()
                    .uri("/api/cash/deposit")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            redirectAttributes.addFlashAttribute("successMessage", 
                "Счет успешно пополнен на " + form.getAmount() + " руб.!");
            
        } catch (RestClientResponseException e) {
            log.error("Error depositing", e);
            String errorMsg = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка пополнения: " + errorMsg);
        }

        return "redirect:/";
    }

    /**
     * Снятие денег
     */
    @PostMapping("/cash/withdraw")
    public String withdraw(@Valid @ModelAttribute("cashForm") FrontendDTO.CashOperationForm form,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }

        try {
            log.info("Withdrawing: {}", form.getAmount());
            
            Map<String, BigDecimal> request = Map.of("amount", form.getAmount());

            gatewayClient.post()
                    .uri("/api/cash/withdraw")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            redirectAttributes.addFlashAttribute("successMessage", 
                "Со счета снято " + form.getAmount() + " руб.!");
            
        } catch (RestClientResponseException e) {
            log.error("Error withdrawing", e);
            String errorMsg = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка снятия: " + errorMsg);
        }

        return "redirect:/";
    }

    /**
     * Перевод денег другому пользователю
     */
    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferForm") FrontendDTO.TransferForm form,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }

        try {
            log.info("Transferring {} to {}", form.getAmount(), form.getToUsername());
            
            Map<String, Object> request = Map.of(
                "toUsername", form.getToUsername(),
                "amount", form.getAmount()
            );

            gatewayClient.post()
                    .uri("/api/transfer")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            redirectAttributes.addFlashAttribute("successMessage", 
                "Перевод " + form.getAmount() + " руб. пользователю " + 
                form.getToUsername() + " выполнен успешно!");
            
        } catch (RestClientResponseException e) {
            log.error("Error transferring", e);
            String errorMsg = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка перевода: " + errorMsg);
        }

        return "redirect:/";
    }

    // ========== Вспомогательные методы ==========

    /**
     * Получить данные своего аккаунта
     */
    private FrontendDTO.AccountInfo getMyAccount() {
        return gatewayClient.get()
                .uri("/api/accounts/me")
                .retrieve()
                .body(FrontendDTO.AccountInfo.class);
    }

    /**
     * Получить список всех аккаунтов (кроме своего)
     */
    private List<FrontendDTO.AccountSummary> getAllAccounts() {
        FrontendDTO.AccountSummary[] arr = gatewayClient.get()
                .uri("/api/accounts/all")
                .retrieve()
                .body(FrontendDTO.AccountSummary[].class);

        return arr == null ? List.of() : List.of(arr);
    }

    /**
     * Извлечь сообщение об ошибке из исключения
     */
    private String extractErrorMessage(RestClientResponseException e) {
        try {
            String body = e.getResponseBodyAsString();
            // Попытка извлечь поле "error" из JSON
            if (body.contains("\"error\"")) {
                int start = body.indexOf("\"error\":\"") + 9;
                int end = body.indexOf("\"", start);
                if (end > start) {
                    return body.substring(start, end);
                }
            }
            return body;
        } catch (Exception ex) {
            return e.getMessage();
        }
    }
}
