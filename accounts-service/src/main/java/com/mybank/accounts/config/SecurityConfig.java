package com.mybank.accounts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Должно совпадать с clientId в Keycloak
    private static final String CLIENT_ID = "accounts-service";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 1) realm roles
            List<String> realmRoles = Optional.ofNullable(jwt.getClaim("realm_access"))
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<String, Object>) m)
                    .map(m -> (List<String>) m.get("roles"))
                    .orElse(List.of());

            // 2) client roles (ваш вариант 1): resource_access.<clientId>.roles
            List<String> clientRoles = Optional.ofNullable(jwt.getClaim("resource_access"))
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<String, Object>) m)
                    .map(m -> m.get(CLIENT_ID))
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<String, Object>) m)
                    .map(m -> (List<String>) m.get("roles"))
                    .orElse(List.of());

            // Собираем authorities (можно добавить префикс ROLE_, если хочешь hasRole)
            List<GrantedAuthority> authorities = new ArrayList<>();
            realmRoles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            clientRoles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));

            // ЛОГ (минимально полезный набор)
            log.info("JWT user={}, azp={}, realmRoles={}, clientRoles({})={}, authorities={}",
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("azp"),
                    realmRoles,
                    CLIENT_ID,
                    clientRoles,
                    authorities.stream().map(GrantedAuthority::getAuthority).toList()
            );

            return authorities;
        });

        // чтобы authentication.getName() был preferred_username (не обязательно)
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }
}
