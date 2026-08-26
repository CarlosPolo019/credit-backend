# API

## Base
- Version: `/api/v1`
- Auth: JWT Bearer para rutas de creditos.
- Publicos: `/api/v1/auth/register`, `/api/v1/auth/login`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.

## Registro
`POST /api/v1/auth/register`

Request:
```json
{
  "fullName": "Maria Perez",
  "document": "123456789",
  "password": "secret123"
}
```

Response `201`:
```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-25T20:00:00Z",
  "user": {
    "username": "123456789",
    "fullName": "Maria Perez",
    "document": "123456789",
    "role": "USER"
  }
}
```

El documento normalizado es unico y se usa como subject del JWT.
`document` solo acepta digitos en registro.

## Login
`POST /api/v1/auth/login`

Request:
```json
{
  "username": "123456789",
  "password": "secret123"
}
```

Response `200`:
```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-25T20:00:00Z",
  "user": {
    "username": "123456789",
    "fullName": "Maria Perez",
    "document": "123456789",
    "role": "USER"
  }
}
```

`username` conserva el nombre del campo para compatibilidad con Web, pero representa la cedula/documento. El usuario demo configurable sigue disponible como fallback.

## Crear Credito
`POST /api/v1/credits`

Auth requerida.

Request:
```json
{
  "clientFirstName": "Pepito",
  "clientSecondName": "",
  "clientFirstSurname": "Perez",
  "clientSecondSurname": "",
  "clientDocument": "100000001",
  "amount": 7800000,
  "interestRate": 2,
  "termMonths": 10,
  "salespersonName": "Comercial Seed"
}
```

`clientDocument` solo acepta digitos. El backend guarda los nombres por partes y deriva `clientName` para busqueda, tabla y correo.

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
- `409`: cedula ya registrada.
- `404`: credito inexistente o inactivo.
- `429`: rate limit.
- `503`: Firestore/Mailgun no disponible.
