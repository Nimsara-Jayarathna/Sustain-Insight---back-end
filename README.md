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

## 🚀 Getting Started

### 1. Clone the repo
```bash
https://github.com/Nimsara-Jayarathna/Sustain-Insight---back-end.git
```


# 2. Install dependencies
```bash
./mvnw clean install
```


# 3. Start PostgreSQL with Docker
```bash
docker run --name news-pg \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=news_db \
  -p 5432:5432 \
  -d postgres:16
```

# 4. Run the backend
```bash
./mvnw spring-boot:run
```


# 5. Verify health endpoint
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```
# 6. Check database tables
```bash
docker exec -it news-pg psql -U postgres -d news_db -c "\dt
```
