# Laboratory work 11

Ushuzov Ruslan, IT1-2404IS. Token-based and federated security: JWT, OAuth 2.1, OpenID Connect, RBAC, ABAC.
The handout is a written exercise (tasks A–F) without individual variants. To back every answer with an
observation, the finance API of the handout is implemented as a working resource server in this project.

Branch lab11 starts at lab10 (dbbeea6), preserving labs 1–10.

## What was added

| Part | Files |
|---|---|
| Dependency | `spring-boot-starter-security-oauth2-resource-server` (Nimbus JOSE) |
| Resource server | `federation/ResourceServerConfiguration` — second `SecurityFilterChain` for `/api/lab11/**`, `@Order(1)`, stateless, Bearer only |
| Token validation (task B) | `JwtDecoder`: RS256 signature with the trusted key, `JwtTimestampValidator`, `JwtIssuerValidator`, `aud` validator, `JwtTypeValidator("at+jwt")` |
| Scope | URL rules `hasAuthority("SCOPE_invoices:read")` etc. |
| RBAC (task D) | `RbacPolicy` — viewer / finance-analyst / billing-admin |
| ABAC (task E) | `AbacPolicy` — department, classification, `X-Device-Managed` |
| Decision + audit (task F) | `InvoiceAccessService`, `AccessAudit` (subject, action, decision, reason, policy version, jti — never the token) |
| API | `GET /api/lab11/me`, `GET/PUT /api/lab11/invoices/{id}`, `GET /api/lab11/reports`, `POST /api/lab11/users` |
| Metadata | RFC 9728 `/.well-known/oauth-protected-resource` (authorization server, scopes) |
| Training issuer | `TrainingKeys`, `TrainingTokenIssuer`, `POST /admin/lab11/token`, `GET /admin/lab11/audit` (dev, ADMIN session + CSRF) |
| Tests | `Lab11TokenSecurityTests` (13 tests with real signed tokens); all 38 tests pass |

The keys are generated in memory at startup; tokens are valid until the next restart.
A real resource server would read the issuer's public keys from its JWKS URI.

## Run (Linux)

~~~bash
cd /mnt/games/SpringFramework/spring-lab-01 && ./run.sh
~~~

Get a token as admin (lab 10 login with CSRF), then call the API:

~~~bash
B=http://localhost:8081; J=admin.txt
T=$(curl -s -c $J -b $J $B/login | grep -o 'name="_csrf"[^>]*value="[^"]*"' | sed 's/.*value="//;s/"$//')
curl -s -c $J -b $J -X POST $B/login -d "username=admin&password=admin123&_csrf=$T" > /dev/null
T=$(curl -s -b $J $B/books/new | grep -o 'name="_csrf"[^>]*value="[^"]*"' | head -1 | sed 's/.*value="//;s/"$//')
TOKEN=$(curl -s -b $J -X POST $B/admin/lab11/token -d "preset=VALID&roles=finance-analyst&department=finance&scope=invoices:read&_csrf=$T")
curl -i -H "Authorization: Bearer $TOKEN" -H "X-Device-Managed: true" $B/api/lab11/invoices/1
~~~

Presets for task B: `VALID`, `BAD_SIGNATURE`, `WRONG_ISSUER`, `WRONG_AUDIENCE`, `EXPIRED`, `ID_TOKEN`.

| Case | Result |
|---|---|
| valid access token | 200 |
| no token | 401 `Bearer resource_metadata=…` |
| untrusted key / edited payload | 401 `Invalid signature` |
| foreign issuer | 401 `The iss claim is not valid` |
| other API | 401 `The aud claim is not valid` |
| expired | 401 `Jwt expired at …` |
| ID Token as access token | 401 `the given typ value needs to be one of [at+jwt]` |
| no `invoices:read` | 403 `insufficient_scope` |
