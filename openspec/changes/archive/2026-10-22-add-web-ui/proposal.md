# Proposal: Add the web UI

## Why

Everything DevPlane does today is reachable only through curl or Swagger UI. To
see the catalog, a developer reads JSON. To create a service, they copy request
bodies out of the README. A web UI turns the Phase 1 features into one place to
browse services and create them: register a service, download its project, and
give it a GitHub repository. It is also the first thing anyone sees when they run
DevPlane, so it is where the platform's design shows.

Roadmap: completes the Phase 1 item "Web UI for browsing the catalog and creating
services", the last item in Phase 1.

## What Changes

- DevPlane serves web pages at its existing address, rendered on the server.
  Every page and form works with JavaScript turned off, and htmx makes two
  interactions smoother when it's on.
- The pages, all following `docs/ui-guidelines.md` and the approved mockups in
  `docs/mockups/`:
  - `/` redirects to the catalog at `/services`.
  - **Catalog** (`/services`):
    - Services sorted by name, 20 to a page, each with its description, owner,
      lifecycle stage, repository, and when it was last updated.
    - An owner filter, and owner names that link to their filtered list. With
      JavaScript on, the filter updates the table without reloading the page.
    - When no services are registered, an introduction to DevPlane takes the
      table's place.
  - **Service page** (`/services/{name}`):
    - The service's details, and a diagram of its path from catalog entry to
      project to repository.
    - The project step has "Download project", which links to the existing zip
      download.
    - Until the service has a repository, the repository step has "Create
      repository" with a private or public choice, and shows GitHub failures in
      place. With JavaScript on, it shows progress while GitHub is called.
  - **Register a service** (`/register`): a form whose field errors appear next to
    each field, using the API's own validation.
  - **Unregister**: a confirmation page that says the service's GitHub repository
    is kept.
- Pages show errors as HTML: a page for an unknown service or path, and one for
  unexpected errors that hides their internals. Paths under `/api/v1` keep their
  problem details responses.
- **Cross-site requests can't change anything.** Pages and the Platform API
  refuse, with 403, any request that changes data and that a browser sends from
  another site. This closes a hole the API already has. DevPlane has no login, so
  any website open in the developer's browser can send it requests. A page with
  one line of script can make that browser call
  `POST /api/v1/services/{name}/repository`, which needs no body, and so create a
  repository with the developer's token. The new HTML forms would open the same
  hole for every page action. Requests from scripts and curl, which send no
  browser headers, are unaffected.
- The API's error message for an overlong description becomes "must be at most
  300 characters", replacing Bean Validation's "size must be between 0 and 300",
  so the API and the register form read the same.

**Out of scope**

- Editing a service's owner, description, or lifecycle in the UI. The API can
  already do it, and the register form can be reused later.
- Choosing a template. Only `java-rest-api` is listed, so pages show it as text.
  A choice appears when a second template is added.
- Authentication, and a light theme
- The public site on GitHub Pages: the next change, with its mockup already in
  `docs/mockups/landing.html`
- Showing whether a GitHub token is configured before "Create repository" is
  pressed. Without one, the repository step explains how to set it.
- Protecting against DNS rebinding by checking the `Host` header. It's a separate
  hardening step, recorded as future work.
- Browser-automated tests. Pages are tested on the server, and what only a browser
  shows is covered by the guidelines' screenshot check.
- Keyboard shortcuts, live updates, and anything from Phase 2 onward
  (deployments, metrics, AI operations)

## Capabilities

### New Capabilities

- `web-ui`: The developer's web pages:
  - browsing and filtering the catalog
  - a service's page and its path from catalog entry to repository
  - registering and unregistering services
  - creating a service's repository
  - how pages show errors, and how they refuse form submissions from other sites

### Modified Capabilities

- `platform-api`: adds a requirement that the API rejects requests that change
  data when a browser sends them from another site.

## Impact

- **Code:**
  - A new `web` package holds the page controllers and page error handling. A
    service page needs the catalog, templates, and provisioning together, so it
    can't live in any one of those packages. `web` depends on all three, and none
    of them depends on `web`.
  - The catalog's `register`, `list`, and `unregister` become public and return
    the read-only `RegisteredService`, which gains the lifecycle and timestamps.
    The JPA entity never leaves the catalog.
  - The request records and exceptions that pages reuse become public, and so
    does `RepositoryProvisioner`.
  - `ApiExceptionHandler` leaves errors outside `/api/` to Spring Boot's HTML
    error pages.
  - A request check in the `api` package refuses requests from other sites.
- **Resources:** Thymeleaf templates in `src/main/resources/templates/`, plus one
  stylesheet and the self-hosted fonts in `src/main/resources/static/`. There's no
  frontend build step.
- **Dependencies:**
  - Spring Boot's Thymeleaf starter
  - htmx 2.0.11, as a WebJar, with `webjars-locator-lite`
  - For tests: the Thymeleaf test starter and jsoup
  - Vendored files: the Atkinson Hyperlegible Next and Mono fonts (SIL Open Font
    License, about 35KB) and a few Lucide icon paths (ISC license)
- **API:**
  - No endpoint is added or changed.
  - Requests that change data and come from a browser on another site now get
    403.
  - The description length message changes as described above.
- **Docs:**
  - README.md: open DevPlane in a browser
  - docs/architecture.md: the `web` package, the catalog's public methods, error
    pages, and the cross-site check
  - docs/ui-guidelines.md: any value the implementation adds
