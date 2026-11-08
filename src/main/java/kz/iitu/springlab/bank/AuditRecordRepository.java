package kz.iitu.springlab.bank;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    long countByMessage(String message);
}
