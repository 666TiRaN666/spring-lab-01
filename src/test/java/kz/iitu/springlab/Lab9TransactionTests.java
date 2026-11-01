package kz.iitu.springlab;

import kz.iitu.springlab.bank.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Isolation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Not @Transactional: every experiment must really commit or roll back in PostgreSQL. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/springlab_test")
@ActiveProfiles("dev")
class Lab9TransactionTests {
    @Autowired TransferService transfers;
    @Autowired AuditService audit;
    @Autowired AuditRecordRepository audits;
    @Autowired AccountRepository accounts;
    @Autowired Lab9Experiments experiments;
    long alice, bob;

    @BeforeEach
    void resetSeedData() {
        experiments.reset();
        alice = experiments.aliceId();
        bob = experiments.bobId();
    }

    private BigDecimal balance(long id) { return accounts.findById(id).orElseThrow().getBalance(); }
    private static BigDecimal money(String value) { return new BigDecimal(value); }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        transfers.transfer(alice, bob, money("100.00"));
        assertEquals(money("900.00"), balance(alice));
        assertEquals(money("600.00"), balance(bob));
    }

    @Test
    void insufficientFundsLeavesBothBalancesUnchanged() {
        var ex = assertThrows(IllegalStateException.class, () -> transfers.transfer(alice, bob, money("5000.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        assertEquals(money("1000.00"), balance(alice));
        assertEquals(money("500.00"), balance(bob));
    }

    @Test
    void uncheckedExceptionAfterFlushedDebitRollsBack() {
        assertThrows(IllegalStateException.class, () -> transfers.transferFailingAfterDebit(alice, bob, money("100.00")));
        assertEquals(money("1000.00"), balance(alice));
        assertEquals(money("500.00"), balance(bob));
    }

    @Test
    void checkedExceptionCommitsByDefault() {
        assertThrows(TransferCheckedException.class, () -> transfers.transferCheckedDefault(alice, bob, money("100.00")));
        assertEquals(money("900.00"), balance(alice), "debit committed although the method failed");
        assertEquals(money("500.00"), balance(bob));
    }

    @Test
    void rollbackForRollsBackCheckedException() {
        assertThrows(TransferCheckedException.class, () -> transfers.transferCheckedRollbackFor(alice, bob, money("100.00")));
        assertEquals(money("1000.00"), balance(alice));
    }

    @Test
    void requiredAuditIsRolledBackWithOuterTransaction() {
        assertThrows(IllegalStateException.class, () -> transfers.outerFailsAfterAudit(false, "t-required"));
        assertEquals(0, audits.countByMessage("t-required"));
    }

    @Test
    void requiresNewAuditSurvivesOuterRollback() {
        assertThrows(IllegalStateException.class, () -> transfers.outerFailsAfterAudit(true, "t-requires-new"));
        assertEquals(1, audits.countByMessage("t-requires-new"));
    }

    @Test
    void mandatoryRequiresExistingTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> audit.recordMandatory("t-mandatory"));
        assertEquals(0, audits.countByMessage("t-mandatory"));
    }

    @Test
    void readCommittedSeesConcurrentCommitOnSecondRead() {
        Map<String, Object> r = experiments.isolation(Isolation.READ_COMMITTED);
        assertEquals("read committed", r.get("postgresLevel"));
        assertEquals(money("1000.00"), r.get("t1FirstRead"));
        assertEquals(money("1050.00"), r.get("t1SecondRead"));
        assertEquals(true, r.get("t1SawCommittedChange"));
        assertEquals("succeeded", r.get("t1UpdateOfSameRow"));
    }

    @Test
    void repeatableReadKeepsSnapshotAndRejectsConflictingUpdate() {
        Map<String, Object> r = experiments.isolation(Isolation.REPEATABLE_READ);
        assertEquals("repeatable read", r.get("postgresLevel"));
        assertEquals(money("1000.00"), r.get("t1SecondRead"));
        assertEquals(false, r.get("t1SawCommittedChange"));
        assertTrue(r.get("t1UpdateOfSameRow").toString().contains("could not serialize access"));
        assertEquals(money("1050.00"), r.get("finalAliceBalance"));
    }

    @Test
    void staleCopyIsRejectedByVersionCheck() {
        Account first = accounts.findById(alice).orElseThrow();
        Account second = accounts.findById(alice).orElseThrow();
        first.credit(money("50.00"));
        accounts.save(first);
        second.debit(money("30.00"));
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> accounts.save(second));
        assertEquals(money("1050.00"), balance(alice));
    }

    @Test
    void flywayAppliedVersionedMigrationsOnce() {
        List<String> versions = experiments.migrations().stream()
                .filter(m -> m.get("version") != null && !"0".equals(m.get("version")))
                .map(m -> (String) m.get("version")).toList();
        assertEquals(List.of("1", "2", "3", "4"), versions);
        assertTrue(experiments.migrations().stream().allMatch(m -> Boolean.TRUE.equals(m.get("success"))));
    }
}
