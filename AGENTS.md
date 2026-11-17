# AGENTS.md

Guidance for coding agents (Claude Code etc.) working in this repository.
Plan and decisions: [docs/PLAN.md](docs/PLAN.md), [docs/TECH_STACK.md](docs/TECH_STACK.md). Build the ERP first, then MES.

## Layout

- `api/`: Spring Boot 4.1, Java 25, Maven (`./mvnw`). Package `com.erplab`, one package per bounded context.
- `web/` (`@erp-lab/web`): Vite + React 19 + TanStack Router (file-based, `src/routes/`) + TanStack Query.
  Code private to one feature goes in its route folder under `-lib/` (the `-` keeps the router from treating it
  as a route); code shared across features in `src/components/` or `src/lib/`.
  - shadcn/ui components in `src/components/ui/` (import `@/components/ui/<name>`); add with
    `cd web && pnpm dlx shadcn@latest add <name>`, then fix the generated `from "cn"` imports to
    `from "@/lib/utils"` (shadcn mis-resolves the alias here). Theme in `src/styles.css` (source of truth `docs/theme.css`).
- `edge/` (Go), `analytics/` (Python): later phases.
- Root `package.json` / pnpm workspace only for `web` and Biome; the api is Maven only.

## Commands

`make` lists every target. The Makefile loads `.env`; `make env` creates it from `.env.example`.
- Services: `make up` (TimescaleDB, Mosquitto), `make down`, `make logs s=db`, `make psql`.
- Data: `make db-reset` (drop everything; stop the api first), `make seed` (demo data into an empty database:
  users `manager`/`sales`/`purchasing`/`warehouse`/`accountant` with `SEED_PASSWORD`, ~4 months of history).
  Db data is a bind mount in `.data/db`.
- Apps on the host: `make dev:api` (migrates on start), `make dev:web`.
- `make build`, `make test` (api; Testcontainers needs docker), `make check` / `make fix` (Biome).
- After changing api request/response records: start the api and run `cd web && pnpm gen:api` (regenerates
  `src/lib/api-types.ts`; use them via `Schemas["…"]` from `@/lib/schemas`). Record names must be unique across
  contexts (Swagger names schemas by simple class name).
- CI: `.github/workflows/ci.yml` (api `mvnw verify`, web Biome + build).

## URLs

`WEB_URL` and `API_URL` in `.env` are the public URLs; their paths are the prefixes (`/web`, `/api`).
- Api: `ApiPathCustomizer` sets the servlet context path from `API_URL`; `WebConfig` adds `/v1` to our
  `@RestController`s. So REST is `/api/v1/...`, GraphQL `/api/graphql`, Swagger `/api/docs`,
  OpenAPI `/api/docs/openapi`, health `/api/actuator/health`.
- Web: `vite.config.ts` reads the root `.env`; `src/lib/config.ts` exposes `restUrl`, `graphqlUrl`, `basePath`.

## Api architecture (DDD + light CQRS)

Contexts (`com.erplab.<context>`): `auth`, `masterdata`, `inventory`, `purchasing`, `sales`, `accounting`, `report`
(read-only), plus the shared kernel `common`. Each context:

```
<Context>Api.java, <Event>.java   public API: the only types other contexts may import (module base package)
domain/            aggregates, value objects, repository interfaces (Spring Data)
application/
  command/         <Context>Commands (validated records) + <Context>CommandHandler (@Transactional, one tx per call)
  query/           <Context>Queries: SQL read models via JdbcClient / SqlPage, never change state
http/              thin REST controllers (@Tag, @PreAuthorize)
```

- Business rules live on aggregates (`order.confirm()`, `stockItem.issue()`, `new JournalEntry(...)` checks balance);
  they throw `DomainException` (422), `NotFoundException` (404), `UnauthorizedException` (401).
- Each context's `application.command` / `application.query` packages are a Modulith named interface (`commands`,
  `queries`) for entry points outside the context (the demo seeder in `seed/`); other contexts must still use the
  base-package API and events.
- Cross-context: by id plus the base-package API (`MasterdataApi`) or events. Events are records in the publisher's base
  package (`GoodsReceived`, `BillPaymentMade`, `SalesOrderConfirmed`/`Cancelled` (reservations), `DeliveryShipped`,
  `InvoiceIssued`, `CreditNoteIssued`, `InvoicePaymentReceived`, `StockValueChanged`) handled
  by plain `@EventListener`s **synchronously in the publishing transaction**: a delivery without stock rolls back, and
  `accounting.AutomaticPostings` books every event, so ledger and documents always agree. Dependency direction:
  masterdata ← purchasing, sales ← inventory ← accounting (no cycles; `ModularityTests` enforces it).
- Real-time: the `realtime` context (STOMP over WebSocket, `/api/ws`, bearer token checked on CONNECT) listens to the
  domain events after commit (`ChangeBroadcaster`) and pushes `ChangeNotice(resources)` to `/topic/changes`: only
  web query-key prefixes, never data. To refresh another list on an event, add its key there. The web side is
  `useRealtime()` (`web/src/lib/realtime.ts`, mounted in `routes/_app.tsx`), which invalidates those keys.
- Permissions: `common.security.Permissions` (strings like `sales:write`); roles map to them in `auth.domain.Role`;
  controllers check with `@PreAuthorize("hasAuthority('...')")`. The JWT carries them in the `perms` claim.
- Entities extend `common.domain.BaseEntity` (id, `@Version`, audit columns, soft delete `deletedAt`); child rows
  (order lines, journal lines) are plain entities/embeddables. Money `NUMERIC(18,2)`, quantities `NUMERIC(18,4)`.
- Document numbers from Postgres sequences: `DocumentNumbers.next("SO", "sales_order_number_seq")`.
- Controllers return plain data; `ResponseEnvelopeAdvice` wraps it in `ResponseEnvelope` (`success`/`fail`/`error`);
  lists return `PageResult` (`items`, `total`, `page`, `size`).
- Audit: `common.audit.CommandAudit` logs every successful public command-handler method (not `@EventListener`s);
  fields named `password`/`refreshToken` are stripped.
- Swagger: every controller has `@Tag(name = "<context>")`; add the tag with a description to `TAGS` in `OpenApiConfig`.
- Schema changes only through Flyway (`src/main/resources/db/migration/V<n>__<name>.sql`); Hibernate validates.
- When concatenating SQL text blocks, add an explicit `" "`: text blocks strip the common leading whitespace.

## Web conventions

- Lists: `DataTable` (TanStack Table v9, server paging). Simple master data: `CrudPage` + `EntityForm` (field
  config, TanStack Form). Pickers: `EntitySelect` (loads a list endpoint). Orders: `OrderForm`, `QuantityDialog`.
- Calls: `api` / `post` / `put` / `del` / `download` from `@/lib/api` (bearer token, one refresh on 401, envelope
  unwrapped, `ApiError` with `issues`). Mutations via `useAction` (toasts + query invalidation).
- Permissions: `useCan()("sales:write")`. A page's required permission comes from its menu item in `routes/_app.tsx`;
  the layout shows "No access" otherwise. The api enforces the same permissions.
