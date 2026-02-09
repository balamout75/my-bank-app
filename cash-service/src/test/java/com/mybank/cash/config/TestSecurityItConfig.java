package com.mybank.cash.template;

import org.junit.jupiter.api.Order;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.Map;

@TestConfiguration
public class TestSecurityItConfig {

    /**
     * Integration-test SecurityFilterChain:
     * - disables CSRF
     * - permits all requests (security is not a test target here)
     */
    @Bean
    @Order(0)
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /**
     * Required by Spring Security OAuth2 Resource Server (JWT) auto-config / your SecurityConfig.
     * Fake decoder for tests: accepts any token and returns a valid Jwt instance.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return token -> new Jwt(
                token,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "it-user", "scope", "cash")
        );
    }

    /**
     * Required by your HttpClientsConfig.authorizedClientManager(...)
     * for OAuth2 client_credentials (RestClient interceptor).
     */
    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration reg = ClientRegistration.withRegistrationId("cash")
                .tokenUri("http://localhost/dummy") // won't be called in tests since clients are mocked
                .clientId("dummy")
                .clientSecret("dummy")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        return new InMemoryClientRegistrationRepository(reg);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repo) {
        return new InMemoryOAuth2AuthorizedClientService(repo);
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository(OAuth2AuthorizedClientService service) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(service);
    }
}
