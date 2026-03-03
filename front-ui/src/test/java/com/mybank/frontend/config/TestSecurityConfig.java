package com.mybank.frontend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("keycloak")
                .clientId("front-ui")
                .clientSecret("dummy")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://localhost/dummy/auth")
                .tokenUri("http://localhost/dummy/token")
                .userInfoUri("http://localhost/dummy/userinfo")
                .userNameAttributeName("preferred_username")
                .scope("openid")
                .build();
        return new InMemoryClientRegistrationRepository(reg);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repo) {
        return new InMemoryOAuth2AuthorizedClientService(repo);
    }
}
