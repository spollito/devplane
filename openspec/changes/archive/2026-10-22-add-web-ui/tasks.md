# Tasks

## 1. Groundwork

- [x] 1.1 Add the dependencies from the design:
  - `spring-boot-starter-thymeleaf`
  - `org.webjars.npm:htmx.org:2.0.11` and `org.webjars:webjars-locator-lite`
  - for tests: `spring-boot-starter-thymeleaf-test` and `org.jsoup:jsoup:1.23.2`

  Also set `user.timezone=UTC` for every Gradle `Test` task. Add the static
  assets:
  - `static/css/devplane.css`, from `docs/mockups/style.css` without the
    mockup-only state rules
  - the Atkinson Hyperlegible Next 400 and 600 and Mono 400 latin `woff2` files
    from Fontsource, with `OFL.txt` and `@font-face` rules

  Verify with an integration test that `/css/devplane.css`, each font, and
  `/webjars/htmx.org/dist/htmx.min.js` return 200, and that `./gradlew build`
  passes.
- [x] 1.2 Add the `web` package:
  - the `layout.html` fragments: head with title, "Skip to content" link, top bar
    with "Services" and "API docs", and the notice area
  - `fragments/icons.html` (Lucide paths, with the ISC attribution) and
    `fragments/lifecycle.html`
  - `PageTimes`, for exact times in the JVM's zone and relative times from the
    application's `Clock`
  - `CatalogPages`, with `GET /` redirecting to `/services`
  - a test helper that parses an `MvcTestResult` into a jsoup `Document`

  Verify the "Opens the catalog from the root" scenario. Unit test `PageTimes`:
  the exact format, and each relative form ("just now", minutes, hours,
  "Yesterday", days, weeks, months, years). Describe the `web` package in
  docs/architecture.md.

## 2. Catalog and provisioning operations for pages

- [x] 2.1 Rework the catalog's public operations:
  - `ServiceCatalog.register`, `list`, and `unregister` become public and return
    `RegisteredService`, which gains `lifecycle`, `createdAt`, and `updatedAt`.
    `update` returns it too.
  - `ServiceResponse` is built from `RegisteredService`.
  - `ServiceEntity` stays inside `ServiceCatalog`.
  - `RegisterServiceRequest`, `ServiceNotFoundException`, and
    `DuplicateServiceNameException` become public.

  Verify that every existing catalog, templates, and provisioning test passes,
  with only `LookUpServiceTests`' expected record updated. Change
  docs/architecture.md's rule to "other features change services only through
  `ServiceCatalog`".
- [x] 2.2 Give `description`'s `@Size` the message "must be at most 300
  characters", from a `CatalogConstraints` constant. Make
  `RepositoryProvisioner.provision` and `ProvisionRepositoryRequest` (with
  `templateOrDefault` and `isPrivate`) public. Verify that the overlong-description
  API test asserts the new message in `errors`, and that the provisioning tests
  pass.

## 3. Errors and cross-site requests

- [x] 3.1 Set up error handling:
  - In `ApiExceptionHandler`, paths outside `/api/` call `response.sendError`
    and return no body, in both `handleExceptionInternal` and
    `handleUnexpected`.
  - Add the `error/4xx.html` and `error/5xx.html` templates.
  - Add `web.PageExceptionHandler`, scoped to the `web` package at highest
    precedence.

  Verify:
  - "Unknown page": `GET /does-not-exist` returns 404 without a problem details
    body, and `GET /error` with the servlet's error attributes renders HTML
    linking to `/services`.
  - "API paths keep problem details", with an `Accept` header of `text/html`
  - "Unexpected error", in a `@WebMvcTest` slice with a test-only page controller
    that throws
  - `ApiExceptionHandlerTests` still passes.

  Describe the three error paths in docs/architecture.md.
- [x] 3.2 Add `api.CrossSiteRequestInterceptor` (Fetch Metadata first, then
  `WebUtils.isSameOrigin`), the `WebMvcConfigurer` that registers it, and
  `CrossSiteRequestException` (403). Verify the platform-api scenarios:
  - "Rejects a request from another site", checking that `FakeGitHub`'s request
    log is empty
  - "Rejects another site's origin"
  - "Accepts DevPlane's own pages and scripts"
  - "Reading is unaffected"

  Describe the check in docs/architecture.md.

## 4. Catalog page

- [x] 4.1 Build `GET /services`:
  - the services table, from a `ServiceRow` view record, with the repository as
    `owner/name` or "None", "No description", and the relative update time with
    the exact time in its `title`
  - the owner under the name on narrow screens
  - paging: 20 per page, numbered from 1; invalid numbers show page 1 and numbers
    past the end show the last page; "21–40 of 57" with Previous and Next;
    "3 services" when everything fits

  Verify the scenarios "Lists services alphabetically", "Shows each service's
  repository", "Shows a missing description", "Shows the second page", "Hides
  paging when everything fits", and "Corrects page numbers that don't exist".
- [x] 4.2 Add filtering and the empty catalog:
  - the "Owner" filter form, owner links, the no-match state, a blank owner
    meaning every service, and paging links that keep the owner
  - the filter's htmx attributes: `hx-get` on input after 300ms or on submit,
    with `hx-select` and `hx-target` on the results region, and
    `hx-push-url="true"`
  - the empty-catalog introduction, using a new `fragments/path.html`

  Verify the scenarios "Lists one owner's services", "Owner names link to their
  list", "Owner with no services", "Blank owner", and "Empty catalog".
- [x] 4.3 Run the screenshot check from docs/ui-guidelines.md on the catalog, at
  1280px and 390px:
  - several pages of services in all three lifecycle stages
  - a filtered list, the no-match state, and the empty catalog

  With JavaScript on, check the "Filters in place" scenario. Fix what the check
  finds.

## 5. Service page

- [x] 5.1 Build `GET /services/{name}`:
  - the summary: `h1`, description or "No description", owner link, lifecycle,
    and the registered and updated times
  - the path steps:
    - Catalog entry
    - Project, with the template name and "Download project" linking to
      `/api/v1/services/{name}/project`
    - Repository: either the form posting to `/services/{name}/repository`, with
      "Private" selected and the template as text, or the provisioned repository
      with its link, time, and template
  - the Unregister section, linking to the confirmation page
  - the unknown-service 404 page

  Verify the scenarios "Shows a service", "Unknown service" (view a service's
  page), "Links to the download", and "Shows a provisioned repository".
- [x] 5.2 Add `RepositoryPages` for `POST /services/{name}/repository`:
  - post, then redirect, carrying flash attributes: the notice, or the error's
    detail and status and the chosen visibility
  - the error notice inside the repository step, with the README token link for
    a 503
  - the pending state's htmx attributes: `hx-post`, `hx-select` and `hx-target`
    on `main`, `hx-disabled-elt`, an `hx-on` handler for `aria-busy`, and the
    "Creating repository…" label that `.htmx-request` shows

  Verify in the shared context the scenarios "Creates a private repository by
  default", "Creates a public repository", "GitHub rejects the token", "Invalid
  visibility", "The service already has a repository", and "Unknown service"
  (explain why a repository wasn't created). Verify "No GitHub token" in a class
  that uses the no-token context.
- [x] 5.3 Run the screenshot check on the service page, at 1280px and 390px:
  - a service with no repository, and one with a repository
  - while a repository is being created: the "Waits for GitHub" scenario, with a
    real token or network throttling
  - after a missing token, and after GitHub rejects the token
  - keyboard focus on "Create repository"

  Fix what the check finds.

## 6. Register a service

- [x] 6.1 Build `GET /register` and `POST /register`:
  - binding to `RegisterServiceRequest`, with `StringTrimmerEditor(true)`
  - the field-message mapping ("Enter …", or the label followed by the message),
    unit tested
  - an error summary with `autofocus` and links to the fields, and
    `aria-invalid` and `aria-describedby` on the fields
  - status 400 for invalid fields, and 409 with a link to the existing service
    for a duplicate name
  - after registering, 303 with the notice "Registered `{name}`."

  Add the 480px form width to docs/ui-guidelines.md. Verify the scenarios
  "Registers a service", "Offers experimental by default", "Reports every invalid
  field", "Rejects an overlong description", "Name already registered", and
  "Rejects a cross-site fetch".
- [x] 6.2 Run the screenshot check on the register form, at 1280px and 390px:
  empty, with several errors with the summary focused, with a duplicate name, and
  the service page's "Registered" notice. Fix what the check finds.

## 7. Unregister a service

- [x] 7.1 Build the `GET /services/{name}/unregister` confirmation page. Build
  `POST /services/{name}/unregister`, which responds 303 to `/services` with the
  notice "Unregistered `{name}`." Verify the scenarios "Asks before
  unregistering", "Unregisters the service", "Unknown service" (unregister),
  "Rejects a form from another site", and "Accepts DevPlane's own forms".
- [x] 7.2 Run the screenshot check at 1280px and 390px on the confirmation page,
  for a service with and without a repository, and on the catalog's
  "Unregistered" notice. Fix what the check finds.

## 8. Integration checks

- [x] 8.1 Add tests for the scenarios "Every action is a link or a form" and
  "Structure of every page". They cover `/services`, `/services/orders-api`,
  `/register`, and `/services/orders-api/unregister`. Verify they pass.
- [x] 8.2 Add a "Using the web UI" part to README.md's "Creating a Service"
  section: open http://localhost:8080 after `./gradlew bootRun`, then register a
  service, download its project, and create its repository. The curl examples
  stay. Then follow it in a browser with a real token:
  - register a service, then download and build its project
  - create its private repository, and check it on GitHub
  - unregister the service, and confirm the repository is kept
  - register and unregister another service with JavaScript turned off
  - delete the test repositories on GitHub
- [x] 8.3 Run the full screenshot check from docs/ui-guidelines.md: every page and
  state from groups 4 to 7 and the error pages, at 1280px and 390px. This covers
  the "Checked in a browser" scenario. Then run `./gradlew build`, confirm CI
  passes, and tick "Web UI for browsing the catalog and creating services" in the
  README roadmap. That completes Phase 1.

## Workflow follow-up

- Run `/opsx:verify add-web-ui`, then `/opsx:archive add-web-ui` to merge this
  change's specs into `openspec/specs/`.
- Next change: the public site on GitHub Pages, from `docs/mockups/landing.html`.
  Redraw its figure with `docs/mockups/figure.py`, once the Interface layer is
  built.
- Future hardening: check the `Host` header against `localhost` and `127.0.0.1`
  to guard against DNS rebinding.
