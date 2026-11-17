# ERP Lab

Mini ERP + MES for a small manufacturer: Spring Boot core, React web, Go edge, Python analytics.
See [docs/PLAN.md](docs/PLAN.md) and [docs/TECH_STACK.md](docs/TECH_STACK.md).

## Quick start

Needs Docker, JDK 25+, Node 22+ and pnpm.

```sh
make env        # create .env (change DB_PORT if 5432 is taken)
make install
make up         # TimescaleDB + Mosquitto
make dev:api    # http://localhost:8080/api  (Swagger: /api/docs)
make dev:web    # http://localhost:5173/web  (sign in: admin / SEED_PASSWORD, default admin12345)
```

## What's in it

ERP (phases 1–5 in the plan): users and roles, master data with BOMs, inventory at moving average cost,
purchase orders with goods receipts, sales orders with deliveries, invoices and payments, double-entry
accounting posted automatically from every business event, dashboard, Excel exports and invoice PDFs.
MES (production, machine monitoring, OEE, quality) comes next.

`make test` runs the domain tests, the module boundary test and a full context test against TimescaleDB (Testcontainers).
