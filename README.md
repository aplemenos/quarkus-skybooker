# Quarkus Skybooker

Two **Quarkus** microservices in the flights domain, built to be compiled to a
**GraalVM native** image and run in **Docker**. A `booking-service` calls a
`flight-service` (request/reply over REST) to check availability and reserve
seats, with fault tolerance so a booking degrades gracefully when the flight
service is unavailable.

> Backend portfolio project. Runs end-to-end from a clone — dev mode auto-starts
> PostgreSQL (Quarkus Dev Services), or run the whole stack with Docker Compose.

---

## Features

- **JAX-RS** annotation-driven REST APIs (request/reply JSON)
- **Service-to-service call** — booking → flight via the **MicroProfile REST Client**
- **Fault Tolerance** — `@Timeout`, `@Retry`, `@Fallback` on the inter-service call
- **CDI** — `@Inject`, `@ApplicationScoped`, `@Qualifier`, `@RequestScoped`, `@Observes`
- **MicroProfile Config** — `@ConfigProperty` with **DEV / TEST / PROD** profiles
- **Persistence** — Hibernate ORM with Panache, **PostgreSQL per service**
- **Monitoring** — SmallRye **Health** (incl. a dependency probe) + Micrometer **Prometheus** metrics
- **Testing** — **JUnit 5 + RestAssured** (+ Mockito for the REST client)
- **GraalVM native + Docker** — native-ready, multi-service `docker compose`

---

## Architecture

```mermaid
flowchart TD
    Client([Client])
    Client -->|POST /bookings| Booking[booking-service]
    Booking -->|REST client + fault tolerance| Flight[flight-service]
    Booking --> BDB[(bookings_db)]
    Flight --> FDB[(flights_db)]
```

Each service owns its **own PostgreSQL database** (database-per-service). The only
coupling between them is the HTTP call from booking-service to flight-service.

### Booking flow

```mermaid
sequenceDiagram
    participant C as Client
    participant B as booking-service
    participant F as flight-service
    C->>B: POST /bookings {flightId, seats}
    B->>F: GET /flights/{id}      (fetch details)
    B->>F: POST /flights/{id}/reserve   (reserve seats)
    alt flight-service OK
        F-->>B: flight + reserved
        B-->>C: 201 CONFIRMED (number & price snapshotted)
    else flight-service unreachable
        Note over B,F: @Timeout → @Retry → @Fallback
        B-->>C: 201 PENDING (reconcile later)
    end
```

---

## Tech stack

| Concern | Choice |
|---------|--------|
| Language / Build | Java 25, Maven (wrapper) |
| Framework | Quarkus 3.38 |
| Native | GraalVM 25 (`native-image`) |
| APIs | Quarkus REST (JAX-RS) |
| Inter-service | MicroProfile REST Client |
| Resilience | SmallRye Fault Tolerance |
| Persistence | Hibernate ORM + Panache, PostgreSQL 18 |
| Config | MicroProfile Config (profiles) |
| Observability | SmallRye Health, Micrometer + Prometheus |
| Testing | JUnit 5, RestAssured, Mockito |
| Ops | Docker, Docker Compose |

---

## Getting started

### Prerequisites
- **GraalVM 25** (JDK 25 + `native-image`) — set as `JAVA_HOME`
- Docker (for PostgreSQL, whether via Dev Services or Compose)

### Run in dev mode (recommended for development)
Each service auto-starts its own PostgreSQL via **Dev Services** — no DB setup.

```bash
# terminal 1
cd flight-service && ./mvnw quarkus:dev      # http://localhost:8701

# terminal 2
cd booking-service && ./mvnw quarkus:dev     # http://localhost:8702
```

Live coding is on: edit code and it reloads automatically. Dev UI at
`http://localhost:8701/q/dev/`.

### Run the whole stack with Docker

```bash
(cd flight-service && ./mvnw package -DskipTests)
(cd booking-service && ./mvnw package -DskipTests)
docker compose up --build
```

- flight-service → http://localhost:8701
- booking-service → http://localhost:8702

### Try it

```bash
# list flights
curl http://localhost:8701/flights

# book (booking-service calls flight-service)
curl -X POST http://localhost:8702/bookings \
  -H 'Content-Type: application/json' \
  -d '{"flightId":1,"passengerName":"Ada Lovelace","seats":2}'
```

---

## API overview

**flight-service** (`:8701`)

| Endpoint | Purpose |
|----------|---------|
| `GET /flights` | list flights |
| `GET /flights/{id}` | one flight |
| `GET /flights/{id}/availability?seats=n` | availability check |
| `POST /flights/{id}/reserve` | reserve seats (atomic) |

**booking-service** (`:8702`)

| Endpoint | Purpose |
|----------|---------|
| `POST /bookings` | create a booking (calls flight-service) |
| `GET /bookings` / `GET /bookings/{id}` | list / fetch bookings |

**Both services**

| Endpoint | Purpose |
|----------|---------|
| `/q/health/live`, `/q/health/ready`, `/q/health` | health |
| `/q/metrics` | Prometheus metrics |

---

## How the key concepts show up

- **Fault tolerance** — `FlightGateway` wraps the flight-service call with
  `@Timeout(2s)`, `@Retry`, and `@Fallback`. Network failures degrade the booking
  to `PENDING`; business errors (404/409) `abortOn`/`skipOn` the fallback and
  surface as proper statuses.
- **Config profiles** — `application.properties` uses `%dev` / `%test` / `%prod`
  prefixes: dev/test use Dev Services PostgreSQL; prod reads a real database and
  the flight-service URL from environment variables.
- **CDI** — a `@Qualifier` (`@Standard`) selects a `PricingStrategy`; a
  `@RequestScoped` `RequestContext` tags logs with a per-request id; `@Observes`
  reacts to a custom `BookingCreatedEvent` and the startup event.
- **Monitoring** — a custom `@Readiness` check reports whether flight-service is
  reachable; custom counters (`bookings_created_total`, `flights_seats_reserved_total`)
  appear at `/q/metrics`.

---

## Testing

```bash
./mvnw test    # per service — JUnit 5 + RestAssured, PostgreSQL via Dev Services
```

booking-service tests use `@InjectMock @RestClient` (Mockito) to control
flight-service, covering the happy path, the **fallback → PENDING** path, and
error propagation — without flight-service actually running.

---

## GraalVM native build

Compile a service to a native executable (needs GraalVM `native-image`):

```bash
cd flight-service
./mvnw package -Dnative
# or build a native container image:
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Native images start in tens of milliseconds and use a fraction of the memory of
the JVM — the main reason to pair Quarkus with GraalVM.

---

## Notes

- **Schema management** — for the runnable demo, Hibernate creates and seeds the
  schema. A real production deployment would own the schema with **Flyway
  migrations** and set the strategy to `validate`.
- **Ports** — dev: flight `8701`, booking `8702`. In containers flight listens on
  `8801` and booking on `8802`, mapped back to host `8701` / `8702`.
