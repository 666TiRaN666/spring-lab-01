# Laboratory work 10

Ushuzov Ruslan, IT1-2404IS. Spring Security: login form, users in the database, BCrypt,
roles at URL and method level, CSRF. Journal 18, variant 18 % 12 + 1 = 7: remember-me with a token table.

Branch lab10 starts at lab09 (d19a816), preserving labs 1–9. The handout starts from main;
in this repository that would discard the previous implementation.

## Run (Linux)

~~~bash
cd /mnt/games/SpringFramework/spring-lab-01 && ./run.sh
~~~

Wait for `Started SpringLab01Application`, open http://localhost:8081/books and log in.
Teaching passwords only (never reused anywhere):

| Username | Password | Roles | Note |
|---|---|---|---|
| user | user123 | USER | reads the catalogue |
| admin | admin123 | USER, ADMIN | /admin, creates books |
| reader | user123 | USER | same password as user — different hash |
| blocked | user123 | USER | enabled = false, cannot log in |

## What was added

| Part | Files |
|---|---|
| Dependencies | `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6`, `spring-security-test` |
| Filter chain | `security/SecurityConfiguration` — rules, form login, remember-me, logout, `BCryptPasswordEncoder` |
| Users | `V5__security.sql` (`app_user`, `user_role`, BCrypt hashes), `AppUser`, `AppUserRepository`, `DatabaseUserDetailsService` |
| Method security | `MethodSecurityConfiguration` (`@EnableMethodSecurity`), `@PreAuthorize("hasRole('ADMIN')")` on `BookService` writes |
| Views | `fragments/whoami.html` (`sec:authentication`, `sec:authorize`, POST logout), `admin/users.html`, `AdminController` |
| CSRF | `books/list.html` form switched to `th:action` so Thymeleaf inserts `_csrf` |
| Variant 7 | `V6__remember_me.sql` (`persistent_logins`), `JdbcTokenRepositoryImpl`, 14-day cookie |
| Tests | `Lab10SecurityTests` (10 tests); all 25 tests pass |

## Access matrix (observed)

| Request | Anonymous | user | admin |
|---|---|---|---|
| GET / | 200 | 200 | 200 |
| GET /books | 302 /login | 200 | 200 |
| POST /books (with token) | 302 /login | 403 | 302 /books |
| GET /admin | 302 /login | 403 | 200 |
| GET /nowhere | 302 /login | 403 | 403 |

## CSRF with curl

The login form is itself a POST, so it needs the token as well:

~~~bash
B=http://localhost:8081
T=$(curl -s -c cookies.txt -b cookies.txt $B/login | grep -o 'name="_csrf"[^>]*value="[^"]*"' | sed 's/.*value="//;s/"$//')
curl -i -c cookies.txt -b cookies.txt -X POST $B/login -d "username=admin&password=admin123&_csrf=$T"
curl -i -b cookies.txt -X POST $B/books -d "title=Forged&author=Lab Ten&genre=FICTION&year=2020&publicationDate=2020-01-01"
~~~

The last call returns 403: the session cookie alone is not enough.
