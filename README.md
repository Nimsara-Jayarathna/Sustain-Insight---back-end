# 📰 Sustain Insight – Backend

This is the **backend service** for the News Aggregator project.
It is built with **Spring Boot** and uses a **Neon PostgreSQL cloud database** for both development and production.
Configuration is **environment-driven** (`.env` + `application.yml`), keeping secrets out of the repo.

---

## ✨ Features

* REST API for news articles, categories, sources, bookmarks, and user preferences
* User authentication with JWT
* Environment-based config with `.env` and Spring profiles
* CORS configuration for frontend integration
* Deployed backend connected to Neon DB (no local DB needed)

---

## 🛠 Tech Stack

* Java 21
* Spring Boot (Web, Data JPA, Security, Validation, Actuator)
* PostgreSQL (via Neon)
* Maven

---

## ⚙️ Environment Setup

Run the following command depending on your platform, replacing placeholders (`<VALUE>`) with your actual Neon credentials and settings:

### macOS / Linux (bash/zsh)

```bash
export PGHOST=<your-neon-host>
export PGPORT=<your-port>
export PGDATABASE=<your-database>
export PGUSER=<your-username>
export PGPASSWORD=<your-password>

export SERVER_PORT=8080
export CORS_ALLOWED_ORIGINS=http://localhost:5173,https://sustain-insight-front-end.vercel.app
```

### Windows (PowerShell)

```powershell
setx PGHOST "<your-neon-host>"
setx PGPORT "<your-port>"
setx PGDATABASE "<your-database>"
setx PGUSER "<your-username>"
setx PGPASSWORD "<your-password>"

setx SERVER_PORT "8080"
setx CORS_ALLOWED_ORIGINS "http://localhost:5173,https://sustain-insight-front-end.vercel.app"
```

Spring Boot automatically maps these environment variables into `application.yml`.

---

## 🚀 Getting Started

### 1. Clone the repo

```bash
git clone https://github.com/Nimsara-Jayarathna/Sustain-Insight---back-end.git
cd Sustain-Insight---back-end
```

### 2. Install dependencies

```bash
mvn clean install
```

### 3. Run the backend

```bash
mvn spring-boot:run
```

### 4. Verify health endpoint

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 👥 Collaboration Workflow

* **Feature branches:**
  Use dedicated branches like:

  * `feature/auth`
  * `feature/articles`
  * `feature/cors-config`

* **Development branch (`dev`):**

  * All work is merged into `dev` via PRs.
  * Requires review and passing checks.
  * Acts as the integration branch before production.

* **Main branch (`main`):**

  * Production-ready branch, directly used for deployment.
  * PRs from `dev` are squashed and merged into `main`.

* **Branch protections:**

  * PRs required for merges.
  * No direct commits or force pushes to `main` or `dev`.
  * Status checks and reviews required.

---

## 📂 Project Structure

```
src/main/java/com/news_aggregator/backend
  ├── config/         # Security & CORS
  ├── controller/     # REST endpoints
  ├── model/          # Entities
  ├── repository/     # JPA repositories
  ├── service/        # Business logic
  └── util/           # Helpers (if needed)

src/main/resources
  ├── application.yml # Config (reads from env)
```

---

## 🔒 Security Notes

* Secrets are provided via environment variables — never hardcoded.
* Only placeholder instructions are included in README.
* Neon DB credentials and JWT secrets must be provided per environment.
* If secrets are exposed in Git history, rotate them immediately.
