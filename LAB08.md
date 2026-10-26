# Laboratory work 8

Ushuzov Ruslan, IT1-2404IS. Journal 18, variant 7: Window and keyset scrolling.

Branch lab08 starts at lab07 (ce7054f), preserving labs 1–7. The laboratory handout starts from main; that would discard the previous implementation in this repository.

## Database

Use PostgreSQL, not H2. The workstation has a local PostgreSQL 16 cluster at
D:\Opencode\SpringFramework\.lab08-tools\pgdata.
Development data is disposable: the dev profile uses create-drop and seeds three
authors, eight books and four tags on every start. The default profile uses validate
and does not create or seed tables. Set DB_URL, DB_USER and DB_PASSWORD to use another instance.

PowerShell 5.1, first window:

~~~powershell
cd D:\Opencode\SpringFramework
& '.\.lab08-tools\pgsql\bin\pg_ctl.exe' -D '.\.lab08-tools\pgdata' -l '.\Lab8\postgres.log' -o '-h 127.0.0.1 -p 5432' -w start
~~~

If the cluster is already running, use pg_ctl status instead of starting a second instance.
The local demonstration databases springlab and springlab_test and the user springlab
are already created. Local demonstration password: springlab.

On a fresh PostgreSQL server, create both databases before running the application or tests.
The springlab role must own both databases and be able to create tables.

## Build and run

PowerShell 5.1, project folder:

~~~powershell
cd D:\SpringFramework\spring-lab-01
.\mvnw.cmd clean package
java -jar target\spring-lab-01-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
~~~

Expected: Tests run: 3, Failures: 0, Errors: 0; BUILD SUCCESS.
Wait for Started SpringLab01Application and LAB08_RESULTS before opening the browser.
Tests use springlab_test; the application uses springlab. No H2 dependency is present.
If authentication fails, verify the database credentials; if the port is occupied,
identify the existing process before starting another instance. Stop the server with Ctrl+C.

Second PowerShell window:

~~~powershell
cd D:\SpringFramework\spring-lab-01
curl.exe -s "http://localhost:8081/api/books?page=0&size=3"
curl.exe -s "http://localhost:8081/api/books?page=1&size=3&sort=year,desc"
curl.exe -s "http://localhost:8081/api/books?genre=TECH&page=0&size=3"
curl.exe -s "http://localhost:8081/api/books?sort=nosuchfield"
curl.exe -s "http://localhost:8081/api/books/scroll"
curl.exe -s "http://localhost:8081/api/books/scroll?after=3"
curl.exe -s "http://localhost:8081/api/books/scroll?after=6"
curl.exe -s "http://localhost:8081/api/lab8/evidence"
~~~

The unsupported sort is deliberately left as the handout experiment: Spring Data rejects
the unknown property at runtime and the application returns HTTP 500. Do not interpret it
as a successful query. A public API should validate sort properties and return HTTP 400.

Open the exact URLs above in the browser and capture the whole window including the address.
The MVC catalogue is http://localhost:8081/books and OpenAPI UI is
http://localhost:8081/swagger-ui/index.html.

## Schema and SQL evidence

~~~powershell
cd D:\Opencode\SpringFramework
$env:PGPASSWORD = "springlab"
& '.\.lab08-tools\pgsql\bin\psql.exe' -h 127.0.0.1 -U springlab -d springlab -c "\d book"
& '.\.lab08-tools\pgsql\bin\psql.exe' -h 127.0.0.1 -U springlab -d springlab -c "\d book_tag"
& '.\.lab08-tools\pgsql\bin\psql.exe' -h 127.0.0.1 -U springlab -d springlab -c "select id,title,genre,author_id,version from book order by id"
Remove-Item Env:\PGPASSWORD
~~~

Book owns author_id and the book_tag join table. Author.books is inverse (mappedBy = author);
there is no books column in author. Tags are shared, so Book.tags does not cascade removal.
The addBook helper updates both Java sides.

Lab8Evidence is dev-only. It flushes and clears the persistence context before every
measurement and records the delta of Hibernate prepareStatementCount. It prints
LAB08_BEGIN / LAB08_END markers with the generated SQL between them. N+1 is 4 statements
for eight books and three distinct authors, then 1 statement using join fetch.
The inverse-only probe inserts author_id = null and is deleted before startup completes.
The archive experiment updates three rows, then resets archived=false for repeatable
API demonstrations. The resulting identity gap is expected.

Page and Slice are measured on the same unarchived eight-row dataset, first page size 3.
Page runs data + count, Slice requests size+1 without a count. The published collection
returns Page so clients can show totalElements and totalPages. Page count queries can be
skipped when the result already proves the total (for example a short first page).

Variant 7 uses Window<Book> and ScrollPosition.keyset(). The first window yields ids 1–3,
then after=3 yields 4–6 and after=6 yields 7–8. The next cursor is exclusive.
Keyset SQL contains id > ? and fetch first; offset paging uses offset ? rows plus a count.
The cursor is restricted to the non-null generated id, which gives stable ordering.

## GitHub pull request procedure

1. Verify the push in PowerShell from D:\SpringFramework\spring-lab-01:
   git log origin/lab08 --oneline -5
2. Open https://github.com/666TiRaN666/spring-lab-01/compare/main...lab08 .
   Verify base: main, compare: lab08, the commit list and the diff. The diff includes
   earlier labs because this repository uses stacked lab branches.
3. Click Create pull request. Title:
   lab08: PostgreSQL, entities, associations, queries, paging

   Description for insertion:

- Replace the in-memory catalogue with PostgreSQL and Spring Data JPA.
- Map Book, Author and Tag with lazy author loading, owning sides, cascade and orphan removal.
- Add five derived queries, named-parameter JPQL and a transactional bulk update.
- Measure N+1 before and after fetch join, and compare Page with Slice.
- Add pagination, sorting and variant 7 Window keyset scrolling while preserving MVC and REST CRUD.
- Validate against PostgreSQL with three integration tests and endpoint regression checks.
4. Click the second Create pull request button to submit.
5. On the resulting /pull/N page, copy its exact URL and check Open, Ready to merge,
   Commits, Files changed and No conflicts. Do not click Merge without a separate request.
   Reviewers and labels must remain unchanged.

## References

- https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
- https://docs.spring.io/spring-data/commons/reference/repositories/scrolling.html
- https://docs.spring.io/spring-boot/reference/data/sql.html
- https://www.postgresql.org/docs/16/

