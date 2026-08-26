# Firestore

## Colecciones
- `credits/{creditId}`
- `credit_audit_logs/{entryId}`
- `email_jobs/{jobId}`
- `users/{documentNormalized}`

## `users`
Campos principales:
- `id`
- `fullName`
- `document`
- `documentNormalized`
- `passwordHash`
- `role`
- `isActive`
- `createdAt`, `updatedAt`

Reglas:
- `documentNormalized` es el identificador unico y el ID del documento.
- `passwordHash` siempre se almacena con BCrypt.
- Login solo acepta usuarios con `isActive == true`.
- El usuario demo por ambiente no se persiste en esta coleccion.

## `credits`
Campos principales:
- `id`
- `clientFirstName`, `clientSecondName`
- `clientFirstSurname`, `clientSecondSurname`
- `clientName`, `clientNameNormalized`
- `clientDocument`, `clientDocumentNormalized`
- `amount`
- `interestRate`
- `termMonths`
- `registeredByUserId`
- `salespersonDocument`, `salespersonDocumentNormalized`
- `salespersonName`, `salespersonNameNormalized`
- `isActive`
- `createdAt`, `updatedAt`, `deletedAt`

Reglas:
- Toda lectura operativa filtra activos.
- El borrado es logico.
- `clientDocument` debe contener solo digitos.
- `clientName` se deriva de las partes del nombre para compatibilidad de busqueda y salida.
- El comercial se deriva del usuario autenticado por JWT y no se acepta como campo de entrada, ni en creacion ni en edicion (`PUT /api/v1/credits/{id}` solo puede cambiar datos del cliente y condiciones).
- Los filtros de texto usan valores normalizados.
- `amount` e `interestRate` se serializan como texto decimal para preservar precision.

## `credit_audit_logs`
Campos principales:
- `id`
- `creditId`
- `action` (`UPDATED` o `DELETED`)
- `changedByUserId`, `changedByDocument`, `changedByName`
- `changedAt`
- `changes`: mapa de campo -> `{before, after}` (vacio en `DELETED`)

Reglas:
- Se escribe una entrada por cada `PUT`/`DELETE` de `/api/v1/credits/{id}`; no hay entrada de creacion.
- En `UPDATED` solo se listan los campos que realmente cambiaron.
- Es de solo lectura desde la API (`GET /api/v1/credits/{id}/audit`); nada la modifica despues de creada.

## `email_jobs`
Campos principales:
- `id`
- `creditId`
- `recipient`
- `clientName`
- `creditAmount`
- `salespersonName`
- `registeredAt`
- `status`
- `attempts`
- `lastError`
- `createdAt`, `processedAt`, `nextAttemptAt`

Estados:
- `PENDING`
- `PROCESSING`
- `SENT`
- `RETRY`
- `FAILED`

## Indices
`firestore.indexes.json` documenta indices previstos. El repositorio actual filtra texto y ordena parte del resultado en memoria para mantener simple la prueba tecnica.
