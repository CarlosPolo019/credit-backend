# Firestore

## Colecciones
- `credits/{creditId}`
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
- `salespersonName`, `salespersonNameNormalized`
- `isActive`
- `createdAt`, `updatedAt`, `deletedAt`

Reglas:
- Toda lectura operativa filtra activos.
- El borrado es logico.
- `clientDocument` debe contener solo digitos.
- `clientName` se deriva de las partes del nombre para compatibilidad de busqueda y salida.
- Los filtros de texto usan valores normalizados.
- `amount` e `interestRate` se serializan como texto decimal para preservar precision.

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
