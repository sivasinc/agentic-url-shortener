# Agentic Software Engineer

A controlled agentic software-engineering system demonstrated through a URL
shortener.

The platform is designed to transform an engineering requirement into a
reviewable and validated software change using controlled agent autonomy.

## Current status

Implemented through Commit 3:

- Spring Boot platform bootstrap
- PostgreSQL and Flyway configuration
- Health, metrics and Prometheus endpoints
- Explicit workflow dependency graphs
- Missing-dependency and cycle validation
- Sequential and parallel task execution
- Join synchronization
- Entry and exit gates
- Versioned cross-stage context
- Task-output lineage
- Workflow and task state transitions
- Approved repository-root enforcement
- Isolated per-workflow repository copies
- Revision-specific workspaces
- Baseline snapshots
- SHA-256 workspace manifests
- Verified workspace rollback
- Path-traversal rejection
- Absolute-path rejection
- Symbolic-link rejection
- Repository file-count and file-size limits
- Controlled file listing, reading and searching
- Build-system detection
- Source, test, migration, configuration and documentation discovery
- Requirement-based impacted-file identification

The platform does not yet claim complete agentic software-engineering
execution.

LLM-backed engineering agents, source and test generation, patch application,
build validation, failure-driven repair, fallback, governance, redirect
analytics and runnable scenarios are implemented in subsequent commits.

## Objective

The completed lifecycle is:

```text
Requirement
    |
    v
Requirement analysis
    |
    +-- ambiguous --> clarification --> replanning
    |
    v
Isolated repository workspace
    |
    v
Repository analysis
    |
    v
Architecture and dependency-aware plan
    |
    v
Source and test generation
    |
    v
Safe patch application
    |
    v
Build and test validation
    |
    +-- failure --> diagnosis --> repair --> retry
    |
    +-- retries exhausted --> fallback or rollback
    |
    v
Documentation and policy evaluation
    |
    v
Human approval
    |
    v
Reviewable diff and engineering summary
```

## Architecture

```text
                         Workflow API
                              |
                              v
                    Dependency Graph Engine
                              |
            +-----------------+-----------------+
            |                                   |
            v                                   v
   Requirement/Repository Agents       Governance and Policies
            |
            v
      Workspace Service
            |
            +-- Approved source repository
            +-- Isolated repository copy
            +-- Immutable baseline snapshot
            +-- Artifacts and logs
            |
            v
      Controlled Tools
            |
            +-- List files
            +-- Read text files
            +-- Search repository
            +-- Apply validated patch (planned)
            +-- Run allowlisted Maven (planned)
```

## Orchestration

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

Independent ready tasks execute concurrently using Java virtual threads.
Dependent tasks execute only after every required predecessor succeeds.

Task outputs are stored with:

- Context key and value
- Producing task ID
- Workflow revision
- Creation timestamp

## Workspace isolation

Every workflow operates on an isolated copy:

```text
agent-workspaces/
`-- {workflowId}/
    `-- revision-{revision}/
        |-- repository/
        |-- snapshots/
        |   `-- baseline/
        |-- artifacts/
        `-- logs/
```

The source repository is never modified directly.

Workspace creation:

1. Resolves the requested repository against the approved root.
2. Rejects absolute paths and traversal.
3. Verifies the real path remains inside the approved root.
4. Rejects symbolic links.
5. Enforces file-count and file-size limits.
6. Copies the repository into a revision-specific workspace.
7. Creates an immutable baseline copy.
8. Generates a SHA-256 manifest.

Rollback:

1. Verifies the workspace is managed by the configured workspace service.
2. Removes only the isolated working repository.
3. Restores the baseline snapshot.
4. Recomputes every file hash.
5. Fails if the restored manifest differs from the baseline.

## Controlled repository reasoning

The Repository Analysis Agent currently identifies:

- Maven, Gradle and Node build systems
- Nested Maven modules
- Production source files
- Test files
- Flyway migrations
- Configuration files
- Documentation files
- Requirement-related impacted files

Repository tools enforce:

- Workspace-relative paths
- File limits
- Text-only model context
- Context-character limits
- Symbolic-link rejection
- Bounded search output inputs

Repository analysis is currently deterministic. Commit 4 adds structured LLM
reasoning on top of this controlled evidence.

## Planned agents

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

Agents will perform real engineering actions. The Implementation Agent must
produce source changes; the Testing Agent must create or modify tests; the
Validation Agent must test the modified workspace; and the Repair Agent must
produce a changed patch after failure.

## Model strategy

```text
EngineeringModel
|-- DeterministicEngineeringModel
`-- LlmEngineeringModel
```

The deterministic provider supports tests, CI and reliable offline scenarios.

The LLM provider supports requirement interpretation, repository-specific
planning, code/test generation, failure diagnosis, repair and replanning.

The deterministic Java layer retains authority over paths, commands, patches,
timeouts, retries, approvals and rollback.

## Demonstration scenarios

### Greenfield

Generate a new URL-shortener vertical slice with source, tests, schema and
documentation.

### Brownfield

Inspect an existing URL shortener and add redirect analytics:

```http
GET /api/v1/urls/{shortCode}/analytics
```

The scenario must fail its first validation, generate a corrected patch and
pass its second validation.

### Ambiguous

Process:

```text
“Improve URL analytics”
→ detect ambiguity
→ request clarification
→ increment revision
→ invalidate downstream work
→ replan
→ resume
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
- JUnit
- AssertJ
- Mockito
- Micrometer
- Prometheus
- Spring Boot Actuator
- Docker Compose
- Java virtual threads

## Prerequisites

- JDK 21
- Docker Desktop
- Git

## Run locally

Start PostgreSQL:

```powershell
docker compose up -d postgres
docker compose ps
```

Run tests:

```powershell
.\mvnw.cmd clean test
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Check health:

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/actuator/health"
```

Check Prometheus:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/actuator/prometheus" |
    Select-Object -ExpandProperty Content |
    Select-String "jvm_"
```

## Configuration

| Environment variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/agentic_software_engineer` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `SERVER_PORT` | `8080` |
| `MODEL_PROVIDER` | `deterministic` |
| `MODEL_BASE_URL` | `https://api.openai.com/v1` |
| `MODEL_API_KEY` | Empty |
| `MODEL_NAME` | `gpt-4.1-mini` |
| `MODEL_TIMEOUT` | `PT60S` |
| `AGENT_WORKSPACE_ROOT` | `./agent-workspaces` |
| `AGENT_REPOSITORY_ROOT` | `./scenario-repositories` |
| `AGENT_MAX_FILES` | `2000` |
| `AGENT_MAX_FILE_SIZE_BYTES` | `1048576` |
| `AGENT_MAX_CONTEXT_CHARACTERS` | `200000` |
| `AGENT_MAX_ATTEMPTS` | `2` |
| `AGENT_COMMAND_TIMEOUT` | `PT120S` |
| `AGENT_MAX_OUTPUT_CHARACTERS` | `100000` |
| `AGENT_MAX_PATCH_FILES` | `30` |
| `AGENT_MAX_PATCH_BYTES` | `524288` |

Model credentials must only be provided through local environment variables and
must never be committed.

## Testing

Current tests cover:

- Application startup
- Valid DAG acceptance
- Missing-dependency rejection
- Cycle rejection
- Parallel branch execution
- Join synchronization
- Context lineage
- Exit-gate failure
- Safe path resolution
- Path-traversal rejection
- Absolute-path rejection
- Isolated repository copying
- Original-repository protection
- Existing-workspace collision rejection
- Workspace dirty-state detection
- Verified rollback
- Build-system discovery
- Source/test/migration/configuration/documentation discovery
- Requirement-based impacted-file identification

Run:

```powershell
.\mvnw.cmd clean test
```

## Security boundaries

- Source repositories are never modified directly.
- Repository access is limited to an approved root.
- Absolute paths and traversal are rejected.
- Symbolic links are rejected.
- File counts and sizes are bounded.
- Binary files are excluded from model context.
- Model output will be treated as untrusted.
- Arbitrary model-generated shell commands will not be executed.
- Patch and build tools will use application-owned policies.

## Current limitations

At Commit 3:

- Workflows are stored in memory.
- No workflow REST API exists yet.
- Repository analysis is deterministic.
- No LLM request is implemented.
- No source or test patch is generated.
- No patch is applied.
- Maven is not executed inside a workspace.
- No failure-driven repair exists.
- No provider fallback exists.
- Human approval continuation is not implemented.
- Clarification-driven replanning is not implemented.
- URL-shortener analytics is not implemented.
- Persistent audit lineage and required reliability metrics are not implemented.

## Commit roadmap

```text
Commit 1: Platform bootstrap
Commit 2: Stateful DAG orchestration
Commit 3: Isolated workspaces and repository reasoning
Commit 4: Deterministic and LLM-backed engineering agents
Commit 5: Source/test generation and patch application
Commit 6: Validation, repair, fallback, rollback and safe stop
Commit 7: URL shortener and redirect analytics
Commit 8: Clarification replanning and governance
Commit 9: Audit lineage, metrics and three scenarios
Commit 10: Testing, CI, Docker and final documentation
```

## Completion rule

The platform is complete only when it demonstrates:

```text
Requirement
→ isolated repository
→ repository analysis
→ real source change
→ real test change
→ patch application
→ validation failure
→ corrected patch
→ successful validation
→ reviewable diff
→ governed final outcome
```