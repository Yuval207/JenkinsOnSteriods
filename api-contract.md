# SystemGuard — API Contract (Phase 1)

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
Phase 1: No authentication required (single-user local mode).
Phase 2+: Bearer token / session-based auth.

---

## Projects

### Create Project
```http
POST /api/v1/projects
Content-Type: application/json

{
  "name": "my-service",
  "repositoryUrl": "https://github.com/org/my-service.git",
  "defaultBranch": "main",
  "description": "My vulnerable demo service",
  "tags": ["java", "spring-boot"]
}
```

**Response: 201 Created**
```json
{
  "id": "uuid",
  "name": "my-service",
  "repositoryUrl": "https://github.com/org/my-service.git",
  "defaultBranch": "main",
  "description": "My vulnerable demo service",
  "tags": ["java", "spring-boot"],
  "status": "CREATED",
  "createdAt": "2026-09-30T18:00:00Z",
  "updatedAt": "2026-09-30T18:00:00Z"
}
```

### List Projects
```http
GET /api/v1/projects?page=0&size=20&sort=createdAt,desc
```

### Get Project
```http
GET /api/v1/projects/{projectId}
```

### Delete Project
```http
DELETE /api/v1/projects/{projectId}
```

### Sync Repository (re-clone / pull)
```http
POST /api/v1/projects/{projectId}/sync
```

**Response: 202 Accepted**
```json
{
  "projectId": "uuid",
  "status": "SYNCING",
  "message": "Repository sync initiated"
}
```

---

## Scans

### Trigger Scan
```http
POST /api/v1/projects/{projectId}/scans
Content-Type: application/json

{
  "branch": "main",
  "scanners": ["SEMGREP", "TRIVY", "GITLEAKS"]
}
```

**Response: 202 Accepted**
```json
{
  "id": "uuid",
  "projectId": "uuid",
  "branch": "main",
  "status": "PENDING",
  "scanners": ["SEMGREP", "TRIVY", "GITLEAKS"],
  "createdAt": "2026-09-30T18:00:00Z"
}
```

### List Scans for Project
```http
GET /api/v1/projects/{projectId}/scans?page=0&size=20&sort=createdAt,desc
```

### Get Scan Details
```http
GET /api/v1/scans/{scanId}
```

**Response: 200 OK**
```json
{
  "id": "uuid",
  "projectId": "uuid",
  "branch": "main",
  "commitHash": "abc123",
  "status": "COMPLETED",
  "progress": 100,
  "jobs": [
    {
      "id": "uuid",
      "scanner": "SEMGREP",
      "status": "COMPLETED",
      "findingCount": 5,
      "startedAt": "2026-09-30T18:00:10Z",
      "completedAt": "2026-09-30T18:01:30Z"
    },
    {
      "id": "uuid",
      "scanner": "TRIVY",
      "status": "COMPLETED",
      "findingCount": 12,
      "startedAt": "2026-09-30T18:00:10Z",
      "completedAt": "2026-09-30T18:00:45Z"
    },
    {
      "id": "uuid",
      "scanner": "GITLEAKS",
      "status": "COMPLETED",
      "findingCount": 2,
      "startedAt": "2026-09-30T18:00:10Z",
      "completedAt": "2026-09-30T18:00:20Z"
    }
  ],
  "summary": {
    "totalFindings": 19,
    "critical": 3,
    "high": 7,
    "medium": 6,
    "low": 3
  },
  "createdAt": "2026-09-30T18:00:00Z",
  "completedAt": "2026-09-30T18:01:30Z"
}
```

### SSE — Scan Progress
```http
GET /api/v1/scans/{scanId}/events
Accept: text/event-stream
```

**Events:**
```
event: scan.progress
data: {"scanId":"uuid","progress":33,"status":"RUNNING","message":"Semgrep completed"}

event: job.completed
data: {"scanId":"uuid","jobId":"uuid","scanner":"SEMGREP","findingCount":5}

event: scan.completed
data: {"scanId":"uuid","status":"COMPLETED","summary":{...}}

event: scan.failed
data: {"scanId":"uuid","status":"FAILED","error":"Trivy timed out"}
```

---

## Findings

### List Findings for Project
```http
GET /api/v1/projects/{projectId}/findings?page=0&size=50&sort=severity,desc
  &severity=CRITICAL,HIGH
  &source=SEMGREP,TRIVY
  &status=OPEN,CONFIRMED
  &findingType=SAST,DEPENDENCY
  &scanId=uuid
```

**Response: 200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "projectId": "uuid",
      "scanRunId": "uuid",
      "source": "SEMGREP",
      "findingType": "SAST",
      "ruleId": "java.lang.security.audit.sqli.jdbc-sqli",
      "title": "SQL Injection via JDBC",
      "severity": "CRITICAL",
      "confidence": "HIGH",
      "status": "OPEN",
      "file": "src/main/java/com/example/UserRepository.java",
      "lineStart": 42,
      "lineEnd": 45,
      "firstSeen": "2026-09-30T18:01:30Z",
      "lastSeen": "2026-09-30T18:01:30Z",
      "remediationStatus": "NONE"
    }
  ],
  "totalElements": 19,
  "totalPages": 1,
  "page": 0,
  "size": 50
}
```

### Get Finding Details
```http
GET /api/v1/findings/{findingId}
```

**Response: 200 OK**
```json
{
  "id": "uuid",
  "projectId": "uuid",
  "scanRunId": "uuid",
  "source": "SEMGREP",
  "findingType": "SAST",
  "ruleId": "java.lang.security.audit.sqli.jdbc-sqli",
  "title": "SQL Injection via JDBC",
  "description": "User input is concatenated directly into a SQL query...",
  "severity": "CRITICAL",
  "confidence": "HIGH",
  "status": "OPEN",
  "fingerprint": "sha256:abc123...",
  "file": "src/main/java/com/example/UserRepository.java",
  "lineStart": 42,
  "lineEnd": 45,
  "evidence": {
    "codeSnippet": "String query = \"SELECT * FROM users WHERE id = \" + userId;",
    "dataFlow": ["UserController.getUser()", "UserService.findById()", "UserRepository.findById()"],
    "references": ["https://cwe.mitre.org/data/definitions/89.html"]
  },
  "firstSeen": "2026-09-30T18:01:30Z",
  "lastSeen": "2026-09-30T18:01:30Z",
  "remediationStatus": "NONE"
}
```

### Update Finding Status
```http
PATCH /api/v1/findings/{findingId}
Content-Type: application/json

{
  "status": "FALSE_POSITIVE",
  "reason": "Input is validated upstream"
}
```

---

## Findings Summary

### Severity Distribution
```http
GET /api/v1/projects/{projectId}/findings/summary
```

**Response: 200 OK**
```json
{
  "totalFindings": 19,
  "bySeverity": {
    "CRITICAL": 3,
    "HIGH": 7,
    "MEDIUM": 6,
    "LOW": 3
  },
  "bySource": {
    "SEMGREP": 5,
    "TRIVY": 12,
    "GITLEAKS": 2
  },
  "byStatus": {
    "OPEN": 17,
    "FALSE_POSITIVE": 2
  }
}
```

---

## Health & Info

### Health Check
```http
GET /actuator/health
```

### API Info
```http
GET /api/v1/info
```

**Response: 200 OK**
```json
{
  "name": "SystemGuard",
  "version": "0.1.0",
  "phase": 1,
  "scanners": ["SEMGREP", "TRIVY", "GITLEAKS"]
}
```

---

## Error Response Format

All errors follow a consistent format:

```json
{
  "timestamp": "2026-09-30T18:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found: uuid",
  "path": "/api/v1/projects/uuid",
  "correlationId": "uuid"
}
```

---

## Pagination Format

All paginated endpoints follow Spring Data conventions:

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "page": 0,
  "size": 20
}
```
