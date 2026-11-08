package kz.iitu.springlab.bank;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditRecord() { }
    public AuditRecord(String message) { this.message = message; }

    public Long getId() { return id; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
