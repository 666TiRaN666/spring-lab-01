package kz.iitu.springlab.federation;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Task F: the decision after the token has been validated and the scope checked by the filter chain.
 * Every decision is audited with subject, action, resource, result, reason, policy version and token id.
 */
@Service
public class InvoiceAccessService {
    private final InvoiceRepository invoices;
    private final RbacPolicy rbac;
    private final AbacPolicy abac;
    private final AccessAudit audit;

    public InvoiceAccessService(InvoiceRepository invoices, RbacPolicy rbac, AbacPolicy abac, AccessAudit audit) {
        this.invoices = invoices;
        this.rbac = rbac;
        this.abac = abac;
        this.audit = audit;
    }

    public record Result(Object data, List<String> trace) { }

    public Result readInvoice(Jwt jwt, long id, boolean deviceManaged) {
        String action = "GET /api/lab11/invoices/" + id;
        List<String> trace = startTrace(jwt, "invoices:read");
        require(jwt, action, rbac.decide(roles(jwt), "invoices:read"), trace);
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("invoice " + id));
        require(jwt, action, abac.canRead(jwt.getClaimAsString("department"), invoice, deviceManaged), trace);
        audit.record(jwt.getSubject(), action, "ALLOW", String.join("; ", trace.subList(2, trace.size())), jwt.getId());
        return new Result(invoice, trace);
    }

    public Result updateInvoice(Jwt jwt, long id, String status) {
        String action = "PUT /api/lab11/invoices/" + id;
        List<String> trace = startTrace(jwt, "invoices:update");
        require(jwt, action, rbac.decide(roles(jwt), "invoices:update"), trace);
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new NoSuchElementException("invoice " + id));
        audit.record(jwt.getSubject(), action, "ALLOW", trace.getLast(), jwt.getId());
        return new Result(invoices.save(invoice.withStatus(status)), trace);
    }

    public Result readReports(Jwt jwt) {
        String action = "GET /api/lab11/reports";
        List<String> trace = startTrace(jwt, "reports:read");
        require(jwt, action, rbac.decide(roles(jwt), "reports:read"), trace);
        audit.record(jwt.getSubject(), action, "ALLOW", trace.getLast(), jwt.getId());
        return new Result(java.util.Map.of("invoices", 4, "openAmount", "100 780.00"), trace);
    }

    public Result manageUsers(Jwt jwt, String username) {
        String action = "POST /api/lab11/users";
        List<String> trace = startTrace(jwt, "users:manage");
        require(jwt, action, rbac.decide(roles(jwt), "users:manage"), trace);
        audit.record(jwt.getSubject(), action, "ALLOW", trace.getLast(), jwt.getId());
        return new Result(java.util.Map.of("created", username), trace);
    }

    static List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles;
    }

    private static List<String> startTrace(Jwt jwt, String scope) {
        List<String> trace = new ArrayList<>();
        trace.add("token: signature, iss, aud, exp, typ at+jwt — valid");
        trace.add("scope: " + scope + " present");
        return trace;
    }

    private void require(Jwt jwt, String action, Decision decision, List<String> trace) {
        trace.add(decision.reason());
        if (!decision.allowed()) {
            audit.record(jwt.getSubject(), action, "DENY 403", decision.reason(), jwt.getId());
            throw new AccessDeniedByPolicyException(decision.reason());
        }
    }
}
