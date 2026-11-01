# Laboratory work 9

Ushuzov Ruslan, IT1-2404IS. Transactions, propagation, isolation, optimistic locking, Flyway.

Branch lab09 starts at lab08 (0120e1b), preserving labs 1–8. The handout creates a new project;
in this repository that would discard the previous implementation, so the work is added as package
`kz.iitu.springlab.bank`.

## What was added

| Part | Files |
|---|---|
| Flyway | `spring-boot-starter-flyway`, `flyway-database-postgresql`; `db/migration/V1__create_accounts.sql`, `V2__add_status.sql`, `V3__create_audit_log.sql`, `V4__index_accounts_owner_name.sql` |
| Entity, repository | `Account` (`@Version`), `AccountRepository`, `AuditRecord`, `AuditRecordRepository` |
| Task A | `TransferService.transfer`, `transferFailingAfterDebit`, `transferCheckedDefault`, `transferCheckedRollbackFor` |
| Task B | `AuditService.recordRequired` / `recordRequiresNew` / `recordMandatory`, `TransferService.outerFailsAfterAudit` |
| Task C, D, E | `Lab9Experiments.isolation`, `optimisticLock`, `migrations` |
| Web | `/api/lab9/accounts`, `POST /api/lab9/transfer`, `/api/lab9/migrations`, `/api/lab9/evidence` (dev) |
| Tests | `Lab9TransactionTests` (12 tests, real commits, PostgreSQL `springlab_test`) |

Schema ownership: Flyway owns `accounts`, `audit_log` and `flyway_schema_history`.
The dev profile of lab 8 still uses Hibernate `create-drop` for the book tables;
`FlywayOwnedTables` (Hibernate `schema_filter_provider`) prevents Hibernate from creating or
dropping the Flyway tables, while `validate` still checks them. `baseline-on-migrate` with
`baseline-version: 0` lets Flyway start on the lab 8 database that already contains tables.

## Run (Linux, bash)

Installed system-wide: Temurin JDK 25 (`temurin-25-jdk`), `gh`, `postgresql-client-16` (psql).
Project-local, excluded from Git: PostgreSQL 16 server and its data in `.lab08-tools/linux/`
(`pgsql/`, `pgdata/`, `logs/`, `env.sh`). Databases `springlab` and `springlab_test`, user and
password `springlab`. The Windows cluster in `.lab08-tools/pgsql` and `.lab08-tools/pgdata`
is not used on Linux.

Terminal 1 — PostgreSQL, build and run:

~~~bash
cd /mnt/games/SpringFramework/spring-lab-01
source .lab08-tools/linux/env.sh
pg_start
./mvnw clean package
java -jar target/spring-lab-01-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
~~~

Expected: `server started` (or `server is running`), `Tests run: 15, Failures: 0, Errors: 0`,
`BUILD SUCCESS`; at startup `Migrating schema "public" to version "1 - create accounts"` …
`"4 - index accounts owner name"` (empty database) or
`Schema "public" is up to date. No migration necessary.` (restart), then `LAB09_RESULTS {...}`
and `Started SpringLab01Application`. Stop the application with Ctrl+C, the database with `pg_stop`.

Terminal 2 — requests and SQL:

~~~bash
curl -s http://localhost:8081/api/lab9/accounts
curl -s -X POST "http://localhost:8081/api/lab9/transfer?from=1&to=2&amount=100.00"
curl -s -i -X POST "http://localhost:8081/api/lab9/transfer?from=1&to=2&amount=5000.00"
curl -s http://localhost:8081/api/lab9/migrations
cd /mnt/games/SpringFramework/spring-lab-01 && source .lab08-tools/linux/env.sh
psql -c "select installed_rank, version, script, success from flyway_schema_history"
~~~

Browser: http://localhost:8081/api/lab9/evidence (all experiments),
http://localhost:8081/api/lab9/migrations (flyway_schema_history).

If the port is busy: `ss -ltnp | grep 808`, then `pkill -f spring-lab-01`.
If `pg_start` reports that port 5432 is in use, another PostgreSQL is running (`ss -ltnp | grep 5432`).

## Observed on PostgreSQL 16.4

| Experiment | Result |
|---|---|
| transfer 100.00 | Alice 1000.00 → 900.00, Bob 500.00 → 600.00 |
| transfer 5000.00 | `Insufficient funds`, HTTP 422, balances unchanged |
| unchecked after flushed debit | rolled back, balances unchanged |
| checked exception, default | **committed**: Alice 900.00, Bob 500.00 (100.00 lost) |
| checked + `rollbackFor` | rolled back |
| REQUIRED audit, outer fails | 0 audit rows |
| REQUIRES_NEW audit, outer fails | 1 audit row survives |
| MANDATORY without transaction | `IllegalTransactionStateException` |
| READ COMMITTED | second read 1050.00 (sees the commit); own update succeeds |
| REPEATABLE READ | second read 1000.00 (snapshot); own update fails `could not serialize access due to concurrent update` |
| stale copy with `@Version` | `ObjectOptimisticLockingFailureException`; balance stays 1050.00, retry after refresh → 1020.00 |
