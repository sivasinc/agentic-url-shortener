# Scenario Repositories

This directory contains controlled repositories used to demonstrate the
agentic software-engineering lifecycle.

The platform never modifies these source repositories directly. Each workflow
creates an isolated copy under `agent-workspaces` before reading or applying
changes.

Executable scenarios:

- `greenfield-seed`: build-only Maven project into which agents generate a new
  URL-shortener application and tests.
- `url-shortener` with `BROWNFIELD`: existing application enhanced with
  redirect analytics.
- `url-shortener` with `AMBIGUOUS`: revision 1 stops for clarification and
  revision 2 replans, generates and validates the clarified change.

Generated build output and workflow workspaces are excluded from Git.
