# DevPlane

A small internal developer platform: service catalog, golden-path templates,
deployment, observability, and AI-assisted operations. It's a personal portfolio
project; README.md has the vision and the phased roadmap.

## Commands

Docker must be running for tests and `bootRun`.

- `./gradlew build`: compile, run all tests (including `templateTest`), and
  package. Must pass before a task is checked off.
- `./gradlew test`: the fast tests. `./gradlew test --tests '*ClassNameTests'` runs
  one test class.
- `./gradlew templateTest`: generates `orders-api` from the `java-rest-api` template
  into `build/generated-projects/` and runs that project's own build. Template
  files live in `src/main/resources/service-templates/`; DevPlane doesn't compile
  them, so this is how template mistakes are caught.
- `./gradlew bootRun`: start the app on http://localhost:8080. Spring Boot starts
  PostgreSQL from `compose.yaml`.
- `./gradlew siteBuild`: assemble the public site from `site/` into `build/site/`,
  with the app's stylesheet and fonts and a generated example project.
- `node --test "site/demo/*.test.js"`: test the demo's logic. It needs Node 22, and
  has no packages to install.

## Web UI

- Web UI pages and the public site follow `docs/ui-guidelines.md`: its design
  values, components, writing style, accessibility minimums, and Never list.
  Add a new color, size, or component to the guidelines before using it.
- `docs/mockups/` holds the approved static mockups. Use them as the visual
  reference.
- A change to a page is done only after the screenshot check in the guidelines:
  Playwright screenshots at 1280px and 390px, checked against the guidelines.
  The Playwright MCP server is configured in `.mcp.json`. It needs Node 20+, and
  it saves screenshots to `.playwright-mcp/`, which Git ignores.

## Spec-driven development with OpenSpec

Behavior is agreed in specs before code is written.

- `openspec/specs/` is the source of truth for current behavior. Its requirements
  change only when a change is archived, never by direct edits.
- `openspec/changes/<name>/` holds work in progress: proposal, delta specs, design,
  and tasks. `openspec/config.yaml` holds the project context and artifact rules.
- Workflow: `/opsx:explore` (optional) → `/opsx:propose` → user review →
  `/opsx:apply` → `/opsx:verify` → `/opsx:archive`.

Rules:

- Any change to observable behavior starts with a change proposal. Fixes with no
  behavior change (typos, dependency bumps, pure refactors) can skip it.
- After proposing, stop. Implementation starts only when the user asks for it.
- Implement only what the change's specs and tasks describe. If they turn out to be
  wrong, update the artifacts (`/opsx:update`) before writing more code.
- After editing anything under `openspec/`, run `openspec validate --all --strict`.
  CI runs the same check.

## Git

- Commits are authored by the person running Claude Code. Commit messages and
  pull request descriptions have no `Co-Authored-By` trailer and no "Generated
  with Claude Code" footer.
- Work on a branch named after the change (for example `add-web-ui`), and open a
  pull request. Anyone with write access can merge pull requests, including their
  own.
- Commit or push only when asked.
