package com.mybank.accounts.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/accounts/me").hasAuthority("ROLE_accounts.read")
                        .requestMatchers(HttpMethod.PUT, "/accounts/me").hasAuthority("ROLE_accounts.write")
                        .requestMatchers(HttpMethod.GET, "/accounts/all").hasAuthority("ROLE_accounts.read")
                        .requestMatchers(HttpMethod.POST, "/accounts/balance").hasAuthority("ROLE_balance.write")
                        .requestMatchers(HttpMethod.POST, "/accounts/transfer").hasAuthority("ROLE_balance.transfer")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> roles = new HashSet<>();

            // 1) realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rr = realmAccess.get("roles");
                if (rr instanceof Collection<?> col) {
                    col.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .peek(role -> System.out.println("realm_role: " + role))
                            .forEach(roles::add);
                }
            }

            // 2) client roles для accounts-service
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Object client = resourceAccess.get("accounts-service"); // clientId в Keycloak
                if (client instanceof Map<?, ?> clientMap) {
                    Object cr = clientMap.get("roles");
                    if (cr instanceof Collection<?> col) {
                        col.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .peek(role -> System.out.println("client_role: " + role))
                                .forEach(roles::add);
                    }
                }
            }

            return roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

        });
        return converter;
    }
}