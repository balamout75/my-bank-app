package com.mybank.frontend.controller;

import com.mybank.frontend.dto.client.CashOperationType;
import com.mybank.frontend.exception.InsufficientFundsClientException;
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

        return renderMain(model, page);
    }

    @PostMapping("/account/update")
    public String updateAccount(
            OAuth2AuthenticationToken authentication,
            @Valid @ModelAttribute("accountUpdateForm") FrontendDTO.AccountUpdateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);
        if (!guardAccountsOrRedirect(page, redirectAttributes)) return "redirect:/";
        if (bindingResult.hasErrors()) {
            page.setAccountUpdateForm(form);
            return renderMain(model, page);
        }
        try {
            dashboardService.updateAccount(authentication, form);
            redirectAttributes.addFlashAttribute("successMessage", "Данные аккаунта успешно обновлены");
        } catch (Exception ex) {
            log.error("ошибка обновления", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении аккаунта");
        }
        return "redirect:/";
    }

    @PostMapping("/cash/deposit")
    public String deposit(
            OAuth2AuthenticationToken authentication,
            @Valid @ModelAttribute("cashOperationForm") FrontendDTO.CashOperationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);
        if (!guardAccountsOrRedirect(page, redirectAttributes)) return "redirect:/";
        if (bindingResult.hasErrors()) {
            page.setCashOperationForm(form);
            return renderMain(model, page);
        }
        try {
            dashboardService.operate(authentication, form, CashOperationType.DEPOSIT);
            redirectAttributes.addFlashAttribute("successMessage", "Счет успешно пополнен");
        } catch (Exception ex) {
            log.error("ошибка пополнения", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при пополнении счета");
        }
        return "redirect:/";
    }

    @PostMapping("/cash/withdraw")
    public String withdraw(
            OAuth2AuthenticationToken authentication,
            @Valid @ModelAttribute("cashOperationForm") FrontendDTO.CashOperationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);
        if (!guardAccountsOrRedirect(page, redirectAttributes)) return "redirect:/";
        if (bindingResult.hasErrors()) {
            page.setCashOperationForm(form);
            return renderMain(model, page);
        }
        try {
            dashboardService.operate(authentication, form, CashOperationType.WITHDRAW);
            redirectAttributes.addFlashAttribute("successMessage", "Деньги успешно сняты");
        } catch (InsufficientFundsClientException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("ошибка снятия", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при снятии денег");
        }
        return "redirect:/";
    }

    @PostMapping("/cash/transfer")
    public String transfer(
            OAuth2AuthenticationToken authentication,
            @Valid @ModelAttribute("cashOperationForm") FrontendDTO.TransferForm  form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        FrontendDTO.MainPageModel page = dashboardService.buildPage(authentication);
        if (!guardAccountsOrRedirect(page, redirectAttributes)) return "redirect:/";
        if (bindingResult.hasErrors()) {
            page.setTransferForm(form);
            return renderMain(model, page);
        }
        try {
            System.out.println("Stage 1");
            dashboardService.transfer(authentication, form);
            redirectAttributes.addFlashAttribute("successMessage", "Средства успешно переведены");
        } catch (InsufficientFundsClientException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("ошибка перевода", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка переводе средств");
        }
        return "redirect:/";
    }

    private String renderMain(Model model, FrontendDTO.MainPageModel page) {
        model.addAttribute("page", page);
        model.addAttribute("accountUpdateForm", page.getAccountUpdateForm());
        model.addAttribute("cashOperationForm", page.getCashOperationForm());
        model.addAttribute("transferForm", page.getTransferForm());
        return "main";
    }

    private boolean guardAccountsOrRedirect(FrontendDTO.MainPageModel page, RedirectAttributes ra) {
        if (!page.isAccountsAvailable()) {
            ra.addFlashAttribute("errorMessage", "Сервис аккаунтов временно недоступен");
            return false;
        }
        return true;
    }



}
