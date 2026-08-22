# Executable Assessment Scenarios

This document is the reviewer walkthrough for the three scenarios required by
the assignment. Each scenario uses the same controlled agentic loop:

```text
requirement analysis -> repository reasoning -> dependency-aware planning
-> source and test generation -> policy-checked patch application
-> Maven validation/repair -> consolidated outcome -> human approval
```

Run PostgreSQL and the platform before using the commands:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Use this polling helper after submitting a workflow:

```powershell
function Wait-EngineeringWorkflow([string] $id) {
    do {
        Start-Sleep -Seconds 1
        $result = Invoke-RestMethod `
            "http://localhost:8080/api/v1/engineering-workflows/$id"
    } while ($result.status -in @("CREATED", "RUNNING"))
    return $result
}
```

## Scenario 1: Greenfield application generation

### Input

The controlled `greenfield-seed` contains Maven configuration and no
application source. The agents must create a working vertical slice rather
than modify an existing application.

```powershell
$greenfield = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "GREENFIELD"
        requirement = "Create a URL shortener with create and redirect APIs"
        repositoryPath = "greenfield-seed"
    } | ConvertTo-Json)

$greenfield = Wait-EngineeringWorkflow $greenfield.id
```

### Decomposition and orchestration

- Requirement analysis creates measurable acceptance criteria.
- Repository analysis confirms a Maven build but no application source.
- The architecture agent plans an application boundary, implementation and
  tests.
- Implementation and testing agents run as dependency-compatible branches.
- The patch agent creates the Spring Boot application, API, service and tests.

### Validation and evidence

Expected state: `AWAITING_APPROVAL`.

```powershell
$greenfield | Select-Object id, revision, status, changedFiles
$greenfield.engineeringPlan.tasks | Select-Object id, name, dependencies
$greenfield.engineeringOutcome.content

Get-Content `
    ".\agent-workspaces\$($greenfield.id)\revision-1\logs\maven-test-attempt-1.log"
```

The changed files must include `GreenfieldUrlShortenerApplication.java`,
`ShortUrlController.java`, `ShortUrlService.java` and
`ShortUrlServiceTest.java`. The Maven log must contain `BUILD SUCCESS`.

## Scenario 2: Brownfield repository enhancement

### Input

The `url-shortener` fixture is an existing tested application. The agents must
reason about its conventions and add analytics without replacing its current
URL behavior.

```powershell
$brownfield = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "BROWNFIELD"
        requirement = "Add total and UTC daily redirect analytics"
        repositoryPath = "url-shortener"
    } | ConvertTo-Json)

$brownfield = Wait-EngineeringWorkflow $brownfield.id
```

### Decomposition and orchestration

- Repository reasoning identifies the existing controller, service, entity,
  repository and test conventions.
- Planning separates production analytics changes from test generation.
- The patch preserves the existing package/build structure and adds an event,
  repository, service, interceptor, configuration and versioned API.
- Validation compiles and tests the complete modified brownfield workspace.

### Validation and evidence

Expected state: `AWAITING_APPROVAL`.

```powershell
$brownfield | Select-Object id, revision, status, changedFiles
$brownfield.engineeringPlan
$brownfield.engineeringOutcome.content

Get-Content `
    ".\agent-workspaces\$($brownfield.id)\revision-1\logs\maven-test-attempt-1.log"
```

The diff must contain analytics production code and tests, and the log must
contain `BUILD SUCCESS`.

## Scenario 3: Ambiguity, clarification and replanning

### Input and deliberate stop

`AMBIGUOUS` is an explicit scenario contract. Revision 1 must stop after the
requirement agent; it cannot silently invent acceptance criteria.

```powershell
$ambiguous = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "AMBIGUOUS"
        requirement = "Improve URL analytics"
        repositoryPath = "url-shortener"
    } | ConvertTo-Json)

$ambiguous = Wait-EngineeringWorkflow $ambiguous.id
$ambiguous | Select-Object id, revision, status, clarificationQuestions
```

Expected state: `AWAITING_CLARIFICATION`, revision `1`, with concrete
clarification questions and no generated patch.

### Human clarification and new revision

```powershell
$ambiguous = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($ambiguous.id)/clarifications" `
    -ContentType "application/json" `
    -Body (@{
        actor = "reviewer@example.com"
        answers = @(
            "Track total and UTC daily redirects per short code",
            "Expose a read-only versioned REST endpoint"
        )
    } | ConvertTo-Json -Depth 4)

$ambiguous = Wait-EngineeringWorkflow $ambiguous.id
```

### Replanning and validation evidence

Expected state: `AWAITING_APPROVAL`, revision `2`.

```powershell
$ambiguous | Select-Object id, revision, status, changedFiles
$ambiguous.requirementAnalysis.normalizedRequirement
$ambiguous.engineeringOutcome.content

Invoke-RestMethod `
    "http://localhost:8080/api/v1/engineering-workflows/$($ambiguous.id)/audit-events" |
    Select-Object revision, type, actor, taskType, occurredAt |
    Format-Table -AutoSize
```

The audit lineage must show `CLARIFICATION_REQUIRED` in revision 1,
`CLARIFICATION_SUBMITTED` in revision 2, a newly generated plan, patch and
successful Maven validation. Revision 2 uses a fresh isolated workspace.

## Scenario acceptance matrix

| Scenario | Repository state | Agent decision | Generated outcome | Validation gate |
|---|---|---|---|---|
| Greenfield | Build-only seed | Establish new application architecture | New source and tests | Generated Maven project passes |
| Brownfield | Existing URL shortener | Preserve conventions and extend behavior | Analytics source and tests | Existing and generated tests pass |
| Ambiguous | Existing URL shortener | Stop and request human input | Revision-2 analytics change | Replanned workspace passes |

All successful scenarios stop at `AWAITING_APPROVAL`; release remains a human
decision. Runtime artifacts are isolated under `agent-workspaces` and are not
committed as fabricated evidence.
