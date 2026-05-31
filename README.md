# <img src="frontend/public/logos/logo.svg" alt="straightmail" width="34"> straightmail

[![Monorepo Full Build](https://github.com/encircle360-oss/straightmail/actions/workflows/monorepo.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/monorepo.yml)
[![Backend Build](https://github.com/encircle360-oss/straightmail/actions/workflows/backend.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/backend.yml)
[![Frontend Build](https://github.com/encircle360-oss/straightmail/actions/workflows/frontend.yml/badge.svg)](https://github.com/encircle360-oss/straightmail/actions/workflows/frontend.yml)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21.2-DD0031?logo=angular&logoColor=white)](https://angular.io)
[![Matrix](https://img.shields.io/badge/Matrix-Join%20Chat-blue?logo=matrix&logoColor=white)](https://matrix.to/#/#oss:encircle360.com)

**straightmail** is a mail sending API with template and i18n support, combined with a modern Angular administration
console for managing email templates, rendering, and sending operations.

<img src="frontend/docs/files/dashboard.png" alt="Dashboard" width="800">

<table width="800">
  <tr>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/templates.png" alt="Templates" width="190">
      <br><sub>Templates</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/edit%20template.png" alt="Edit Template" width="190">
      <br><sub>Edit Template</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/render%20preview%20html.png" alt="Render Preview HTML" width="190">
      <br><sub>Render Preview</sub>
    </td>
    <td valign="top" width="25%" rowspan="2" align="center" valign="top">
      <img src="frontend/docs/files/mobile.png" alt="Mobile View" width="100">
      <br><sub>Mobile View</sub>
    </td>
  </tr>
  <tr>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/send%20mail%20template.png" alt="Send Mail Template" width="190">
      <br><sub>Send Mail</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/help.png" alt="Help Panel" width="190">
      <br><sub>Help Panel</sub>
    </td>
    <td valign="top" width="25%" align="center">
      <img src="frontend/docs/files/mail.png" alt="Mail Output" width="190">
      <br><sub>Mail Output</sub>
    </td>
  </tr>
</table>

## straightmail Monorepo

This repository contains the straightmail backend and admin frontend as a monorepo.

```
straightmail/
├── backend/          # Spring Boot Application (Java 21)
├── frontend/         # Angular Admin Frontend
├── docker/           # Mode-specific Docker Compose stacks
├── .github/workflows # CI/CD pipelines
└── README.md
```

## Getting Started

### Local Development

**One-time setup — backend local config:**

The backend reads local overrides from `backend/src/main/resources/application-local.yml`,
which is gitignored. Create it from the example and generate an encryption key:

```bash
cd backend/src/main/resources
cp application-local.yml.example application-local.yml

# Generate a random AES-256 key and paste it into the `encryption.key` value
openssl rand -base64 32
```

Open `application-local.yml` and replace `encryption.key: CHANGE_ME_GENERATE_RANDOM_KEY`
with the generated value. Adjust SMTP, auth, and tenant settings as needed.

**Terminal 1 — Backend:**

```bash
cd backend
./gradlew bootRun
# API available at http://localhost:50003/api/
```

**Terminal 2 — Frontend:**

```bash
cd frontend
npm install
npm start
# Dev server at http://localhost:4200 (proxies /api → :50003)
```

### Docker Compose

The `docker/` directory contains four ready-to-use Compose stacks:

| Stack               | Auth            | Database   | Templates       |
|---------------------|-----------------|------------|-----------------|
| `minimal.yml`       | None (open)     | —          | File-based only |
| `apikey-sqlite.yml` | API-Key         | SQLite     | File + Database |
| `oidc-sqlite.yml`   | OIDC / Keycloak | SQLite     | File + Database |
| `oidc-postgres.yml` | OIDC / Keycloak | PostgreSQL | File + Database |

**Prerequisite:** Build the backend JAR first:

```bash
cd backend && ./gradlew bootJar
```

Start a stack (example — OIDC + SQLite):

```bash
cd docker && docker compose -f oidc-sqlite.yml up
```

> **Encryption key:** Each stack sets `services.backend.environment.ENCRYPTION_KEY`
> in its compose file. The default is a placeholder — generate a real 32-byte key
> with `openssl rand -base64 32` and replace it before any shared or production use.
> See [docker/README.md → Encryption Key](docker/README.md#encryption-key).

See [docker/README.md](docker/README.md) for detailed configuration per stack.

### Full Production Build (Backend serves Frontend)

```bash
cd backend
./gradlew build
# Angular is built and embedded into the Spring Boot JAR
```

Skip the Angular build (backend-only):

```bash
SKIP_FRONTEND_BUILD=true ./gradlew build
```

## Template Sources

straightmail supports three independent template sources that can be combined:

| Source   | Description                                                     | Enabled by default                     |
|----------|-----------------------------------------------------------------|----------------------------------------|
| Database | Templates stored in SQLite / PostgreSQL (full CRUD via UI/API)  | Yes (profile `database`)               |
| File     | Read-only `.ftl` files mounted from the host filesystem         | Yes (`TEMPLATE_FILE_ENABLED=true`)     |
| Git-sync | Templates cloned from a per-tenant Git repository on a schedule | Yes (`TEMPLATE_GIT_SYNC_ENABLED=true`) |

### File Templates

Place FreeMarker templates under `templates/{tenant-id}/` in the project root. The directory is mounted into the
container at `/resources/templates`.

```
templates/
├── acme-1/
│   ├── welcome.ftl           # HTML body
│   ├── welcome_subject.ftl   # Subject line
│   └── welcome_plain.ftl     # Plain-text body (optional)
└── acme-2/
    └── ...
```

| Environment variable    | Default                | Description                          |
|-------------------------|------------------------|--------------------------------------|
| `TEMPLATE_FILE_ENABLED` | `false`                | Enable file-based template source    |
| `TEMPLATE_FILE_PATH`    | `/resources/templates` | Root directory scanned for templates |

### Git-Sync Templates

When enabled, the backend periodically clones a Git repository per tenant and loads `.ftl` files from the working tree.
The Git repo URL and optional access token are configured per tenant via the UI or API (stored encrypted in the
database).

| Environment variable        | Default       | Description                                          |
|-----------------------------|---------------|------------------------------------------------------|
| `TEMPLATE_GIT_SYNC_ENABLED` | `false`       | Enable Git-sync template source                      |
| `GIT_SYNC_CRON`             | `0 0 * * * *` | Cron expression for the sync job (hourly by default) |
| `GIT_SYNC_LOCK_MIN`         | `PT50M`       | Minimum lock duration (ShedLock)                     |
| `GIT_SYNC_LOCK_MAX`         | `PT1H`        | Maximum lock duration (ShedLock)                     |

Both sources are enabled in all Docker Compose variants. To disable one, set the corresponding variable to `"false"`.

## Authentication

straightmail supports two authentication modes configured at startup:

| Mode       | `AUTH_MODE` value  | Description                                                                                                                                              |
|------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| OIDC / JWT | `oidc` (default)   | JWT from an OIDC provider (e.g. Keycloak). Roles are derived from `realm_access.roles` → `ROLE_*`. The tenant is resolved from a configurable JWT claim. |
| API-Key    | `api-key`          | `X-API-KEY` request header carrying a global or per-tenant SHA-256 hash. The tenant is determined by a key lookup in the database.                       |
| None       | `none`             | All endpoints are open. Suitable for internal services with network-level protection.                                                                     |

Key environment variables:

| Variable               | Default | Description                                                                |
|------------------------|---------|----------------------------------------------------------------------------|
| `AUTH_MODE`            | `oidc`  | Authentication mode: `oidc`, `api-key`, or `none`                          |
| `JWT_TENANT_CLAIM`     | —       | JWT claim that contains the single tenant ID                               |
| `JWT_TENANT_IDS_CLAIM` | —       | JWT claim that contains a list of accessible tenant IDs                    |
| `API_KEY`              | —       | Global API key (SHA-256 hash) used when the `database` profile is inactive |

## Operation Modes (Spring Profiles)

The backend behaviour is controlled by Spring profiles set via `SPRING_PROFILES_ACTIVE`:

| Profile      | Effect                                                                                                                                                                                                                                |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `database`   | Activates database storage for templates and SMTP config, enables the Tenant-CRUD API (`/v1/tenants`), and per-tenant Git-sync. Without this profile the backend runs file-only with a global SMTP config from environment variables. |
| `production` | Disables Swagger UI / API docs (`/swagger-ui`, `/v3/api-docs`), tunes Tomcat thread pool (`max=50`, `min-spare=10`).                                                                                                                  |
| `dev`        | Local development overrides (localhost SMTP relay).                                                                                                                                                                                   |

Example — full local stack:

```bash
SPRING_PROFILES_ACTIVE=dev,database ./gradlew bootRun
```

## Multi-Tenancy

When running with the `database` profile, straightmail is fully multi-tenant:

- **Per-tenant SMTP** — Each tenant configures its own SMTP server, credentials, and sender address via the UI or API.
- **Per-tenant Git-sync** — Each tenant can point to an independent Git repository for template storage, synced on a
  configurable cron schedule (ShedLock protected for HA setups).
- **Infrastructure-as-code provisioning** — Tenants declared under `tenants.config.*` in `application.yml` are
  automatically created or updated at startup (`TenantReconciliationService`). The `default` tenant is never removed,
  enabling zero-touch tenant provisioning without manual API calls.
- **Encryption at rest** — All secrets (SMTP password, Git token, API key) are encrypted using AES-256-GCM by the
  built-in `EncryptionService`. Secrets are never returned in plain text by the API.

## Documentation

- [Backend README](backend/README.md)
- [Frontend README](frontend/README.md)
- [API Endpoints](http://localhost:50003/swagger-ui.html)

## Ports

| Service     | Port(s)     |
|-------------|-------------|
| Backend API | 50003       |
| Management  | 50004       |
| Frontend    | 4200        |
| PostgreSQL  | 5432        |
| Mailpit     | 1025 / 8025 |
| pgAdmin     | 5050        |

## encircle360 OSS Matrix Channel

Join our community for support, discussions, and updates regarding our open-source projects.

[![Matrix](https://img.shields.io/badge/Matrix-Join%20Chat-blue?logo=matrix&logoColor=white)](https://matrix.to/#/#oss:encircle360.com)
