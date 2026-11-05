# ShelfLife

[![ShelfLife CI](https://github.com/ammiroo98/Shelflife/actions/workflows/ci.yml/badge.svg)](https://github.com/ammiroo98/Shelflife/actions/workflows/ci.yml)

ShelfLife is a REST API for a small library. I built it to practice designing a Spring Boot backend, storing related data in PostgreSQL, and testing the rules for checking out and returning books.

## What it does

- Create and look up catalog items, physical copies, and members. An item is the catalog entry; each copy belongs to one item.
- Check out a copy to a member, return it, and look up current or past loans.
- Reject checkout if the member is suspended, has reached the active loan limit, or the copy is already on loan. New members start as active; there is no endpoint to change their status.
- Allow up to **two active loans per member** by default, with a **14-day loan period**. Both settings are in [application.properties](src/main/resources/application.properties).
- Keep returned loans in the database. A loan is active until its return date is set. Late returns are allowed, but returning the same loan twice is rejected.

## How it's built

- **Java 21, Spring Boot 3.5, and Maven:** Spring MVC handles HTTP requests, and the Maven Wrapper pins the Maven version.
- **Spring Data JPA, Hibernate, and PostgreSQL 17:** Repositories store catalog entries, copies, members, and loans. Hibernate updates the development schema on startup using `ddl-auto=update`.
- **Docker and Docker Compose:** The Dockerfile builds the JAR with Maven and runs it in a Java 21 runtime image. Compose runs the API and PostgreSQL together.
- **JUnit 5, Mockito, and MockMvc:** Tests cover domain rules, lending services, and HTTP responses, with optional tests against PostgreSQL.

The code is split into `catalog`, `membership`, `lending`, and `web` packages. Controllers validate request records and return response records with IDs for related resources. Simple reads and creates use repositories directly. Checkout and return go through transactional services: checkout checks the stored loans before saving a new one, and return updates the existing loan inside the transaction. Two checkout requests for the same copy can still both succeed if they arrive at the same time. The transactions do not yet prevent that race condition.

## Run with Docker Compose

Install Docker with the Compose plugin. You don't need Java, Maven, or a separate PostgreSQL installation for this setup. Make sure port 8080 is free.

From the repository root, enter a local database password in Bash or Zsh. The prompt keeps the password out of your shell history, and the export makes it available to Compose. Don't put a real password in the repository.

```sh
printf 'Local database password: '
IFS= read -rs DB_PASSWORD
printf '\n'
export DB_PASSWORD

docker compose up -d --build
```

The API is available at `http://localhost:8080/api/v1`. Compose starts the app after the database passes its health check. To check startup logs, run `docker compose logs -f app`.

Stop the stack from the same shell:

```sh
docker compose down
```

PostgreSQL data stays in the named volume. Use the same password on later starts: PostgreSQL uses it when it first initializes the volume.

## API

All paths below start with `/api/v1`. Send JSON bodies with `Content-Type: application/json`. Use the IDs returned by the create endpoints for later requests.

| Method | Path | Purpose or example request body |
| --- | --- | --- |
| POST | `/items` | Create an item: `{"title":"The Hobbit"}` |
| GET | `/items`, `/items/{id}` | List items or read one item |
| POST | `/copies` | Add a physical copy: `{"itemId":"<item UUID>"}` |
| GET | `/copies`, `/copies/{id}` | List copies or read one copy |
| POST | `/members` | Register a member: `{"name":"Alex"}` |
| GET | `/members`, `/members/{id}` | List members or read one member |
| POST | `/loans` | Check out a copy: `{"memberId":"<member UUID>","copyId":"<copy UUID>"}` |
| GET | `/loans`, `/loans/{id}` | List active and returned loans or read one loan |
| POST | `/loans/{id}/return` | Return a loan; no request body |

For example, create an item with:

```sh
curl -i http://localhost:8080/api/v1/items \
  -H 'Content-Type: application/json' \
  -d '{"title":"The Hobbit"}'
```

Creates and checkouts return `201 Created` with a `Location` header. Reads and returns return `200 OK`; an empty list is `[]`. Invalid input returns `400`, missing resources return `404`, and borrowing conflicts return `409`. Error bodies include `status`, `error`, and `message`, with `fieldErrors` for request validation failures.

## Tests and CI

With a Java 21 JDK installed, run:

```sh
./mvnw clean verify
```

On Windows, use `mvnw.cmd`. The default build runs the domain, service, and MVC tests without a database.

The repository tests and Spring context test are optional. To run them, use a dedicated PostgreSQL test database reachable from your host. Set `DB_URL` to its JDBC URL, `DB_USERNAME` to its user, and export `DB_PASSWORD` using the prompt above. The Compose database does not publish a host port, so it is not directly available to the host test command. The test user needs permission to update the test schema.

```sh
SHELFLIFE_POSTGRES_TESTS=true ./mvnw clean verify
```

[GitHub Actions](.github/workflows/ci.yml) runs on pushes and pull requests. It sets up Temurin Java 21 on Ubuntu, caches Maven dependencies, and runs `./mvnw -B clean verify`. The workflow does not start PostgreSQL, so the optional database tests are skipped in CI.
