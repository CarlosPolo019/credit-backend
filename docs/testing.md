# Testing

## Comandos
```bash
mvn test
mvn package
docker build -t credit-backend:local .
```

## Casos Cubiertos
- Normalizacion de input.
- Login de usuario registrado por cedula.
- Rechazo de password invalido.
- Fallback del usuario demo.
- Creacion de credito y `EmailJob(PENDING)`.
- Edicion de credito (`update`): actualiza campos y registra entrada `UPDATED` en `credit_audit_logs`; no registra entrada si nada cambio; rechazo cuando el credito no existe.
- Borrado de credito (`delete`): registra entrada `DELETED` en `credit_audit_logs`.
- Worker marca `SENT`.
- Worker programa retry cuando el proveedor de correo falla.
- Sincronizacion de cliente (`ClientService.upsert`): crea si es nuevo, actualiza conservando `createdAt` si ya existia, nunca lanza (falla silenciosa con log si el repositorio falla).
- JWT: el claim `role` viaja y vuelve intacto en `validate(createToken(subject, role))`.
- Creacion de usuario por admin (`UserService.create`): default a `role: "USER"` sin `role` en el request, respeta `role: "ADMIN"` explicito, rechazo de cedula duplicada, rechazo de cedula no numerica.

### HTTP (`@WebMvcTest`, `src/test/java/com/fya/credits/controller/`)
A diferencia de los tests de arriba (Mockito puro, prueban la logica de servicio aislada), estos levantan la capa web real de Spring (`MockMvc`) contra el `SecurityConfig`/`JwtAuthenticationFilter`/`RateLimitFilter` reales — solo el `Service` del controller bajo prueba esta mockeado (`@MockitoBean`). Cubren lo que un test de servicio no puede: codigos de estado HTTP reales, forma del JSON serializado, y que el rol/token efectivamente bloquea o deja pasar la request a traves de la cadena de filtros, no solo que el metodo de servicio se comporta bien en aislamiento.

`@WebMvcTest` no instancia el bean `Firestore` de `FirebaseConfig` (que intentaria credenciales GCP reales) — por eso corren sin credenciales, a diferencia de un `@SpringBootTest` completo.

- `CreditControllerTest`: `POST /api/v1/credits` sin token → `401`; con token valido → `201` + header `Location` + body; con body invalido (`amount <= 0`) → `400` con `VALIDATION_ERROR`; `GET /api/v1/credits` con token → `200` con items; `GET /api/v1/credits/{id}` de un credito inexistente → `404`.
- `AuthControllerTest`: `POST /api/v1/auth/login` sin ningun header (publico) con credenciales validas → `200` + token; con `BadCredentialsException` del servicio → `401`; con `username` vacio → `400` `VALIDATION_ERROR`.
- `UserControllerTest`: `POST /api/v1/users` sin token → `401`; con token `USER` → `403` (nunca llega a `UserService`); con token `ADMIN` → `201`, y confirma que la respuesta **no** trae `token` (no es un login).

## Casos Que Deben Mantenerse
- Rate limit de login, crear y listar (los tests HTTP de arriba suben los limites via `@TestPropertySource` para no toparse con ellos — el comportamiento del propio `RateLimitFilter` en `429` no tiene test dedicado).
- `PUT` no permite cambiar el comercial y registra auditoria solo con los campos que cambiaron.
- `GET /api/v1/credits/{id}/audit` retorna el historial mas reciente primero.
- Worker marca `FAILED` al llegar a max attempts.
- `POST`/`PUT /api/v1/credits` sincronizan el cliente en `clients` (verificar `clientService.upsert(...)` se llama con los campos correctos).
- `GET /api/v1/email-jobs` responde `403` con un token de rol distinto a `ADMIN` (mismo patron que `UserControllerTest`, sin test HTTP dedicado todavia).

## Bloqueos De Entorno
El proyecto requiere Maven y Java 21. Si el entorno local no los tiene, usar Docker o CI.
