# Agentic Software Engineer

A controlled agentic software-engineering system demonstrated through a URL
shortener.

The platform is designed to transform an engineering requirement into a
reviewable and validated software change using controlled agent autonomy.

## Current status

Implemented through Commit 2:

- Spring Boot platform bootstrap
- PostgreSQL and Flyway configuration
- Health, metrics and Prometheus endpoints
- Explicit workflow dependency graphs
- Missing-dependency validation
- Cycle detection
- Sequential task execution
- Parallel task-wave execution
- Join synchronization
- Entry gates
- Exit gates
- Versioned cross-stage context
- Task-output lineage
- Workflow state transitions
- Task state transitions
- In-memory workflow repository

The platform does not yet claim complete agentic software-engineering
execution.

Repository isolation, repository reasoning, LLM-backed agents, source and test
generation, patch application, validation, failure-driven repair, fallback,
rollback, safe stop, clarification-driven replanning, URL analytics and the
three demonstration scenarios will be implemented in subsequent commits.

## Objective

The completed system will execute the following lifecycle:

```text
Engineering requirement
        |
        v
Requirement understanding
        |
        +-- ambiguous --> human clarification --> dynamic replanning
        |
        v
Repository inspection
        |
        v
Dependency-aware engineering plan
        |
        v
Source-code and test generation
        |
        v
Safe patch validation and application
        |
        v
Build and test execution
        |
        +-- failure --> diagnosis --> corrected patch --> retry
        |
        +-- retries exhausted --> fallback or rollback
        |
        v
Documentation and risk analysis
        |
        v
Policy evaluation
        |
        v
Human release approval
        |
        v
Reviewable diff and engineering summary
```

The URL shortener is the engineering target used to demonstrate this lifecycle.
It is not a replacement for the agentic engineering system itself.

## Assignment coverage

The completed implementation is planned to demonstrate:

- Requirement interpretation and normalization
- Ambiguity detection
- Clarification questions
- Actionable task decomposition
- Explicit task dependencies
- Brownfield repository reasoning
- Architecture and data-flow reasoning
- Stateful, non-linear orchestration
- Sequential and parallel execution
- Synchronization and join tasks
- Entry and exit gates
- Cross-stage context
- Decision lineage
- Human approval checkpoints
- Bounded retries
- Fallback
- Real workspace rollback
- Safe stop
- Security policies
- Compliance policies
- Change-control policies
- Audit-grade traceability
- Workflow success rate
- Retry frequency
- Rollback frequency
- Mean time to recovery
- End-to-end latency
- Dynamic replanning
- Production-quality code generation
- API and schema generation
- Unit and integration test generation
- Documentation generation
- Greenfield execution
- Brownfield execution
- Ambiguous-requirement execution
- URL-shortener core APIs
- Redirect analytics
- Reliability features

## Orchestration model

Engineering work is represented as a directed acyclic graph.

```text
                    Requirement analysis
                              |
                              v
                     Repository analysis
                              |
                              v
                        Architecture
                              |
                +-------------+-------------+
                |                           |
                v                           v
          Implementation              Test generation
                |                           |
                +-------------+-------------+
                              |
                              v
                         Validation
                              |
                  +-----------+-----------+
                  |                       |
               success                  failure
                  |                       |
                  v                       v
          Documentation                Repair
                  |                       |
                  |                       +----> Validation
                  v
          Policy evaluation
                  |
                  v
           Human approval
                  |
                  v
         Release readiness
```

The complete graph will evolve dynamically based on:

- Scenario type
- Requirement ambiguity
- Repository analysis
- Risk level
- Validation results
- Human clarification
- Policy outcomes
- Remaining retry budget

## Implemented workflow behavior

### Graph validation

Before execution, the platform verifies:

- The graph contains at least one task.
- Every dependency refers to an existing task.
- A task does not depend on itself.
- The graph contains no dependency cycle.

An invalid graph is rejected before any engineering task starts.

### Sequential execution

A dependent task cannot start until all its dependencies have succeeded.

```text
Requirement analysis
        |
        v
Repository analysis
        |
        v
Architecture
```

### Parallel execution

Independent ready tasks execute concurrently using Java virtual threads.

```text
             Architecture
                  |
        +---------+---------+
        |                   |
        v                   v
 Implementation       Test generation
```

### Join synchronization

A join task starts only after all required branches have succeeded.

```text
 Implementation       Test generation
        |                   |
        +---------+---------+
                  |
                  v
             Validation
```

### Entry gates

A task can begin only when:

1. All dependencies have succeeded.
2. Its configured entry gate passes.
3. A handler is registered for its task type.

Supported gate types currently include:

- No gate
- Dependencies succeeded
- Required context keys present
- Human approval

Human approval execution is completed in a later governance commit.

### Exit gates

After a task executes, its outputs are written to the workflow context. Its exit
gate must then pass before the task can be marked successful.

This prevents a task from succeeding when required engineering evidence is
missing.

### Cross-stage context

Task outputs are stored as versioned context entries containing:

- Context key
- Context value
- Producing task ID
- Workflow revision
- Creation timestamp

This provides initial output lineage between engineering stages.

Persistent audit events and explicit causation identifiers will be added later.

## Workflow states

```text
CREATED
   |
   v
RUNNING
   |
   +-- clarification required --> AWAITING_CLARIFICATION
   |
   +-- human approval needed --> AWAITING_APPROVAL
   |
   +-- all tasks succeed -----> COMPLETED
   |
   +-- task or gate fails ----> FAILED
   |
   +-- safe stop requested ---> SAFE_STOPPED
```

The current engine implements:

- `CREATED`
- `RUNNING`
- `AWAITING_APPROVAL`
- `COMPLETED`
- `FAILED`
- `SAFE_STOPPED`

Clarification submission, replanning, approval continuation and complete safe
stop behavior are added in later commits.

## Task states

```text
PENDING
   |
   v
RUNNING
   |
   +-- successful execution and exit gate --> SUCCEEDED
   |
   +-- exception or rejected exit gate ----> FAILED
   |
   +-- safe stop before execution ---------> CANCELLED
```

## Planned agents

The completed platform will use logical engineering agents coordinated by the
deterministic workflow engine:

```text
Requirement Agent
Repository Agent
Architecture Agent
Implementation Agent
Testing Agent
Validation Agent
Repair Agent
Documentation Agent
Risk and Policy Agent
```

These agents will perform real engineering actions. They will not merely create
generic Markdown plans.

The Implementation Agent must produce source-file changes. The Testing Agent
must create or update tests. The Validation Agent must run tests against the
modified workspace. The Repair Agent must produce a changed patch after a
validation failure.

## Model-provider strategy

The platform will provide a vendor-neutral engineering model interface with two
implementations:

```text
EngineeringModel
|-- DeterministicEngineeringModel
`-- LlmEngineeringModel
```

### Deterministic provider

Used for:

- Repeatable unit tests
- CI
- Offline demonstration
- Controlled failure injection
- Predictable scenario validation

It must still produce real source and test changes in fixture repositories.

### LLM provider

Used for:

- Requirement interpretation
- Clarification generation
- Repository-specific planning
- Code generation
- Test generation
- Failure diagnosis
- Patch repair
- Dynamic replanning

Model credentials are supplied through environment variables and must never be
committed or written to application logs.

## Controlled autonomy

The deterministic Java layer will control:

- Workflow state
- Task graph
- Entry and exit gates
- Repository boundaries
- Patch validation
- Command allowlists
- Timeouts
- Retry limits
- Approval requirements
- Fallback
- Rollback
- Safe stop
- Audit records
- Artifact storage

The model will provide bounded engineering reasoning. It will not receive
unrestricted filesystem or shell access.

## Planned repository safety

Every workflow will operate on an isolated repository copy:

```text
agent-workspaces/
`-- {workflowId}/
    `-- revision-{revision}/
        |-- repository/
        |-- snapshots/
        |-- artifacts/
        `-- logs/
```

The source scenario repository will never be modified directly.

Repository and patch controls will include:

- Approved repository root
- Relative paths only
- Path-traversal rejection
- Symbolic-link escape rejection
- File count limits
- File size limits
- Context size limits
- Patch file-count limits
- Patch size limits
- Expected original-file hashes
- Protected-file policies
- Before and after hashes
- Reviewable unified diff
- Baseline restoration

These controls are implemented beginning in Commit 3.

## Demonstration scenarios

### Greenfield

The system will receive a requirement for a new URL-shortener capability and
generate real source files, schema definitions, tests and documentation in an
isolated workspace.

### Brownfield

The system will inspect an existing URL shortener and implement redirect
analytics.

Expected analytics capability:

```http
GET /api/v1/urls/{shortCode}/analytics
```

Example response:

```json
{
  "shortCode": "Ab12Cd34",
  "totalRedirects": 125,
  "redirectsByDay": [
    {
      "date": "2026-08-21",
      "count": 125
    }
  ]
}
```

The demonstration must include:

```text
Initial generated patch
        |
        v
Validation failure
        |
        v
Failure diagnosis
        |
        v
Corrected patch
        |
        v
Successful validation
```

### Ambiguous

The system will receive:

```text
Improve URL analytics.
```

It must:

1. Detect missing information.
2. Produce clarification questions.
3. Pause before implementation.
4. Accept a human clarification.
5. Increment the workflow revision.
6. Invalidate affected downstream work.
7. Generate a revised graph.
8. Resume execution.
9. Produce validated engineering output.

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring MVC
- Jakarta Validation
- Spring Data JPA
- Hibernate ORM
- PostgreSQL 17
- H2 for tests
- Flyway
- Maven Wrapper
- Lombok
- JUnit
- AssertJ
- Mockito
- Micrometer
- Prometheus
- Spring Boot Actuator
- Docker Compose
- Java virtual threads

## Project structure

```text
src/main/java/com/prasad/agentic_software_engineer/
|-- AgenticSoftwareEngineerApplication.java
|-- agent/
|   |-- requirement/
|   |-- repository/
|   |-- architecture/
|   |-- implementation/
|   |-- testing/
|   |-- validation/
|   |-- repair/
|   `-- documentation/
|-- model/
|   |-- deterministic/
|   `-- llm/
|-- orchestration/
|   |-- controller/
|   |-- domain/
|   |-- engine/
|   |-- exception/
|   |-- gate/
|   `-- repository/
|-- workspace/
|-- patch/
|-- tool/
|   |-- repository/
|   `-- build/
|-- governance/
|-- policy/
|-- audit/
|-- artifact/
|-- idempotency/
|-- url/
|-- analytics/
|-- common/
|   |-- error/
|   `-- web/
`-- config/
```

Packages are created only when their first implementation class is added. Git
does not track empty directories.

## Prerequisites

Install:

- JDK 21
- Docker Desktop
- Git

A separate Maven installation is not required because the Maven Wrapper is
included.

Verify:

```powershell
java -version
docker version
docker compose version
git --version
```

## Local development

### Start PostgreSQL

```powershell
docker compose up -d postgres
docker compose ps
```

Wait until:

```text
agentic-software-engineer-postgres   healthy
```

### Run tests

```powershell
.\mvnw.cmd clean test
```

Expected:

```text
BUILD SUCCESS
```

### Start the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts at:

```text
http://localhost:8080
```

### Check health

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/actuator/health"
```

Expected:

```json
{
  "status": "UP"
}
```

### Check Prometheus

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/actuator/prometheus" |
    Select-Object -ExpandProperty Content |
    Select-String "jvm_"
```

## Configuration

| Setting | Environment variable | Default |
|---|---|---|
| Server port | `SERVER_PORT` | `8080` |
| Database URL | `DB_URL` | `jdbc:postgresql://localhost:5432/agentic_software_engineer` |
| Database username | `DB_USERNAME` | `postgres` |
| Database password | `DB_PASSWORD` | `postgres` |
| Model provider | `MODEL_PROVIDER` | `deterministic` |
| Model base URL | `MODEL_BASE_URL` | `https://api.openai.com/v1` |
| Model API key | `MODEL_API_KEY` | Empty |
| Model name | `MODEL_NAME` | `gpt-4.1-mini` |
| Model timeout | `MODEL_TIMEOUT` | `PT60S` |
| Model maximum output tokens | `MODEL_MAX_OUTPUT_TOKENS` | `8000` |
| Agent workspace root | `AGENT_WORKSPACE_ROOT` | `./agent-workspaces` |
| Allowed repository root | `AGENT_REPOSITORY_ROOT` | `./scenario-repositories` |
| Maximum repository files | `AGENT_MAX_FILES` | `2000` |
| Maximum file size | `AGENT_MAX_FILE_SIZE_BYTES` | `1048576` |
| Maximum repository context | `AGENT_MAX_CONTEXT_CHARACTERS` | `200000` |
| Maximum task attempts | `AGENT_MAX_ATTEMPTS` | `2` |
| Command timeout | `AGENT_COMMAND_TIMEOUT` | `PT120S` |
| Maximum command output | `AGENT_MAX_OUTPUT_CHARACTERS` | `100000` |
| Maximum patch files | `AGENT_MAX_PATCH_FILES` | `30` |
| Maximum patch size | `AGENT_MAX_PATCH_BYTES` | `524288` |

## Security

Current and planned safety principles include:

- No committed model credentials
- No credentials in logs
- Source repositories are never modified directly
- Model output is treated as untrusted input
- No arbitrary model-generated shell commands
- Repository paths are restricted to an approved root
- Build commands are application-controlled
- High-impact actions require approval
- Every generated artifact will be attributable to its producing task

## Testing

Current test coverage includes:

- Application context startup
- Valid dependency graph acceptance
- Missing-dependency rejection
- Cycle rejection
- Parallel branch execution
- Join synchronization
- Versioned task-output lineage
- Exit-gate rejection
- Workflow failure propagation

Run:

```powershell
.\mvnw.cmd clean test
```

## Current limitations

At Commit 2:

- Workflows are stored in memory.
- No external workflow API exists yet.
- No scenario repository has been inspected yet.
- No isolated workspace has been created yet.
- No LLM call is implemented yet.
- No source patch is generated or applied yet.
- No generated test is executed yet.
- No validation repair loop exists yet.
- Human approval continuation is not implemented yet.
- Clarification submission and replanning are not implemented yet.
- Rollback currently exists only as a planned workspace capability.
- URL-shortener APIs and redirect analytics are not implemented yet.
- Audit-grade persistence and reliability metrics are not implemented yet.

These are explicit incremental limitations, not claims of completed agentic
functionality.

## Commit roadmap

```text
Commit 1
Bootstrap the agentic software-engineering platform

Commit 2
Stateful dependency-graph orchestration

Commit 3
Isolated workspaces and controlled repository reasoning

Commit 4
Deterministic and LLM-backed engineering agents

Commit 5
Source/test generation, patch validation and application

Commit 6
Build validation, failure repair, fallback, rollback and safe stop

Commit 7
URL shortener and redirect analytics through agentic execution

Commit 8
Clarification-driven replanning and governed approvals

Commit 9
Audit lineage, required reliability metrics and three scenarios

Commit 10
Comprehensive testing, CI, Docker and final documentation
```

## Completion rule

The platform will not be described as a completed agentic software-engineering
system until it demonstrates:

```text
Requirement
→ repository analysis
→ real source-code change
→ real test change
→ patch application
→ validation failure
→ corrected patch
→ successful validation
→ reviewable diff
→ human-controlled final outcome
```