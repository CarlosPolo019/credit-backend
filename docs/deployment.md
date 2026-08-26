# Deployment

## Docker
```bash
docker build -t credit-backend:local .
docker run --env-file .env -p 8080:8080 credit-backend:local
```

## Render
Render debe usar las variables de `.env.example`.

Variables criticas:
- `JWT_SECRET`
- `DEMO_USER_USERNAME`
- `DEMO_USER_PASSWORD_HASH`
- `FIREBASE_*`
- `MAILGUN_*`
- `CREDIT_NOTIFICATION_EMAIL`

## OpenAPI
- Swagger UI: `/swagger-ui/index.html`
- JSON: `/v3/api-docs`

## Notas
- `EMAIL_WORKER_ENABLED=false` permite levantar API sin enviar correo.
- Para produccion, usar `EMAIL_WORKER_ENABLED=true` solo cuando Mailgun y Firestore esten configurados.
- El free tier puede dormir; los `email_jobs` pendientes quedan persistidos.

