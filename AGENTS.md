# AGENTS.md

Guia operativa para agentes que trabajen en `credit-backend`.

## Mapa Rapido
- Stack: Java 21, Spring Boot 3.5.16, Maven, Firestore, Firebase Admin SDK, JWT, Bucket4j, Mailgun REST.
- Paquete base: `com.fya.credits`.
- Capas activas: `config`, `security`, `controller`, `dto`, `model`, `repository`, `service`, `service/email`, `background`, `exception`.
- Persistencia: Firestore es la unica fuente de verdad para `credits`, `email_jobs` y `users`.
- Auth: `POST /api/v1/auth/register` crea usuarios por cedula; `POST /api/v1/auth/login` emite JWT por cedula y conserva fallback demo configurable por ambiente.
- Creditos: todas las consultas operativas filtran `isActive == true`; `DELETE` es borrado logico.
- Email: `POST /credits` crea `EmailJob(PENDING)` y responde sin esperar a Mailgun; el worker programado envia y marca `SENT`, `RETRY` o `FAILED`.
- Seed: `scripts/seed-firestore/data/credits.json` contiene el anexo con documentos numericos `100000001..100000010`.

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
- Nunca exponer errores crudos de Firebase, Mailgun, JWT o BCrypt al cliente.
- No acceder a Firestore desde controladores; usar servicios y repositorios.
- `amount` e `interestRate` se tratan como `BigDecimal`.
- La fecha oficial la genera backend con `Clock`.
- No versionar `.env`, credenciales Firebase/Mailgun, private keys, tokens ni passwords reales.

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
- Worker conserva transiciones `PENDING/RETRY -> PROCESSING -> SENT|RETRY|FAILED`.
- README, `.env.example`, `docs/**` y `document/agents/**` quedan sincronizados.
