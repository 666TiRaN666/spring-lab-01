package kz.iitu.springlab.bank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Inner service of Task B. It is a separate bean, so calls from TransferService pass through the proxy. */
@Service
public class AuditService {
    private final AuditRecordRepository audits;
    public AuditService(AuditRecordRepository audits) { this.audits = audits; }

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordRequired(String message) { audits.saveAndFlush(new AuditRecord(message)); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequiresNew(String message) { audits.saveAndFlush(new AuditRecord(message)); }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordMandatory(String message) { audits.saveAndFlush(new AuditRecord(message)); }
}
