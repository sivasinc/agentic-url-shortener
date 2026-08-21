# Agentic Software Engineer

A controlled agentic software-engineering system demonstrated through a URL
shortener.

The platform is intended to transform an engineering requirement into a
reviewable and validated software change.

## Current status

Project bootstrap only.

Agentic execution, repository reasoning, patch application, validation,
failure-driven repair, governance and demonstration scenarios will be added in
subsequent commits.

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Micrometer
- Prometheus
- Docker Compose

## Verify

```powershell
docker compose up -d postgres
.\mvnw.cmd clean test