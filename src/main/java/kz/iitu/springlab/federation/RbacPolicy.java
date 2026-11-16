package kz.iitu.springlab.federation;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Task D: role → permissions, exactly as in the handout. */
@Component
public class RbacPolicy {
    static final Map<String, Set<String>> PERMISSIONS = Map.of(
            "viewer", Set.of("invoices:read"),
            "finance-analyst", Set.of("invoices:read", "reports:read"),
            "billing-admin", Set.of("invoices:read", "invoices:update", "users:manage"));

    /** The first role that grants the permission, or empty when no assigned role does. */
    public Optional<String> grantingRole(Collection<String> roles, String permission) {
        return roles.stream().filter(r -> PERMISSIONS.getOrDefault(r, Set.of()).contains(permission)).findFirst();
    }

    public Decision decide(List<String> roles, String permission) {
        return grantingRole(roles, permission)
                .map(r -> Decision.allow("RBAC: role " + r + " grants " + permission))
                .orElseGet(() -> Decision.deny("RBAC: none of " + roles + " grants " + permission));
    }
}
