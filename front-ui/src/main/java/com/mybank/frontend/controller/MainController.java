package com.mybank.frontend.controller;

import com.mybank.frontend.service.DashboardService;
import com.mybank.frontend.viewmodel.FrontendDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequiredArgsConstructor
public class MainController {

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
}
