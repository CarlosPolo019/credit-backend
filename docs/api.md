# API

## Base
- Version: `/api/v1`
- Auth: JWT Bearer para rutas de creditos.
- Publicos: `/api/v1/auth/login`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.

## Login
`POST /api/v1/auth/login`

Request:
```json
{
  "username": "demo",
  "password": "demo12345"
}
```

Response `200`:
```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-25T20:00:00Z",
  "user": {
    "username": "demo",
    "role": "USER"
  }
}
```

## Crear Credito
`POST /api/v1/credits`

Auth requerida.

Request:
```json
{
  "clientName": "Pepito Perez",
  "clientDocument": "SEED-001",
  "amount": 7800000,
  "interestRate": 2,
  "termMonths": 10,
  "salespersonName": "Comercial Seed"
}
```

Response `201`: `CreditResponse`. Tambien crea un `EmailJob(PENDING)`.

## Listar Creditos
`GET /api/v1/credits?clientName=&clientDocument=&salesperson=&sortBy=createdAt&direction=desc`

Auth requerida. Solo retorna activos.

Campos de orden permitidos:
- `createdAt`
- `amount`

Direcciones:
- `asc`
- `desc`

## Obtener Credito
`GET /api/v1/credits/{id}`

Auth requerida. Si el credito esta inactivo retorna `404`.

## Borrar Credito
`DELETE /api/v1/credits/{id}`

Auth requerida. Hace borrado logico: `isActive=false`, `deletedAt=now`, `updatedAt=now`.

## Errores
- `400`: validacion o query no permitida.
- `401`: credenciales invalidas o token ausente/invalido.
- `404`: credito inexistente o inactivo.
- `429`: rate limit.
- `503`: Firestore/Mailgun no disponible.

