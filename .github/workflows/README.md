# CI / CD Workflows

straightmail uses five GitHub Actions workflows to cover the full development and release lifecycle —
from feature branch validation through to production image publishing with supply-chain attestations.

## Overview

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| Feature CI | [`feature.yml`](feature.yml) | push to `feature/**`, `fix/**`, `chore/**` | Validate before a PR is opened |
| Backend CI | [`backend.yml`](backend.yml) | push / PR to `main` (`backend/**`) | Build, test, publish backend image |
| Frontend CI | [`frontend.yml`](frontend.yml) | push / PR to `main` (`frontend/**`) | Unit tests + Playwright E2E |
| Monorepo CI | [`monorepo.yml`](monorepo.yml) | push to `main` | Full build + combined image |
| Release | [`release.yml`](release.yml) | push to `master` or tag `v*.*.*` | Production image with SBOM & provenance |

---

## Workflows

### `feature.yml` — Feature Branch CI

Runs on every push to `feature/**`, `fix/**`, or `chore/**`. Ensures that both backend and
frontend build and pass all tests before a pull request is opened. Test reports (JaCoCo,
Playwright) are uploaded as artifacts on failure for quick diagnosis.

**Jobs:** `backend` (build + test) · `frontend` (unit + E2E)  
**Artifacts:** backend test report · `playwright-report` (on failure)

---

### `backend.yml` — Backend CI

Runs on push and pull requests to `main` that touch files under `backend/**`. Builds the JAR
with Java 21 and Gradle, executes the test suite with JaCoCo coverage reporting, and — on
direct pushes to `main` — publishes a backend-only multi-arch Docker image to `ghcr.io`.

**Jobs:** `build` · `test` (JaCoCo) · `docker` (main branch only)  
**Image:** `ghcr.io/encircle360-oss/straightmail` — platforms `linux/amd64`, `linux/arm64`

---

### `frontend.yml` — Frontend CI

Runs on push and pull requests to `main` that touch files under `frontend/**`. Executes Vitest
unit tests and Playwright E2E tests (against the `:4299` e2e serve target). Playwright reports
are retained for 30 days.

**Jobs:** `test` (Vitest unit + Playwright E2E)  
**Artifacts:** `playwright-report` (30-day retention)

---

### `monorepo.yml` — Monorepo CI

Runs on every push to `main`. Builds the full production monorepo — Angular frontend embedded
into the Spring Boot JAR via `./gradlew build` — runs independent backend and frontend test
jobs, then publishes the combined image to `ghcr.io`. This is the image referenced by the
Docker Compose stacks in `docker/`.

**Jobs:** `backend` · `frontend` · `docker-combined`  
**Image:** `ghcr.io/encircle360-oss/straightmail` — platforms `linux/amd64`, `linux/arm64`

---

### `release.yml` — Release

Triggered on push to `master` and on semantic version tags (`v*.*.*`). Builds the production
`bootJar` and publishes a multi-arch image to `ghcr.io` tagged with the full semver, major,
minor, and `latest`. Includes build provenance and SBOM attestations for supply-chain
transparency. Only one release job runs at a time (concurrency guard, no mid-flight cancellation).

**Jobs:** `release`  
**Image tags:** `0.5.0` · `0.5` · `0` · `latest`  
**Security:** SBOM + build provenance via `actions/attest-build-provenance`

---

## Required Permissions & Secrets

No repository secrets need to be configured manually.

| Requirement | Used by | Details |
|---|---|---|
| `GITHUB_TOKEN` | all `docker` / `release` jobs | Auto-provided by GitHub — used to push images to `ghcr.io` |
| `packages: write` | `backend.yml`, `monorepo.yml`, `release.yml` | Declared in each workflow's `permissions:` block |
| `id-token: write` | `release.yml` | Required for SBOM and provenance attestations |
| `attestations: write` | `release.yml` | Required for `actions/attest-build-provenance` |
