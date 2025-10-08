# 📰 Sustain Insight – Backend

This is the **backend service** for the *Sustain Insight News Aggregator* project.
It is built with **Spring Boot** and uses a **Neon PostgreSQL cloud database** for both development and production.
Configuration is **environment-driven** (`.env` + `application.yml`), keeping secrets out of the repo and supporting easy deployment.

---

## ✨ System-Wide Features

### 🧠 Core Platform
- Aggregates sustainability, ESG, renewable energy, and climate-related news in real time
- Integrates with the **News API** for global article sourcing
- Cleans, categorizes, and stores news with time-based filtering
- Periodic background jobs fetch new data automatically (`@Scheduled` tasks)

### 🔐 Authentication & Security
- Secure user authentication using **JWT**
- Password hashing via **Spring Security**
- End-to-end **password reset flow** with expiring tokens (auto-cleanup included)
- HTML-formatted password reset emails with branded templates
- Email verification and system notifications (SMTP / Mailtrap for dev)
- CORS restrictions and rate-safe endpoints for production

### 🧾 Articles, Categories & Sources
- CRUD endpoints for articles, categories, and sources
- Source & category-based filtering and search
- Pagination and sorting support (fully backend-driven)
- `/latest`, `/feed`, and `/all` article endpoints with custom limits
- Configurable cutoff window via `FEED_HOURS_WINDOW`

### ⭐ Personalization
- User preference-driven recommendations
- Personalized "For You" feed based on reading and bookmarking patterns
- Content relevance logic using category affinity (extendable)

### 🔖 Bookmarks & Saved Articles
- Add / remove bookmarks per authenticated user
- Paginated bookmark retrieval with metadata
- Smart deduplication to prevent duplicate saves

### 📈 Insights & Analytics
- Article view and engagement tracking (future-ready)
- Trending sources, popular categories, and most-bookmarked analytics
- Insights endpoints for dashboard visualization (in progress)

### 🧩 Email & Notification Services
- SMTP-based email dispatch (Mailtrap for dev, replaceable with SendGrid / SES)
- Branded HTML templates for password reset and system alerts
- Configurable sender identity via environment variables

### 🕒 Scheduled Tasks
- Automated **news fetchers** triggered periodically
- **Token cleanup scheduler** for removing expired password reset tokens
- Safe task disabling via `fetching.enabled` flag in `.env`

### ⚙️ Configuration
- **Environment-driven setup** (`.env` + `application.yml`)
- No secrets stored in code — only referenced via placeholders
- Configurable pagination, time windows, and CORS domains
- Multi-environment compatibility (local / staging / production)

---

## 🛠 Tech Stack

| Layer      | Technology                                                        |
| ---------- | ----------------------------------------------------------------- |
| Language   | **Java 21**                                                       |
| Framework  | **Spring Boot 3** (Web, Security, Data JPA, Validation, Actuator) |
| Database   | **PostgreSQL (Neon Cloud)**                                       |
| Build Tool | **Maven**                                                         |
| Email      | **Spring Mail + Mailtrap (Dev)**                                  |

---

## ⚙️ Environment Setup

All configuration values are injected from **environment variables**.
You can define them either via a `.env` file or directly in your terminal.
Below is the recommended `.env` layout.

### 🧬 `.env` Example (recommended)

```bash
# --- DATABASE CONFIGURATION ---
PGHOST="your-neon-host"
PGPORT=5432
PGDATABASE="your-database"
PGUSER="your-username"
PGPASSWORD="your-password"
DATABASE_URL="postgresql://${PGUSER}:${PGPASSWORD}@${PGHOST}:${PGPORT}/${PGDATABASE}"

# --- SERVER CONFIGURATION ---
PORT=8080
CORS_ALLOWED_ORIGINS="your-frontend-url"

# --- NEWS API CONFIGURATION ---
NEWS_API_KEY="your-newsapi-key"
NEWS_FETCHING_ENABLED="0"

# --- FEED CONFIGURATION ---
FEED_HOURS_WINDOW=6

# --- PAGINATION SETTINGS ---
PAGINATION_DEFAULT_SIZE=9
PAGINATION_MAX_SIZE=12

# --- LATEST ARTICLES SETTINGS ---
LATEST_DEFAULT_LIMIT=5
LATEST_MAX_LIMIT=6

# --- FRONTEND LINK ---
FRONTEND_URL="your-frontend-url"

# --- BRANDING ---
BRAND_NAME="Sustain-Insight"
BRAND_DOMAIN="your-brand-domain"

# --- GOOGLE MAIL CONFIGURATION ---
GOOGLE_CLIENT_ID="your-google-client-id"
GOOGLE_CLIENT_SECRET="your-google-client-secret"
GOOGLE_REFRESH_TOKEN="your-google-refresh-token"
GOOGLE_SENDER_EMAIL="your-sender-email"

# --- JSON Web Token ---
JWT_KEY="your-jwt-key"
```

> ⚠️ **Important:**
> Do **not** commit your `.env` file — add it to `.gitignore`.
> Each developer and environment (dev, staging, prod) must maintain its own values.

---

### 💻 macOS / Linux (bash/zsh)

```bash
# Load env variables and start Spring Boot
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run
```

✅ This automatically exports all variables from `.env` before running the app.

To confirm a variable:

```bash
echo $PGHOST
```

---

### 🦠 Windows (PowerShell)

```powershell
# Load .env into environment
Get-Content .env | ForEach-Object {
  if ($_ -match '^(.*?)=(.*)$') {
    setx $matches[1] $matches[2]
  }
}

# Run the backend
mvn spring-boot:run
```

Verify a variable:

```powershell
echo $env:PGHOST
```

---

### 🧱 Windows (CMD Alternative)

```cmd
setx PGHOST "your-neon-host"
setx PGPORT "5432"
setx PGDATABASE "your-database"
setx PGUSER "your-username"
setx PGPASSWORD "your-password"
mvn spring-boot:run
```



## 🚀 Getting Started

### 1️⃣ Clone the repository

```bash
git clone https://github.com/Nimsara-Jayarathna/Sustain-Insight---back-end.git
cd Sustain-Insight---back-end
```

### 2️⃣ Install dependencies

```bash
mvn clean install
```

### 3️⃣ Start the backend

```bash
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run
```

### 4️⃣ Verify API health

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 👥 Collaboration Workflow

* **Feature branches** – use meaningful names:

  * `feature/auth`
  * `feature/news-fetching`
  * `feature/password-reset`
* **Development branch (`dev`)**

  * All merges happen here via PRs.
  * PRs require review and CI pass.
* **Main branch (`main`)**

  * Production-ready.
  * Merges from `dev` after verification.

**Branch Protection Rules:**

* PR required for all merges
* No direct commits or force pushes
* All checks must pass

---

## 🗂️ Directory Structure

```
src/main/java/com/news_aggregator/backend
 ├── config/           # Security, CORS
 ├── controller/       # REST endpoints (Auth, Articles, etc.)
 ├── model/            # JPA entities
 ├── repository/       # Data repositories
 ├── service/          # Business logic (Auth, Email, Token cleanup)
 └── util/             # Helper utilities

src/main/resources
 ├── application.yml   # Reads variables from .env
```

---

## 🔒 Security Notes

* All credentials are stored in `.env` (never hardcoded)
* API keys, DB URLs, and JWT secrets should be rotated if exposed
* Mailtrap and frontend URLs are environment-specific
* Password reset tokens auto-expire and are cleaned up every hour

---



## 🦯 License

This project is maintained by the **Sustain Insight Team**.
© 2025 Sustain Insight. All rights reserved.
