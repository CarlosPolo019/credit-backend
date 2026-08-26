# Credit Backend

Spring Boot backend for the Fya credit technical test.

## Stack
- Java 21
- Spring Boot 3.5.16
- Spring Web, Validation, Security, Scheduling, Actuator
- Firebase Admin SDK with Cloud Firestore
- JWT with JJWT
- Bucket4j rate limiting
- Mailgun REST API
- Springdoc OpenAPI

## Run
```bash
mvn spring-boot:run
```

The demo login defaults to `demo / demo12345` only when `DEMO_USER_PASSWORD_HASH` is empty. Production should set a BCrypt hash.

## API
- `POST /api/v1/auth/login`
- `POST /api/v1/credits`
- `GET /api/v1/credits`
- `GET /api/v1/credits/{id}`
- `DELETE /api/v1/credits/{id}`
- `/actuator/health`
- `/swagger-ui/index.html`
- `/v3/api-docs`

Credit routes require `Authorization: Bearer <token>`.

## Firestore
Collections:
- `credits`
- `email_jobs`

Operational credit reads always return active records only. `DELETE` performs a soft delete by setting `isActive=false`, `deletedAt`, and `updatedAt`.

## Email Worker
`POST /credits` stores an `EmailJob(PENDING)` and returns `201` without waiting for Mailgun. The scheduled worker processes eligible jobs when `EMAIL_WORKER_ENABLED=true`.

Required Mailgun variables:
- `MAILGUN_API_KEY`
- `MAILGUN_DOMAIN`
- `MAILGUN_BASE_URL`
- `MAILGUN_FROM_EMAIL`
- `MAILGUN_FROM_NAME`
- `CREDIT_NOTIFICATION_EMAIL=fyasocialcapital@gmail.com`

## Seed
```bash
cd scripts/seed-firestore
npm install
npm run seed
```

The seed uses the 10 annex records and fills missing document/salesperson values as `SEED-001..SEED-010` and `Comercial Seed`.

## Test And Build
```bash
mvn test
mvn package
```

Docker build uses Maven with Java 21:

```bash
docker build -t credit-backend:local .
```

## Deploy
Render should provide environment variables from `.env.example`. The free tier may sleep, but pending email jobs remain persisted in Firestore and are picked up after restart.

## Documentacion Operativa
- `AGENTS.md`: reglas de trabajo para agentes.
- `docs/api.md`: contrato HTTP y errores.
- `docs/firestore.md`: colecciones, campos e invariantes.
- `docs/email-worker.md`: flujo Mailgun y reintentos.
- `docs/seed-firestore.md`: seed del anexo.
- `docs/testing.md`: pruebas esperadas y bloqueos de entorno.
- `docs/deployment.md`: Docker, Render y variables.
- `docs/commit-guide.md`: versionado y commits.
- `document/agents/`: playbooks para subagentes, contexto, skills y templates.
