# ERP Lab – Tech Stack

Polyglot manufacturing platform: **ERP** (business) + **MES** (shop floor).
Java for the core, Go for the edge, Python for analytics.

## Overview

| Area         | Technology                                                      | Purpose                                          |
| ------------ | --------------------------------------------------------------- | ------------------------------------------------ |
| Core backend | Java 25 (LTS), Spring Boot 4.1, Maven                           | ERP + MES business modules                       |
| Edge         | Go 1.23+                                                        | Machine simulator, MQTT collector                |
| Analytics    | Python 3.12, FastAPI                                            | OEE analytics, predictive maintenance, defect AI |
| Web          | React, Vite, TypeScript, TanStack, shadcn/ui, Tailwind, ECharts | Admin UI, shop-floor screens, dashboards         |
| Database     | PostgreSQL 17 + TimescaleDB                                     | Business data + machine time-series              |
| Messaging    | Eclipse Mosquitto (MQTT)                                        | Machine → collector telemetry                    |
| Runtime      | Docker Compose, Makefile                                        | Local environment, single command entry point    |

## Backend – Java (`api/`)

| Concern               | Choice                                                                                                                                                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| REST                  | Spring Web, under configurable prefix + version (default `/api/v1`)                                                                                                                                                       |
| GraphQL               | Spring for GraphQL (schema-first `.graphqls`), GraphiQL in dev; queries for reads/reports, mutations reuse application services                                                                                           |
| API docs              | springdoc-openapi (OpenAPI 3 + Swagger UI), one tag per context                                                                                                                                                           |
| Response shape        | `ResponseEnvelope<T>` for every response – `success` (`data`), `fail` (4xx, `code`/`message`, validation `issues`), `error` (5xx); added by `ResponseBodyAdvice` + `@RestControllerAdvice`, controllers return plain data |
| Security | Spring Security OAuth2 resource server, HS256 JWT (access 15 min / refresh 7 days), role → permission authorities, `@PreAuthorize` |
| Persistence | Spring Data JPA (Hibernate) for aggregates; `JdbcClient` SQL read models for queries/lists |
| Migrations            | Flyway                                                                                                                                                                                                                    |
| Mapping / boilerplate | Lombok (records for DTOs and commands; no mapper library) |
| Validation            | Jakarta Validation                                                                                                                                                                                                        |
| Document workflow     | Enum-based state machine (Draft → Confirmed → Done → Cancelled)                                                                                                                                                           |
| Module events | Spring Application Events, handled synchronously in the publishing transaction (Kafka/Redpanda later if needed) |
| Real-time | Spring WebSocket (STOMP) – with MES machine monitoring |
| Cache | Caffeine (later, when needed) |
| Reports               | Apache POI (Excel), OpenPDF (PDF)                                                                                                                                                                                         |
| Auditing | JPA Auditing (`@CreatedBy`, `@LastModifiedDate`), soft delete; `audit_log` of every successful command (AOP) |
| Concurrency           | Optimistic locking (`@Version`) on stock                                                                                                                                                                                  |
| Testing               | JUnit 5, Mockito, Testcontainers (PostgreSQL)                                                                                                                                                                             |

Architecture: **modular monolith** with pragmatic **DDD** – one deployable, one package per bounded context:

```
com.erplab
 ├── common        base entity, errors, audit, security utils
 ├── auth          users, roles, permissions, JWT
 ├── masterdata    products, BOM, units, warehouses, customers, suppliers, machines, lines
 ├── inventory     stock, movements, transfers
 ├── purchasing    purchase orders, goods receipts
 ├── sales         sales orders, deliveries, invoices
 ├── accounting    chart of accounts, journal entries, payments
 ├── production    production orders, work orders, routing    (MES)
 ├── shopfloor     job start/stop, output & scrap reporting   (MES)
 ├── quality       inspections, defect codes, SPC              (MES)
 ├── traceability  lots, serials, genealogy                    (MES)
 └── report        dashboards, exports
```

Each module (bounded context) is laid out as (DDD + light CQRS):

```
<context>
 ├── domain            aggregates, value objects, domain errors, domain events, repository interfaces
 ├── application
 │    ├── command      <Verb><Noun>Command + handler (writes)
 │    └── query        Get<Noun>Query + handler (reads)
 ├── infrastructure    repository implementations (JPA), external adapters
 ├── http              REST controllers, request/response DTOs
 └── graphql           resolvers (optional; call the same handlers as http)
```

Rules:

- **Dependencies point inwards** – `http`/`graphql` → `application` → `domain`; `infrastructure`
  implements `domain` interfaces. `domain` imports nothing from Spring web or other contexts.
- **Commands** change state: one command = one transaction (`@Transactional` on the handler); load the
  aggregate, call domain methods (`order.confirm()`), save, return at most the new id.
- **Queries** never change state; they may skip the domain and read with SQL (`JdbcClient`, `SqlPage`) straight into DTOs.
- **Rich aggregates where invariants matter** – inventory (no negative stock, `@Version`),
  sales / purchase / production orders (state transitions), accounting (debit = credit).
  Master data (units, customers, warehouses) stays thin.
- **Cross-context** – reference other contexts by ID; talk via commands/queries or domain events
  (`SalesOrderConfirmed`, `GoodsReceived`, `WorkOrderCompleted`, … via Spring Application Events),
  never by importing another context's entities or repositories. Verified with Spring Modulith tests.
- **Thin controllers / resolvers** – validate DTO, run a command or query, map the result.
- Seeders and scripts go through command handlers, so seeded data follows the same rules.

## Edge – Go (`edge/`)

| Component   | Libraries              | Purpose                                                                    |
| ----------- | ---------------------- | -------------------------------------------------------------------------- |
| `simulator` | Eclipse Paho MQTT      | Simulate 10–50 machines (status, cycle count, temperature, alarms)         |
| `collector` | Eclipse Paho MQTT, pgx | Subscribe to MQTT, batch-insert into TimescaleDB, emit state-change events |

MQTT topic convention: `factory/{line}/{machine}/{metric}`

## Analytics – Python (`analytics/`)

| Concern        | Choice                                               |
| -------------- | ---------------------------------------------------- |
| API            | FastAPI, Uvicorn                                     |
| Data           | pandas, SQLAlchemy / psycopg                         |
| ML             | scikit-learn (anomaly detection, failure prediction) |
| Vision (later) | ONNX Runtime for defect image classification         |
| Packaging      | uv                                                   |

## Web (`web/`)

| Concern        | Choice                                                                                                                                  |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Build          | Vite, TypeScript, pnpm                                                                                                                  |
| Lint / format  | Biome (root `biome.json`)                                                                                                               |
| UI             | shadcn/ui (Radix) + Tailwind CSS v4, components in `src/components/ui`                                                                      |
| Theme          | shadcn tokens + extra semantic colors `info` / `success` / `warning` / `neutral` (`src/styles.css`; source [theme.css](theme.css))            |
| Routing        | TanStack Router, file-based (`src/routes/`); feature-private code in `routes/<feature>/-lib/`                                           |
| Data | TanStack Query; `fetch` wrapper (`@/lib/api`) with token refresh |
| API types | openapi-typescript from the OpenAPI spec (`pnpm gen:api` → `src/lib/api-types.ts`) |
| Tables / Forms | TanStack Table, TanStack Form                                                                                                           |
| Charts         | ECharts (OEE, SPC, Pareto, trends)                                                                                                      |
| Real-time      | STOMP over WebSocket (with MES)                                                                                                         |
| Scanning       | Browser camera QR/barcode scanning for lots and work orders                                                                             |

### Web config

Same two variables as the backend, from the shared `.env` (runtime `config.js` override so one
Docker image works anywhere):

| Variable  | Default                     | Used for                                                       |
| --------- | --------------------------- | -------------------------------------------------------------- |
| `WEB_URL` | `http://localhost:5173/web` | path → Vite `base` + TanStack Router `basepath`                |
| `API_URL` | `http://localhost:8080/api` | base for REST (`/v1`), GraphQL (`/graphql`), WebSocket (`/ws`) |

## URLs (configurable)

Two variables in the shared `.env`; each prefix is inferred from the URL path:

```env
WEB_URL=http://localhost:5173/web     # prod: https://erp.example.com/web
API_URL=http://localhost:8080/api     # prod: https://erp.example.com/api (or https://api.example.com/)
```

Backend: `ApiPathCustomizer` reads `API_URL` and sets the servlet context path to its path (`/api`). The URLs are also used for:

- CORS – allowed origin = origin of `WEB_URL`
- Swagger – OpenAPI `servers` = `API_URL`
- Absolute links in emails / PDFs / password reset = `WEB_URL` + route

Sub-paths are fixed in code:

```yaml
spring:
  graphql:
    path: /graphql
springdoc:
  api-docs:
    path: /docs/openapi
  swagger-ui:
    path: /docs
```

| Endpoint          | Default URL         |
| ----------------- | ------------------- |
| Web app           | `/web/`             |
| REST              | `/api/v1/...`       |
| GraphQL           | `/api/graphql`      |
| GraphiQL (dev)    | `/api/graphiql`     |
| Swagger UI        | `/api/docs`         |
| OpenAPI JSON      | `/api/docs/openapi` |
| WebSocket (STOMP) | `/api/ws`           |

Change `API_URL` / `WEB_URL` in `.env` (other host, port or prefix) without touching code.
Swagger UI and GraphiQL are behind auth (or disabled) in production.

## Infrastructure (`compose.yaml`)

| Service     | Image                                | ~Size                |
| ----------- | ------------------------------------ | -------------------- |
| Database    | `timescale/timescaledb:latest-pg17`  | ~400 MB              |
| MQTT broker | `eclipse-mosquitto:2`                | ~15 MB               |
| Backend     | `eclipse-temurin:21-jre-alpine` base | ~200 MB              |
| Analytics   | `python:3.12-slim` base              | ~500 MB with ML libs |
| Edge        | `scratch` / `distroless` Go binaries | ~20 MB               |

Estimated total footprint: **5–7 GB** (images, dependencies, data, node_modules).

## Repo conventions

- Flat layout: one top-level folder per app (`api/`, `web/`, `edge/`, `analytics/`); shadcn components live in `web/`.
- `Makefile` is the entry point (`make` lists targets): `make env`, `make up` / `down` / `logs s=api`,
  `make dev:api`, `make dev:web`, `make psql`, `make migrate`, `make seed`, `make check` / `fix`.
- `.env.example` documents every variable (commented); `make env` copies it to `.env`, which the
  Makefile and `compose.yaml` both read. `compose.prod.yaml` for production.
- `AGENTS.md` at the root: layout, commands and architecture rules for coding agents.

## Tooling

- JDK 25+ (system JDK 27 works; the build targets 25)
- Go 1.23+, Python 3.12 + uv, Node 22 + pnpm
- DBeaver or pgAdmin for DB access
- GitHub Actions: build + test each component
