# Thin wrapper around docker compose, Maven and pnpm. Run `make` for the list.
# Values from .env are exported to every command (create it with `make env`);
# a variable already set in the shell wins, so .env is rewritten to `?=` first.
.make.env: $(wildcard .env)
	@if [ -f .env ]; then sed -E 's/^([A-Za-z_][A-Za-z0-9_]*)=/\1 ?= /' .env > $@; else : > $@; fi
-include .make.env
export

COMPOSE := docker compose
MVN     := cd api && ./mvnw -B

.DEFAULT_GOAL := help
.PHONY: help env install up down logs ps psql dev\:api dev\:web build test check fix clean

help: ## Show this help
	@awk '/^## --- / { t = $$0; gsub(/^## --- | ---$$/, "", t); printf "\n\033[1m%s\033[0m\n", t; next } \
	  match($$0, /^([a-zA-Z0-9_.-]|\\:)+:.*## /) { \
	  match($$0, /^([a-zA-Z0-9_.-]|\\:)+/); n = substr($$0, 1, RLENGTH); \
	  gsub(/\\:/, ":", n); d = $$0; sub(/.*## /, "", d); printf "  \033[36m%-12s\033[0m %s\n", n, d }' $(MAKEFILE_LIST)

## --- Setup ---
env: ## Create .env from .env.example (never overwrites)
	@if [ -f .env ]; then echo ".env already exists"; else cp .env.example .env && echo "Created .env, edit it"; fi

install: ## Install web dependencies and resolve api dependencies
	pnpm install
	$(MVN) -q dependency:go-offline

## --- Services (docker) ---
up: ## Start db and mqtt
	$(COMPOSE) up -d --wait

down: ## Stop services
	$(COMPOSE) down

logs: ## Follow logs (s=db for one service)
	$(COMPOSE) logs -f $(s)

ps: ## List services
	$(COMPOSE) ps

psql: ## Open psql in the db
	$(COMPOSE) exec db psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

## --- Apps (host) ---
dev\:api: ## Run the api (Spring Boot, migrates on start)
	$(MVN) spring-boot:run

dev\:web: ## Run the web dev server
	pnpm --filter @erp-lab/web dev

## --- Build & quality ---
build: ## Build api jar and web bundle
	$(MVN) -q package -DskipTests
	pnpm -r build

test: ## Run api tests (needs docker for Testcontainers)
	$(MVN) test

check: ## Biome check (web)
	pnpm check

fix: ## Biome fix
	pnpm check:fix

clean: ## Remove build outputs
	$(MVN) -q clean
	rm -rf web/dist
