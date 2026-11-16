package kz.iitu.springlab.web;

import kz.iitu.springlab.federation.AccessAudit;
import kz.iitu.springlab.federation.TrainingTokenIssuer;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Lab 11 training endpoints under /admin/** (lab 10 rules: ADMIN session + CSRF token), dev profile only.
 * They issue synthetic tokens for the experiments and show the audit trail.
 */
@Profile("dev")
@RestController
@RequestMapping("/admin/lab11")
public class Lab11AdminController {
    private final TrainingTokenIssuer issuer;
    private final AccessAudit audit;

    public Lab11AdminController(TrainingTokenIssuer issuer, AccessAudit audit) {
        this.issuer = issuer;
        this.audit = audit;
    }

    @PostMapping(value = "/token", produces = MediaType.TEXT_PLAIN_VALUE)
    public String token(@RequestParam(defaultValue = "VALID") TrainingTokenIssuer.Preset preset,
                        @RequestParam(defaultValue = "user-1042") String sub,
                        @RequestParam(defaultValue = "finance-analyst") String roles,
                        @RequestParam(defaultValue = "finance") String department,
                        @RequestParam(defaultValue = "invoices:read profile") String scope) {
        List<String> roleList = Arrays.stream(roles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return issuer.issue(new TrainingTokenIssuer.Request(preset, sub, roleList, department, scope));
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit() { return audit.records(); }
}
