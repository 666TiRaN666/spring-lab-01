package kz.iitu.springlab.bank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class TransferService {
    private final AccountRepository repository;
    private final AuditService audit;

    public TransferService(AccountRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    // Task A: both updates commit together or not at all.
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        to.credit(amount);
        repository.save(from);
        repository.save(to);
    }

    // Task A: the debit UPDATE reaches PostgreSQL (flush), then an unchecked exception rolls it back.
    @Transactional
    public void transferFailingAfterDebit(Long fromId, Long toId, BigDecimal amount) {
        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        if (amount.signum() > 0) throw new IllegalStateException("Simulated failure after debit, before credit");
        to.credit(amount);
    }

    // Task A: a checked exception does not mark the transaction for rollback, so the debit is committed.
    @Transactional
    public void transferCheckedDefault(Long fromId, Long toId, BigDecimal amount) throws TransferCheckedException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        throw new TransferCheckedException("Checked failure after debit (default rules)");
    }

    // Task A: rollbackFor turns the same checked exception into a rollback.
    @Transactional(rollbackFor = TransferCheckedException.class)
    public void transferCheckedRollbackFor(Long fromId, Long toId, BigDecimal amount) throws TransferCheckedException {
        Account from = repository.findById(fromId).orElseThrow();
        from.debit(amount);
        repository.saveAndFlush(from);
        throw new TransferCheckedException("Checked failure after debit (rollbackFor)");
    }

    // Task B: the outer transaction fails after the inner audit method has returned.
    @Transactional
    public void outerFailsAfterAudit(boolean requiresNew, String message) {
        if (requiresNew) audit.recordRequiresNew(message);
        else audit.recordRequired(message);
        throw new IllegalStateException("Outer transaction failed after audit returned");
    }
}
