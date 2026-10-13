# SystemGuard — Phase Plan

## Phase 1: Foundation + First Vertical Slice

**Goal:** Complete working pipeline from repo → scan → findings → UI.

### Deliverables

| # | Component | Details |
|---|-----------|---------|
| 1 | Infrastructure | Docker Compose (Postgres, Redis, Kafka, MinIO) |
| 2 | Spring Boot Control Plane | Maven multi-module, modular monolith |
| 3 | Project Module | Project/repo registration, Git clone, build detection |
| 4 | Scan Module | Scan orchestration, job dispatch via Kafka, progress SSE |
| 5 | Scanner Workers | Semgrep, Trivy, Gitleaks — Docker-based execution |
| 6 | Finding Module | Unified model, normalization, dedup/fingerprinting |
| 7 | Object Storage | MinIO integration for raw scanner artifacts |
| 8 | Next.js Dashboard | Overview, projects, scan history, findings, finding detail |
| 9 | SSE | Live scan/job progress updates |
| 10 | Demo Repo | Vulnerable Spring Boot app for end-to-end testing |
| 11 | Database Migrations | Flyway for all Phase 1 tables |
| 12 | API Documentation | SpringDoc OpenAPI |
| 13 | Tests | Unit tests for domain logic, integration tests for key flows |

### Acceptance Criteria
1. ✅ Connect demo vulnerable Spring repo as a project
2. ✅ Click "Scan" and see async progress via SSE
3. ✅ See normalized findings in the dashboard
4. ✅ View finding details with source, severity, evidence, code location
5. ✅ All scanners (Semgrep, Trivy, Gitleaks) produce real results
6. ✅ Docker Compose brings up entire stack with one command
7. ✅ README with setup instructions

### Not In Scope (Phase 1)
- Architecture analysis / AST
- Risk scoring
- Policy engine
- Remediation / AI
- Verification
- GitHub PR creation

---

## Phase 2: Architecture Intelligence + Contextual Risk

**Goal:** Understand application architecture, map findings to graph, enable contextual risk.

### Deliverables

| # | Component | Details |
|---|-----------|---------|
| 1 | AST Analysis | JavaParser for Java/Spring projects |
| 2 | App Graph | Nodes (services, endpoints, DBs, topics), Edges (calls, reads, writes) |
| 3 | Finding-to-Graph | Map findings to affected assets in the graph |
| 4 | Blast Radius | Graph traversal from finding to impacted resources |
| 5 | Risk Engine | Deterministic contextual scoring with explanation |
| 6 | Policy Engine | Architecture policies, evaluation, violation reporting |
| 7 | Checkov Integration | IaC scanner for Dockerfiles, Terraform, K8s manifests |
| 8 | Architecture UI | Graph visualization, service relationships, findings overlay |

### Acceptance Criteria
1. ✅ For a SQL injection finding → show endpoint → controller → service → database path
2. ✅ Architecture violations clearly show involved services/resources and violated policy
3. ✅ Risk explanation shows WHY a finding is high/critical
4. ✅ Checkov scans Dockerfiles and IaC

---

## Phase 3: Autonomous Remediation + Verified Fix

**Goal:** LLM-assisted remediation with deterministic verification.

### Deliverables

| # | Component | Details |
|---|-----------|---------|
| 1 | Remediation Orchestration | Full lifecycle (REQUESTED → VERIFIED/REJECTED) |
| 2 | LLM Abstraction | Provider interface (OpenAI, Anthropic, local) |
| 3 | Context Builder | Finding + code + architecture → LLM prompt |
| 4 | Patch Generation | LLM-generated code/config patches |
| 5 | Git Worktree | Branch/worktree management for patches |
| 6 | Verification Env | Ephemeral Docker container, isolated, resource-limited |
| 7 | Build Execution | Run project build in isolation |
| 8 | Test Execution | Run unit/integration tests |
| 9 | Security Rescan | Re-run applicable scanner in isolation |
| 10 | Policy Re-check | Verify architecture policies still pass |
| 11 | Result Model | VERIFIED/REJECTED with evidence |
| 12 | GitHub PR | Create PR only after VERIFIED status |

### Acceptance Criteria
1. ✅ Platform explains a real finding
2. ✅ Generates a patch
3. ✅ Applies patch in isolated environment
4. ✅ Builds project successfully
5. ✅ Runs tests
6. ✅ Rescans for the original finding
7. ✅ Checks architecture policies
8. ✅ Marks VERIFIED only when all checks pass
9. ✅ Creates PR only after verification

---

## Phase 4+ (Future)

- Runtime analysis via OpenTelemetry
- GitHub App with webhooks
- OWASP ZAP for dynamic/API security
- Multi-language support (Python, Go, JS/TS)
- Kubernetes deployment with Helm
- RBAC and multi-tenancy
- Notification channels (Slack, email)
- SBOM generation
- Compliance reporting
