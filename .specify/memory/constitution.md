# TechStore Constitution

## Core Principles

### I. Domain-Driven Microservices

The system MUST be organized around independent business capabilities.
Each microservice MUST have a clearly defined responsibility and bounded
business context.

Microservices MUST NOT share their databases. Each service owns its
persistent data and exposes access through explicit APIs or events.

Communication between services MUST use well-defined contracts.
Synchronous REST communication SHOULD be used when an immediate response
is required, while asynchronous messaging SHOULD be preferred for
event-driven workflows and decoupled processes.

### II. Hexagonal Architecture

Each backend microservice MUST follow Hexagonal Architecture
(Ports and Adapters).

The domain MUST contain business rules and MUST NOT depend on
frameworks, databases, messaging systems, HTTP or infrastructure
technologies.

Dependencies MUST point toward the domain and application layers.

External technologies such as Spring Boot, JPA, PostgreSQL and Kafka
MUST be implemented through adapters.

### III. Clean Code and SOLID

The codebase MUST follow Clean Code and SOLID principles.

Classes and methods MUST have clear responsibilities.
Business logic MUST NOT be duplicated unnecessarily.

Complexity MUST be justified.

The implementation SHOULD favor simple and maintainable solutions
over unnecessary abstractions or premature optimization.

### IV. Test-First Quality

Business rules MUST be covered by automated unit tests.

Use cases MUST have automated tests.

Changes involving persistence, REST APIs or inter-service communication
SHOULD include appropriate integration tests.

The project SHOULD use JUnit, Mockito and Testcontainers where applicable.

A feature MUST NOT be considered complete while its required tests are
failing.

### V. API and Contract Discipline

All backend services MUST expose explicit and documented contracts.

REST APIs MUST use appropriate HTTP methods and status codes.

API contracts MUST be documented using OpenAPI.

Breaking API changes MUST be explicitly identified and evaluated before
implementation.

Inter-service events MUST have documented schemas and stable event names.

### VI. Security by Default

Authentication and authorization MUST be implemented for protected
operations.

Passwords MUST never be stored in plain text and MUST use a secure
password hashing mechanism such as BCrypt.

JWT MUST be used for stateless authentication where applicable.

Administrative operations MUST require explicit authorization.

Sensitive information MUST NOT be written to application logs.

### VII. Observability

Every production-oriented microservice MUST provide sufficient
observability to diagnose failures.

Services SHOULD expose health and operational metrics through
Spring Boot Actuator and compatible monitoring systems.

Logs MUST provide enough contextual information to identify the
service, operation and relevant request or correlation identifier.

Failures in asynchronous processing MUST be observable and diagnosable.

### VIII. Frontend Architecture

The React frontend MUST separate presentation, application logic,
API communication and reusable components.

The frontend MUST NOT implement authoritative business rules that
belong to backend services.

Security-sensitive calculations such as order totals, discounts,
inventory availability and payment amounts MUST be validated by the
backend.

The frontend SHOULD provide a responsive and consistent user experience.

### IX. Infrastructure and Reproducibility

The development environment MUST be reproducible.

Infrastructure dependencies SHOULD be containerized using Docker.

Local development SHOULD be executable through Docker Compose where
practical.

Configuration MUST be externalized from application code.

Secrets MUST NOT be committed to the repository.

### X. Spec-Driven Development

Development MUST follow the Spec Kit workflow.

Features SHOULD progress through:

Specify
→ Clarify
→ Plan
→ Tasks
→ Analyze
→ Implement
→ Converge

Implementation MUST remain consistent with the approved specification,
implementation plan and task list.

When implementation requirements change, the relevant specification
artifacts MUST be updated rather than silently deviating from them.

## Technology Constraints

The initial technology stack is:

- Java 21+
- Spring Boot
- Maven
- React
- TypeScript
- Vite
- PostgreSQL
- Apache Kafka
- Spring Security
- JWT
- Docker
- Docker Compose
- JUnit 5
- Mockito
- Testcontainers
- OpenAPI

Technology choices MAY evolve when justified by project requirements,
but changes MUST preserve the architectural principles defined in this
constitution.

## Development Workflow

Every feature MUST have clearly defined functional requirements before
implementation.

The development process SHOULD proceed incrementally.

Large features MUST be divided into smaller independently implementable
increments.

Before implementation:

1. Requirements MUST be defined.
2. Ambiguities SHOULD be resolved.
3. The technical plan MUST be created.
4. Tasks MUST be generated.
5. Consistency MUST be verified.

During implementation:

1. Tasks SHOULD be completed incrementally.
2. Tests MUST be maintained.
3. Architectural boundaries MUST be respected.
4. Documentation MUST be updated when contracts change.

After implementation:

1. Tests MUST pass.
2. The implementation MUST be checked against the specification.
3. Remaining gaps MUST be documented as tasks.
4. The feature MUST converge with its specification.

## Governance

This constitution is the highest-level architectural and development
guideline for the TechStore project.

All specifications, plans and implementations MUST comply with these
principles.

When a requirement conflicts with this constitution, the conflict MUST
be explicitly identified and resolved before implementation.

Amendments to this constitution MUST be documented and versioned.

Changes to architectural principles MUST include a clear justification.

Practical implementation details that are likely to change SHOULD NOT
be placed in the constitution and SHOULD instead be documented in the
appropriate specification or implementation plan.

**Version**: 1.0.0 | **Ratified**: 2026-09-26 | **Last Amended**: 2026-09-26