package com.mybank.cash.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@TestConfiguration
public class OAuth2TestBeans {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("dummy")
                .tokenUri("http://localhost/dummy")
                .clientId("dummy")
                .clientSecret("dummy")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        return new InMemoryClientRegistrationRepository(reg);
    }
}