# SystemGuard — System Architecture

## Vision

SystemGuard is a developer-facing Software Assurance Platform that analyzes software systems
at multiple layers (code, dependency, API, architecture, infrastructure, runtime), correlates
findings, understands application architecture, determines blast radius, explains evidence,
proposes remediation, generates patches, verifies fixes in isolation, and creates PRs only
after successful verification.

**Differentiating Principle:**
> "Do not just find a problem. Understand its context, fix it, and prove that the fix is safe."

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Next.js / TypeScript)                  │
│  Dashboard · Findings · Architecture Graph · Remediation · Policies     │
│  SSE for live updates · TanStack Query for data fetching                │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ REST / SSE
┌──────────────────────────────────▼──────────────────────────────────────┐
│                    CONTROL PLANE (Spring Boot 3.x — Modular Monolith)   │
│                                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌─────────────┐ │
│  │ Project  │ │  Scan    │ │ Finding  │ │ Arch      │ │ Remediation │ │
│  │ Module   │ │ Module   │ │ Module   │ │ Module    │ │ Module      │ │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘ └─────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌─────────────┐ │
│  │ Policy   │ │ Risk     │ │ Verify   │ │ GitHub    │ │ Auth        │ │
│  │ Module   │ │ Module   │ │ Module   │ │ Module    │ │ Module      │ │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘ └─────────────┘ │
│                                                                         │
│  Shared: Events · Domain Models · DTOs · Config · Exception Handling    │
└───┬──────────┬──────────┬──────────────┬────────────────────────────────┘
    │          │          │              │
    ▼          ▼          ▼              ▼
┌────────┐ ┌────────┐ ┌────────┐  ┌───────────┐
│Postgres│ │ Redis  │ │ Kafka  │  │  MinIO    │
│  (SoT) │ │(Cache/ │ │(Async  │  │ (S3-compat│
│        │ │ Locks) │ │ Events)│  │  Storage) │
└────────┘ └────────┘ └────────┘  └───────────┘
                          │
           ┌──────────────┼──────────────────┐
           ▼              ▼                  ▼
    ┌─────────────┐ ┌──────────────┐ ┌─────────────────┐
    │  Scanner    │ │  Scanner     │ │  Verification   │
    │  Workers    │ │  Workers     │ │  Workers        │
    │  (Docker)   │ │  (Docker)    │ │  (Ephemeral     │
    │             │ │              │ │   Docker)       │
    │  Semgrep    │ │  Trivy       │ │                 │
    │  Gitleaks   │ │  Checkov     │ │  Build+Test     │
    │             │ │              │ │  Rescan+Policy  │
    └─────────────┘ └──────────────┘ └─────────────────┘
```

---

## Module Boundaries

### 1. Project Module (`com.systemguard.project`)
- Project / repository registration
- Git repository ingestion (clone, metadata extraction)
- Build/project type detection (Maven, Gradle, npm, Docker, Terraform)
- Repository metadata (languages, frameworks, size)

### 2. Scan Module (`com.systemguard.scan`)
- Scan orchestration (create scan run, dispatch scanner jobs)
- Scanner job lifecycle (PENDING → RUNNING → COMPLETED / FAILED)
- Scanner worker framework (abstract base for all scanners)
- SSE scan progress broadcasting
- Retry / dead-letter handling

### 3. Finding Module (`com.systemguard.finding`)
- Unified Finding model (scanner-agnostic)
- Finding normalization (scanner output → canonical finding)
- Fingerprinting & deduplication
- Finding lifecycle (OPEN → CONFIRMED → MITIGATED → RESOLVED → FALSE_POSITIVE)
- Raw scanner artifact storage (→ MinIO)

### 4. Architecture Module (`com.systemguard.architecture`)
- AST analysis (JavaParser for Java/Spring)
- Application graph (nodes, edges, relationships)
- Service/API/database/Kafka/Redis relationship detection
- Blast radius traversal
- Finding-to-graph mapping

### 5. Risk Module (`com.systemguard.risk`)
- Deterministic contextual risk scoring
- Risk factor composition (reachability, auth boundary, asset type, etc.)
- Risk explanation generation (human-readable WHY)
- No arbitrary AI scores

### 6. Policy Module (`com.systemguard.policy`)
- Architecture policy definition (data/config-driven)
- Policy evaluation engine
- Policy violation reporting
- Built-in policy library

### 7. Remediation Module (`com.systemguard.remediation`)
- Remediation orchestration lifecycle
- LLM provider abstraction (interface-based)
- Context building for LLM (finding + code + architecture)
- Patch generation
- Git branch/worktree management

### 8. Verification Module (`com.systemguard.verification`)
- Ephemeral Docker environment management
- Build execution
- Test execution
- Security rescan in isolation
- Policy re-evaluation
- VERIFIED / REJECTED determination
- Result recording

### 9. GitHub Module (`com.systemguard.github`)
- Repository access (PAT initially, GitHub App later)
- PR creation
- Status checks
- Webhooks (later)

### 10. Auth Module (`com.systemguard.auth`)
- User/organization management
- Authentication (session-based initially)
- Authorization

### 11. Shared / Common (`com.systemguard.common`)
- Domain events
- Exception handling
- Correlation ID propagation
- Structured logging
- DTOs
- Object storage abstraction (S3-compatible)
- Configuration

---

## Data Architecture

### PostgreSQL (Source of Truth)
All domain entities, relationships, findings, policies, verification results.
Flyway for schema migrations.
JSONB for flexible metadata/evidence (bounded size).

### Redis
- Distributed locks (scan dedup, idempotency)
- Short-lived job state caching
- Rate limiting
- SSE connection coordination
- Cache for expensive queries

### Kafka
Asynchronous event-driven orchestration between control plane and workers.
All consumers are idempotent (using eventId + Redis dedup keys).
Dead-letter topics for failed processing.

### MinIO (S3-compatible)
- Raw scanner output artifacts
- Repository snapshots
- Patch files
- Verification logs
- Large evidence files

---

## Security Boundaries

```
┌─────────────────────────────────────────────┐
│           TRUSTED ZONE                      │
│  Control Plane · PostgreSQL · Redis · Kafka │
│  MinIO · Frontend                           │
└─────────────────────┬───────────────────────┘
                      │ Kafka events only
                      │ No direct DB access
┌─────────────────────▼───────────────────────┐
│         SEMI-TRUSTED ZONE                   │
│  Scanner Workers (read-only repo access)    │
│  CPU/mem limited · isolated network         │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│         UNTRUSTED ZONE                      │
│  Verification Workers (execute user code)   │
│  Ephemeral · CPU/mem limited · no host      │
│  Docker socket · no prod secrets · timeout  │
│  Isolated network · non-root · temp FS      │
└─────────────────────────────────────────────┘
```

---

## Technology Stack Summary

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Frontend         | Next.js, TypeScript, Tailwind CSS, TanStack Query |
| Backend          | Java 21, Spring Boot 3.x, Spring Web/Security/Data JPA |
| Database         | PostgreSQL 16                       |
| Cache/Locks      | Redis 7                             |
| Messaging        | Apache Kafka (KRaft mode)           |
| Object Storage   | MinIO (S3-compatible)               |
| Build            | Maven (multi-module)                |
| Containers       | Docker, Docker Compose              |
| Migrations       | Flyway                              |
| SAST             | Semgrep                             |
| Dependencies     | Trivy                               |
| Secrets          | Gitleaks                            |
| IaC              | Checkov (Phase 2)                   |
| AST              | JavaParser (Phase 2)                |
| API Docs         | SpringDoc OpenAPI                   |
| Testing          | JUnit 5, Testcontainers, Mockito    |

---

## Deployment Architecture (Local Dev)

```yaml
# docker-compose.yml services
postgres:     PostgreSQL 16
redis:        Redis 7
kafka:        Apache Kafka (KRaft single-node)
minio:        MinIO (S3-compatible)
backend:      SystemGuard Control Plane (Spring Boot)
frontend:     SystemGuard Dashboard (Next.js dev server)
```

For production: Kubernetes (Phase 3+), with Helm charts.

---

## Observability Strategy

| Concern        | Approach                                  |
|----------------|-------------------------------------------|
| Logging        | Structured JSON (SLF4J + Logback)         |
| Correlation    | correlationId propagated via MDC + Kafka headers |
| Metrics        | Micrometer → Prometheus endpoint          |
| Tracing        | OpenTelemetry (interface ready, wired in Phase 3) |
| Health         | Spring Boot Actuator /health, /ready      |
| Audit          | audit_logs table for security-sensitive ops |
