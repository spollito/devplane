# DevPlane

DevPlane is an internal developer platform for standardized service creation,
deployment, observability, and AI-assisted operations.

## Overview

Modern engineering teams often repeat the same setup work when creating and
operating services: repositories, infrastructure, CI/CD, observability,
deployment configuration, and operational tooling.

DevPlane aims to provide a standardized self-service platform for these workflows.

## Vision

Developers should be able to define what they want to build while DevPlane
handles the underlying platform requirements.

Example:

> Create a Java REST API with PostgreSQL and deploy it to staging.

DevPlane will eventually automate:

- Service scaffolding
- Repository provisioning
- CI/CD configuration
- Infrastructure provisioning
- Containerized deployment
- Observability
- Service ownership
- Operational diagnostics
- AI-assisted remediation

## Project Roadmap

Each phase is a thin slice that works end to end, runs locally, and costs $0.
Items marked *stretch* are optional extras once the rest of their phase works.

### Phase 1 — Platform Core
- [x] Service catalog: services with an owner and a lifecycle stage
- [x] Platform API: versioned REST API with OpenAPI docs
- [x] Golden-path service template: Java REST API with PostgreSQL
- [x] Service creation workflow: generate a new service from the template
- [x] Repository provisioning: push the generated service to a new GitHub repository
- [x] Web UI for browsing the catalog and creating services

### Phase 2 — Deployment
- [ ] Containerization: generated services include a Dockerfile
- [ ] CI/CD: generated services include a GitHub Actions pipeline
- [ ] Kubernetes deployment to a local kind cluster
- [ ] Deployment history and status for each service
- [ ] *Stretch:* cloud infrastructure with Infrastructure as Code (Terraform)

### Phase 3 — Observability
- [ ] Metrics: generated services expose Prometheus metrics
- [ ] Logs: generated services write structured JSON logs
- [ ] Service health monitoring shown in the catalog
- [ ] *Stretch:* Grafana dashboards and distributed tracing (OpenTelemetry)

### Phase 4 — AI Operations
- [ ] Deployment diagnostics: a tool-using agent that explains failed deployments
- [ ] Human approval before any suggested fix is applied
- [ ] MCP server exposing the catalog and workflows to AI assistants
- [ ] *Stretch:* automated remediation for known failure types

## Architecture

Architecture documentation will live in:

`docs/architecture.md`

Design decisions for individual features are recorded in each change's
`design.md` (see Development Workflow below).

## Running Locally

Prerequisites:

- JDK 21
- A running Docker runtime, such as Docker Desktop. DevPlane uses it for its
  local PostgreSQL database and for integration tests.

Gradle comes with the repository through the wrapper, so it doesn't need to be
installed.

```sh
./gradlew bootRun        # start DevPlane on http://localhost:8080 (starts PostgreSQL too)
./gradlew test           # run the fast tests
./gradlew templateTest   # generate a project from each template and build it (a minute or two)
./gradlew build          # compile, run every test including templateTest, and package
```

While DevPlane is running, the interactive API documentation is at
http://localhost:8080/swagger-ui.html.

`templateTest` leaves the project it generates in
`build/generated-projects/orders-api`, so you can open it, or run it with
`./gradlew bootRun` from that directory.

## Creating a Service

### Using the web UI

After `./gradlew bootRun`, open http://localhost:8080 in a browser:

1. Press **Register service**, and enter the service's name, owner, and lifecycle
   stage.
2. On the service's page, press **Download project** for a zip of its generated
   project.
3. With a GitHub token set (see [Push it to GitHub](#push-it-to-github)), choose
   private or public and press **Create repository**.

Every page works with JavaScript turned off.

### Using the API

With DevPlane running, register a service in the catalog, then download the
project DevPlane generates for it:

```sh
# Register the service
curl -X POST http://localhost:8080/api/v1/services \
  -H "Content-Type: application/json" \
  -d '{"name": "orders-api", "owner": "payments-team", "description": "Order management REST API"}'

# List the templates you can generate from
curl http://localhost:8080/api/v1/templates

# Download the generated project, then build it
curl -o orders-api.zip http://localhost:8080/api/v1/services/orders-api/project
unzip orders-api.zip
cd orders-api
./gradlew build
```

The project comes from the `java-rest-api` template unless you add
`?template=<name>` to the download. Each download generates the project from the
service's current details, and DevPlane keeps no copy of it.

### Push it to GitHub

DevPlane can also create a GitHub repository for the service and push the
generated project to it. It needs a fine-grained personal access token:

1. On GitHub, open **Settings → Developer settings → Personal access tokens →
   Fine-grained tokens** and generate a new token.
2. Give it a short expiration, such as 30 days.
3. Under **Repository access**, choose **All repositories**. GitHub requires this
   for a token that creates repositories.
4. Under **Repository permissions**, set **Administration** and **Contents** to
   **Read and write**.

Then start DevPlane with the token, and provision the service's repository:

```sh
export DEVPLANE_GITHUB_TOKEN=github_pat_...
./gradlew bootRun

# In another terminal: create a private repository from the java-rest-api template
curl -X POST http://localhost:8080/api/v1/services/orders-api/repository
```

The response holds the repository's `url`, and `GET /api/v1/services/orders-api`
shows it too. Send a JSON body to choose the template or make the repository
public, for example `{"template": "java-rest-api", "visibility": "public"}`.
Each service gets one repository, named after the service, in the account that
owns the token. DevPlane never deletes repositories, including when a service is
unregistered.

## Development Workflow

DevPlane is built with spec-driven development using
[OpenSpec](https://github.com/Fission-AI/OpenSpec). Every feature starts as a
change proposal, and code is written against specs that were agreed on first.

- `openspec/specs/`: how the system behaves today, one folder per capability
- `openspec/changes/`: proposed changes, each with a proposal, spec changes,
  a design, and a task list
- `openspec/changes/archive/`: completed changes, kept as a record of decisions

Each change goes through the same loop, driven from Claude Code:

1. **Explore** (`/opsx:explore`, optional): think through the problem first
2. **Propose** (`/opsx:propose`): write the proposal, specs, design, and tasks
3. **Review**: read and adjust the plan before any code is written
4. **Apply** (`/opsx:apply`): implement the tasks, with tests
5. **Verify** (`/opsx:verify`): check the code against the specs
6. **Archive** (`/opsx:archive`): merge the change's specs into `openspec/specs/`

Fixes that don't change behavior, such as typos, dependency bumps, and
refactors, skip the proposal. CI validates every spec and change on each push
and pull request.

To browse specs and changes locally, install the OpenSpec CLI (Node.js 20.19+),
at the version CI uses:

```sh
npm install -g @fission-ai/openspec@1.14.1
openspec list     # changes in progress
openspec view     # interactive dashboard
```

## Status

DevPlane is under active development, starting with Phase 1. Changes in
progress live in `openspec/changes/`.
