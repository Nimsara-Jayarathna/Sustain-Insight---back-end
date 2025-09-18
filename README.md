# 📰 Sustain Insight – Backend

This is the **backend service** for the News Aggregator assignment project.  
It is built with **Spring Boot** and uses **PostgreSQL** with **Flyway migrations** for database schema management.

---

## ✨ Features
- REST API for news articles, categories, sources, bookmarks, and user preferences
- User authentication with email + password (JWT planned)
- PostgreSQL database with Flyway migration scripts
- Clean project structure for easy collaboration

---

## 🛠 Tech Stack
- Java 21
- Spring Boot (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL
- Flyway (DB migrations)
- Maven

---
## 🗄️ Database Setup (PostgreSQL)

Follow these steps to set up PostgreSQL locally:

### 1. Install PostgreSQL
- **Windows / macOS:** [Download from postgresql.org](https://www.postgresql.org/download/)
- **Linux (Ubuntu/Debian):**
  ```bash
  sudo apt update && sudo apt install postgresql postgresql-contrib
  ```

### 2. Verify the installation
  ```bash
  psql --version
  ```
### 3. PostgreSQL Setup (Quick Start)
  **macOS (Homebrew):**
  ```bash
  brew services start postgresql
  ```

  **Linux:**
  ```bash
  sudo service postgresql start
  ```
  
  **Windows:**
  ```bash
  Start PostgreSQL service via Services or pgAdmin.
  ```
### 4. Create the Database:
  ```bash
  psql -U postgres
  ```

### 5. Then inside psql:
  ```bash
  CREATE DATABASE news_db;
  \q
  ```

### 6. (Optional) Set Password for postgres User:
  ```bash
  psql -U postgres
  ALTER USER postgres PASSWORD 'postgres';
  \q
  ```

### 7. (Optional) Environment Variables (defaults shown below):
  ```bash
  export DB_HOST=localhost
  export DB_PORT=5432
  export DB_NAME=news_db
  export DB_USER=postgres
  export DB_PASS=postgres
  ```


## 🚀 Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/Nimsara-Jayarathna/Sustain-Insight---back-end.git
```

### 2. Install dependencies
```bash
mvn clean install
```

### 4. Run the backend
```bash
mvn spring-boot:run
```

### 5. Verify health endpoint
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```


## 👥 Collaboration Workflow

- **Work on feature branches:**  
  Each backend contributor should create a dedicated branch for their feature or fix.  
  Example branch names:
  - `feat/backend-auth`
  - `feat/backend-articles`
  - `feat/backend-preferences`
  - `fix/flyway-migration`

- **Open Pull Requests (PRs) to `main`:**  
  - After implementing a feature or fix, push your branch and open a PR.  
  - Request a code review from at least one teammate.  
  - Merge only after review approval and passing build/tests.

- **`main` is protected:**  
  - Direct commits and force pushes to `main` are blocked.  
  - All code changes must go through PR + review.  
  - Keeps the backend always stable for demos and integration with the frontend.

- **Flyway migration workflow:**  
  - Any schema change must be done via a new migration script in `src/main/resources/db/migration`.  
  - Use sequential naming (`V2__add_bookmark_table.sql`, `V3__add_index.sql`).
  - Never edit an already-applied migration — create a new one instead.

