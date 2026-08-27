# Seed Firestore

## Ubicacion
- Script: `scripts/seed-firestore/seed.js`
- Limpieza: `scripts/seed-firestore/wipe.js`
- Datos: `scripts/seed-firestore/data/credits.json`, `scripts/seed-firestore/data/users.json`

## Datos Del Anexo
El anexo incluye nombre, monto, plazo e interes. Como no trae documento ni comercial:
- `clientDocument`: `100000001` a `100000010`
- `salespersonDocument`: se distribuye entre los usuarios seed.

El seed guarda `clientFirstName`, `clientSecondName`, `clientFirstSurname` y `clientSecondSurname`, y deriva `clientName` para compatibilidad con la tabla y los correos. Tambien deriva la coleccion `clients` (un doc por `clientDocument` unico, mismo esquema que usa el endpoint `GET /api/v1/clients`) — asi el autocomplete de cliente del formulario de creditos tiene datos desde el primer seed, sin depender de que la app haya sincronizado nada todavia.

## Usuarios Seed
El script tambien carga perfiles en `users`:
- `900100001` / `demo12345` / Carlos Escorcia — `role: "ADMIN"` (unico con acceso a Correos y Clientes en `credit-web`)
- `900100002` / `demo12345` / Jennifer Navarro — `role: "USER"`
- `900100003` / `demo12345` / Adriana Castellano — `role: "USER"`

La cedula es el identificador unico de login. El password se guarda como BCrypt. El rol viaja en el JWT (`JwtService`) y Spring Security lo usa para restringir `/api/v1/email-jobs/**` a `ADMIN`; en `credit-web` ademas oculta/redirige las vistas Correos y Clientes.

## Limpieza (arrancar de cero)
`wipe.js` borra **todos** los documentos de `credits` y `email_jobs` (no toca `users`). Es destructivo y va contra el proyecto de Firestore que tengan configuradas las credenciales — confirmar el proyecto antes de correrlo en produccion.
```bash
cd scripts/seed-firestore
npm install
npm run wipe   # borra credits + email_jobs
npm run seed   # recrea los 10 creditos, los 3 usuarios (Carlos en ADMIN) y los 10 clientes derivados
```

## Ejecucion (sin limpiar, solo upsert)
```bash
cd scripts/seed-firestore
npm install
npm run seed
```

## Credenciales
Usar una de estas opciones:
- `GOOGLE_APPLICATION_CREDENTIALS` apuntando a service account local.
- Variables `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`.
- `FIREBASE_SERVICE_ACCOUNT_JSON`.

No versionar credenciales reales.
