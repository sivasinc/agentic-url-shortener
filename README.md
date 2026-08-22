# Agentic Software Engineer

A controlled agentic software-engineering platform demonstrated by evolving a
real URL-shortener repository. The system accepts an engineering requirement,
analyzes a brownfield codebase, proposes source and test changes, applies them
inside an isolated workspace, runs Maven validation, repairs failures within a
bounded retry policy, and pauses for human decisions at explicit governance
gates.

## Current status

Implemented through Commit 10:

- Stateful dependency-graph execution with entry and exit gates
- Parallel execution of independent tasks
- Isolated, revision-aware repository workspaces
- Repository inspection with approved-path enforcement
- Deterministic and OpenAI-backed engineering models
- Structured requirement analysis, plans, source patches and test patches
- Patch validation, application, SHA-256 evidence and generated diffs
- Real Maven validation with bounded failure-driven repair
- Rollback verification when validation fails
- A runnable URL-shortener baseline and generated analytics scenario
- Asynchronous workflow submission and status polling
- Human clarification with a new workflow revision
- Release-readiness approval and rejection gates
- Safe stop with active Maven-process cancellation and workspace rollback
- Domain and model unit tests for governance transitions
- PostgreSQL-backed audit lineage for workflows, agents, tools and decisions
- Sanitized JSON evidence artifacts for every structured agent output
- Prometheus metrics for agent calls, task duration, validation and repair
- A deterministic failure scenario proving validation-driven repair
- GitHub Actions verification with enforced JaCoCo coverage
- Hardened non-root container packaging and health-checked Docker Compose
- Architecture and reviewer demonstration documentation

The deterministic provider makes the complete demonstration reproducible and
free in local development and CI. The OpenAI provider exercises the same agent
contracts when reviewer-supplied credentials are available.

## Agentic engineering loop

```text
Requirement + repository
          |
          v
Create isolated revision workspace
          |
          v
Requirement agent ---- ambiguous ----> human clarification
          |                                  |
          |<--------- new revision ----------+
          v
Repository analysis agent
          |
          v
Architecture/planning agent
          |
          v
Implementation agent -> test agent
          |
          v
Validate and apply structured patch
          |
          v
Run allowlisted Maven validation
          |
          +---- failure ----> repair agent ----> bounded retry
          |
          v
Release-readiness human approval
          |
          +---- reject/stop ----> rollback
          |
          v
Completed change + diff + validation evidence
```

This is an agentic software-engineering system rather than a manually operated
workflow API: specialized agents generate repository-specific engineering
outputs, controlled tools apply and validate those outputs, and validation
feedback is supplied to a repair agent for another bounded attempt.

## Safety and governance

The deterministic orchestrator owns the controls around model reasoning:

- All repository paths are resolved beneath an approved root.
- Changes are applied only to isolated workflow workspaces.
- Proposed files and patches are structurally validated before writing.
- Build execution uses an allowlisted Maven command and a fixed timeout.
- Retry count and concurrent workflow execution are bounded.
- Ambiguous requirements stop at `AWAITING_CLARIFICATION`.
- Validated changes stop at `AWAITING_APPROVAL`.
- Rejected or safely stopped workflows are terminal.
- Safe stop terminates an active Maven process and verifies rollback.
- Invalid lifecycle transitions return `409 Conflict` as Problem Details.
- Model credentials are supplied only through environment variables.

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring MVC and Bean Validation
- Spring Data JPA, PostgreSQL and Flyway
- Maven Wrapper
- JUnit 5, AssertJ and Mockito
- Micrometer and Prometheus
- Docker Compose
- GitHub Actions and JaCoCo

## Project structure

```text
src/main/java/com/prasad/agentic_software_engineer
|-- agent/                 logical requirement, planning, coding, test and repair agents
|-- audit/                 durable event lineage, redaction and evidence storage
|-- model/                 provider-neutral model contracts and implementations
|-- orchestration/         workflow DAG, engine, gates, governance and API
|-- patch/                 proposal validation, application and diff evidence
|-- repository/            controlled brownfield repository inspection
|-- validation/            allowlisted Maven build execution
`-- workspace/             isolated revisions, snapshots and rollback

src/main/resources/deterministic/url-analytics
`-- *.java.template        reproducible analytics change generated by the deterministic model

scenario-repositories/url-shortener
`-- runnable brownfield URL-shortener fixture used by the end-to-end scenario
```

See [Architecture](docs/ARCHITECTURE.md) for component boundaries, trust
controls and failure behavior. Reviewers can use the concise
[Reviewer Guide](docs/REVIEWER-GUIDE.md) to exercise the agentic repair loop.
The assessment's single summary deliverable is the
[Consolidated Engineering Outcome](docs/ENGINEERING-OUTCOME.md).

The required greenfield, brownfield and ambiguous executions are documented
with copy-paste commands and expected evidence in
[Executable Assessment Scenarios](docs/SCENARIOS.md).

## Prerequisites

- JDK 21
- Docker Desktop
- PowerShell on Windows, or an equivalent shell

## Run locally

From the repository root:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

The application listens on `http://localhost:8080` by default. Verify it with:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

### Run the complete container stack

```powershell
docker compose up --build -d
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

The application container runs as a non-root user with a read-only root
filesystem. Named volumes provide only the writable workspace and Maven cache
needed to validate generated Java repositories at runtime.

## Model providers

### Deterministic provider

This is the default and requires no credential:

```powershell
$env:MODEL_PROVIDER = "deterministic"
Remove-Item Env:MODEL_API_KEY -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

For the URL-analytics requirement, it returns concrete templates from
`src/main/resources/deterministic/url-analytics`. The generated code is still
validated, applied to a copied repository and compiled by the normal controlled
toolchain; it is not written directly into the platform repository.

### OpenAI provider

The optional provider performs real model-backed requirement interpretation,
planning, implementation generation, test generation and repair:

```powershell
$env:MODEL_PROVIDER = "openai"
$env:MODEL_API_KEY = Read-Host "Paste API key"
$env:MODEL_NAME = "gpt-4.1-mini"
.\mvnw.cmd spring-boot:run
```

Never commit an API key. The OpenAI boundary has mocked contract tests, so the
normal build and CI do not require network access or paid credentials. A live
call requires a reviewer-provided valid key.

## End-to-end workflow

### 1. Submit a requirement

Workflow creation is asynchronous and returns `202 Accepted`:

```powershell
$request = @{
    scenarioType = "BROWNFIELD"
    requirement = "Add total and daily redirect analytics"
    repositoryPath = "url-shortener"
} | ConvertTo-Json

$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body $request

$workflow | Select-Object id, revision, status, modelProvider
```

### 2. Poll workflow state

```powershell
do {
    Start-Sleep -Seconds 1
    $workflow = Invoke-RestMethod `
        "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)"

    $workflow | Select-Object id, revision, status, failureMessage
} while ($workflow.status -in @("CREATED", "RUNNING"))
```

For an unambiguous successful requirement, the expected state before release is
`AWAITING_APPROVAL`.

### 3. Approve or reject release readiness

Approve:

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/approvals/release-readiness" `
    -ContentType "application/json" `
    -Body (@{
        actor = "reviewer@example.com"
        approved = $true
        reason = "Generated change and validation evidence reviewed"
    } | ConvertTo-Json)
```

After approval, poll again until the state is `COMPLETED`. To reject, send the
same request with `approved = $false`; the result is `REJECTED` and the workspace
is rolled back.

### 4. Inspect the engineering outcome

```powershell
$workflow.changedFiles
$workflow.diff
$workflow.engineeringPlan
$workflow.engineeringOutcome.relativePath
$workflow.engineeringOutcome.content

Get-ChildItem `
    ".\agent-workspaces\$($workflow.id)\revision-$($workflow.revision)\repository" `
    -Recurse
```

The response and workspace contain the generated source/test changes and build
evidence. Generated files are intentionally excluded from Git because they are
runtime outputs.

## Clarification flow

Submit a deliberately ambiguous requirement:

```powershell
$ambiguous = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "AMBIGUOUS"
        requirement = "Improve analytics"
        repositoryPath = "url-shortener"
    } | ConvertTo-Json)
```

Poll until `AWAITING_CLARIFICATION`, inspect `clarificationQuestions`, then
answer:

```powershell
$ambiguous = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($ambiguous.id)/clarifications" `
    -ContentType "application/json" `
    -Body (@{
        actor = "developer@example.com"
        answers = @(
            "Track total redirects and UTC daily redirects per short code",
            "Expose the result through a read-only REST endpoint"
        )
    } | ConvertTo-Json -Depth 4)
```

The same workflow ID resumes in revision 2 using a fresh workspace and the
clarification history. It cannot reuse stale task outputs from revision 1.

## Safe stop

Safe stop is valid only for a non-terminal workflow:

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/safe-stop" `
    -ContentType "application/json" `
    -Body (@{
        actor = "operator@example.com"
        reason = "Operator requested controlled termination"
    } | ConvertTo-Json)
```

The workflow becomes `SAFE_STOPPED`, pending/running tasks are cancelled, an
active Maven child process is terminated, and the current workspace is restored
to its initial snapshot. Repeating a terminal transition returns `409 Conflict`.

## Audit lineage and evidence

Every workflow transition, logical agent invocation, controlled patch/build
operation, validation attempt, repair and human decision is stored in
PostgreSQL with workflow ID and revision lineage:

```powershell
$events = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/audit-events"

$events |
    Select-Object revision, type, actor, taskType, detail, evidenceArtifact, occurredAt |
    Format-Table -AutoSize
```

Retrieve the sanitized structured agent outputs:

```powershell
$evidence = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/evidence"

$evidence | Select-Object eventId, artifact, mediaType
$evidence[0].content
```

Evidence is written beneath
`agent-workspaces/<workflow-id>/revision-<n>/artifacts/audit` and stored with
the durable audit event. Configured API keys, bearer credentials and commonly
named secret fields are redacted before either copy is persisted.

## Failure-driven repair demonstration

The deterministic provider includes an explicit assessment scenario that makes
the first generated change fail compilation and then repairs it using the same
validation-feedback loop used by the OpenAI provider:

```powershell
$repairDemo = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "BROWNFIELD"
        requirement = "Add total and daily redirect analytics and demonstrate repair"
        repositoryPath = "url-shortener"
    } | ConvertTo-Json)
```

Poll until `AWAITING_APPROVAL`, then inspect its audit events. Expected lineage
includes:

```text
VALIDATION_ATTEMPT_FAILED
REPAIR_STARTED
VALIDATION_ATTEMPT_SUCCEEDED
APPROVAL_REQUIRED
```

The phrase `demonstrate repair` is intentionally limited to the deterministic
assessment provider. In OpenAI mode, repair is triggered naturally by actual
compiler or test failures and receives the previous patch plus bounded Maven
failure output.

## Operational metrics

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/actuator/prometheus" |
    Select-Object -ExpandProperty Content |
    Select-String "agentic_"
```

The platform exports counters and timers for audit events, workflow outcomes,
task executions, model invocations, validation attempts, repair attempts and
workflow/task duration. Metric tags use bounded enums such as provider, agent,
task and outcome; workflow IDs are deliberately excluded to avoid unbounded
cardinality.

## Workflow states

```text
CREATED -> RUNNING
RUNNING -> AWAITING_CLARIFICATION -> CREATED (new revision) -> RUNNING
RUNNING -> AWAITING_APPROVAL -> RUNNING -> COMPLETED
AWAITING_APPROVAL -> REJECTED
non-terminal state -> SAFE_STOPPED
RUNNING -> FAILED
```

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `MODEL_PROVIDER` | `deterministic` | `deterministic` or `openai` |
| `MODEL_API_KEY` | empty | Optional live model credential |
| `MODEL_NAME` | configured model | OpenAI model name |
| `AGENT_WORKSPACE_ROOT` | `./agent-workspaces` | Isolated runtime workspaces |
| `AGENT_MAX_ATTEMPTS` | `2` | Maximum validation/repair attempts |
| `AGENT_COMMAND_TIMEOUT` | `PT120S` | Maven validation timeout |
| `DB_URL` | local PostgreSQL URL | JDBC connection |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |

## Testing

Run the complete suite:

```powershell
.\mvnw.cmd clean verify
```

The `verify` lifecycle generates `target/site/jacoco/index.html` and fails when
aggregate line coverage is below 60%.

Coverage includes workflow state transitions, dependency gates, safe path
resolution, repository analysis, structured model parsing, deterministic model
behavior, patch validation/application, rollback, Maven validation, repair and
URL-shortener fixture behavior. OpenAI tests use a mocked HTTP boundary.

## Continuous integration

`.github/workflows/ci.yml` runs on pushes to `main` and pull requests. It:

1. Sets up Temurin Java 21 with Maven dependency caching.
2. Runs the complete Maven `verify` lifecycle.
3. Enforces the JaCoCo coverage gate.
4. Uploads Surefire and JaCoCo reports even after failures.
5. Builds the production container after tests pass.

CI uses the deterministic provider and never requires a model credential.

## Known production extension

Workflow execution state remains in memory, so a process restart cannot resume
an active workflow. Audit events and sanitized evidence content are durable in
PostgreSQL, while workspace artifacts remain on disk. A horizontally scaled
production deployment would persist workflow/task state with optimistic locking
and use leased queue workers. This boundary is explicit and does not affect the
demonstrated requirement-to-change, validation, repair and governance loop.

## Commit roadmap

1. Platform bootstrap
2. Stateful DAG orchestration
3. Isolated workspaces and repository reasoning
4. Deterministic and OpenAI-backed engineering agents
5. Source/test patch validation and application
6. Executable validation, repair and rollback
7. URL-shortener baseline and generated analytics scenario
8. Human clarification, release governance and safe stop
9. Audit lineage, metrics and demonstration scenarios
10. CI, Docker, coverage and final documentation
