# ADR-001: Modular Monolith Control Plane

## Status
**Accepted**

## Date
2026-09-30

## Context

SystemGuard requires a backend that:
1. Orchestrates multiple scanner integrations (Semgrep, Trivy, Gitleaks, Checkov)
2. Manages complex domain workflows (scan → finding → remediation → verification)
3. Maintains a unified data model across all modules
4. Supports Kafka-based async communication with isolated workers
5. Needs to be developed and iterated on rapidly by a small team
6. Must be production-quality but not prematurely distributed

We considered three architectural options:

### Option A: Microservices from Day 1
- Separate services per domain (project-service, scan-service, finding-service, etc.)
- Independent deployments, databases, teams

**Rejected because:**
- Premature complexity for an MVP/Phase 1
- Cross-service transactions and consistency are hard
- Operational overhead of managing 8+ services with a small team
- Domain boundaries are not yet stable enough

### Option B: Simple Monolith
- Single module, flat package structure
- All code in one Maven module

**Rejected because:**
- No enforced boundaries → domain coupling
- Harder to extract modules later
- Harder to reason about which code belongs where

### Option C: Modular Monolith (Selected)
- Single deployable Spring Boot application
- Multiple Maven modules with explicit dependencies
- Clear package boundaries per domain
- Shared infrastructure (DB, Redis, Kafka connections)
- Modules communicate via in-process method calls and domain events
- Heavy compute (scanners, verification) runs in separate Docker containers

## Decision

We will build the SystemGuard control plane as a **modular monolith**.

### Structure

```
systemguard-backend/
├── pom.xml                          (parent POM)
├── systemguard-common/              (shared models, events, config)
├── systemguard-app/                 (Spring Boot application, wiring)
├── systemguard-project/             (project & repo management)
├── systemguard-scan/                (scan orchestration & workers)
├── systemguard-finding/             (unified finding model)
├── systemguard-architecture/        (AST, app graph — Phase 2)
├── systemguard-risk/                (contextual risk — Phase 2)
├── systemguard-policy/              (architecture policies — Phase 2)
├── systemguard-remediation/         (LLM-assisted remediation — Phase 3)
├── systemguard-verification/        (isolated verification — Phase 3)
└── systemguard-github/              (GitHub integration)
```

### Rules

1. **Each module has its own Maven module** with declared dependencies.
2. **Modules depend on `systemguard-common`** for shared types and events.
3. **No circular dependencies** between modules.
4. **Modules expose services via interfaces** in their public API package.
5. **Database entities are module-scoped** — a module owns its tables.
6. **Cross-module queries use DTOs**, not direct entity access.
7. **Heavy compute runs out-of-process** — the control plane dispatches work via Kafka to Docker-based workers.

### Module Dependency Graph

```
                    systemguard-common
                    /    |    |    \
                   /     |    |     \
        project  scan  finding  github  ...
                   \     |    /
                    \    |   /
                  systemguard-app (wires everything)
```

## Consequences

### Positive
- Clear domain boundaries from day one
- Easy to reason about module ownership
- Single deployment simplifies operations
- Shared database enables efficient cross-domain queries
- Modules can be extracted into services later if needed
- Faster development velocity than microservices

### Negative
- All modules share one JVM — a memory leak in one affects all
- Schema migrations must be coordinated across modules
- Cannot independently scale modules (mitigated by heavy compute being out-of-process)

### Risks
- Module boundaries may need adjustment as the domain evolves
- Team discipline required to respect module boundaries

## Related Decisions
- ADR-002 (future): Kafka Event Contracts
- ADR-003 (future): Scanner Worker Isolation Model
- ADR-004 (future): LLM Provider Abstraction
