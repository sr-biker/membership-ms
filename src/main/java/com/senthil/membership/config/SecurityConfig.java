package com.senthil.membership.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Spring Boot only autoconfigures a JwtDecoder bean when
    // spring.security.oauth2.resourceserver.jwt.issuer-uri is actually set (local
    // docker-compose leaves it unset, so jwtDecoder is null below) -- its
    // presence/absence *is* the enabled/disabled switch, no separate flag needed.
    // Autoconfigured as a SupplierJwtDecoder -- issuer resolution is deferred to first
    // token validation, not done at bean-creation time, so an unreachable Keycloak no
    // longer crashes the app at startup the way manually calling
    // JwtDecoders.fromIssuerLocation() did. Confirmed via docker logs
    // (jwtDecoder=SupplierJwtDecoder@...) and a real request against an unreachable
    // issuer-uri: app boots fine, POST/PUT/DELETE correctly 401 without a token.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Autowired(required = false) JwtDecoder jwtDecoder) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (jwtDecoder == null) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        // Health/info must stay open regardless of auth -- k8s
                        // liveness/readiness probes hit these without a bearer token.
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));

        return http.build();
    }
}
