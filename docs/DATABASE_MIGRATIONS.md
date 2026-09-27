# Database migrations

Sustain Insight treats **Flyway as the only owner of database schema changes**. Hibernate does not create or update tables; it validates that the migrated database matches the JPA model.

## Startup order

1. PostgreSQL becomes reachable.
2. Flyway validates migration history and applies every pending `V*__*.sql` migration.
3. Hibernate runs with `ddl-auto=validate` and fails startup if the schema does not match the entity model.
4. Spring finishes startup and scheduled jobs can run.
5. `/actuator/health` becomes `UP` and Docker marks the backend healthy.

This means an empty supported PostgreSQL database can be supplied to the same application artifact and brought to the current schema automatically.

## Migration files

- `V1__baseline.sql` is the historical baseline and must not be edited after deployment.
- `V2__complete_application_schema.sql` upgrades the baseline to the complete current entity schema, including authentication/session, ingestion, insights, and supporting indexes.
- Every future database change must be a new file, for example `V3__add_delivery_tracking.sql`.

Never rename, delete, or modify a migration that has already been executed in a shared environment. Flyway stores checksums in `flyway_schema_history` and will reject unexpected history changes.

## Environment variables

Normal fresh databases should use:

```env
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
FLYWAY_BASELINE_VERSION=1
FLYWAY_VALIDATE_ON_MIGRATE=true
FLYWAY_CLEAN_DISABLED=true
```

`FLYWAY_CLEAN_DISABLED=true` is a safety control and should remain enabled in staging and production.

### Existing legacy database without Flyway history

`FLYWAY_BASELINE_ON_MIGRATE=true` is only for an existing database that is known to already contain the complete **V1 baseline schema** but does not yet contain `flyway_schema_history`. With baseline version `1`, Flyway records that legacy baseline and then applies V2 and later migrations.

Do not enable baseline-on-migrate merely to bypass a migration error. Verify the legacy schema first, take a backup, then use it deliberately for the first managed startup. Return the setting to `false` afterward.

## Deployment behavior

The staging Compose file waits for PostgreSQL health before starting the backend. The backend image exposes an HTTP health check against `/actuator/health`. GitHub Actions waits for the container to become healthy and prints the application logs if migration or schema validation fails.

A migration failure never deletes the PostgreSQL volume. Do not use `docker compose down -v`, `flyway clean`, or manual `DROP` operations as an automatic recovery mechanism.

## CI migration verification

The staging workflow creates a real PostgreSQL 16 service, builds the application, starts the packaged JAR against an empty database, and requires `/actuator/health` to report `UP`. This proves that:

- the migration chain works from an empty database;
- PostgreSQL accepts the SQL;
- Hibernate validates the resulting schema against the current entities;
- application startup succeeds without manual schema preparation.

## Checking migration history

On a running database:

```sql
SELECT installed_rank, version, description, type, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

A failed migration should be diagnosed and corrected with a new, safe migration. Do not rewrite successfully applied migration history in shared environments.
