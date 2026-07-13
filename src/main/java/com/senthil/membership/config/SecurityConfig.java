package com.senthil.membership.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Off by default -- local docker-compose has no Keycloak to validate tokens against.
    // Explicitly enabled via SECURITY_ENABLED (kind/prod Helm values), where
    // app.security.issuer-uri points at the in-cluster Keycloak. Deliberately its own
    // property namespace, not spring.security.oauth2.resourceserver.* -- that would let
    // Spring Boot's own autoconfiguration try to build a JwtDecoder (and fetch the OIDC
    // discovery document over the network) at context startup any time the property is
    // merely present, even with an empty value, which would break docker-compose/tests
    // with no Keycloak reachable regardless of this flag.
    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${app.security.issuer-uri:}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        // Health/info must stay open regardless of auth -- k8s
                        // liveness/readiness probes hit these without a bearer token.
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    // Only invoked (and only hits the network to fetch Keycloak's OIDC discovery
    // document) when securityEnabled is true -- see the comment above.
    private JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
