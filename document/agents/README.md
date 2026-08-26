# Agent Playbooks

Playbooks para coordinar agentes en `credit-backend`.

## Indice
- `context-management.md`: como mantener contexto y estado en sesiones largas.
- `workflow-patterns.md`: patrones research-first, prompt chaining y orchestrator-workers.
- `skills-guide.md`: criterio para usar skills locales o globales.
- `templates/spring-endpoint-readme-template.md`: plantilla para documentar endpoints Spring.

## Regla Base
El agente principal conserva la responsabilidad de integracion, validacion y commits. Los subagentes producen cambios o auditorias con ownership declarado y sin solapar archivos.

