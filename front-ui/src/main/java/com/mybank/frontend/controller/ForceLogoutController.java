package com.mybank.frontend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ForceLogoutController {

    private final LogoutSuccessHandler oidcLogoutSuccessHandler;

    @GetMapping("/force-logout")
    public void forceLogout(HttpServletRequest request,
                            HttpServletResponse response,
                            Authentication authentication) throws Exception {

        // 1) локально очищаем SecurityContext + invalidate session
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        // 2) дальше делаем OIDC logout (уйдёт на Keycloak end_session_endpoint)
        oidcLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }
}