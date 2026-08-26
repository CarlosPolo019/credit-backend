# Commit Guide

## Reglas
- Repositorio independiente: inicializar Git dentro de `credit-backend`.
- Usar Conventional Commits.
- No versionar secretos ni artefactos.
- Revisar `git status --short --ignored` antes del primer commit.

## Commits Sugeridos
```bash
git init
git add .
git commit -m "chore: bootstrap credit backend"
```

Para cambios documentales posteriores:
```bash
git add AGENTS.md README.md docs document
git commit -m "docs: add backend agent operating guide"
```

## Validacion Antes De Commit
```bash
mvn test
mvn package
```

Si no hay Maven/Java 21 local:
```bash
docker build -t credit-backend:local .
```

