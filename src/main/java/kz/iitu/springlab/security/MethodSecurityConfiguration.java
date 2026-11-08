package kz.iitu.springlab.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Turns on @PreAuthorize: the rule travels with the service method, not only with the URL. */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfiguration { }
