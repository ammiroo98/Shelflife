# ShelfLife

A Java 21 / Spring Boot 3.5.13 library management application.
`Item`, `Copy`, `Member`, and `Loan` are persisted. Each Copy references one required Item.
Each Loan references one required Copy and Member, with no cascade persistence.
Member status is stored by enum name (`ACTIVE` or `SUSPENDED`), not ordinal.

CheckoutService is a Spring service. Its transactional checkout checks stored active-loan
counts and copy availability, then saves and returns the new Loan through LoanRepository.
It does not yet protect against concurrent checkout attempts. Call it through the
Spring-managed bean for transaction interception; direct construction in unit tests
does not exercise transactions.

ReturnService loads a Loan by UUID in a Spring transaction and delegates to
`Loan.returnOn(LocalDate.now(clock))`. Hibernate dirty checking writes the changed
return date at transaction commit; the service does not call `save` or flush manually.
Missing loans produce a BorrowingException. Return concurrency protection is deferred.

Borrowing configuration in `application.properties`:

- `shelflife.lending.max-active-loans=2`
- `shelflife.lending.loan-duration-days=14`

Both values must be positive and can be overridden with Spring configuration.
The injected application Clock uses the JVM's default time zone; tests use a fixed Clock.

## Requirements

- JDK 21 with `JAVA_HOME` configured
- Maven Wrapper included (Maven 3.9.11)
- Internet access for initial dependency downloads
- A running PostgreSQL server and an existing database/user for application startup

## Database configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/shelflife` | JDBC connection URL |
| `DB_USERNAME` | `shelflife` | Database user |
| `DB_PASSWORD` | None; must be supplied | Database password |
| `SHELFLIFE_POSTGRES_TESTS` | Unset; database tests skipped | Set to exactly `true` to run database tests |

Provide the password through your shell or IDE environment; do not commit credentials.
For example, in zsh, `read -rs 'DB_PASSWORD?Database password: '` followed by
`export DB_PASSWORD` reads the password without displaying it or putting it in shell history.
Set `DB_URL` and `DB_USERNAME` if your local setup differs from the defaults.

Hibernate uses `spring.jpa.hibernate.ddl-auto=update`: on context startup it compares
entity mappings with the existing schema and attempts to create/update mapped tables.
It does not create the PostgreSQL database or user. The user needs schema modification privileges.
This is a development convenience, not a versioned, reviewable migration history;
it is not a reliable way to handle every schema change or production rollout.
No migration tool or manual schema script is included.

## Run

After configuring the database environment:

```sh
./mvnw spring-boot:run
```

The application starts on port 8080. Stop with Ctrl+C.

## Basic REST API

All routes use `/api/v1`. Creation and checkout accept JSON request bodies with
`Content-Type: application/json`. Read/creation/return responses are flat JSON DTOs,
not JPA entities. IDs are assigned by the application; clients supply only relationship
IDs and creation fields. New members retain the existing ACTIVE default.

| Method | Path | JSON request / purpose |
| --- | --- | --- |
| POST | `/api/v1/items` | `{"title":"The Hobbit"}` |
| GET | `/api/v1/items/{id}` | Read item |
| POST | `/api/v1/copies` | `{"itemId":"<item UUID>"}` |
| GET | `/api/v1/copies/{id}` | Read copy |
| POST | `/api/v1/members` | `{"name":"Alex"}` |
| GET | `/api/v1/members/{id}` | Read member |
| POST | `/api/v1/loans` | `{"memberId":"<member UUID>","copyId":"<copy UUID>"}` |
| GET | `/api/v1/loans/{id}` | Read loan |
| POST | `/api/v1/loans/{id}/return` | Return loan; no request body |

Response fields:

- Item: `id`, `title`
- Copy: `id`, `itemId`
- Member: `id`, `name`, `status`
- Loan: `id`, `copyId`, `memberId`, `borrowedOn`, `dueOn`, `returnedOn`, `active`

Dates use ISO calendar dates (`yyyy-MM-dd`); an active loan has `returnedOn: null`.
Each response record has a small manual `from(...)` mapping method. No nested entities
or Hibernate proxy metadata are sent to clients. Entity JSON annotations are no longer needed.
No change to Open EntityManager in View configuration is made in this increment.

Create/checkout responses use 201 with a Location header. Reads and successful returns
use 200. Controller lookups return 404 when absent; Spring MVC rejects missing bodies,
malformed JSON, and malformed UUIDs with 400. Field validation is deferred to Increment 4.3.
Domain/business exceptions retain default server-error handling for now, including a
missing loan in ReturnService; the error contract remains deferred to Increment 4.4.

```sh
curl -X POST http://localhost:8080/api/v1/items \
  -H 'Content-Type: application/json' -d '{"title":"The Hobbit"}'
curl -X POST http://localhost:8080/api/v1/members \
  -H 'Content-Type: application/json' -d '{"name":"Alex"}'
```

Use the returned UUIDs in subsequent Copy and checkout request bodies. No new endpoints
or validation annotations are introduced.

## Tests and build

```sh
./mvnw clean verify
```

The normal build runs plain domain/business tests without PostgreSQL.
The complete Spring context test and real PostgreSQL repository tests are explicitly
skipped unless `SHELFLIFE_POSTGRES_TESTS=true`.
A successful normal build therefore does not verify persistence or application startup.

To run all tests, configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for a dedicated,
existing PostgreSQL test database, then run:

```sh
SHELFLIFE_POSTGRES_TESTS=true ./mvnw clean verify
```

The repository tests save, flush, clear the persistence context, and reload entities
to verify database round trips with caller-assigned UUIDs, including the Copy-to-Item
relationship, a suspended Member's status, and an active Loan's dates and relationships.
They use the real database with no embedded replacement.
Their data transactions roll back, but Hibernate schema
updates can remain, so use a dedicated test database.

On Windows, use `mvnw.cmd` in place of `./mvnw` and set environment variables using your shell.
