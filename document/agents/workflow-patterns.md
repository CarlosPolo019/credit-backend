# Workflow Patterns

## Research-First
Leer primero los archivos relacionados y patrones existentes. Para endpoints, revisar controlador, DTO, servicio, repositorio, tests y documentacion.

## Prompt Chaining
Usar pasos con checkpoint:
1. Entender contrato actual.
2. Ajustar modelo/servicio.
3. Actualizar tests.
4. Actualizar docs.
5. Validar.

## Orchestrator-Workers
Usar subagentes cuando una tarea pueda separarse por ownership:
- API/security
- Firestore/repository
- Worker/email
- Docs/CI

Cada worker reporta archivos tocados y comando de validacion. El orquestador resuelve conflictos y hace commits.

## Evaluator
Para cambios de seguridad o persistencia, usar un subagente read-only de revision antes del commit final.

