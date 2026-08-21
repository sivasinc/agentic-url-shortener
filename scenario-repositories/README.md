# Scenario Repositories

This directory contains controlled repositories used to demonstrate the
agentic software-engineering lifecycle.

The platform never modifies these source repositories directly. Each workflow
creates an isolated copy under `agent-workspaces` before reading or applying
changes.

Planned scenarios:

- `greenfield-seed`: minimal project used for new-feature generation
- `brownfield-url-shortener`: working URL shortener enhanced with analytics
- `ambiguous-url-shortener`: repository used for clarification and replanning

Generated build output and workflow workspaces are excluded from Git.