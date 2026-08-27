# AGENTS.md

Guia operativa para agentes que trabajen en `credit-backend`.

## Mapa Rapido
- Stack: Java 21, Spring Boot 3.5.16, Maven, Firestore, Firebase Admin SDK, JWT, Bucket4j, Resend Email API.
- Paquete base: `com.fya.credits`.
- Capas activas: `config`, `security`, `controller`, `dto`, `model`, `repository`, `service`, `service/email`, `background`, `exception`.
- Persistencia: Firestore es la unica fuente de verdad para `credits`, `email_jobs`, `users` y `clients`.
- Auth: `POST /api/v1/auth/login` emite JWT por cedula y conserva fallback demo configurable por ambiente. El JWT lleva el `role` del usuario como claim (`JwtService`); `JwtAuthenticationFilter` lo usa para otorgar `ROLE_<role>`. No hay auto-registro publico — no existe `POST /api/v1/auth/register` (se elimino: toda cuenta se crea via `/api/v1/users`, admin-only, ver el punto siguiente).
- Usuarios (admin): `POST /api/v1/users` (`UserController`/`UserService`) es la unica forma de crear una cuenta, exclusiva de `ADMIN` (`SecurityConfig.hasRole("ADMIN")`, mismo patron que `/api/v1/email-jobs/**`) — `role` opcional en el body (default `"USER"`, tambien acepta `"ADMIN"` porque el caller ya es admin). No emite `LoginResponse`/token para la cuenta creada (`UserResponse`, solo datos basicos) — no es un login, es una accion administrativa. Usado por `credit-web` en `/users` para crear comerciales de prueba.
- Roles: `AppUser.role` distingue `ADMIN` (solo el seed `900100001`, Carlos Escorcia, mas cualquier cuenta que un admin cree con `role: "ADMIN"` via `/api/v1/users`) de `USER` (todos los demas). `SecurityConfig` exige `ADMIN` para `/api/v1/email-jobs/**` y `POST /api/v1/users` (403 JSON si no); el resto de la API no discrimina por rol.
- Creditos: todas las consultas operativas filtran `isActive == true`; `DELETE` es borrado logico.
- Edicion y auditoria: `PUT /api/v1/credits/{id}` edita datos del cliente y condiciones (nunca el comercial); cada `PUT`/`DELETE` registra una entrada en `credit_audit_logs`, consultable via `GET /api/v1/credits/{id}/audit`.
- Clientes: `ClientService.upsert(...)` sincroniza la coleccion `clients` (doc ID = documento normalizado, igual que `users`) en cada `POST`/`PUT /api/v1/credits` — evita nombres inconsistentes para la misma cedula entre creditos. `GET /api/v1/clients` expone el listado completo, sin rol requerido (lo consume el autocomplete de cedula de `credit-web`).
- Email: `POST /credits` crea `EmailJob(PENDING)` y responde sin esperar a Resend; el worker programado envia y marca `SENT`, `RETRY` o `FAILED`. El correo es HTML; el logo usa una URL de produccion fija y el boton de detalle usa `APP_FRONTEND_BASE_URL`.
- Seed: `scripts/seed-firestore/data/credits.json` contiene el anexo con documentos numericos `100000001..100000010`; `data/users.json` contiene perfiles comerciales para login (Carlos Escorcia con `role: "ADMIN"`); `seed.js` tambien deriva `clients` a partir de los creditos. `wipe.js` borra `credits`+`email_jobs` para arrancar de cero (destructivo, produccion).

## Protocolo De Inicio
1. Ejecutar `pwd` y confirmar que estas en `credit-backend`.
2. Revisar `git status --short --branch` si existe `.git`.
3. Leer este archivo, `README.md` y el documento relevante en `docs/`.
4. Buscar patrones existentes con `rg` antes de editar.
5. Si la tarea toca varias areas, usar subagentes con ownership no solapado.

## Protocolo De Cierre
1. Ejecutar los comandos de validacion disponibles.
2. Actualizar `README.md`, `docs/**` o `document/agents/**` si cambio comportamiento.
3. Revisar que `.env`, secretos, `target/` y logs no esten staged.
4. Crear commit Conventional Commit por checkpoint funcional.
5. Reportar bloqueos externos con comando y salida relevante.

## Protocolo De Subagentes
Cada subagente debe declarar al inicio:
- `Scope`
- `Files owned`
- `Files read-only`
- `Deliverable`
- `Validation command`

Reglas:
- No dos subagentes modifican el mismo paquete o documento al mismo tiempo.
- El agente principal integra, valida y decide commits.
- Cerrar subagentes al terminar.

## Convenciones
- Mantener el codigo en los paquetes actuales; no crear un segundo scaffold ni otra clase `@SpringBootApplication`.
- Nunca exponer errores crudos de Firebase, Resend, JWT o BCrypt al cliente.
- No acceder a Firestore desde controladores; usar servicios y repositorios.
- `amount` e `interestRate` se tratan como `BigDecimal`.
- La fecha oficial la genera backend con `Clock`.
- No versionar `.env`, credenciales Firebase/Resend, private keys, tokens ni passwords reales.

## Documentacion Obligatoria
- Cambios de endpoints: actualizar `docs/api.md`.
- Cambios de Firestore: actualizar `docs/firestore.md`.
- Cambios del worker/correo: actualizar `docs/email-worker.md`.
- Cambios del seed: actualizar `docs/seed-firestore.md`.
- Cambios de despliegue o variables: actualizar `README.md`, `.env.example` y `docs/deployment.md`.
- Cambios de pruebas: actualizar `docs/testing.md`.
- Cambios de flujo de agentes: actualizar `document/agents/**`.

## Comandos
- `mvn spring-boot:run`
- `mvn test`
- `mvn package`
- `docker build -t credit-backend:local .`
- `cd scripts/seed-firestore && npm install && npm run seed`

## Git Checkpoints
- Primer commit sugerido: `chore: bootstrap credit backend`.
- Commit documental sugerido: `docs: add backend agent operating guide`.

## Definition Of Done
- Tests pasan o se documenta el bloqueo de entorno exacto.
- OpenAPI sigue exponiendo auth y credit endpoints.
- `POST /credits` crea `Credit` y `EmailJob(PENDING)` antes de devolver `201`.
- `DELETE` conserva borrado logico y las consultas no devuelven inactivos.
- `PUT` nunca cambia el comercial; `PUT`/`DELETE` siguen registrando su entrada en `credit_audit_logs`.
- Worker conserva transiciones `PENDING/RETRY -> PROCESSING -> SENT|RETRY|FAILED`.
- README, `.env.example`, `docs/**` y `document/agents/**` quedan sincronizados.
