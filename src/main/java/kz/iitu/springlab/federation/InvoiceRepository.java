package kz.iitu.springlab.federation;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Synthetic invoices that cover every row of the Task E table. */
@Repository
public class InvoiceRepository {
    private final Map<Long, Invoice> invoices = new ConcurrentHashMap<>(Map.of(
            1L, new Invoice(1, "INV-2026-001", "finance", "internal", new BigDecimal("1200.00"), "OPEN"),
            2L, new Invoice(2, "INV-2026-002", "finance", "confidential", new BigDecimal("98000.00"), "OPEN"),
            3L, new Invoice(3, "INV-2026-003", "finance", "public", new BigDecimal("350.00"), "PAID"),
            4L, new Invoice(4, "INV-2026-004", "sales", "internal", new BigDecimal("780.00"), "OPEN")));

    public Optional<Invoice> findById(long id) { return Optional.ofNullable(invoices.get(id)); }
    public Invoice save(Invoice invoice) { invoices.put(invoice.id(), invoice); return invoice; }
}
