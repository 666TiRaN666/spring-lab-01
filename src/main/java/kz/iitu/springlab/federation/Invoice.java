package kz.iitu.springlab.federation;

import java.math.BigDecimal;

/** Resource attributes used by the ABAC policy: department and classification. */
public record Invoice(long id, String number, String department, String classification, BigDecimal amount, String status) {
    public Invoice withStatus(String newStatus) {
        return new Invoice(id, number, department, classification, amount, newStatus);
    }
}
