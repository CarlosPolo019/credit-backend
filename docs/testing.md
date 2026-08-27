# Testing

## Comandos
```bash
mvn test
mvn package
docker build -t credit-backend:local .
```

## Casos Cubiertos
- Normalizacion de input.
- Registro de usuario por cedula y password.
- Rechazo de cedula duplicada.
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

## Casos Que Deben Mantenerse
- Login valido e invalido por cedula.
- JWT requerido en `/api/v1/credits`.
- Validaciones de request.
- Rate limit de login, crear y listar.
- Listado activo con filtros y orden.
- `GET` de credito inactivo retorna `404`.
- `DELETE` realiza soft delete y registra auditoria.
- `PUT` no permite cambiar el comercial y registra auditoria solo con los campos que cambiaron.
- `GET /api/v1/credits/{id}/audit` retorna el historial mas reciente primero.
- Worker marca `FAILED` al llegar a max attempts.
- `POST`/`PUT /api/v1/credits` sincronizan el cliente en `clients` (verificar `clientService.upsert(...)` se llama con los campos correctos).
- `GET /api/v1/email-jobs` responde `403` con un token de rol distinto a `ADMIN`.
- `POST /api/v1/users` responde `401` sin token y `403` con un token de rol distinto a `ADMIN` (mismo trato que `/api/v1/email-jobs`).

## Bloqueos De Entorno
El proyecto requiere Maven y Java 21. Si el entorno local no los tiene, usar Docker o CI.
