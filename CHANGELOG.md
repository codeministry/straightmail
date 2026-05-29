# Changelog

All notable changes to the straightmail monorepo are documented in this file.
For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [
`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.5.0] - 2026-05-29

For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md) and [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

### Added

- Multi-tenancy support: per-tenant SMTP, API keys, and JWT tenant resolution
- No-authentication (`auth.mode=none`) and API-key (`auth.mode=api-key`) modes alongside OIDC
- Modular Docker Compose stacks (`docker/oidc-sqlite.yml`, `api-key-sqlite.yml`, etc.)
- Feature branch CI workflow (`feature.yml`) for `feature/**`, `fix/**`, `chore/**`
- Monorepo full-build workflow (`monorepo.yml`) producing a combined backend+frontend JAR image
- Frontend standalone build artifact (`frontend-dist`) uploaded by `frontend.yml` on every `main` push

### Changed

- Replaced MailHog with Mailpit across all Docker Compose configs and integration tests
- Fixed `frontend.yml` path filters (`../../frontend/**` → `frontend/**`) so the workflow now triggers correctly
- Added `if: github.ref == 'refs/heads/main'` guard to `backend.yml` docker job (was pushing on PRs)
- SQLite replaces PostgreSQL as default database backend (Liquibase migrations updated)
- Keycloak hostname handling refactored; JWK/issuer URLs split for OIDC setups

## [0.4.0] - 2026-05-21

Security release. For component-specific changes see [`backend/CHANGELOG.md`](backend/CHANGELOG.md).

### Security

- Upgraded off end-of-life Spring Boot 2.5.6 → 3.5.14 (Java 17 → 21, Temurin)
- Patched known-vulnerable dependencies: jsoup 1.18.3 (`Whitelist` → `Safelist`), springdoc-openapi 2.8.17, MapStruct 1.6.3, Lombok 1.18.46
- Supply-chain hardening: published images ship an SBOM and build provenance attestation
- Swagger no longer exposes a hardcoded server URL; the OpenAPI host follows the request host (#2)

### Added

- `EmailRequest.senderName` for `Display Name <addr@example.com>` From headers (#1)
- GitHub Actions CI replacing GitLab CI: build/test on PR + `master`, multi-arch (amd64/arm64) images to `ghcr.io`

### Changed

- Migrated `javax.*` → `jakarta.*`; added `lombok-mapstruct-binding`; Gradle wrapper → 8.10.2
- Switched tests from embedded MongoDB to GreenMail; Dockerfile base → `eclipse-temurin:21-jre`
- Rewrote README (badges, migration banner, contributing/support, disclaimer, Apache-2.0)

### Removed

- MongoDB integration — file-based templates only

## [0.2.0] - 2026-03-16

### Structure

- Moved backend source into `backend/` subdirectory
- Added Angular admin console as `frontend/` via `git subtree`
- Integrated backend and frontend into a unified Gradle build:
  - Added `npmInstall`, `npmBuild`, `copyClientDist` Gradle tasks with `SKIP_FRONTEND_BUILD` flag
  - Added `SpaController` for Angular SPA routing fallback under the backend
  - Added Angular `proxy.conf.json` routing `/api` to backend port during development

### Infrastructure

- Added `docker-compose.yml` with PostgreSQL (incl. health check), backend, and frontend services
- Enabled multi-platform Docker builds (amd64 + arm64) via QEMU and Docker Buildx in CI

### CI/CD

- Added initial GitHub Actions workflows for build, test, and Docker image publishing (`main-deploy.yml`)
- Split into dedicated `backend.yml` and `frontend.yml` workflows; removed obsolete `pr-validation.yml` and
  `release-deploy.yml`

### Documentation

- Added `CONTRIBUTING.md` with guidelines for issues, features, and pull requests
- Added `LICENSE` (Apache 2.0)
- Updated root `README.md` with feature description, UI previews, technology stack badges
