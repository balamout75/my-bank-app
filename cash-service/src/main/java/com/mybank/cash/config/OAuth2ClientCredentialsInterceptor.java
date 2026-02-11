package com.mybank.cash.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.security.oauth2.client.*;

import java.io.IOException;

public class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {

    private final OAuth2AuthorizedClientManager clientManager;
    private final String registrationId;

    public OAuth2ClientCredentialsInterceptor(OAuth2AuthorizedClientManager clientManager, String registrationId) {
        this.clientManager = clientManager;
        this.registrationId = registrationId;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal("cash-service") // любое строковое имя, для client_credentials это ок
                .build();

        OAuth2AuthorizedClient client = clientManager.authorize(authorizeRequest);

        if (client != null && client.getAccessToken() != null) {
            request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
        }

        return execution.execute(request, body);
    }
}
