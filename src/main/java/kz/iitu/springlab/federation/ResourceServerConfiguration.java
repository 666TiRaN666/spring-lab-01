package kz.iitu.springlab.federation;

import com.nimbusds.jose.JOSEException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

/**
 * Lab 11: the finance API under /api/lab11/** is a stateless OAuth 2 resource server.
 * It runs before the lab 10 form-login chain (@Order(1)) and accepts only Bearer JWT access tokens.
 */
@Configuration
public class ResourceServerConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain resourceServerChain(HttpSecurity http, JwtDecoder decoder, AccessAudit audit) throws Exception {
        var entryPoint = new BearerTokenAuthenticationEntryPoint();
        var deniedHandler = new BearerTokenAccessDeniedHandler();
        http
            .securityMatcher("/api/lab11/**", "/.well-known/oauth-protected-resource/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/.well-known/oauth-protected-resource/**").permitAll()
                // scope = what the client was allowed to ask for; RBAC and ABAC run afterwards in InvoiceAccessService
                .requestMatchers(HttpMethod.GET, "/api/lab11/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/lab11/invoices/**").hasAuthority("SCOPE_invoices:read")
                .requestMatchers(HttpMethod.PUT, "/api/lab11/invoices/**").hasAuthority("SCOPE_invoices:update")
                .requestMatchers(HttpMethod.GET, "/api/lab11/reports").hasAuthority("SCOPE_reports:read")
                .requestMatchers(HttpMethod.POST, "/api/lab11/users").hasAuthority("SCOPE_users:manage")
                .anyRequest().denyAll())
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.decoder(decoder))
                // RFC 9728: the 401 points the client to this document, which names the authorization server
                .protectedResourceMetadata(meta -> meta.protectedResourceMetadataCustomizer(m -> m
                        .authorizationServer(TokenSettings.ISSUER)
                        .resourceName("University finance API (lab 11)")
                        .tlsClientCertificateBoundAccessTokens(false)   // tokens are not bound to a client certificate here
                        .scope("invoices:read").scope("invoices:update").scope("reports:read").scope("users:manage")))
                // record rejected tokens and missing scopes too — without the bearer token itself
                .authenticationEntryPoint((request, response, ex) -> {
                    audit.record("anonymous", request.getMethod() + " " + request.getRequestURI(),
                            "DENY 401", "token rejected: " + ex.getMessage(), null);
                    entryPoint.commence(request, response, ex);
                })
                .accessDeniedHandler((request, response, ex) -> {
                    audit.record(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "?",
                            request.getMethod() + " " + request.getRequestURI(), "DENY 403", "insufficient scope", null);
                    deniedHandler.handle(request, response, ex);
                }))
            // Bearer tokens are sent explicitly in the Authorization header, never automatically by the browser,
            // and no session cookie is created: there is nothing for a cross-site request to ride on.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * Task B in code. The signature is checked with the trusted public key and RS256 only (no "none", no HS256);
     * then every claim check of the handout's table runs as an explicit validator.
     */
    @Bean
    JwtDecoder jwtDecoder(TrainingKeys keys) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.trusted().toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .validateType(false)          // typ is checked below by JwtTypeValidator, with a readable message
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),                                   // exp and nbf, 60 s clock skew
                new JwtIssuerValidator(TokenSettings.ISSUER),                   // iss
                new JwtClaimValidator<Collection<String>>("aud",               // aud
                        aud -> aud != null && aud.contains(TokenSettings.AUDIENCE)),
                new JwtTypeValidator(TokenSettings.ACCESS_TOKEN_TYPE))));      // token purpose: at+jwt, not an ID Token
        return decoder;
    }
}
