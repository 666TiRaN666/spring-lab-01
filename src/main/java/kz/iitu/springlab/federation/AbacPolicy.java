package kz.iitu.springlab.federation;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Task E: a user may read an invoice only when all three conditions hold.
 * subject.department = resource.department, resource.classification ∈ {internal, public},
 * environment.device_managed = true. The first failing condition is reported as the reason.
 */
@Component
public class AbacPolicy {
    static final Set<String> READABLE_CLASSIFICATIONS = Set.of("internal", "public");

    public Decision canRead(String subjectDepartment, Invoice invoice, boolean deviceManaged) {
        if (subjectDepartment == null || !subjectDepartment.equals(invoice.department()))
            return Decision.deny("ABAC: subject.department " + subjectDepartment
                    + " ≠ resource.department " + invoice.department());
        if (!READABLE_CLASSIFICATIONS.contains(invoice.classification()))
            return Decision.deny("ABAC: resource.classification " + invoice.classification() + " is not internal/public");
        if (!deviceManaged)
            return Decision.deny("ABAC: environment.device_managed is false");
        return Decision.allow("ABAC: department, classification and managed device all match");
    }
}
