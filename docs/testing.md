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
- Edicion de credito (`update`), incluye rechazo cuando el credito no existe.
- Worker marca `SENT`.
- Worker programa retry cuando Mailgun falla.

## Casos Que Deben Mantenerse
- Login valido e invalido por cedula.
- JWT requerido en `/api/v1/credits`.
- Validaciones de request.
- Rate limit de login, crear y listar.
- Listado activo con filtros y orden.
- `GET` de credito inactivo retorna `404`.
- `DELETE` realiza soft delete.
- Worker marca `FAILED` al llegar a max attempts.

## Bloqueos De Entorno
El proyecto requiere Maven y Java 21. Si el entorno local no los tiene, usar Docker o CI.
