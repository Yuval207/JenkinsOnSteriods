# JenkinsOnSteroids — Developer-Facing Software Assurance Platform

JenkinsOnSteroids is a next-generation security platform that shifts left without shifting the burden. It aggregates SAST, SCA, Secrets, and (eventually) Runtime context into a single normalized view, complete with context, severity, and automated remediation generation.

## Phase 1: Foundation (Current Status)
The current Phase 1 builds the base modular monolith control plane, scanner execution engine, unified finding model, and Next.js frontend UI.

## Project Structure
- **backend/**: Spring Boot 3 modular monolith.
  - `systemguard-common`: Shared enums, events, exceptions.
  - `systemguard-project`: Git cloning, ZIP package management, and repository metadata detection.
  - `systemguard-scan`: Orchestrates scan runs, triggers async scanner jobs.
  - `systemguard-finding`: Normalizes and deduplicates raw scanner outputs into a unified model, plus AI auto-fix.
  - `systemguard-app`: Wires the application together (Flyway, Config, Entrypoint).
- **frontend/**: Next.js + Tailwind CSS UI using TanStack React Query.
- **demo-vulnerable-app/**: Intentionally vulnerable Spring Boot repository used to test scanners.

## Dependencies (Cloud-native / Docker Compose)
All primary infrastructure runs on free serverless cloud tiers (zero Docker containers required for the database/storage):
- **PostgreSQL 16+**: Supabase
- **Redis 7**: Upstash
- **S3 Storage**: Supabase Storage / Cloudflare R2

## How to Run Locally

### 1. Build & Run Backend
You need Java 21+ and Maven.
```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run -pl systemguard-app
```
The backend runs on `http://localhost:8080`. API documentation is available at `http://localhost:8080/swagger-ui.html`.

### 2. Run Frontend
In a new terminal:
```bash
cd frontend
npm install
npm run dev
```
The Next.js dashboard will be available at `http://localhost:3000`.

## Architecture Principles
1. **Modular Monolith**: Strict isolation between domains (Project, Scan, Finding) using package-private visibility where possible.
2. **Scanner Agnostic**: The `FindingNormalizer` ensures Semgrep, Trivy, and Gitleaks outputs all conform to one schema.
3. **Artifact Storage**: Raw outputs from scanners can be huge; they are pushed directly to S3 and only metadata/normalized findings are saved to Postgres.
4. **Autonomous AI Remediation & Packaging**: Vulnerabilities can be automatically patched on disk, and repositories or ZIP packages can be downloaded immediately with security fixes applied.
