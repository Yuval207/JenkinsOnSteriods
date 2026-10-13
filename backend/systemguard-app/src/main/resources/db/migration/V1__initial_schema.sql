-- V1__initial_schema.sql
-- SystemGuard Phase 1 database schema

-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(1024) NOT NULL,
    default_branch VARCHAR(255) NOT NULL DEFAULT 'main',
    description TEXT,
    local_path VARCHAR(1024),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    last_commit_hash VARCHAR(64),
    detected_languages TEXT,
    detected_build_tools TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE project_tags (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);

-- Scan Runs
CREATE TABLE scan_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    branch VARCHAR(255),
    commit_hash VARCHAR(64),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    total_findings INTEGER NOT NULL DEFAULT 0,
    critical_count INTEGER NOT NULL DEFAULT 0,
    high_count INTEGER NOT NULL DEFAULT 0,
    medium_count INTEGER NOT NULL DEFAULT 0,
    low_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    correlation_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scan_runs_project ON scan_runs(project_id);

-- Scan Jobs (one per scanner per scan run)
CREATE TABLE scan_jobs (
    id UUID PRIMARY KEY,
    scan_run_id UUID NOT NULL REFERENCES scan_runs(id) ON DELETE CASCADE,
    scanner VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    finding_count INTEGER NOT NULL DEFAULT 0,
    raw_result_path VARCHAR(1024),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scan_jobs_run ON scan_jobs(scan_run_id);

-- Unified Findings
CREATE TABLE findings (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    scan_run_id UUID NOT NULL REFERENCES scan_runs(id) ON DELETE CASCADE,
    scan_job_id UUID REFERENCES scan_jobs(id),
    source VARCHAR(50) NOT NULL,
    finding_type VARCHAR(50) NOT NULL,
    rule_id VARCHAR(512),
    title VARCHAR(1024) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    confidence VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    fingerprint VARCHAR(128) NOT NULL,
    file_path VARCHAR(1024),
    line_start INTEGER,
    line_end INTEGER,
    code_snippet TEXT,
    evidence JSONB,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remediation_status VARCHAR(50) NOT NULL DEFAULT 'NONE',
    cve_id VARCHAR(64),
    cwe_id VARCHAR(64),
    package_name VARCHAR(512),
    installed_version VARCHAR(128),
    fixed_version VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_findings_project ON findings(project_id);
CREATE INDEX idx_findings_scan_run ON findings(scan_run_id);
CREATE INDEX idx_findings_fingerprint ON findings(fingerprint);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_status ON findings(status);
CREATE INDEX idx_findings_source ON findings(source);
CREATE INDEX idx_findings_type ON findings(finding_type);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255),
    entity_id UUID,
    user_id UUID,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
