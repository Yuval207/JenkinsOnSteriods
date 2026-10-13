# SystemGuard — Developer-Facing Software Assurance Platform

SystemGuard is a next-generation security platform that shifts left without shifting the burden. It aggregates SAST, SCA, Secrets, and (eventually) Runtime context into a single normalized view, complete with context, severity, and automated remediation generation.

## Phase 1: Foundation (Current Status)
The current Phase 1 builds the base modular monolith control plane, scanner execution engine, unified finding model, and Next.js frontend UI.

## Project Structure
- **backend/**: Spring Boot 3 modular monolith.
  - `systemguard-common`: Shared enums, events, exceptions.
  - `systemguard-project`: Git cloning and repository metadata management.
  - `systemguard-scan`: Orchestrates scan runs, triggers async scanner jobs.
  - `systemguard-finding`: Normalizes and deduplicates raw scanner outputs into a unified model.
  - `systemguard-app`: Wires the application together (Flyway, Config, Entrypoint).
- **frontend/**: Next.js + Tailwind CSS UI using TanStack React Query.
- **demo-vulnerable-app/**: Intentionally vulnerable Spring Boot repository used to test scanners.

## Dependencies (Docker Compose)
The stack uses `docker-compose.yml` to spin up:
- **PostgreSQL 16**: Relational data (projects, findings, scan runs).
- **Redis 7**: Future caching and distributed locking.
- **Kafka 3.8**: Event streaming for scan progress and async jobs.
- **LocalStack (S3)**: S3-compatible object storage for massive raw scanner output artifacts.

## How to Run Locally

### 1. Start Infrastructure
```bash
docker compose up -d
```
Check that PostgreSQL (5433), Kafka (9092), and LocalStack (4566) are running.

### 2. Build & Run Backend
You need Java 21+ and Maven.
```bash
cd backend
mvn clean install -DskipTests
cd systemguard-app
mvn spring-boot:run
```
The backend runs on `http://localhost:8080`. API documentation is available at `http://localhost:8080/swagger-ui.html`.

### 3. Run Frontend
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
3. **Artifact Storage**: Raw outputs from scanners can be huge; they are pushed directly to LocalStack (S3) and only metadata/normalized findings are saved to Postgres.
