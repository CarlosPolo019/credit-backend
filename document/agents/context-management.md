# Context Management

## Inicio De Sesion
1. Confirmar directorio con `pwd`.
2. Revisar `git status --short --branch`.
3. Leer `AGENTS.md`, `README.md` y el doc de dominio afectado.
4. Usar `rg` para localizar controladores, servicios, repositorios o tests relacionados.

## Sesiones Largas
- Mantener el contexto pequeno: citar paths y cargar archivos solo cuando sean necesarios.
- Registrar decisiones en el commit o en `docs/commit-guide.md` cuando afecten arquitectura.
- Para tareas largas, usar checkpoints funcionales: backend compilable, tests relevantes, docs sincronizados.

## Cierre
1. Ejecutar validaciones disponibles.
2. Confirmar que no hay secretos ni artefactos ignorados staged.
3. Dejar commit descriptivo por checkpoint.
4. Reportar bloqueos externos con comandos y salida relevante.

