package kz.iitu.springlab;

import kz.iitu.springlab.federation.AccessAudit;
import kz.iitu.springlab.federation.TrainingTokenIssuer;
import kz.iitu.springlab.federation.TrainingTokenIssuer.Preset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Lab 11 with real RS256 tokens from the training issuer: Task B, D, E and the audit of Task F. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/springlab_test")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class Lab11TokenSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired TrainingTokenIssuer issuer;
    @Autowired AccessAudit audit;

    @BeforeEach
    void clearAudit() { audit.clear(); }

    private String token(Preset preset, String role, String department, String scope) {
        return issuer.issue(new TrainingTokenIssuer.Request(preset, "user-1042", List.of(role), department, scope));
    }

    private String financeAnalyst(Preset preset) { return token(preset, "finance-analyst", "finance", "invoices:read profile"); }

    private ResultActions getInvoice(String token, long id, boolean managed) throws Exception {
        return mvc.perform(get("/api/lab11/invoices/" + id)
                .header("Authorization", "Bearer " + token).header("X-Device-Managed", String.valueOf(managed)));
    }

    // ---- Task B: token validation checklist ----

    @Test
    void validAccessTokenIsAccepted() throws Exception {
        getInvoice(financeAnalyst(Preset.VALID), 1, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value("INV-2026-001"));
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mvc.perform(get("/api/lab11/invoices/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer resource_metadata=")));
        mvc.perform(get("/.well-known/oauth-protected-resource/api/lab11/invoices/1"))   // RFC 9728 metadata
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorization_servers[0]").value("https://identity.example.edu"))
                .andExpect(jsonPath("$.scopes_supported").isArray());
    }

    @Test
    void signatureFromUntrustedKeyIsRejected() throws Exception {
        expectInvalidToken(financeAnalyst(Preset.BAD_SIGNATURE), "Invalid signature");
    }

    @Test
    void foreignIssuerIsRejected() throws Exception {
        expectInvalidToken(financeAnalyst(Preset.WRONG_ISSUER), "iss");
    }

    @Test
    void tokenForAnotherApiIsRejected() throws Exception {
        expectInvalidToken(financeAnalyst(Preset.WRONG_AUDIENCE), "aud");
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        expectInvalidToken(financeAnalyst(Preset.EXPIRED), "expired");
    }

    @Test
    void idTokenPresentedAsAccessTokenIsRejected() throws Exception {
        expectInvalidToken(financeAnalyst(Preset.ID_TOKEN), "typ");
    }

    @Test
    void tokenWithoutInvoicesReadScopeIsDenied() throws Exception {
        getInvoice(token(Preset.VALID, "finance-analyst", "finance", "profile"), 1, true)
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", containsString("insufficient_scope")));
    }

    @Test
    void tamperedPayloadBreaksTheSignature() throws Exception {
        String[] parts = token(Preset.VALID, "viewer", "sales", "invoices:read").split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(parts[1])).replace("sales", "finance").getBytes());
        expectInvalidToken(parts[0] + "." + forgedPayload + "." + parts[2], "Invalid signature");
    }

    private void expectInvalidToken(String token, String reason) throws Exception {
        getInvoice(token, 1, true)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("invalid_token")))
                .andExpect(header().string("WWW-Authenticate", containsString(reason)));
    }

    // ---- Task D: RBAC (scope present, the role decides) ----

    @Test
    void rbacDecisionsFromTheHandout() throws Exception {
        String alex = token(Preset.VALID, "viewer", "finance", "invoices:read invoices:update");
        getInvoice(alex, 1, true).andExpect(status().isOk());                                          // ALLOW
        mvc.perform(put("/api/lab11/invoices/1").header("Authorization", "Bearer " + alex))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("none of [viewer] grants invoices:update")));  // DENY
        String sam = token(Preset.VALID, "finance-analyst", "finance", "reports:read");
        mvc.perform(get("/api/lab11/reports").header("Authorization", "Bearer " + sam))
                .andExpect(status().isOk());                                                         // ALLOW
        String riley = token(Preset.VALID, "billing-admin", "finance", "users:manage");
        mvc.perform(post("/api/lab11/users").param("username", "new-clerk").header("Authorization", "Bearer " + riley))
                .andExpect(status().isOk());                                                         // ALLOW
    }

    // ---- Task E: ABAC ----

    @Test
    void abacCasesFromTheHandout() throws Exception {
        getInvoice(financeAnalyst(Preset.VALID), 1, true).andExpect(status().isOk());                 // case 1 ALLOW
        getInvoice(token(Preset.VALID, "finance-analyst", "sales", "invoices:read"), 1, true)         // case 2
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value(containsString("department")));
        getInvoice(financeAnalyst(Preset.VALID), 2, true)                                             // case 3
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value(containsString("classification")));
        getInvoice(financeAnalyst(Preset.VALID), 3, false)                                            // case 4
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value(containsString("device_managed")));
    }

    // ---- Task F: audit without the bearer token ----

    @Test
    void auditRecordsDecisionsButNeverTheToken() throws Exception {
        String allowed = financeAnalyst(Preset.VALID);
        String expired = financeAnalyst(Preset.EXPIRED);
        getInvoice(allowed, 1, true);
        getInvoice(token(Preset.VALID, "finance-analyst", "sales", "invoices:read"), 1, true);
        getInvoice(expired, 1, true);
        var records = audit.records();
        assertEquals(List.of("ALLOW", "DENY 403", "DENY 401"), records.stream().map(r -> r.get("decision")).toList());
        assertEquals("user-1042", records.getFirst().get("subject"));
        assertNotNull(records.getFirst().get("tokenId"));
        String all = records.toString();
        assertFalse(all.contains(allowed) || all.contains(expired.split("\\.")[2]), "no raw token in the audit");
    }

    // ---- lab 10 still protects the training issuer ----

    @Test
    void trainingIssuerRequiresAdminSession() throws Exception {
        mvc.perform(post("/admin/lab11/token")).andExpect(status().isForbidden());   // no CSRF token, no session
        mvc.perform(get("/admin/lab11/audit").accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/login"));
    }
}
