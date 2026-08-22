# Consolidated Engineering Outcome

This document is the single summary deliverable for the assessment. It brings
together the implementation plan, design rationale, generated artifacts,
validation evidence, assumptions, risks and limitations. Runtime workflows also
generate their own revision-specific `artifacts/engineering-outcome.md` using
the same categories before entering the release-readiness approval gate.

## Requirement interpretation

Build an agentic software-engineering system that accepts an engineering
requirement and a repository, reasons about the requested change, generates
production and test code, validates the result using real engineering tools,
responds to failures with bounded repair/replanning, supports human governance,
and produces reviewable evidence rather than only managing workflow status.

The platform proves three distinct executions: a greenfield application built
from a build-only seed, a brownfield URL-shortener enhancement, and an ambiguous
requirement that requires clarification and replanning. The URL shortener is a
demonstration repository; it is not the platform's primary product.

## Delivery plan

1. Establish a production Spring Boot baseline, persistence and migrations.
2. Implement a stateful dependency graph with deterministic gates and parallel
   task execution.
3. Isolate repositories in revision-aware workspaces with snapshots, path
   controls and rollback verification.
4. Define provider-neutral engineering-model contracts and implement both a
   deterministic CI provider and an OpenAI-backed reasoning provider.
5. Add specialized requirement, repository, architecture, implementation,
   testing, repair and documentation agents.
6. Validate structured patches, apply them only inside the workspace and record
   hashes plus a reviewable diff.
7. Execute the repository's Maven tests, feed bounded failures to the repair
   agent and retry within policy.
8. Add clarification, release approval/rejection and safe-stop controls.
9. Persist audit lineage and sanitized agent request/response evidence and
   expose bounded operational metrics.
10. Package CI, coverage enforcement, hardened containers, architecture and
    reviewer documentation.
11. Provide executable greenfield, brownfield and ambiguous walkthroughs with
    distinct decomposition, generation and validation evidence.

## Architecture rationale

The architecture separates probabilistic reasoning from deterministic control.
Models may interpret requirements and propose plans or patches, but they cannot
directly access arbitrary files, execute arbitrary commands, approve their own
release or bypass retry limits. Java orchestration owns workflow state, path and
patch policy, command allowlists, timeouts, approvals, audit persistence,
rollback and safe stop.

This design was selected because an autonomous coding system needs model
flexibility without granting unconstrained machine access. It also permits a
deterministic provider to exercise the complete tool loop in CI while preserving
the same contracts used by the live OpenAI provider.

See [ARCHITECTURE.md](ARCHITECTURE.md) for component and trust-boundary detail.

## Implemented engineering outcomes

- Requirements are normalized into acceptance criteria, assumptions, risks and
  clarification questions.
- Ambiguous work pauses for human answers and restarts in a clean revision.
- Repository reasoning uses controlled read/search tools and bounded context.
- Architecture, implementation and testing agents produce structured outputs.
- Patch operations enforce normalized paths, operation policy, size limits and
  expected SHA-256 hashes.
- Maven validation is real, allowlisted, time-bounded and output-bounded.
- A failing patch can be rolled back, repaired and validated again.
- Validated work pauses for release-readiness approval.
- Safe stop terminates active validation and verifies workspace restoration.
- Agent, tool and governance events are persisted with revision lineage.
- Evidence is redacted before filesystem and PostgreSQL persistence.
- Every validated workflow generates a consolidated outcome artifact before
  approval.
- Scenario intent is preserved in downstream repository context: greenfield
  creates a new application, brownfield extends existing code, and ambiguous
  work cannot proceed without a new clarified revision.

## Artifact inventory

| Artifact | Purpose |
|---|---|
| Workflow API response | Current state, plan, changed files, diff and consolidated outcome |
| `artifacts/engineering-outcome.md` | Per-revision plan/rationale/artifacts/validation/risks/assumptions/limitations |
| `artifacts/audit/*.json` | Sanitized agent input/output envelopes |
| Applied-patch diff artifact | Reviewable generated source and test changes |
| Resulting SHA-256 manifest | Integrity evidence for generated files |
| `logs/maven-test-attempt-<n>.log` | Executable validation and failure evidence |
| PostgreSQL audit events | Durable workflow, agent, tool and human-decision lineage |
| Prometheus endpoint | Bounded workflow, model, task, validation and repair metrics |
| `target/site/jacoco` | Test coverage evidence |
| GitHub Actions artifacts | Surefire and JaCoCo reports retained from CI |
| `docs/ARCHITECTURE.md` | Architecture, trust boundaries and failure behavior |
| `docs/REVIEWER-GUIDE.md` | Minimal reproducible assessment demonstration |
| `docs/SCENARIOS.md` | Three end-to-end scenario walkthroughs and acceptance matrix |
| `scenario-repositories/greenfield-seed` | Build-only seed for generated greenfield source and tests |

## Validation evidence

The platform Maven `verify` lifecycle performs compilation, Spring context and
Flyway validation, unit tests, mocked OpenAI HTTP contract tests and JaCoCo
coverage enforcement. CI repeats this lifecycle on every push to `main` and
every pull request before building the container.

The deterministic repair scenario deliberately creates a Java compilation
failure. Its expected audit sequence is:

```text
VALIDATION_ATTEMPT_FAILED
REPAIR_STARTED
VALIDATION_ATTEMPT_SUCCEEDED
DOCUMENTATION / engineering-outcome.md generated
APPROVAL_REQUIRED
```

This provides executable evidence that build feedback changes the next agent
action rather than merely recording a failed status.

## Assumptions

- Submitted repositories are Maven-based Java projects containing the Maven
  wrapper expected by the controlled build tool.
- Repository paths are relative to the configured approved repository root.
- Java 21 is the execution target for the platform and demonstration fixture.
- PostgreSQL and the configured workspace filesystem are available while the
  service runs.
- The deterministic analytics templates are an offline test fixture, not a
  claim of general reasoning capability.
- A reviewer who selects the OpenAI provider supplies a valid credential through
  an environment variable.
- Human reviewers inspect the diff, validation evidence, risks and limitations
  before approving release readiness.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Prompt injection in repository content | Treat repository text as untrusted data; restrict the model to structured outputs and controlled tools |
| Model generates unsafe or stale changes | Path/operation/size policy and expected-hash checks before writes |
| Generated change breaks the repository | Real Maven validation, bounded repair and rollback verification |
| Runaway model/build execution | Provider timeout, command timeout, output bounds, retry bounds and workflow concurrency limit |
| Credential leakage into evidence | Environment-only credentials and centralized redaction before persistence |
| High-cardinality telemetry | Metrics use bounded provider/task/outcome tags and exclude workflow IDs |
| Human gate bypass | Release task cannot execute until revision-specific approval context exists |
| Container privilege abuse | Non-root user, read-only root filesystem, dropped capabilities and controlled writable volumes |
| Audit loss | Audit events and sanitized evidence content are persisted in PostgreSQL |

## Trade-offs

- The deterministic provider improves reproducibility but supports only the
  bounded assessment scenario; arbitrary engineering requirements require the
  OpenAI provider.
- Including a JDK and warmed Maven cache makes the runtime image larger, but
  generated Java validation is a required runtime capability.
- Evidence duplication in PostgreSQL and workspace files costs storage but
  improves durability and direct inspectability.
- Human approval reduces full autonomy but is intentional for high-impact
  release decisions.

## Limitations

- Active workflow/task state is in memory. A process restart preserves audit
  evidence but cannot resume interrupted work.
- The current deployment runs one orchestrator instance; distributed execution
  would require durable workflow state, worker leases and idempotent task claims.
- The controlled build tool currently supports Maven projects only.
- The OpenAI path is contract-tested with a mocked HTTP boundary but cannot be
  live-tested without reviewer-supplied credentials and network access.
- The deterministic repair trigger is deliberately explicit and is only for
  repeatable demonstration; real OpenAI repairs are triggered by natural build
  failures.
- Generated changes remain in isolated workspaces. Publishing a branch or pull
  request is intentionally outside the assessment's authorized tool scope.

## Release-readiness conclusion

The system demonstrates the complete agentic engineering loop: interpret,
clarify, inspect, plan, generate, apply, validate, diagnose, repair, document and
request human approval. Deterministic controls bound model autonomy, and the
result is accompanied by a consolidated outcome, source diff, test logs, hashes,
audit lineage and operational metrics.

Production expansion should prioritize durable workflow execution before
horizontal scaling or automated source-control publication.
