# Banking API — Prueba técnica (entidad financiera)

API REST en **Java 17 + Spring Boot 3** para que los trabajadores de una entidad financiera administren **clientes**, **productos financieros** (cuenta corriente / cuenta de ahorros) y **transacciones** (consignación, retiro, transferencia), además de consultar el **estado de cuenta**.

- Arquitectura **hexagonal** (puertos y adaptadores)
- Base de datos **PostgreSQL 16** con migraciones **Flyway** (DDL + DML)
- **Dockerizado** (`docker compose up`) y documentado con **Swagger UI**
- Tests unitarios con **JUnit 5 + Mockito + MockMvc**, cobertura con **JaCoCo**
- CI con **GitHub Actions**, flujo de ramas **Git Flow**

---

## 1. Cómo ejecutar

### Opción A — Todo con Docker (recomendada)

```bash
docker compose up --build
```

| Recurso | URL |
|---|---|
| Swagger UI (probar la API) | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5433` (db `banking`, user `banking`, pass `banking`) |

Para apagar: `docker compose down` (agrega `-v` para borrar los datos).

### Opción B — Local (Java 17 + Maven)

```bash
docker compose up -d db      # solo la base de datos
mvn spring-boot:run
```

### Tests y cobertura

```bash
mvn verify
# reporte de cobertura: target/site/jacoco/index.html
```

Al arrancar, Flyway crea el esquema y carga **datos de demostración**: 2 clientes y 3 cuentas (`5300000001`, `3300000001`, `5300000002`).

---

## 2. Arquitectura

```
                    ┌───────────────────────────────────────────────┐
  HTTP/JSON         │                  APLICACIÓN                   │        PostgreSQL
 ───────────►  ┌────┴─────┐   ┌─────────────────────┐   ┌──────────┴────┐   ◄──────────
  (adaptador   │Controller│──►│  Puertos de entrada │   │  Adaptador    │    (adaptador
   de entrada) │  + DTO   │   │   (use cases)       │   │  JPA          │     de salida)
               └────┬─────┘   │        ▲            │   └──────────┬────┘
                    │         │  Services (lógica)  │              │
                    │         │        │            │   ┌──────────┴────┐
                    │         │        ▼            │   │ Puertos de    │
                    │         │  Modelo de dominio  │──►│ salida (repo) │
                    │         └─────────────────────┘   └───────────────┘
                    └───────────────────────────────────────────────┘
        La dependencia siempre apunta hacia el dominio (el dominio no conoce Spring ni JPA).
```

```
src/main/java/com/trinity/banking
├── domain
│   ├── model        Client, Account, Transaction, enums  (entity)
│   ├── exception    BusinessRule / NotFound / Duplicate
│   └── port
│       ├── in       Casos de uso (interfaces) + commands
│       └── out      Puertos de repositorio (interfaces)   (repository)
├── application
│   └── service      Implementación de los casos de uso    (service)
├── infrastructure
│   └── adapter
│       ├── in/web   Controllers, DTOs, @RestControllerAdvice (controller)
│       └── out/persistence  Entidades JPA, Spring Data repos, adapters, mapper
└── config           Clock, OpenAPI
```

**Cumplimiento de las capas pedidas (entity, service, controller, repository):**
`entity` → `domain/model` (+ entidades JPA en infraestructura) · `service` → `application/service` · `controller` → `adapter/in/web` · `repository` → puertos `domain/port/out` + adaptadores JPA.

---

## 3. Endpoints

Base: `/api/v1`

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/clients` | Crear cliente (solo mayores de edad) |
| GET | `/clients` · `/clients/{id}` | Listar / consultar |
| PUT | `/clients/{id}` | Actualizar (recalcula fecha de modificación) |
| DELETE | `/clients/{id}` | Eliminar (bloqueado si tiene productos) |
| POST | `/accounts` | Crear cuenta `CHECKING` (33…) o `SAVINGS` (53…) |
| GET | `/accounts?clientId=` · `/accounts/{id}` | Listar / consultar |
| PATCH | `/accounts/{id}/status` | `ACTIVE` / `INACTIVE` / `CANCELLED` |
| GET | `/accounts/{id}/statement?from=&to=` | Estado de cuenta (por defecto, mes en curso) |
| POST | `/transactions/deposits` | Consignación |
| POST | `/transactions/withdrawals` | Retiro |
| POST | `/transactions/transfers` | Transferencia (débito en origen + crédito en destino) |

Errores con formato único: `{ timestamp, status, error, message, path, details[] }`
`400` validación · `404` no existe · `409` duplicado · `422` regla de negocio · `500` inesperado.

### Ejemplos

```bash
# Crear cliente
curl -X POST localhost:8080/api/v1/clients -H "Content-Type: application/json" -d '{
  "identificationType":"CC","identificationNumber":"1075123456",
  "firstName":"Brayan","lastName":"Aya","email":"brayan@example.com","birthDate":"2000-05-20"}'

# Crear cuenta de ahorros con saldo inicial
curl -X POST localhost:8080/api/v1/accounts -H "Content-Type: application/json" \
  -d '{"clientId":1,"type":"SAVINGS","initialBalance":100000}'

# Consignación
curl -X POST localhost:8080/api/v1/transactions/deposits -H "Content-Type: application/json" \
  -d '{"accountNumber":"5300000001","amount":50000,"description":"Consignación"}'

# Transferencia
curl -X POST localhost:8080/api/v1/transactions/transfers -H "Content-Type: application/json" \
  -d '{"originAccountNumber":"5300000001","destinationAccountNumber":"3300000001","amount":25000}'

# Estado de cuenta
curl "localhost:8080/api/v1/accounts/1/statement?from=2026-11-01&to=2026-11-30"
```

---

## 4. Requerimientos → dónde se cumplen

| Requerimiento | Implementación |
|---|---|
| Cliente: campos mínimos | `Client` / tabla `clients` |
| Fecha de creación automática | `ClientService.create` (`Clock` inyectado) |
| Fecha de modificación automática | `ClientService.update` |
| Menor de edad no puede existir | `Client.isAdult` + `ClientService` **y** trigger `trg_clients_adult` en BD |
| No eliminar cliente con productos | `ClientService.delete` + FK `ON DELETE RESTRICT` |
| Correo `xxxx@xxxxx.xxx` (opcional) | `@Email` + `@Pattern` en el DTO y `CHECK` en BD |
| Nombre/apellido ≥ 2 caracteres (opcional) | `@Size` en el DTO, `ClientService` y `CHECK` en BD |
| Solo cuenta corriente o de ahorros | enum `AccountType` + `CHECK` en BD |
| Producto solo existe con cliente | `AccountService.create` + FK `client_id NOT NULL` |
| Número único de 10 dígitos, ahorros `53…`, corriente `33…` | `AccountService.generateAccountNumber` + `UNIQUE` + `CHECK` |
| Cuenta nueva queda activa | `AccountService.create` (`status = ACTIVE`) |
| Ahorros nunca con saldo < 0 | `Account.debit` + `CHECK ck_accounts_savings_balance` |
| Activar / inactivar en cualquier momento | `Account.changeStatus` |
| Cancelar solo con saldo = 0 | `Account.changeStatus` |
| Consignación, retiro, transferencia | `TransactionService` |
| Saldo y saldo disponible se actualizan | `Account.credit/debit` → `refreshAvailableBalance` |
| Transferencia: crédito en destino y débito en origen | 2 movimientos con la misma `reference` |
| Transferir solo entre cuentas existentes | `404` si alguna no existe |
| Estados de cuenta | `GET /accounts/{id}/statement` |

### Supuestos de diseño (documentados para la sustentación)

- **Saldo disponible**: ahorros = saldo; corriente = saldo + cupo de sobregiro de $1.000.000 (`Account.CHECKING_OVERDRAFT_LIMIT`). Por eso una corriente puede quedar en negativo hasta ese cupo y la de ahorros nunca.
- Solo las cuentas `ACTIVE` pueden recibir o enviar dinero.
- Una cuenta `CANCELLED` no se puede reactivar.
- `updatedAt` es `null` hasta la primera modificación.
- El campo **exenta de GMF** se almacena y expone; **no se descuenta el 4×1000** porque el enunciado no lo exige. Se puede agregar en `TransactionService` usando `Account.isGmfExempt()`.
- El saldo inicial de una cuenta se registra como movimiento de consignación, así el estado de cuenta siempre cuadra.
- No se incluye front: el enunciado exige "como mínimo un proyecto backend"; Swagger UI hace de interfaz de pruebas.

---

## 5. Guía de sustentación

### Arquitectura
**Hexagonal (puertos y adaptadores).** El dominio y los casos de uso no dependen de frameworks; la web (Spring MVC) y la persistencia (JPA/PostgreSQL) son adaptadores intercambiables. Ventajas: testeabilidad (los services se prueban con puertos simulados), bajo acoplamiento y facilidad para cambiar de BD o exponer otro canal (mensajería, gRPC…).

### Patrones de diseño
| Patrón | Dónde |
|---|---|
| Ports & Adapters | Toda la estructura del proyecto |
| Repository | `*RepositoryPort` + adaptadores JPA |
| Dependency Injection | Constructores con `@RequiredArgsConstructor` |
| DTO | `infrastructure/adapter/in/web/dto` (el dominio no se expone) |
| Mapper | `PersistenceMapper`, `XxxResponse.from(...)` |
| Builder | Lombok `@Builder` en modelo y entidades |
| Command | `ClientCommand`, `CreateAccountCommand` |
| Rich Domain Model | `Account` encapsula `credit`, `debit`, `changeStatus` |
| Controller Advice | `GlobalExceptionHandler` (manejo centralizado de errores) |
| Singleton | Beans de Spring |

### Principios SOLID
- **S**: cada clase tiene una sola razón de cambio (`ClientService` clientes, `TransactionService` movimientos, `AccountStatementService` consultas).
- **O**: nuevos tipos de transacción/cuenta se agregan sin modificar los controllers; el sobregiro vive en un único punto.
- **L**: los adaptadores JPA sustituyen a los puertos sin cambiar el comportamiento esperado por los services.
- **I**: casos de uso y repositorios en interfaces pequeñas y específicas (no un "God repository").
- **D**: los services dependen de abstracciones (`port.out`), no de JPA; los controllers dependen de `port.in`.

### Principios ACID
- **Atomicidad**: cada caso de uso es un `@Transactional`; una transferencia (2 saldos + 2 movimientos) se confirma completa o se revierte completa.
- **Consistencia**: reglas en el dominio **y** en la BD (`CHECK`, `UNIQUE`, `FK`, trigger de mayoría de edad); ningún camino puede dejar datos inválidos.
- **Aislamiento**: bloqueo pesimista (`SELECT … FOR UPDATE`) sobre las cuentas implicadas; en transferencias se bloquea siempre en el mismo orden para **evitar deadlocks**.
- **Durabilidad**: PostgreSQL con WAL y volumen Docker persistente (`banking-data`).

### Bases de datos: DDL y DML
- **DDL** (`V1__create_schema.sql`): `CREATE TABLE`, `CONSTRAINT`, `INDEX`, función y `TRIGGER`.
- **DML** (`V2__seed_demo_data.sql`): `INSERT` de datos de demostración; en runtime, JPA emite `INSERT/UPDATE/DELETE/SELECT`.
- Las migraciones versionadas con Flyway hacen el esquema reproducible en cualquier ambiente (`ddl-auto=none`).

### Servicios en la nube
- **GitHub** (repositorio) y **GitHub Actions** (CI: compila, ejecuta tests, genera reporte JaCoCo y construye la imagen Docker).
- El proyecto no depende de ningún servicio propietario y está listo para desplegarse: imagen Docker + PostgreSQL administrado (p. ej. AWS ECS/Fargate + RDS, o Render/Railway). Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### Versionamiento (Git Flow)
`main` (producción, solo releases con tag) ← `release/x.y.z` ← `develop` ← `feature/*` (y `hotfix/*` desde `main`).
Commits con **Conventional Commits** (`feat:`, `fix:`, `test:`, `docs:`, `build:`).
El script `scripts/git-flow-bootstrap.sh` crea el historial con ese flujo.

### Pruebas
- **Dominio**: reglas de `Account` y mayoría de edad (incluido el caso límite "cumple 18 hoy / mañana").
- **Service** (Mockito): reglas de negocio de clientes, cuentas, transacciones y estado de cuenta.
- **Controller** (`@WebMvcTest` + MockMvc): códigos HTTP, validaciones y formato de error.
