# Firestore

## Colecciones
- `credits/{creditId}`
- `email_jobs/{jobId}`

## `credits`
Campos principales:
- `id`
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

