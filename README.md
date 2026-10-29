# ShelfLife

A Java 21 / Spring Boot 3.5.13 library management application.
`Item` and `Copy` are persisted. Each Copy references one required Item.

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

The application starts on port 8080. Stop with Ctrl+C. No REST endpoints exist yet.

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
relationship. They use the real database with no embedded replacement.
Their data transactions roll back, but Hibernate schema
updates can remain, so use a dedicated test database.

On Windows, use `mvnw.cmd` in place of `./mvnw` and set environment variables using your shell.
