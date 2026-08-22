# Agentic Software Engineer

A controlled agentic software-engineering system demonstrated through a URL
shortener repository.

The platform transforms an engineering requirement and an existing repository
into a reviewable, executable and validated software change.

## Current status

Implemented through Commit 6:

- Stateful dependency-graph orchestration
- Isolated repository workspaces
- Verified workspace rollback
- Controlled repository discovery and context assembly
- Deterministic and OpenAI-backed engineering models
- Requirement, repository, architecture, implementation, testing and repair
  agents
- Structured requirement analyses, plans and patch proposals
- Path, protected-file, hash, file-count and patch-size policies
- Real source and test patch application
- Reviewable changed-file lists and SHA-256 evidence
- Fixed, allowlisted Maven validation
- Build timeouts and bounded output capture
- Build-log artifacts for each attempt
- Failure-driven repair
- Bounded retry attempts
- Verified rollback after unrecoverable validation failure
- Deterministic CI-compatible execution
- Mocked HTTP contract testing for the OpenAI boundary

Clarification resumption, human governance, audit persistence, metrics,
demonstration scenarios and CI packaging are completed in later commits.

## Agentic lifecycle

```text
Engineering requirement
        |
        v
Requirement Agent
        |
        +-- ambiguous --> await clarification
        |
        v
Isolated repository workspace
        |
        v
Repository Analysis Agent
        |
        v
Repository Context Assembler
        |
        v
Architecture Agent
        |
        v
Implementation Agent
        |
        v
Testing Agent
        |
        v
Patch policy validation
        |
        v
Apply source and test changes
        |
        v
Run fixed Maven validation command
        |
        +--------------------+
        |                    |
     success              failure
        |                    |
        v                    v
Validated outcome       Repair Agent
                             |
                             v
                       Restore baseline
                             |
                             v
                       Apply corrected patch
                             |
                             v
                       Retry within budget
                             |
                    +--------+--------+
                    |                 |
                 success          exhausted
                    |                 |
                    v                 v
              Final evidence    Verified rollback
```

## Why this is agentic

The system does not only expose a workflow-management API.

When configured with the OpenAI provider, model-backed agents:

- Interpret arbitrary engineering requirements.
- Detect ambiguity.
- Analyze repository-specific context.
- Produce dependency-aware engineering plans.
- Generate production source changes.
- Generate corresponding tests.
- Diagnose compiler and test failures.
- Produce corrected patches.
- Generate change documentation.

The deterministic Java control plane retains authority over:

- Workflow state
- Task dependencies
- Entry and exit gates
- Repository boundaries
- Context and patch limits
- Model-provider selection
- Patch validation
- Expected file hashes
- Build commands
- Timeouts
- Retry budgets
- Rollback
- Approval policy
- Audit evidence

The model never receives direct filesystem or shell access. Model output is
treated as untrusted input and must pass deterministic policy validation.

## Model providers

```text
EngineeringModel
|-- DeterministicEngineeringModel
`-- LlmEngineeringModel
    `-- OpenAiResponsesClient
```

### Deterministic provider

The deterministic provider is the default.

It is used for:

- Unit tests
- Integration tests
- CI
- Offline development
- Repeatable demonstration fixtures
- Predictable failure and recovery scenarios

It generates compilable source and test patches, but it is a controlled test
implementation rather than a substitute for live model reasoning.

Start it with:

```powershell
$env:MODEL_PROVIDER = "deterministic"
Remove-Item Env:MODEL_API_KEY -ErrorAction SilentlyContinue

.\mvnw.cmd spring-boot:run
```

### OpenAI provider

The OpenAI adapter calls:

```text
POST /v1/responses
```

It requests strict JSON-schema output and maps the response into validated Java
domain records.

Configure it locally:

```powershell
$env:MODEL_PROVIDER = "openai"
$env:MODEL_BASE_URL = "https://api.openai.com/v1"
$env:MODEL_API_KEY = Read-Host "Paste API key"
$env:MODEL_NAME = "gpt-4.1-mini"
$env:MODEL_MAX_OUTPUT_TOKENS = "4000"

.\mvnw.cmd spring-boot:run
```

The API key must never be committed to source control, configuration files,
tests, documentation, screenshots or logs.

The OpenAI adapter is production-capable and covered by mocked HTTP contract
tests. Live execution requires the operator or reviewer to supply
`MODEL_API_KEY`. Automated tests and CI never require external model
credentials and use the deterministic provider.

## Controlled Maven validation

Only this fixed command is allowed:

```text
Windows:
cmd.exe /d /s /c mvnw.cmd --batch-mode --no-transfer-progress clean test

Linux/macOS:
./mvnw --batch-mode --no-transfer-progress clean test
```

Neither users nor models can provide executable command text.

Validation runs:

- Inside the isolated repository workspace
- With a configurable timeout
- With bounded output capture
- With combined standard output and standard error
- With one log artifact per attempt
- With descendant-process termination on timeout

Example artifacts:

```text
agent-workspaces/
`-- {workflowId}/
    `-- revision-1/
        |-- repository/
        |-- snapshots/
        |   `-- baseline/
        |-- artifacts/
        |   |-- patch-proposal.json
        |   `-- final.diff
        `-- logs/
            |-- maven-test-attempt-1.log
            `-- maven-test-attempt-2.log
```

## Failure-driven repair

When Maven validation fails:

1. Compiler and test evidence is converted into a `ValidationFailure`.
2. The Repair Agent receives:
  - The engineering plan
  - The previous complete patch
  - The validation failure
  - The repository context
3. The Repair Agent produces a complete replacement patch.
4. The workspace is restored to its verified baseline.
5. The corrected patch is policy-validated and applied.
6. Maven validation runs again.
7. Execution stops when validation succeeds or the retry budget is exhausted.
8. Exhausted workflows restore and verify the baseline before failing.

Default attempt budget:

```yaml
agentic:
  execution:
    max-attempts: 2
```

## Repository isolation

Source repositories are read only from the approved root:

```text
scenario-repositories/
```

Every workflow receives a private copy:

```text
agent-workspaces/{workflowId}/revision-{revision}/repository
```

The original repository is never modified.

Controls include:

- Relative repository paths only
- Canonical root containment
- Symbolic-link rejection
- File-count limits
- Individual file-size limits
- Repository-context limits
- Protected-path rejection
- Patch file-count limits
- Total patch-size limits
- Expected SHA-256 validation for updates and deletes
- Baseline-hash verification after rollback

## API

Create an engineering workflow:

```http
POST /api/v1/engineering-workflows
Content-Type: application/json
```

Example:

```json
{
  "scenarioType": "BROWNFIELD",
  "requirement": "Add total and daily redirect analytics",
  "repositoryPath": "url-shortener"
}
```

PowerShell:

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body '{
      "scenarioType": "BROWNFIELD",
      "requirement": "Add total and daily redirect analytics",
      "repositoryPath": "url-shortener"
    }'

$workflow.status
$workflow.modelProvider
$workflow.changedFiles
$workflow.diff
$workflow.tasks
```

Retrieve a workflow:

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)"
```

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring MVC
- Jakarta Validation
- Spring Data JPA
- PostgreSQL 17
- H2
- Flyway
- Maven Wrapper
- Lombok
- Jackson
- OpenAI Responses API
- JUnit
- AssertJ
- Mockito
- Micrometer
- Prometheus
- Spring Boot Actuator
- Docker Compose
- Java virtual threads

## Run locally

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Run tests:

```powershell
$env:MODEL_PROVIDER = "deterministic"
.\mvnw.cmd clean test
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Check health:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/actuator/health"
```

## Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `MODEL_PROVIDER` | `deterministic` | Model implementation |
| `MODEL_BASE_URL` | OpenAI API | Model endpoint |
| `MODEL_API_KEY` | Empty | Optional live provider credential |
| `MODEL_NAME` | `gpt-4.1-mini` | Model name |
| `MODEL_TIMEOUT` | `PT60S` | Model-call timeout |
| `MODEL_MAX_OUTPUT_TOKENS` | `8000` | Output limit |
| `AGENT_WORKSPACE_ROOT` | `./agent-workspaces` | Isolated workspaces |
| `AGENT_REPOSITORY_ROOT` | `./scenario-repositories` | Approved repository root |
| `AGENT_MAX_ATTEMPTS` | `2` | Validation attempt budget |
| `AGENT_COMMAND_TIMEOUT` | `PT120S` | Maven timeout |
| `AGENT_MAX_OUTPUT_CHARACTERS` | `100000` | Build-output limit |
| `AGENT_MAX_FILES` | `2000` | Repository file limit |
| `AGENT_MAX_FILE_SIZE_BYTES` | `1048576` | Individual file limit |
| `AGENT_MAX_CONTEXT_CHARACTERS` | `200000` | Model context limit |
| `AGENT_MAX_PATCH_FILES` | `30` | Patch operation limit |
| `AGENT_MAX_PATCH_BYTES` | `524288` | Total patch limit |

## Testing

Tests cover:

- Application startup
- DAG validation
- Parallel execution and joins
- Entry and exit gates
- Context lineage
- Safe repository boundaries
- Workspace isolation
- Rollback verification
- Repository discovery
- Impacted-file identification
- Requirement ambiguity detection
- Dependency-aware planning
- Source proposal generation
- Test proposal generation
- Patch path and size policies
- Expected-hash validation
- Safe patch application
- OpenAI structured-response parsing without a network call
- Successful Maven execution evidence
- Failed Maven execution evidence
- Build-log artifact creation

Run:

```powershell
.\mvnw.cmd clean test
```

## Current limitations

At Commit 6:

- Live OpenAI execution requires reviewer-supplied credentials.
- The deterministic provider remains the CI default.
- Human clarification submission and revision-aware replanning are incomplete.
- Safe stop is represented in the domain but is not yet an asynchronous
  process-interruption API.
- Governance approvals are incomplete.
- Audit events are not yet durably persisted.
- Final reliability metrics and demonstration scenarios are incomplete.
- GitHub Actions and final container packaging are added in Commit 10.

## Commit roadmap

```text
Commit 1: Platform bootstrap
Commit 2: Stateful DAG orchestration
Commit 3: Isolated workspaces and repository reasoning
Commit 4: Deterministic and OpenAI-backed engineering agents
Commit 5: Source/test patch validation and application
Commit 6: Executable validation, repair and rollback
Commit 7: URL shortener and redirect analytics
Commit 8: Clarification, governance and safe stop
Commit 9: Audit lineage, metrics and demonstration scenarios
Commit 10: CI, Docker, testing and final documentation
```

## Completion rule

The platform is complete only when it demonstrates:

```text
Requirement
→ isolated repository
→ model-backed requirement and repository reasoning
→ production source proposal
→ test proposal
→ policy validation
→ isolated patch application
→ executable Maven validation
→ failure diagnosis
→ corrected patch
→ bounded retry
→ successful validation or verified rollback
→ reviewable diff and evidence
→ governed final outcome
```