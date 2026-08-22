# Reviewer Guide

Start with [Executable Assessment Scenarios](SCENARIOS.md) and run all three
walkthroughs. They demonstrate distinct greenfield generation, brownfield
enhancement, and ambiguity-driven clarification/replanning paths rather than
three labels over the same implementation.

This guide demonstrates the assessment's agentic behavior without requiring a
paid model credential.

## Fast verification

```powershell
.\mvnw.cmd clean verify
docker compose up --build -d
docker compose ps
```

Wait for both services to become healthy:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

## Prove the feedback loop

Submit the controlled repair scenario:

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows" `
    -ContentType "application/json" `
    -Body (@{
        scenarioType = "BROWNFIELD"
        requirement = "Add total and daily redirect analytics and demonstrate repair"
        repositoryPath = "url-shortener"
    } | ConvertTo-Json)

do {
    Start-Sleep -Seconds 2
    $workflow = Invoke-RestMethod `
        "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)"
} while ($workflow.status -in @("CREATED", "RUNNING"))

$workflow | Select-Object id, revision, status, modelProvider, failureMessage
```

Expected status: `AWAITING_APPROVAL`.

Retrieve lineage:

```powershell
$events = Invoke-RestMethod `
    "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/audit-events"

$events |
    Select-Object revision, type, actor, taskType, detail |
    Format-Table -AutoSize
```

The critical sequence is:

```text
VALIDATION_ATTEMPT_FAILED
REPAIR_STARTED
VALIDATION_ATTEMPT_SUCCEEDED
APPROVAL_REQUIRED
```

That sequence proves the application generated a change, executed a real build,
fed failure evidence into a repair agent, replaced the failed patch, re-ran the
build and stopped at a human policy gate.

## Inspect generated outputs

```powershell
$workflow.changedFiles
$workflow.diff
$workflow.engineeringOutcome.relativePath
$workflow.engineeringOutcome.content

$evidence = Invoke-RestMethod `
    "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/evidence"

$evidence | Select-Object eventId, artifact, mediaType
$evidence[0].content
```

The evidence envelope identifies the logical agent, provider/model, workflow
revision, selected request context and structured response. Secret-like fields
are redacted before database or filesystem persistence.

`engineeringOutcome.content` is the Requirement #8 consolidated deliverable. It
combines the plan, rationale, generated artifacts, validation evidence, risks,
assumptions and limitations for this exact workflow revision.

## Complete governance

```powershell
$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/engineering-workflows/$($workflow.id)/approvals/release-readiness" `
    -ContentType "application/json" `
    -Body (@{
        actor = "reviewer@example.com"
        approved = $true
        reason = "Generated diff and validation evidence reviewed"
    } | ConvertTo-Json)
```

Poll once more and confirm `COMPLETED`.

## Optional live model

The same workflow can use OpenAI without code changes:

```powershell
$env:MODEL_PROVIDER = "openai"
$env:MODEL_API_KEY = Read-Host "OpenAI API key"
$env:MODEL_NAME = "gpt-4.1-mini"
docker compose up --build -d
```

The key must not be committed. Automated tests mock the HTTP boundary, so a
credential is not required to review or build the submission.

## Clean up

```powershell
docker compose down
```

Add `-v` only when intentionally deleting PostgreSQL, workspace and Maven-cache
volumes.
