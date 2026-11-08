package kz.iitu.springlab;

import jakarta.servlet.http.Cookie;
import kz.iitu.springlab.catalog.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Lab 10: the access matrix, BCrypt users from the database, CSRF and remember-me (variant 7). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/springlab_test")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional   // MockMvc runs in the test thread, so created books and tokens are rolled back
class Lab10SecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired BookService books;

    // ---- task 3: the access matrix ----

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/books").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin").accept(MediaType.TEXT_HTML)).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/nowhere").accept(MediaType.TEXT_HTML)).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void userReadsCatalogueButIsForbiddenEverywhereElse() throws Exception {
        var asUser = user("user").roles("USER");
        mvc.perform(get("/books").with(asUser))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ROLE_USER")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Administration"))));
        mvc.perform(get("/admin").with(asUser)).andExpect(status().isForbidden());
        mvc.perform(get("/nowhere").with(asUser)).andExpect(status().isForbidden());
        mvc.perform(post("/books").with(asUser).with(csrf()).params(validBook("By user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminIsAllowedOnAdminAndCreatesBooks() throws Exception {
        var asAdmin = user("admin").roles("USER", "ADMIN");
        mvc.perform(get("/admin").with(asAdmin))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$2a$10$")));
        mvc.perform(post("/books").with(asAdmin).with(csrf()).params(validBook("Genuine")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"));
        mvc.perform(get("/nowhere").with(asAdmin)).andExpect(status().isForbidden());
    }

    // ---- task 3.2: the method rule catches what the URL rule lets through ----

    @Test
    void userPassesUrlRuleForApiButMethodRuleRefusesDelete() throws Exception {
        mvc.perform(delete("/api/books/1").with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertTrue(books.findById(1L).isPresent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void serviceMethodRefusesUserEvenWithoutController() {
        assertThrows(AccessDeniedException.class, () -> books.delete(1L));
    }

    // ---- task 4: CSRF ----

    @Test
    void postWithoutCsrfTokenIsForbiddenEvenForAdmin() throws Exception {
        mvc.perform(post("/books").with(user("admin").roles("ADMIN")).params(validBook("Forged")))
                .andExpect(status().isForbidden());
    }

    @Test
    void formsContainHiddenCsrfField() throws Exception {
        mvc.perform(get("/books/new").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/books").with(user("user").roles("USER")))   // the logout form
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    // ---- task 2: users, BCrypt, UserDetailsService ----

    @Test
    void passwordsAreStoredAsBcryptHashesOnly() {
        List<Map<String, Object>> rows = jdbc.queryForList("select username, password_hash from app_user order by id");
        assertEquals(List.of("user", "admin", "reader", "blocked"), rows.stream().map(r -> r.get("username")).toList());
        for (Map<String, Object> row : rows) {
            String hash = (String) row.get("password_hash");
            assertTrue(hash.startsWith("$2a$10$"), hash);
            assertEquals(60, hash.length());
            assertFalse(hash.contains("123"), "no plaintext");
        }
        String userHash = jdbc.queryForObject("select password_hash from app_user where username = 'user'", String.class);
        String readerHash = jdbc.queryForObject("select password_hash from app_user where username = 'reader'", String.class);
        assertNotEquals(userHash, readerHash, "same password, different salt");
        assertTrue(encoder.matches("user123", userHash));
        assertTrue(encoder.matches("user123", readerHash));
    }

    @Test
    void loginUsesDatabaseUsersAndBcrypt() throws Exception {
        mvc.perform(formLogin().user("admin").password("admin123"))
                .andExpect(authenticated().withUsername("admin").withRoles("USER", "ADMIN"));
        mvc.perform(formLogin().user("user").password("user123"))
                .andExpect(authenticated().withUsername("user").withRoles("USER"));
        // wrong password, unknown user and disabled user all get the same answer
        mvc.perform(formLogin().user("user").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user("nobody").password("user123"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user("blocked").password("user123"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    // ---- variant 7: remember-me ----

    @Test
    void rememberMeIssuesPersistentTokenThatLogsInWithoutSession() throws Exception {
        MvcResult login = mvc.perform(post("/login").with(csrf())
                        .param("username", "user").param("password", "user123").param("remember-me", "on"))
                .andExpect(authenticated().withUsername("user"))
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().maxAge("remember-me", 14 * 24 * 60 * 60))
                .andExpect(cookie().httpOnly("remember-me", true))
                .andReturn();
        assertEquals(1, jdbc.queryForObject("select count(*) from persistent_logins where username = 'user'", Integer.class));

        Cookie rememberMe = login.getResponse().getCookie("remember-me");
        mvc.perform(get("/books").cookie(rememberMe))          // no session, only the cookie
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("user"));

        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "admin123"))
                .andExpect(cookie().doesNotExist("remember-me"));   // checkbox not ticked
    }

    private static org.springframework.util.MultiValueMap<String, String> validBook(String title) {
        var form = new org.springframework.util.LinkedMultiValueMap<String, String>();
        form.add("title", title);
        form.add("author", "Lab Ten");
        form.add("year", "2020");
        form.add("genre", "FICTION");
        form.add("publicationDate", "2020-01-01");
        return form;
    }
}
