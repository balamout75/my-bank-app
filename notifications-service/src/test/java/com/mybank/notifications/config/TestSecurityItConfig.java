package com.mybank.notifications.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

@TestConfiguration
public class TestSecurityItConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        // Нужен лишь для старта контекста (resource-server jwt)
        return token -> new Jwt(
                token,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "alice", "preferred_username", "alice", "scope", "cash")
        );
    }
}
