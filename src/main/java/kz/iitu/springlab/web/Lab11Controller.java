package kz.iitu.springlab.web;

import kz.iitu.springlab.federation.AccessDeniedByPolicyException;
import kz.iitu.springlab.federation.InvoiceAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Lab 11 finance API. Reached only with a valid Bearer access token (ResourceServerConfiguration). */
@RestController
@RequestMapping("/api/lab11")
public class Lab11Controller {
    private final InvoiceAccessService access;

    public Lab11Controller(InvoiceAccessService access) { this.access = access; }

    /** The verified claims of the presented token — the same reading as Task A, but after signature checks. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("header", jwt.getHeaders());
        m.put("iss", jwt.getIssuer().toString());
        m.put("sub", jwt.getSubject());
        m.put("aud", jwt.getAudience());
        m.put("iat", jwt.getIssuedAt());
        m.put("exp", jwt.getExpiresAt());
        m.put("jti", jwt.getId());
        m.put("scope", jwt.getClaimAsString("scope"));
        m.put("roles", jwt.getClaimAsStringList("roles"));
        m.put("department", jwt.getClaimAsString("department"));
        return m;
    }

    /** environment.device_managed comes from a header that a device-posture gateway would set, not from the token. */
    @GetMapping("/invoices/{id}")
    public InvoiceAccessService.Result invoice(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                               @RequestHeader(name = "X-Device-Managed", defaultValue = "false") boolean managed) {
        return access.readInvoice(jwt, id, managed);
    }

    @PutMapping("/invoices/{id}")
    public InvoiceAccessService.Result update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                              @RequestParam(defaultValue = "PAID") String status) {
        return access.updateInvoice(jwt, id, status);
    }

    @GetMapping("/reports")
    public InvoiceAccessService.Result reports(@AuthenticationPrincipal Jwt jwt) { return access.readReports(jwt); }

    @PostMapping("/users")
    public InvoiceAccessService.Result users(@AuthenticationPrincipal Jwt jwt, @RequestParam String username) {
        return access.manageUsers(jwt, username);
    }

    @ExceptionHandler(AccessDeniedByPolicyException.class)
    public ProblemDetail denied(AccessDeniedByPolicyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access denied by policy");
        return problem;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail notFound(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
