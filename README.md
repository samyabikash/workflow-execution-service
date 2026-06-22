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
  changes required (Open/Closed Principle). Unknown step names **fail fast** with a
  clear error rather than silently succeeding.
- **Domain** (`domain/`) — `Workflow`, `StepDefinition`, `Execution`,
  `StepExecution`, `ExecutionStatus`.
- **Repositories** (`repository/`) — in-memory, thread-safe (`ConcurrentHashMap`).
  Workflow creation is atomic (`putIfAbsent`) and executions are stored/returned as
  immutable snapshots.

### Step types

The core step types required by the exercise are implemented first:

- **`transform`** — reads a value from the step input (or the shared context),
  applies a transformation (`upper`, `lower`, `trim`, `toString`), and writes the
  result back into the context under a target key.
- **`validate`** — example validation step (a contract is "valid" when a
  `contractId` is supplied).
- **`delay`** — pauses execution for `durationMillis`. Invalid or negative
  durations fail the step.

Two additional handlers, **`approve`** and **`execute`**, are included purely to
demonstrate the extensibility model (consuming prior step output and producing new
output).

### Execution model

Execution is **asynchronous**. Calling the execute endpoint registers a new
execution, returns **`202 Accepted`** with the execution id (and a `Location`
header pointing at the state endpoint), and drives the steps on a background
thread. Clients **poll** the state endpoint while the workflow runs. The engine
persists state **after every step transition**, so the "track state" endpoint
reflects live progress.

A step can read any output produced by a prior step via the shared context. For
example, `approve` reads the `valid` flag produced by `validate`. The accumulated
context is exposed as `finalContext` on the execution history.

If a step throws, the execution is marked `FAILED`, the failing step records its
error, all remaining steps are marked `SKIPPED`, and the partial `finalContext` is
preserved to aid troubleshooting.

### Execution statuses

`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `SKIPPED` (per-step, downstream of a
failure), and `CANCELLED` (reserved for future cancellation support).

---

## API

Base path: `/api/v1`

| Method | Path                                 | Description                                       |
|--------|--------------------------------------|---------------------------------------------------|
| POST   | `/workflows`                         | Create (store) a workflow definition              |
| GET    | `/workflows/{workflowId}`            | Get a workflow definition                         |
| POST   | `/workflows/{workflowId}/executions` | Execute a workflow (async, returns `202 Accepted`)|
| GET    | `/executions/{executionId}/state`    | Track execution state (id, status, currentStep)   |
| GET    | `/executions/{executionId}`          | Full execution history (step-level + finalContext)|
| GET    | `/workflows/{workflowId}/executions` | All executions for a workflow                     |

### Example: create a workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "contract-review",
    "steps": [
      { "name": "transform", "input": { "sourceKey": "contractId", "contractId": "  123  ", "operation": "trim", "targetKey": "contractId" } },
      { "name": "delay", "input": { "durationMillis": 1000 } },
      { "name": "validate", "input": { "contractId": "123" } },
      { "name": "approve" }
    ]
  }'
```

### Example: execute the workflow (async)

```bash
curl -i -X POST http://localhost:8080/api/v1/workflows/contract-review/executions
```

Returns `202 Accepted`:

```json
{
  "executionId": "abc123...",
  "workflowId": "contract-review",
  "status": "PENDING",
  "currentStep": 0,
  "steps": [ { "name": "transform", "status": "PENDING" } ]
}
```

### Example: poll state

```bash
curl http://localhost:8080/api/v1/executions/{executionId}/state
```

```json
{
  "executionId": "abc123...",
  "status": "RUNNING",
  "currentStep": 1
}
```

### Example: final history (includes finalContext)

```bash
curl http://localhost:8080/api/v1/executions/{executionId}
```

```json
{
  "executionId": "abc123...",
  "status": "COMPLETED",
  "currentStep": 4,
  "finalContext": {
    "contractId": "123",
    "valid": true,
    "approvedBy": "system"
  },
  "steps": [
    { "name": "transform", "status": "COMPLETED", "output": { "contractId": "123" } },
    { "name": "delay", "status": "COMPLETED", "output": { "delayedMillis": 1000 } },
    { "name": "validate", "status": "COMPLETED", "output": { "valid": true } },
    { "name": "approve", "status": "COMPLETED", "output": { "approvedBy": "system" } }
  ]
}
```

---

## Key Assumptions

- **`transform`, `validate`, and `delay`** are the required step types; `approve`
  and `execute` are illustrative extensibility examples.
- **Workflow IDs are caller-provided and unique.** Re-creating an existing ID
  returns `409 Conflict` (enforced atomically).
- **Step input** is static and declared at definition time. Inter-step data flow
  happens through the shared runtime context (prior outputs), exposed as
  `finalContext`.
- **Asynchronous execution**: the execute endpoint returns immediately with an
  execution id to poll.
- **In-memory persistence** is sufficient to demonstrate the design; data is lost
  on restart.
- `currentStep` is the zero-based index of the in-progress step while running, and
  equals the step count once completed.

---

## Tradeoffs Made

- **In-memory store over a database** — fastest path to a clear, runnable demo.
  The repository abstraction is a localized swap to JPA/Postgres.
- **Strategy handlers over a generic scripting/DSL step** — explicit, type-safe,
  and easy to read; richer dynamic steps can be layered on later.
- **No authentication/authorization** — out of scope for the exercise.
- **`Map<String,Object>` context** — flexible and matches the JSON-shaped
  examples, at the cost of compile-time type safety on step payloads.

---

## What I Would Improve With An Additional Week

1. **Durable persistence** — JPA + PostgreSQL (or an event-sourced execution log),
   with optimistic locking on execution state. The repository abstraction already
   provides a clean migration path.
2. **Resilience** — per-step retries with backoff, per-step timeouts, idempotency
   keys; resumable/replayable executions after a crash.
3. **Cancellation** — a cancellation endpoint driving the reserved `CANCELLED`
   state.
4. **Retention** — time- and size-based cleanup / archival of execution history.
5. **Richer workflow model** — conditional branching, parallel/fan-out steps, and
   explicit input/output mapping expressions between steps.
6. **Observability** — structured logging, metrics (Micrometer/Prometheus),
   distributed tracing, and per-step latency dashboards.
7. **API hardening** — pagination on history, OpenAPI/Swagger docs, versioning,
   authn/authz.

---

## Running Locally

### Prerequisites

- JDK 17+
- Maven 3.9+ (or use the bundled `mvnw` wrapper described below)

### Build & run

```bash
./mvnw clean package
java -jar target/workflow-execution-service-1.0.0.jar
```

Or run directly:

```bash
./mvnw spring-boot:run
```

The service starts on **http://localhost:8080**.

> **Maven wrapper:** generate the wrapper once with `mvn -N wrapper:wrapper`,
> which creates `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`. Commit these so the
> project can be built without a local Maven installation.

### End-to-end smoke test

With the service running, exercise the full flow (create -> execute -> poll ->
final context) with the bundled script:

```bash
./scripts/smoke-test.sh
# or against a custom host:
./scripts/smoke-test.sh http://localhost:8080
```

The script creates a workflow that demonstrates `transform`, `delay`, and context
propagation, starts an execution, polls its state, and prints the final workflow
context.
