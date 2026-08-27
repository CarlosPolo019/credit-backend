# Deployment

## Docker
```bash
docker build -t credit-backend:local .
docker run --env-file .env -p 8080:8080 credit-backend:local
```

## Render
Render debe usar las variables de `.env.example`.

El plan gratuito de Render apaga la instancia tras ~15 min sin trafico; el primer request despues de eso puede tardar 50s+ mientras arranca de nuevo (cold start). El workflow `.github/workflows/keep-alive.yml` pinguea `/actuator/health` cada 10 minutos para que nunca llegue a dormirse.

Variables criticas:
- `JWT_SECRET`
- `DEMO_USER_USERNAME`
- `DEMO_USER_PASSWORD_HASH`
- `FIREBASE_*`
- `FIRESTORE_HEALTH_TIMEOUT_SECONDS` — default `15`; evita falsos negativos del health check cuando Firestore tarda al arrancar despues de un deploy/cold start.
- `RESEND_API_KEY`
- `RESEND_FROM_EMAIL`
- `RESEND_FROM_NAME`
- `CREDIT_NOTIFICATION_EMAIL`
- `APP_CORS_ALLOWED_ORIGINS` — debe incluir el dominio de produccion del frontend (ver "CORS En Produccion" abajo), si no el login/API fallan por CORS aunque el resto este bien configurado.

Para correo real con Resend, `RESEND_FROM_EMAIL` debe usar un dominio verificado en Resend. El destinatario final se configura con `CREDIT_NOTIFICATION_EMAIL`.

## CORS En Produccion
`credit-web` se sirve en `https://fyatest.cmescorcia.com` (dominio personalizado sobre Vercel, ver `credit-web/document/deployment.md`). En Render, `APP_CORS_ALLOWED_ORIGINS` debe incluir ese origen ademas de los de desarrollo local:
```
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8081,https://fyatest.cmescorcia.com
```
Default local (sin esta variable): `http://localhost:5173,http://localhost:8081` (ver `application.yml`).

## Deploy Manual Desde El Action
El deploy a produccion **no es automatico**. El workflow `.github/workflows/deploy-backend.yml` corre solo con `workflow_dispatch` (boton manual):
1. GitHub -> pestaña **Actions** -> workflow **Deploy Backend** -> **Run workflow** -> rama `main` -> Run.
2. Hace un `POST` al Deploy Hook de Render (secret `RENDER_DEPLOY_HOOK_URL` del repo), que dispara un rebuild/redeploy del servicio ya existente.

En Render, el servicio debe tener **Auto-Deploy: No** (Settings del servicio) — si queda en "Yes", Render tambien redespliega solo en cada push, y se pierde el control manual.

Secret requerido en el repo (`gh secret set RENDER_DEPLOY_HOOK_URL --repo CarlosPolo019/credit-backend`): la URL del Deploy Hook (Render -> servicio -> Settings -> Deploy Hook).

## Dominio Personalizado
Produccion vive en `https://fyatest-api.cmescorcia.com` (en vez de la URL larga `*.onrender.com` por defecto). El dominio `cmescorcia.com` se compro en Squarespace pero **el DNS real lo maneja Cloudflare** — los registros cargados en el panel de Squarespace no tienen ningun efecto. Pasos:
1. Render: servicio `credit-backend` -> Settings -> Custom Domains -> Add Custom Domain -> `fyatest-api.cmescorcia.com`. Render muestra el valor CNAME a usar (normalmente el propio hostname `.onrender.com` del servicio).
2. Cloudflare (no Squarespace) -> dominio `cmescorcia.com` -> DNS -> Records -> Add record: `CNAME`, Name `fyatest-api`, Target el valor que dio Render, **Proxy status: DNS only** (nube gris, no naranja — con el proxy activado Render no puede validar el dominio ni emitir SSL).
3. Esperar propagacion (unos minutos); Render verifica y emite SSL automaticamente.
4. En `credit-web` (Vercel), `VITE_API_BASE_URL` debe apuntar a `https://fyatest-api.cmescorcia.com` (ver `credit-web/document/deployment.md`) — requiere redeploy del frontend porque Vite hornea la variable en el build.

## OpenAPI
- Swagger UI: `/swagger-ui/index.html`
- JSON: `/v3/api-docs`

## Notas
- `EMAIL_WORKER_ENABLED=false` permite levantar API sin enviar correo.
- Para produccion, usar `EMAIL_WORKER_ENABLED=true` solo cuando Resend y Firestore esten configurados.
- El free tier puede dormir; los `email_jobs` pendientes quedan persistidos.
