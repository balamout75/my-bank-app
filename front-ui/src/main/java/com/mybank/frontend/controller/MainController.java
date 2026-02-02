package com.mybank.frontend.controller;

import com.mybank.frontend.service.DashboardService;
import com.mybank.frontend.viewmodel.FrontendDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private final DashboardService dashboardService;

    @GetMapping("/")
    public String dashboard(
            OAuth2AuthenticationToken authentication,
            Model model,
            @ModelAttribute("successMessage") String successMessage,
            @ModelAttribute("errorMessage") String errorMessage
    ) {
        FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);

        // flash messages (если пришли после redirect)
        if (successMessage != null && !successMessage.isBlank()) page.setSuccessMessage(successMessage);
        if (errorMessage != null && !errorMessage.isBlank()) page.setErrorMessage(errorMessage);

        model.addAttribute("page", page);
        return "main";
    }

    @PostMapping("/account/update")
    public String updateAccount(
            OAuth2AuthenticationToken authentication,
            @Valid @ModelAttribute("accountUpdateForm") FrontendDTO.AccountUpdateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {

        // Если валидация не прошла — возвращаем ту же страницу БЕЗ редиректа
        if (bindingResult.hasErrors()) {
            FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);
            page.setAccountUpdateForm(form);
            model.addAttribute("page", page);
            log.error("bindingResult.hasErrors(): {}", bindingResult.getAllErrors());
            return "main";
        }

        try {
            dashboardService.updateAccount(authentication, form);
            redirectAttributes.addFlashAttribute("successMessage", "Данные аккаунта успешно обновлены");
        } catch (Exception ex) {
            log.error("ошибка обновления");
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении аккаунта");
        }

        return "redirect:/";
    }

}
