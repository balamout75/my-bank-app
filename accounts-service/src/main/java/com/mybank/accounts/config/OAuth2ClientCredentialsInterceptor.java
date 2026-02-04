package com.mybank.accounts.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.io.IOException;

public class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {

    private final OAuth2AuthorizedClientManager clientManager;
    private final String clientRegistrationId;

    public OAuth2ClientCredentialsInterceptor(
            OAuth2AuthorizedClientManager clientManager,
            String clientRegistrationId) {
        this.clientManager = clientManager;
        this.clientRegistrationId = clientRegistrationId;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal("accounts-service")
                .build();

        OAuth2AuthorizedClient authorizedClient = clientManager.authorize(authorizeRequest);

        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            String token = authorizedClient.getAccessToken().getTokenValue();
            request.getHeaders().setBearerAuth(token);
        }

        return execution.execute(request, body);
    }
}