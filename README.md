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
  macOS (Homebrew):
  ```bash
  brew services start postgresql
  ```

  Linux:
  ```bash
  sudo service postgresql start
  ```
  
  Windows:
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
https://github.com/Nimsara-Jayarathna/Sustain-Insight---back-end.git
```

# 2. Install dependencies
```bash
mvn clean install
```

# 4. Run the backend
```bash
mvn spring-boot:run
```

# 5. Verify health endpoint
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

