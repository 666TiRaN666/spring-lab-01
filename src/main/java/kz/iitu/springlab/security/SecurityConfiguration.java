package kz.iitu.springlab.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    /** Variant 7: lifetime of the remember-me cookie and of its row in persistent_logins. */
    static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(14);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository tokens,
                                    UserDetailsService users) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // anonymous: home page (/ is forwarded to /index.html), login, styles, error page
                .requestMatchers("/", "/index.html", "/login", "/css/**", "/error").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // user: read the catalogue; administrator: create, edit, delete
                .requestMatchers(HttpMethod.GET, "/books/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/books/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
                // REST API of labs 1–9: any logged-in user; writes are guarded by @PreAuthorize in BookService
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form.permitAll())
            .rememberMe(remember -> remember
                .tokenRepository(tokens)
                .userDetailsService(users)
                .tokenValiditySeconds((int) REMEMBER_ME_VALIDITY.toSeconds()))
            .logout(logout -> logout.logoutSuccessUrl("/?logout"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Remember-me tokens live in persistent_logins, created by Flyway V6 (not by createTableOnStartup). */
    @Bean
    PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }
}
