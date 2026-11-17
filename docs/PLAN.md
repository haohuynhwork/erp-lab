# ERP Lab – Plan

Goal: build a portfolio-grade mini **ERP + MES** for a small manufacturer, learning
Spring Boot (Java), Go and Python along the way. See [TECH_STACK.md](TECH_STACK.md).

## Notes

- **Build ERP first**, then MES.
- **Backend: pragmatic DDD** – bounded context per module, rich aggregates only where invariants matter (see TECH_STACK.md).
- **Web: TanStack** (Router, Query, Table, Form) + **shadcn/ui** + Tailwind.

## Scope

**ERP** – business side: master data, inventory, purchasing, sales, accounting, reports.
**MES** – shop floor: production orders, work orders, machine monitoring, OEE, quality, traceability.

Integration flow:

```
Sales order ──► Production order (MES) ──► Work orders per routing step
                     │                            │
     Purchasing ◄── material shortage      machine data (Go/MQTT)
                     │                            │
 Inventory ◄── material issue / finished goods receipt
                     │
 Accounting ◄── journal entries (COGS, inventory value)
```

## Repository layout

```
erp-lab/
├── api/              Spring Boot (ERP + MES core)
├── web/              React + Vite + TanStack + shadcn/ui
├── edge/             Go – simulator/, collector/ (MQTT → TimescaleDB)
├── analytics/        Python FastAPI
├── docs/             plan, tech stack, theme, ADRs
├── .env.example
├── compose.yaml / compose.prod.yaml
├── Makefile
└── AGENTS.md
```

## Phases

### Phase 0 – Setup (week 1)

- [ ] Free disk space on `/` for Docker (npm, gradle, `~/.cache` ≈ 11 GB)
- [ ] Install JDK 25, Go, Python 3.12 + uv, Node 22 + pnpm
- [x] Repo skeleton (`api/`, `web/`), `.gitignore`, README, `AGENTS.md`
- [x] `Makefile`, `.env.example`, Biome
- [x] `compose.yaml`: TimescaleDB, Mosquitto
- [x] Spring Boot skeleton: Flyway, OpenAPI, `ResponseEnvelope` + global error handling, audit base entity
- [ ] CI: GitHub Actions build + test

ERP first (phases 1–5), then MES (6–10).

### Phase 1 – Foundation ✅

- [x] Auth: users, roles → permissions, JWT access/refresh, `@PreAuthorize`, admin seeded from `SEED_PASSWORD`
- [x] Master data: products (types, BOM), units, categories, warehouses, partners (customer / supplier / both)
- [x] API foundation: `WEB_URL` / `API_URL` config (prefixes inferred), REST `/api/v1`, GraphQL endpoint, Swagger UI / OpenAPI, CORS
- [x] Web shell: login, token refresh, layout, menu and page access by permission, CRUD pages
- [x] Generated API types (`pnpm gen:api`, openapi-typescript)

### Phase 2 – Inventory & Purchasing ✅

- [x] Stock per product × warehouse at moving average cost; movement ledger (receipt / issue / transfer / count)
- [x] Optimistic locking on stock, never negative, low-stock flag (below product minimum)
- [x] Purchase orders → partial goods receipts → stock increase (event `GoodsReceived`)
- [x] Document state machine (Draft → Confirmed → Done → Cancelled)

### Phase 3 – Sales ✅

- [x] Sales order (VAT) → partial deliveries → invoice of delivered quantities → customer payments
- [x] Stock decrease via `DeliveryShipped`, fails as a whole when stock is short
- [x] Quotations (draft orders with valid-until date and PDF), stock reservation on confirm
- [x] Credit notes against invoices
- [ ] Sales order can trigger a production order (with MES)

### Phase 4 – Accounting ✅

- [x] Chart of accounts (seeded accounts for automatic postings)
- [x] Auto journal entries from goods receipt, delivery (COGS), invoice (revenue + VAT), payments, stock adjustments
- [x] Receivables / payables per partner, supplier payments, manual entries, reversals
- [x] Trial balance
- [x] Profit and loss, balance sheet
- [x] Supplier bills matched to receipts, bill payments (replace free-standing supplier payments)
- [ ] VAS chart (Circular 200/133)

### Phase 5 – Reports ✅

- [x] Dashboard: sales, receivable / overdue, payable, inventory value, open orders, 12-month chart, top products
- [x] Excel exports (stock, orders, invoices, trial balance), invoice PDF

### Phase 6 – Production / MES core

- [ ] MES master data: machines, production lines, routings (operations)
- [ ] Production orders from sales orders or manual
- [ ] Explode into work orders per routing step
- [ ] Material issue from inventory (BOM), finished goods receipt
- [ ] Shop-floor screen: start / pause / finish job, report good & scrap qty
- [ ] QR code labels for work orders and lots

### Phase 7 – Machine monitoring

- [ ] Go simulator: N machines publishing status, cycles, temperature, alarms over MQTT
- [ ] Go collector: MQTT → TimescaleDB hypertables (batched)
- [ ] Machine state changes → backend → WebSocket
- [ ] Live machine status board (React + ECharts)

### Phase 8 – OEE & downtime

- [ ] OEE = availability × performance × quality, per machine / shift / line
- [ ] Downtime reason codes, operator reason entry
- [ ] Pareto chart of downtime, OEE trend dashboard

### Phase 9 – Quality & traceability

- [ ] Inspection plans, inspection results, defect codes
- [ ] SPC control charts (X̄-R)
- [ ] Lot / serial genealogy: material lot → work order → finished lot
- [ ] Forward / backward trace search

### Phase 10 – Analytics

- [ ] FastAPI service reading TimescaleDB
- [ ] OEE analytics and shift comparison
- [ ] Anomaly detection / failure prediction on sensor data (scikit-learn)
- [ ] Expose predictions to backend + dashboard

### Phase 11 – Polish

- [x] Testcontainers: context + migrations, HTTP flow test (purchase → cash, ledger checks); domain unit tests
- [x] Seed demo data (`make seed`), audit log, CI (GitHub Actions)
- [ ] `compose.prod.yaml`, Dockerfiles, architecture diagram, demo video
- [ ] Optional: deploy to a VPS

## Key design decisions

| Decision             | Choice                               | Reason                                                                                       |
| -------------------- | ------------------------------------ | -------------------------------------------------------------------------------------------- |
| Architecture         | Modular monolith                     | Simple to run; modules can be split later                                                    |
| Domain design        | DDD + light CQRS                     | Bounded contexts, command/query handlers, rich aggregates for core invariants                |
| API style            | REST + GraphQL                       | REST for commands/CRUD (Swagger-documented); GraphQL for flexible reads, dashboards, reports |
| Module communication | Spring events (synchronous)          | Posting runs in the business transaction, so stock and ledger always match the documents     |
| Stock consistency    | Optimistic locking + movement ledger | Stock = sum of movements, auditable                                                          |
| Time-series          | TimescaleDB in same Postgres         | One database to operate                                                                      |
| Deletes              | Soft delete + audit fields           | Business records must stay traceable                                                         |

## Stretch goals

- Multi-company / multi-warehouse
- Vietnam e-invoice integration
- Redpanda event bus, split MES into its own service
- Defect image classification (ONNX)
- Mobile shop-floor app (PWA)
