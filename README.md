# Workflow Execution Service

A minimal service for defining and executing sequential workflows. Each workflow is
a sequence of steps; each step may consume input and produce output that becomes
available to subsequent steps.

Built with **Java 17** and **Spring Boot 3**.

---

## Architecture Overview

The service follows a clean, layered architecture:

```
Controller (REST)  ->  Service (orchestration)  ->  Engine (execution)  ->  Repository (storage)
```

- **API layer** (`api/`) — REST controllers + centralized exception handling.
- **Service layer** (`service/`) — use-case orchestration, ID generation, validation.
- **Engine** (`engine/`) — the core sequential execution loop. Maintains a shared
  **context map** of accumulated step outputs and feeds it to each step.
- **Step handlers** (`engine/handlers/`) — a `StepHandler` strategy per step type.
  New step types are added simply by registering a new Spring bean — no engine
  changes required (Open/Closed Principle). Unknown step names fall back to a
  default no-op handler.
- **Domain** (`domain/`) — `Workflow`, `StepDefinition`, `Execution`,
  `StepExecution`, `ExecutionStatus`.
- **Repositories** (`repository/`) — in-memory, thread-safe (`ConcurrentHashMap`).

### Execution model

Execution is **synchronous**: calling the execute endpoint runs all steps in order
and returns the final result. The engine persists state **after every step
transition**, so the "track state" endpoint reflects live progress (and would do so
across threads if execution were made asynchronous).

A step can read any output produced by a prior step via the shared context. For
example, `approve` reads the `valid` flag produced by `validate`. If a step throws,
the execution is marked `FAILED`, the failing step records its error, and no further
steps run.

---

## API

Base path: `/api/v1`

| Method | Path                                 | Description                                     |
|--------|--------------------------------------|-------------------------------------------------|
| POST   | `/workflows`                         | Create (store) a workflow definition            |
| GET    | `/workflows/{workflowId}`            | Get a workflow definition                       |
| POST   | `/workflows/{workflowId}/executions` | Execute a workflow                              |
| GET    | `/executions/{executionId}/state`    | Track execution state (id, status, currentStep) |
| GET    | `/executions/{executionId}`          | Full execution history (step-level)             |
| GET    | `/workflows/{workflowId}/executions` | All executions for a workflow                   |

### Example: create a workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "contract-review",
    "steps": [
      { "name": "validate", "input": { "contractId": "123" } },
      { "name": "approve" },
      { "name": "execute" }
    ]
  }'
```

### Example: execute the workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflows/contract-review/executions
```

Response:

```json
{
  "executionId": "abc123...",
  "workflowId": "contract-review",
  "status": "COMPLETED",
  "steps": [
    {
      "name": "validate",
      "status": "COMPLETED",
      "output": {
        "valid": true
      }
    },
    {
      "name": "approve",
      "status": "COMPLETED",
      "output": {
        "approvedBy": "system"
      }
    },
    {
      "name": "execute",
      "status": "COMPLETED",
      "output": {
        "result": "success"
      }
    }
  ]
}
```

### Example: track state

```bash
curl http://localhost:8080/api/v1/executions/{executionId}/state
```

```json
{
  "executionId": "abc123...",
  "status": "COMPLETED",
  "currentStep": 3
}
```

---

## Key Assumptions

- **Step semantics are illustrative.** `validate`, `approve`, and `execute` contain
  simple demonstration logic. The point is the *engine + handler extensibility*, not
  the business rules of any specific step.
- **Workflow IDs are caller-provided and unique.** Re-creating an existing ID returns
  `409 Conflict`.
- **Step input** is static and declared at definition time. Inter-step data flow
  happens through the shared runtime context (prior outputs).
- **Synchronous execution** is acceptable at this scope; executions complete within
  a single request.
- **In-memory persistence** is sufficient to demonstrate the design; data is lost on
  restart.
- `currentStep` is the zero-based index of the in-progress step while running, and
  equals the step count once completed.

---

## Tradeoffs Made

- **In-memory store over a database** — fastest path to a clear, runnable demo.
  Repositories are isolated behind interfaces-in-spirit so swapping to JPA/Postgres
  is a localized change.
- **Synchronous over async/queue-based execution** — simpler state transitions and
  easier to test within the time box. The engine already persists per-step, so the
  async path is a natural evolution.
- **Strategy handlers over a generic scripting/DSL step** — explicit, type-safe, and
  easy to read; richer dynamic steps can be layered on later.
- **No authentication/authorization** — out of scope for the exercise.
- **`Map<String,Object>` context** — flexible and matches the JSON-shaped examples,
  at the cost of compile-time type safety on step payloads.

---

## What I Would Improve With An Additional Week

1. **Durable persistence** — JPA + PostgreSQL (or an event-sourced execution log),
   with optimistic locking on execution state.
2. **Asynchronous execution** — submit returns `202 Accepted` immediately; a worker
   pool / queue drives steps. The per-step persistence already supports this.
3. **Resilience** — per-step retries with backoff, timeouts, and idempotency keys;
   resumable/replayable executions after a crash.
4. **Richer workflow model** — conditional branching, parallel/fan-out steps, and
   explicit input/output mapping expressions between steps (instead of a flat context).
5. **Observability** — structured logging, metrics (Micrometer/Prometheus),
   distributed tracing, and per-step latency dashboards.
6. **API hardening** — pagination on history, OpenAPI/Swagger docs, versioning,
   authn/authz, and request idempotency.
7. **Testing** — expand to contract tests, concurrency tests, and failure-injection
   tests for the engine.

---

## Running Locally

### Prerequisites

- JDK 17+
- Maven 3.9+ (or use the bundled `mvnw` if you add the wrapper)

### Build & run

```bash
mvn clean package
java -jar target/workflow-execution-service-1.0.0.jar
```

Or run directly:

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

### Quick smoke test

```bash
# create
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -d '{"workflowId":"contract-review","steps":[{"name":"validate","input":{"contractId":"123"}},{"name":"approve"},{"name":"execute"}]}'

# execute
curl -X POST http://localhost:8080/api/v1/workflows/contract-review/executions
```
