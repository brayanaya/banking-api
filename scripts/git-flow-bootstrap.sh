#!/usr/bin/env bash
# Crea el repositorio con un historial ordenado siguiendo Git Flow:
#   main (producción) <- release/x.y.z <- develop <- feature/*
# Uso:  bash scripts/git-flow-bootstrap.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -d .git ]; then
  echo "Ya existe un repositorio git aquí. Abortando para no pisar tu historial." >&2
  exit 1
fi

git init -b main
git add .gitignore
git commit -m "chore: initial commit"

git checkout -b develop

# feature <nombre> <mensaje> <paths...>
feature() {
  local name="$1" msg="$2"; shift 2
  git checkout -b "feature/$name" develop
  git add "$@"
  git commit -m "$msg"
  git checkout develop
  git merge --no-ff "feature/$name" -m "merge: feature/$name into develop"
}

P=src/main/java/com/trinity/banking
R=src/main/resources

feature project-setup \
  "build: add Spring Boot project, Docker and docker-compose setup" \
  pom.xml Dockerfile docker-compose.yml .dockerignore $R/application.yml \
  $P/BankingApplication.java $P/config

feature database-schema \
  "feat(db): add Flyway DDL (clients, accounts, transactions) and demo DML seed" \
  $R/db

feature domain-model \
  "feat(domain): add domain model, business exceptions and hexagonal ports" \
  $P/domain

feature application-services \
  "feat(application): implement client, account, transaction and statement use cases" \
  $P/application

feature persistence-adapter \
  "feat(persistence): add JPA entities, repositories and persistence adapters" \
  $P/infrastructure/adapter/out

feature rest-api \
  "feat(api): add REST controllers, DTOs with validation and global error handling" \
  $P/infrastructure/adapter/in

feature unit-tests \
  "test: add JUnit tests for domain, services and controllers" \
  src/test

feature ci-and-docs \
  "docs: add README, GitHub Actions CI and git flow bootstrap script" \
  .github README.md scripts

# Release 1.0.0
git checkout -b release/1.0.0 develop
git checkout main
git merge --no-ff release/1.0.0 -m "release: 1.0.0"
git tag -a v1.0.0 -m "Version 1.0.0"
git checkout develop
git merge --no-ff release/1.0.0 -m "merge: release/1.0.0 into develop" || true

echo
git log --oneline --graph --all --decorate
echo
echo "Listo. Ahora conecta tu repo de GitHub y sube todo:"
echo "  git remote add origin git@github.com:<tu-usuario>/<tu-repo>.git"
echo "  git push -u origin main develop --tags"
echo "  git push origin --all"
