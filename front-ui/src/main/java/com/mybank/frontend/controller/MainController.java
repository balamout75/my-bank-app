package com.mybank.frontend.controller;

import com.mybank.frontend.dto.FrontendDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

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
            
            String url = gatewayUrl + "/api/accounts/me";
            restTemplate.put(url, form);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Данные успешно обновлены!");
            
        } catch (HttpClientErrorException e) {
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
            
            String url = gatewayUrl + "/api/cash/deposit";
            Map<String, BigDecimal> request = Map.of("amount", form.getAmount());
            
            restTemplate.postForEntity(url, request, String.class);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Счет успешно пополнен на " + form.getAmount() + " руб.!");
            
        } catch (HttpClientErrorException e) {
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
            
            String url = gatewayUrl + "/api/cash/withdraw";
            Map<String, BigDecimal> request = Map.of("amount", form.getAmount());
            
            restTemplate.postForEntity(url, request, String.class);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Со счета снято " + form.getAmount() + " руб.!");
            
        } catch (HttpClientErrorException e) {
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
            
            String url = gatewayUrl + "/api/transfer";
            Map<String, Object> request = Map.of(
                "toUsername", form.getToUsername(),
                "amount", form.getAmount()
            );
            
            restTemplate.postForEntity(url, request, String.class);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Перевод " + form.getAmount() + " руб. пользователю " + 
                form.getToUsername() + " выполнен успешно!");
            
        } catch (HttpClientErrorException e) {
            log.error("Error transferring", e);
            String errorMsg = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ошибка перевода: " + errorMsg);
        }

        return "redirect:/";
    }

    /**
     * Выход из системы
     */
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    // ========== Вспомогательные методы ==========

    /**
     * Получить данные своего аккаунта
     */
    private FrontendDTO.AccountInfo getMyAccount() {
        String url = gatewayUrl + "/api/accounts/me";
        ResponseEntity<FrontendDTO.AccountInfo> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            FrontendDTO.AccountInfo.class
        );
        return response.getBody();
    }

    /**
     * Получить список всех аккаунтов (кроме своего)
     */
    private List<FrontendDTO.AccountSummary> getAllAccounts() {
        String url = gatewayUrl + "/api/accounts/all";
        ResponseEntity<List<FrontendDTO.AccountSummary>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<FrontendDTO.AccountSummary>>() {}
        );
        return response.getBody();
    }

    /**
     * Извлечь сообщение об ошибке из исключения
     */
    private String extractErrorMessage(HttpClientErrorException e) {
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
