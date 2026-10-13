# SystemGuard — Kafka Event Contracts

## General Event Envelope

Every Kafka event follows this envelope structure:

```json
{
  "eventId": "uuid",
  "eventType": "scan.requested",
  "timestamp": "2026-09-30T18:00:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": { ... }
}
```

### Headers
- `X-Event-Id`: UUID (for idempotency)
- `X-Correlation-Id`: UUID (for tracing)
- `X-Event-Type`: string

### Idempotency
Consumers MUST be idempotent. Use `eventId` as the deduplication key.
Pattern: Check Redis `SET NX` with `eventId` before processing. TTL: 24h.

### Dead-Letter Topics
Every topic has a corresponding DLT: `{topic}.dlt`
Failed events are published to the DLT after configured retry attempts (default: 3).

---

## Topics & Events

### `scan.requested`

**Producer:** Scan Module (API layer)
**Consumer:** Scan Module (orchestrator)

Triggered when a user initiates a scan.

```json
{
  "eventId": "uuid",
  "eventType": "scan.requested",
  "timestamp": "2026-09-30T18:00:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "scanRunId": "uuid",
    "repositoryUrl": "https://github.com/org/repo.git",
    "branch": "main",
    "commitHash": "abc123def456",
    "repoLocalPath": "/data/repos/uuid",
    "scanners": ["SEMGREP", "TRIVY", "GITLEAKS"]
  }
}
```

---

### `scanner.requested`

**Producer:** Scan Module (orchestrator)
**Consumer:** Scanner Workers

One event per scanner job. Dispatched after scan.requested is processed.

```json
{
  "eventId": "uuid",
  "eventType": "scanner.requested",
  "timestamp": "2026-09-30T18:00:05Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "scanRunId": "uuid",
    "scanJobId": "uuid",
    "scanner": "SEMGREP",
    "repositoryUrl": "https://github.com/org/repo.git",
    "branch": "main",
    "commitHash": "abc123def456",
    "repoLocalPath": "/data/repos/uuid",
    "config": {
      "rules": "auto",
      "timeout": 300
    }
  }
}
```

**Scanner values:** `SEMGREP`, `TRIVY`, `GITLEAKS`, `CHECKOV`

---

### `scanner.completed`

**Producer:** Scanner Workers
**Consumer:** Scan Module (orchestrator), Finding Module (normalizer)

Published when a scanner job finishes (success or failure).

```json
{
  "eventId": "uuid",
  "eventType": "scanner.completed",
  "timestamp": "2026-09-30T18:01:30Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "scanRunId": "uuid",
    "scanJobId": "uuid",
    "scanner": "SEMGREP",
    "status": "COMPLETED",
    "rawResultPath": "s3://systemguard/scans/uuid/semgrep-output.json",
    "findingCount": 5,
    "durationMs": 85000,
    "error": null
  }
}
```

**status values:** `COMPLETED`, `FAILED`, `TIMED_OUT`

---

### `finding.normalized`

**Producer:** Finding Module (normalizer)
**Consumer:** Finding Module (deduplicator), Risk Module (Phase 2)

Published after raw scanner output is normalized into canonical findings.

```json
{
  "eventId": "uuid",
  "eventType": "finding.normalized",
  "timestamp": "2026-09-30T18:01:35Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "scanRunId": "uuid",
    "scanJobId": "uuid",
    "scanner": "SEMGREP",
    "findingCount": 5,
    "findingIds": ["uuid1", "uuid2", "uuid3", "uuid4", "uuid5"]
  }
}
```

---

### `analysis.requested` (Phase 2)

**Producer:** Scan Module (after all scanners complete)
**Consumer:** Architecture Module

```json
{
  "eventId": "uuid",
  "eventType": "analysis.requested",
  "timestamp": "2026-09-30T18:02:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "scanRunId": "uuid",
    "repoLocalPath": "/data/repos/uuid",
    "analysisTypes": ["AST", "DEPENDENCY_GRAPH", "ARCHITECTURE"]
  }
}
```

---

### `remediation.requested` (Phase 3)

**Producer:** Remediation Module (API layer)
**Consumer:** Remediation Module (orchestrator)

```json
{
  "eventId": "uuid",
  "eventType": "remediation.requested",
  "timestamp": "2026-09-30T18:05:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "remediationId": "uuid",
    "findingId": "uuid",
    "strategy": "AUTO_PATCH"
  }
}
```

---

### `remediation.completed` (Phase 3)

**Producer:** Remediation Module
**Consumer:** Verification Module

```json
{
  "eventId": "uuid",
  "eventType": "remediation.completed",
  "timestamp": "2026-09-30T18:06:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "remediationId": "uuid",
    "findingId": "uuid",
    "status": "PATCH_GENERATED",
    "patchPath": "s3://systemguard/patches/uuid/fix.patch",
    "explanation": "..."
  }
}
```

---

### `verification.requested` (Phase 3)

**Producer:** Remediation Module
**Consumer:** Verification Module

```json
{
  "eventId": "uuid",
  "eventType": "verification.requested",
  "timestamp": "2026-09-30T18:06:30Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "verificationId": "uuid",
    "remediationId": "uuid",
    "findingId": "uuid",
    "patchPath": "s3://systemguard/patches/uuid/fix.patch",
    "repoLocalPath": "/data/repos/uuid",
    "branch": "main",
    "checks": ["BUILD", "TEST", "RESCAN", "POLICY"]
  }
}
```

---

### `verification.completed` (Phase 3)

**Producer:** Verification Module
**Consumer:** Remediation Module, Notification Module

```json
{
  "eventId": "uuid",
  "eventType": "verification.completed",
  "timestamp": "2026-09-30T18:10:00Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "verificationId": "uuid",
    "remediationId": "uuid",
    "findingId": "uuid",
    "result": "VERIFIED",
    "checks": {
      "BUILD": { "passed": true, "durationMs": 45000 },
      "TEST": { "passed": true, "durationMs": 60000, "testsPassed": 42, "testsFailed": 0 },
      "RESCAN": { "passed": true, "findingResolved": true, "newBlockingFindings": 0 },
      "POLICY": { "passed": true, "violations": 0 }
    },
    "logPath": "s3://systemguard/verifications/uuid/output.log"
  }
}
```

**result values:** `VERIFIED`, `REJECTED`

---

### `notification.requested`

**Producer:** Various modules
**Consumer:** Notification Module

```json
{
  "eventId": "uuid",
  "eventType": "notification.requested",
  "timestamp": "2026-09-30T18:10:05Z",
  "projectId": "uuid",
  "correlationId": "uuid",
  "payload": {
    "type": "SCAN_COMPLETED",
    "title": "Scan completed for my-service",
    "message": "Found 19 issues (3 critical, 7 high)",
    "channels": ["DASHBOARD"],
    "metadata": {
      "scanId": "uuid",
      "projectId": "uuid"
    }
  }
}
```

---

## Topic Configuration

| Topic | Partitions | Retention | Replication (prod) |
|-------|-----------|-----------|-------------------|
| scan.requested | 4 | 7d | 3 |
| scanner.requested | 8 | 7d | 3 |
| scanner.completed | 8 | 7d | 3 |
| finding.normalized | 4 | 7d | 3 |
| analysis.requested | 2 | 7d | 3 |
| remediation.requested | 2 | 7d | 3 |
| remediation.completed | 2 | 7d | 3 |
| verification.requested | 2 | 7d | 3 |
| verification.completed | 2 | 7d | 3 |
| notification.requested | 2 | 3d | 3 |

Local development: 1 partition, no replication.

## Consumer Groups

| Group | Topics |
|-------|--------|
| systemguard-scan-orchestrator | scan.requested, scanner.completed |
| systemguard-scanner-worker | scanner.requested |
| systemguard-finding-normalizer | scanner.completed |
| systemguard-finding-dedup | finding.normalized |
| systemguard-analysis | analysis.requested |
| systemguard-remediation | remediation.requested, verification.completed |
| systemguard-verification | verification.requested |
| systemguard-notification | notification.requested |
