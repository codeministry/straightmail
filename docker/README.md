# straightmail — Docker Compose Modes

This folder contains four pre-configured Compose stacks, one per operating mode.
Each stack starts fully without any manual configuration: databases, Keycloak (including
an imported realm), Mailpit, and the app are ready to use immediately.

## Prerequisites (Build)

The backend and frontend images are built from source. The Dockerfile expects a
pre-compiled JAR — **before the first `docker compose up`, a one-time build is required:**

```bash
# Build the backend JAR (from the repo root):
cd backend && ./gradlew bootJar && cd ..

# Frontend can optionally be pre-built (docker compose build handles it automatically):
# cd frontend && npm install && npm run build && cd ..
```

Afterwards `docker compose -f compose/<mode>.yml up` works directly. After code changes
to the backend, run `./gradlew bootJar` again, then `docker compose ... build` or
`up --build`.

---

## System Requirements

- **Docker** (Desktop or Engine) — required for all modes
- All stacks are started from the **repository root** (not from `compose/`).

---

## Overview

| File                | Auth            | Database   | Templates       | Additional Services           |
|---------------------|-----------------|------------|-----------------|-------------------------------|
| `minimal.yml`       | None            | —          | Classpath files | Mailpit                       |
| `apikey-sqlite.yml` | API-Key         | SQLite     | Database (CRUD) | Mailpit                       |
| `oidc-sqlite.yml`   | OIDC / Keycloak | SQLite     | Database (CRUD) | Mailpit, Keycloak             |
| `oidc-postgres.yml` | OIDC / Keycloak | PostgreSQL | Database (CRUD) | Mailpit, Keycloak, PostgreSQL |

---

## Modes

### `minimal.yml` — No Auth, File Templates

The simplest stack for quickly testing the API with classpath-based FreeMarker templates.
No Spring `database` profile → no JPA, no tenant context, all endpoints open.

```bash
docker compose -f compose/minimal.yml up
```

| Service        | URL                        |
|----------------|----------------------------|
| Backend API    | http://localhost:50003/api |
| Mailpit Web UI | http://localhost:8025      |

No credentials or headers required.

---

### `apikey-sqlite.yml` — API-Key Auth, SQLite, DB Templates

Full stack with API-key authentication and tenant management.
The `acme` tenant is automatically created in the DB on backend startup from the
`TENANTS_CONFIG_*` environment variables (`TenantReconciliationService`).

```bash
docker compose -f compose/apikey-sqlite.yml up
```

| Service        | URL                        | Credentials                                              |
|----------------|----------------------------|----------------------------------------------------------|
| Angular UI     | http://localhost:4200      | —                                                        |
| Backend API    | http://localhost:50003/api | `X-API-KEY: dev-api-key-change-me` + `X-Tenant-ID: acme` |
| Mailpit Web UI | http://localhost:8025      | —                                                        |

**Important env vars to override:**

```bash
# Set a custom API key (required for any non-local environment):
API_KEY=my-secure-key docker compose -f compose/apikey-sqlite.yml up

# Or via a .env file in the repo root:
echo "API_KEY=my-secure-key" > .env
docker compose -f compose/apikey-sqlite.yml up
```

Additional tenants can be configured by adding `TENANTS_CONFIG_1_*`, `TENANTS_CONFIG_2_*`,
etc. to the `apikey-sqlite.yml`.

---

### `oidc-sqlite.yml` — OIDC Auth, SQLite, Keycloak

Full OIDC stack. Keycloak starts with the pre-configured `straightmail` realm
(frontend, `tenant_id` mapper, `ADMIN` role, test user). No manual Keycloak setup required.

```bash
docker compose -f compose/oidc-sqlite.yml up
```

| Service        | URL                        | Credentials                 |
|----------------|----------------------------|-----------------------------|
| Angular UI     | http://localhost:4200      | `admin` / `admin`           |
| Backend API    | http://localhost:50003/api | Bearer token (via Keycloak) |
| Keycloak Admin | http://localhost:8090      | `admin` / `admin`           |
| Mailpit Web UI | http://localhost:8025      | —                           |

The test user `admin` has:

- Attribute `tenant_id=acme` (passed as a JWT claim)
- Realm role `ADMIN`

The backend waits for Keycloak via a `healthcheck` before starting.

---

### `oidc-postgres.yml` — OIDC Auth, PostgreSQL, Keycloak

Same as `oidc-sqlite.yml`, but with PostgreSQL instead of SQLite. Suitable for testing
multi-replica setups, HikariCP connection pooling, and full SQL feature support.

```bash
docker compose -f compose/oidc-postgres.yml up
```

| Service        | URL                        | Credentials                                        |
|----------------|----------------------------|----------------------------------------------------|
| Angular UI     | http://localhost:4200      | `admin` / `admin`                                  |
| Backend API    | http://localhost:50003/api | Bearer token (via Keycloak)                        |
| Keycloak Admin | http://localhost:8090      | `admin` / `admin`                                  |
| PostgreSQL     | `localhost:5432`           | `straightmail` / `straightmail` DB: `straightmail` |
| Mailpit Web UI | http://localhost:8025      | —                                                  |

---

## General Notes

### Stop a stack and remove volumes

```bash
# Stop (keep volumes):
docker compose -f compose/<mode>.yml down

# Stop and remove all volumes (clean restart):
docker compose -f compose/<mode>.yml down -v
```

### View logs for a specific service

```bash
docker compose -f compose/<mode>.yml logs -f backend
docker compose -f compose/<mode>.yml logs -f keycloak
```

### Clear the Docker build cache (after backend/frontend changes)

```bash
docker compose -f compose/<mode>.yml build --no-cache
docker compose -f compose/<mode>.yml up
```

### Encryption Key

All stacks use the same development key (`ENCRYPTION_KEY`) from `application.yml`.
This key **must not** be used for production or shared environments. For staging/prod,
set a dedicated 32-byte Base64 key:

```bash
# Generate a new key:
openssl rand -base64 32
```
