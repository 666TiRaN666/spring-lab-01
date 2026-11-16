package kz.iitu.springlab.bank;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/** Runs the Task A–E experiments against the real database and returns observations as maps. */
@Service
public class Lab9Experiments {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private final AccountRepository accounts;
    private final AuditRecordRepository audits;
    private final TransferService transfers;
    private final AuditService audit;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;

    public Lab9Experiments(AccountRepository accounts, AuditRecordRepository audits, TransferService transfers,
                           AuditService audit, JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.accounts = accounts;
        this.audits = audits;
        this.transfers = transfers;
        this.audit = audit;
        this.jdbc = jdbc;
        this.txManager = txManager;
    }

    public long aliceId() { return accounts.findByOwnerName("Alice").orElseThrow().getId(); }
    public long bobId() { return accounts.findByOwnerName("Bob").orElseThrow().getId(); }

    /** Puts the seed data of V1 back so every experiment starts from Alice 1000.00 / Bob 500.00. */
    public void reset() {
        jdbc.update("update accounts set balance = 1000.00 where owner_name = 'Alice'");
        jdbc.update("update accounts set balance = 500.00 where owner_name = 'Bob'");
        jdbc.update("delete from audit_log");
    }

    public Map<String, BigDecimal> balances() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbc.query("select owner_name, balance from accounts order by id",
                rs -> { result.put(rs.getString(1), rs.getBigDecimal(2)); });
        return result;
    }

    public Map<String, Object> runAll() {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("database", jdbc.queryForObject("select version()", String.class));
        results.put("taskA", taskA());
        results.put("taskB", taskB());
        results.put("taskC_READ_COMMITTED", isolation(Isolation.READ_COMMITTED));
        results.put("taskC_REPEATABLE_READ", isolation(Isolation.REPEATABLE_READ));
        results.put("taskD", optimisticLock());
        results.put("taskE", migrations());
        reset();
        return results;
    }

    public Map<String, Object> taskA() {
        long alice = aliceId(), bob = bobId();
        Map<String, Object> a = new LinkedHashMap<>();

        reset();
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("before", balances());
        transfers.transfer(alice, bob, HUNDRED);
        ok.put("after", balances());
        a.put("1_successfulTransfer100", ok);

        reset();
        a.put("2_insufficientFunds", attempt(() -> transfers.transfer(alice, bob, new BigDecimal("5000.00"))));

        reset();
        a.put("3_uncheckedAfterDebit", attempt(() -> transfers.transferFailingAfterDebit(alice, bob, HUNDRED)));

        reset();
        a.put("4_checkedDefault", attempt(() -> transfers.transferCheckedDefault(alice, bob, HUNDRED)));

        reset();
        a.put("5_checkedWithRollbackFor", attempt(() -> transfers.transferCheckedRollbackFor(alice, bob, HUNDRED)));
        reset();
        return a;
    }

    public Map<String, Object> taskB() {
        Map<String, Object> b = new LinkedHashMap<>();
        reset();
        Map<String, Object> required = attempt(() -> transfers.outerFailsAfterAudit(false, "REQUIRED audit"));
        required.put("auditRowsAfterOuterRollback", audits.countByMessage("REQUIRED audit"));
        b.put("REQUIRED", required);

        Map<String, Object> requiresNew = attempt(() -> transfers.outerFailsAfterAudit(true, "REQUIRES_NEW audit"));
        requiresNew.put("auditRowsAfterOuterRollback", audits.countByMessage("REQUIRES_NEW audit"));
        b.put("REQUIRES_NEW", requiresNew);

        Map<String, Object> mandatory = attempt(() -> audit.recordMandatory("MANDATORY audit"));
        mandatory.put("auditRows", audits.countByMessage("MANDATORY audit"));
        b.put("MANDATORY_withoutTransaction", mandatory);
        reset();
        return b;
    }

    /**
     * Task C. Transaction T1 reads Alice's balance twice; between the reads an independent transaction T2
     * on another thread (and another JDBC connection) adds 50.00 and commits. Reads go through JDBC,
     * not through EntityManager, because the persistence context would return the cached entity.
     */
    public Map<String, Object> isolation(Isolation level) {
        reset();
        long alice = aliceId();
        TransactionTemplate t1 = new TransactionTemplate(txManager);
        t1.setIsolationLevel(level.value());
        TransactionTemplate t2 = new TransactionTemplate(txManager);
        t2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String sql = "select balance from accounts where id = ?";
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("isolation", level.name());
        r.put("postgresLevel", null);
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            t1.executeWithoutResult(status -> {
                r.put("postgresLevel", jdbc.queryForObject("show transaction_isolation", String.class));
                r.put("t1FirstRead", jdbc.queryForObject(sql, BigDecimal.class, alice));
                try {
                    other.submit(() -> t2.executeWithoutResult(s ->
                            jdbc.update("update accounts set balance = balance + 50.00 where id = ?", alice))).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new IllegalStateException(e);
                }
                r.put("t2Committed", "+50.00");
                BigDecimal second = jdbc.queryForObject(sql, BigDecimal.class, alice);
                r.put("t1SecondRead", second);
                r.put("t1SawCommittedChange", second.compareTo((BigDecimal) r.get("t1FirstRead")) != 0);
                // T1 now writes the same row that T2 has already changed.
                jdbc.update("update accounts set balance = balance - 10.00 where id = ?", alice);
                r.put("t1UpdateOfSameRow", "succeeded");
            });
        } catch (RuntimeException e) {
            r.put("t1UpdateOfSameRow", "failed: " + e.getClass().getSimpleName() + ": " + rootMessage(e));
        } finally {
            other.shutdown();
        }
        r.put("finalAliceBalance", balances().get("Alice"));
        reset();
        return r;
    }

    /** Task D. Two copies of the same row are loaded in separate persistence contexts. */
    public Map<String, Object> optimisticLock() {
        reset();
        long alice = aliceId();
        Map<String, Object> d = new LinkedHashMap<>();
        Account first = accounts.findById(alice).orElseThrow();   // own transaction, then detached
        Account second = accounts.findById(alice).orElseThrow();  // another transaction, another copy
        d.put("copiesLoadedWithVersion", List.of(first.getVersion(), second.getVersion()));

        first.credit(new BigDecimal("50.00"));
        Account saved = accounts.save(first);
        d.put("firstCopyCommitted", Map.of("balance", saved.getBalance(), "version", saved.getVersion()));

        second.debit(new BigDecimal("30.00"));
        try {
            accounts.save(second);
            d.put("staleCopy", "committed (lost update!)");
        } catch (ObjectOptimisticLockingFailureException e) {
            d.put("staleCopy", "rejected");
            d.put("exception", e.getClass().getName());
            d.put("message", e.getMessage());
        }
        d.put("balanceAfterConflict", balances().get("Alice"));

        // Safe response: reload the current state and re-apply the operation once.
        Account fresh = accounts.findById(alice).orElseThrow();
        fresh.debit(new BigDecimal("30.00"));
        Account retried = accounts.save(fresh);
        d.put("afterRefreshAndRetry", Map.of("balance", retried.getBalance(), "version", retried.getVersion()));
        reset();
        return d;
    }

    public List<Map<String, Object>> migrations() {
        return jdbc.queryForList("select installed_rank, version, description, script, checksum, installed_on, success "
                + "from flyway_schema_history order by installed_rank");
    }

    private Map<String, Object> attempt(ThrowingRunnable action) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("before", balances());
        try {
            action.run();
            r.put("outcome", "completed");
        } catch (Exception e) {
            r.put("outcome", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        r.put("after", balances());
        return r;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
