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
  "termMonths": 10
}
```

`clientDocument` solo acepta digitos. El backend guarda los nombres por partes y deriva `clientName` para busqueda, tabla y correo.
El comercial se toma del subject del JWT, se consulta en `users` y se guarda como `salespersonName`, `salespersonDocument` y `registeredByUserId`.

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

## Editar Credito
`PUT /api/v1/credits/{id}`

Auth requerida. Mismo body que crear (`clientFirstName`, `clientSecondName`, `clientFirstSurname`, `clientSecondSurname`, `clientDocument`, `amount`, `interestRate`, `termMonths`). No permite cambiar el comercial (`salespersonName`/`registeredByUserId` quedan igual, son del registro original). Recalcula `clientName`/normalizados y `updatedAt`. Response `200`: `CreditResponse`. Registra una entrada `UPDATED` en el historial de auditoria con el detalle de los campos que cambiaron.

## Borrar Credito
`DELETE /api/v1/credits/{id}`

Auth requerida. Hace borrado logico: `isActive=false`, `deletedAt=now`, `updatedAt=now`. Registra una entrada `DELETED` en el historial de auditoria.

## Historial De Un Credito
`GET /api/v1/credits/{id}/audit`

Auth requerida. Lista las ediciones y el borrado de un credito, mas reciente primero.

Response `200`:
```json
[
  {
    "id": "abc123",
    "creditId": "CR-100000001",
    "action": "UPDATED",
    "changedByUserId": "900100001",
    "changedByDocument": "900100001",
    "changedByName": "Carlos Escorcia",
    "changedAt": "2026-08-26T20:00:00Z",
    "changes": {
      "amount": { "before": "7800000", "after": "9000000" },
      "termMonths": { "before": "10", "after": "12" }
    }
  }
]
```

`action` es `UPDATED` o `DELETED`. En `DELETED`, `changes` viene vacio (el registro completo ya quedo inactivo).

## Exportar Credito A PDF
`GET /api/v1/credits/{id}/pdf`

Auth requerida. Genera en el servidor (OpenPDF) el mismo certificado de una pagina que `credit-web` genera en el cliente con jsPDF — mismos datos, mismos colores de marca. Pensado para `credit-mobile`, que no tiene una libreria de render de PDF: descarga el binario ya listo en vez de construirlo en el dispositivo.

Response `200`: `application/pdf`, `Content-Disposition: attachment; filename="credito-{id}.pdf"`.

## Listar Trabajos De Correo
`GET /api/v1/email-jobs?status=&search=&sortBy=createdAt&direction=desc`

Auth requerida. Lista toda la coleccion `email_jobs` (no solo los pendientes), pensado para monitoreo/soporte.

Query params:
- `status`: uno de `PENDING`, `PROCESSING`, `SENT`, `RETRY`, `FAILED`. Omitir para traer todos.
- `search`: texto libre, busca coincidencia parcial en `clientName` o `recipient`.

Campos de orden permitidos:
- `createdAt` (default)
- `status`

Response `200`: `EmailJobListResponse`:
```json
{
  "items": [
    {
      "id": "abc123",
      "creditId": "CR-100000001",
      "recipient": "fyasocialcapital@gmail.com",
      "clientName": "Pepito Perez",
      "creditAmount": 7800000,
      "salespersonName": "Carlos Escorcia",
      "registeredAt": "2026-08-25T20:00:00Z",
      "status": "FAILED",
      "attempts": 3,
      "lastError": "Resend respondio 401",
      "createdAt": "2026-08-25T20:00:00Z",
      "processedAt": "2026-08-25T20:05:00Z",
      "nextAttemptAt": null
    }
  ],
  "total": 1
}
```

`lastError` es un solo string que se sobreescribe en cada intento (no hay historial por intento).

## Errores
- `400`: validacion o query no permitida.
- `401`: credenciales invalidas o token ausente/invalido.
- `409`: cedula ya registrada.
- `404`: credito inexistente o inactivo.
- `429`: rate limit.
- `503`: Firestore/proveedor de correo no disponible.
