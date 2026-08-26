# Seed Firestore

## Ubicacion
- Script: `scripts/seed-firestore/seed.js`
- Datos: `scripts/seed-firestore/data/credits.json`

## Datos Del Anexo
El anexo incluye nombre, monto, plazo e interes. Como no trae documento ni comercial:
- `clientDocument`: `100000001` a `100000010`
- `salespersonDocument`: se distribuye entre los usuarios seed.

El seed guarda `clientFirstName`, `clientSecondName`, `clientFirstSurname` y `clientSecondSurname`, y deriva `clientName` para compatibilidad con la tabla y los correos.

## Usuarios Seed
El script tambien carga perfiles en `users`:
- `900100001` / `demo12345` / Carlos Escorcia
- `900100002` / `demo12345` / Jennifer Navarro
- `900100003` / `demo12345` / Adriana Castellano

La cedula es el identificador unico de login. El password se guarda como BCrypt.

## Ejecucion
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
