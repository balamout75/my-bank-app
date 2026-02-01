package com.mybank.frontend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityTokenService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public String requireAccessToken(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            throw new IllegalStateException("Expected OAuth2AuthenticationToken but got: " + authentication);
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauth.getAuthorizedClientRegistrationId(),
                oauth.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("No authorized client / access token for user=" + oauth.getName());
        }

        return client.getAccessToken().getTokenValue();
    }
}
