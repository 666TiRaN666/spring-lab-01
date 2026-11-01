package kz.iitu.springlab.web;

import kz.iitu.springlab.bank.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/lab9")
public class Lab9Controller {
    private final AccountRepository accounts;
    private final TransferService transfers;
    private final Lab9Experiments experiments;
    private final Optional<Lab9Evidence> evidence;

    public Lab9Controller(AccountRepository accounts, TransferService transfers,
                          Lab9Experiments experiments, Optional<Lab9Evidence> evidence) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.experiments = experiments;
        this.evidence = evidence;
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts() {
        return accounts.findAll().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("ownerName", a.getOwnerName());
            m.put("balance", a.getBalance());
            m.put("version", a.getVersion());
            m.put("status", a.getStatus());
            return m;
        }).toList();
    }

    @PostMapping("/transfer")
    public List<Map<String, Object>> transfer(@RequestParam Long from, @RequestParam Long to, @RequestParam BigDecimal amount) {
        transfers.transfer(from, to, amount);
        return accounts();
    }

    @GetMapping("/migrations")
    public List<Map<String, Object>> migrations() { return experiments.migrations(); }

    /** Results of all experiments, collected at startup of the dev profile. */
    @GetMapping("/evidence")
    public Map<String, Object> evidence() {
        return evidence.map(Lab9Evidence::getResults).orElse(Map.of("hint", "start with --spring.profiles.active=dev"));
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class, NoSuchElementException.class})
    public ProblemDetail badTransfer(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, String.valueOf(ex.getMessage()));
        problem.setTitle("Transfer rejected, transaction rolled back");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail conflict(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Account was changed by another transaction; reload and retry");
        problem.setTitle("Optimistic lock conflict");
        return problem;
    }
}
