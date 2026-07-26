# Arohan API

The Java 17 and Spring Boot API for Arohan. It uses MySQL 8, Flyway migrations,
Spring Security, BCrypt passwords and signed JWT access tokens.

## Local development

Start MySQL and run `scripts/mysql-local-setup.sql` once in MySQL Workbench,
then:

```powershell
mvn spring-boot:run
```

The API runs at `http://localhost:8081/api/v1`. Local defaults are documented in
`src/main/resources/application.properties` and can be overridden with
environment variables.

## Verification

```powershell
mvn test
mvn -DskipTests package
```

GitHub Actions runs `mvn --batch-mode verify` on every push and pull request.

## Render preview

The committed `Dockerfile` creates a reproducible multi-stage image and runs the
application as a non-root user. `render.yaml` describes the free preview service.

Configure these secrets in Render:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `FRONTEND_ORIGIN`

Render generates `JWT_SECRET` from the Blueprint. The `prod` profile refuses to
start when required database, JWT or frontend-origin values are missing.
`FRONTEND_ORIGIN_PATTERNS` is optional and may contain comma-separated,
project-scoped Vercel preview patterns. Never configure a global `*` origin.

Flyway automatically creates and upgrades the schema at startup. Never manually
edit `flyway_schema_history`, and never commit real credentials.
