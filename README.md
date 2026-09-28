# Sustain Insight Backend

Sustain Insight is a Spring Boot backend that powers a sustainability-focused news experience. It ingests articles from external providers, organises them into rich domain objects, and exposes REST APIs for authentication, personalisation, and content delivery. The service also manages user sessions, bookmarks, and insight tracking to tailor the reading experience.

This README serves as an onboarding guide for new contributors and a reference for running the application in any environment.

---

## Contents

- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Environment Configuration](#environment-configuration)
- [Running the Application](#running-the-application)
- [Database & Persistence Notes](#database--persistence-notes)
- [Directory Structure](#directory-structure)
- [Development Workflow](#development-workflow)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

The backend is a layered Spring Boot application:

1. **Controllers** expose REST endpoints secured via JWT.
2. **Services** contain business logic (article orchestration, session management, insights, email flows, etc.).
3. **Repositories** interact with PostgreSQL using Spring Data JPA.
4. **Schedulers** run periodic tasks for fetching news and cleaning expired tokens.

Entities are mapped with JPA and rely on PostgreSQL features (UUID columns, updatable timestamps). Every deployable environment is managed through environment variables that Spring resolves at runtime.

---

## Key Features

- **Authentication**
  - JWT-based access and refresh tokens with device-aware session tracking.
  - Password hashing using Argon2 with configurable parameters.
  - Email verification, password resets, and email-change OTP flows.

- **Content & Personalisation**
  - Article ingestion pipelines with deduplication by title, source, and timestamp.
  - Category and source associations for flexible filtering.
  - Personalised feeds based on user preferences, bookmarks, and insights.

- **Insights & Engagement**
  - Per-user insight records in the `insights` table.
  - Aggregate insight counts stored directly on the `articles` table for fast queries.
  - Endpoints to toggle insights and retrieve counts efficiently.

- **Lifecycle Automation**
  - Scheduled fetchers pull news at configurable intervals.
  - Automatic cleanup of expired password-reset tokens, refresh tokens (plus session deactivation), and email-verification tokens.
  - Email-change OTPs automatically expire and are purged.

- **Communication**
  - Provider-isolated Brevo transactional email for password resets, verification, and email-change notifications.
  - Branded email templates referencing environment-driven settings.

---

## Tech Stack

| Layer             | Technology                                                                 |
| ----------------- | --------------------------------------------------------------------------- |
| Language          | Java 21                                                                     |
| Framework         | Spring Boot 3 (Web, Security, Data JPA, Validation, Actuator, Scheduling)  |
| Database          | PostgreSQL (local or managed: Azure Postgres, AWS RDS, Neon, etc.)         |
| Build & Run       | Maven + Spring Boot plugin                                                  |
| Authentication    | Spring Security with JWT                                                    |
| Email             | Brevo transactional email + Thymeleaf templates                            |

---

## Environment Configuration

Configuration is pulled from environment variables. To simplify onboarding:

1. **Use the committed template** – `.env.example` contains the local configuration contract and `.env.staging.example` contains the VPS staging contract.
2. **Create a working file** – copy `.env.example` to `.env` and populate it with actual secrets. Never commit the real `.env`.
3. **For GitHub staging** – paste the completed `.env.staging.example` content into the repository secret `APP_ENV_FILE`.

```bash
# --- DATABASE ---
PGHOST=""
PGPORT="5432"
PGDATABASE=""
PGUSER=""
PGPASSWORD=""
# Optional: override discrete fields above with a single JDBC-style URL.
DATABASE_URL=""

# --- SERVER & CORS ---
PORT="8080"
CORS_ALLOWED_ORIGINS="http://localhost:3000"

# --- AUTH ---
JWT_KEY=""
JWT_ACCESS_EXPIRATION="1800000"
JWT_REFRESH_EXPIRATION="86400000"

# --- NEWS / FETCHING ---
NEWS_API_KEY=""
GUARDIAN_API_KEY=""
NEWS_FETCHING_ENABLED="1"
NEWS_FETCHING_DELAY="60000"
NEWS_FETCHING_SCHEDULED_LIMIT="10"
SYNTHESIS_TRIGGER_THRESHOLD="100"
CLUSTERING_TFIDF_THRESHOLD="0.5"

# --- FEED / PAGINATION ---
FEED_HOURS_WINDOW="48"
PAGINATION_DEFAULT_SIZE="9"
PAGINATION_MAX_SIZE="15"
LATEST_DEFAULT_LIMIT="5"
LATEST_MAX_LIMIT="6"

# --- EMAIL / BRANDING ---
FRONTEND_URL="http://localhost:5173"
BRAND_NAME="Sustain Insight"
EMAIL_PROVIDER="brevo"
BREVO_API_KEY=""
BREVO_SENDER_EMAIL="no-reply@your-domain.com"
BREVO_SENDER_NAME="Sustain Insight"

# --- PASSWORD ENCODER ---
SECURITY_PASSWORD_ARGON2_SALT_LENGTH="16"
SECURITY_PASSWORD_ARGON2_HASH_LENGTH="32"
SECURITY_PASSWORD_ARGON2_PARALLELISM="2"
SECURITY_PASSWORD_ARGON2_MEMORY="16384"
SECURITY_PASSWORD_ARGON2_ITERATIONS="3"
SECURITY_COOKIES_SECURE="false"
```

Run `cp .env.example .env` for local development. For the email architecture and provider-replacement rules, see [`docs/EMAIL_SERVICE.md`](docs/EMAIL_SERVICE.md).

---

## Running the Application

### 1. Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL instance reachable from your machine
- `.env` file with valid values (see above)

### 2. Install dependencies

```bash
mvn clean install
```

### 3. Start the backend

#### macOS / Linux

```bash
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run
```

#### Windows PowerShell

```powershell
$envFile = Get-Content .env | Where-Object {$_ -and ($_ -notmatch '^\s*#')}
foreach ($line in $envFile) {
  $parts = $line -split '=',2
  if ($parts.Length -eq 2) { [System.Environment]::SetEnvironmentVariable($parts[0], $parts[1]) }
}
mvn spring-boot:run
```

#### Git Bash / WSL

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Directory Structure

```
Sustain-Insight---back-end
├─ src
│  ├─ main
│  │  ├─ java/com/news_aggregator/backend
│  │  │  ├─ config/         # Security configuration, scheduling, Jackson setup
│  │  │  ├─ controller/     # REST controllers (Auth, Articles, Insights, etc.)
│  │  │  ├─ dto/            # DTO classes sent to the frontend
│  │  │  ├─ email/          # Provider-isolated email config, adapter, templates, model
│  │  │  ├─ exception/      # Custom exception types
│  │  │  ├─ model/          # JPA entities (User, Article, RefreshToken, UserSession,…)
│  │  │  ├─ payload/        # Request/response payload objects
│  │  │  ├─ repository/     # Spring Data repositories
│  │  │  └─ service/        # Business logic, orchestration, schedulers
│  │  └─ resources
│  │     ├─ application.yml # Spring configuration reading from environment variables
│  │     └─ templates/email # Thymeleaf transactional email templates
│  └─ test                  # Unit / integration tests
├─ pom.xml                  # Maven project descriptor
├─ mvnw / mvnw.cmd          # Maven wrapper scripts
└─ README.md
```

---

## Development Workflow

- **Branching**
  - `main` – production-ready builds.
  - `dev` – integration branch for verified features.
  - `feature/*`, `bugfix/*` – work branches.

- **Pull Requests**
  - Base on `dev`, squash or rebase as needed.
  - Run `mvn clean install` locally before pushing.
  - Add tests for new behaviour when practical.

- **Code Style**
  - Favour constructor injection (already enforced by Lombok).
  - Keep transactional boundaries in service layer.
  - Use scheduled tasks sparingly and guard with feature flags where possible.

---

## Troubleshooting

- **Cannot connect to database**
  - Verify environment variables (`PGHOST`, `PGUSER`, etc.).
  - Confirm the DB allows your IP. Managed services might require SSL (`spring.datasource.hikari.data-source-properties.sslmode=require`).

- **Connections timing out**
  - Tune HikariCP settings in `application.yml`:
    - `spring.datasource.hikari.maxLifetime`
    - `spring.datasource.hikari.idleTimeout`
    - `spring.datasource.hikari.keepaliveTime`


- **Scheduled jobs misbehaving**
  - Check the cron expressions in the corresponding service.
  - Set `NEWS_FETCHING_ENABLED=0` (and similar flags) to disable jobs for local testing.

---

Happy coding! If you add significant functionality, remember to update both this README and `env.example` so future contributors know how to configure and run the project.

---

## Support

Maintained by the Sustain Insight team.  
Have questions, ideas, or feedback? Contact `contact.sustain-insight@blipzo.xyz`.

© 2025 Sustain Insight. All rights reserved.

## Database schema management

Database schema changes are managed by Flyway. On startup, Flyway applies pending migrations from `src/main/resources/db/migration`, then Hibernate validates the resulting schema with `ddl-auto=validate`. Fresh PostgreSQL databases therefore require no manual table creation. See `docs/DATABASE_MIGRATIONS.md` for the migration contract, legacy-baseline procedure, and safety rules.


## Runtime and deployment hardening

The staging-ready backend uses Flyway-managed PostgreSQL migrations, Hibernate schema validation, Brevo behind a provider abstraction, Docker health checks, and explicit transactional boundaries for lazy JPA relationships. See:

- `docs/DATABASE_MIGRATIONS.md`
- `docs/EMAIL_SERVICE.md`
- `docs/RUNTIME_HARDENING.md`

## Runtime hardening notes

See `docs/PERSISTENCE_RUNTIME_HARDENING.md` for the persistence-boundary and lazy-loading hardening applied for production/staging operation with `spring.jpa.open-in-view=false`.
