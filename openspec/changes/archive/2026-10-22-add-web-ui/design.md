# Design: Add the web UI

## Context

See proposal.md for why, and the specs for the behavior. What exists today:

- DevPlane is one Spring Boot web MVC application with feature packages:
  - `catalog`
  - `templates`, which uses `catalog`
  - `provisioning`, which uses both
  - `api`, the problem details handler
- In `catalog`, only `ServiceCatalog.lookUp` and `recordRepository` are public.
  `register`, `list`, `update`, and `unregister` are package-private and return
  the package-private `ServiceEntity`. The request records and the catalog's
  exceptions are package-private too.
- In `provisioning`, `RepositoryProvisioner` and `ProvisionRepositoryRequest` are
  package-private. Every provisioning failure is an `ErrorResponseException` whose
  `detail` DevPlane writes itself.
- `ApiExceptionHandler` is a global `@RestControllerAdvice`. Today it answers every
  path, including paths outside `/api/`, with problem details.
- `src/main/resources/templates/` and `static/` are empty.
- Integration tests share one context through `@IntegrationTest` (MockMvc,
  PostgreSQL in a container, `MutableClock` in UTC, and `FakeGitHub`).
  `MissingGitHubTokenTests` has its own context without a token. Handler tests use
  `@WebMvcTest` slices with a test-only controller.
- `docs/ui-guidelines.md` is binding, and `docs/mockups/` holds the approved
  mockups. `docs/mockups/style.css` is the starting point for the real
  stylesheet.

## Goals / Non-Goals

**Goals:**

- Package dependencies keep pointing one way. Nothing depends on the new `web`
  package.
- Each validation rule is written once. The register form and the API use the
  same constraints and messages.
- Every page and state renders in automated tests, so mistakes in a template's
  expressions fail the build rather than a browser.
- `./gradlew build` and CI need no Node, npm, or browser.

**Non-Goals:**

- A component framework or a design-system package. Thymeleaf fragments and one
  stylesheet are enough for four pages.
- Server code that knows about htmx. Every endpoint returns a full page, and htmx
  picks out the part it needs.

## Decisions

### 1. Server-rendered pages with Thymeleaf, and htmx in two places

Pages are Thymeleaf templates rendered by Spring MVC controllers, and forms are
plain HTML forms. htmx enhances two interactions, using attributes only:

| Interaction | htmx attributes | Without JavaScript |
| --- | --- | --- |
| Owner filter | `hx-get="/services"`, triggered on input after 300ms or on submit, with `hx-select` and `hx-target` set to the results region and `hx-push-url="true"` | The form submits `GET /services?owner=…` |
| Create repository | `hx-post` with `hx-select="main"`, `hx-target="main"`, and `hx-swap="outerHTML"`. `hx-disabled-elt="find button, find fieldset"`, and an `hx-on` handler that sets `aria-busy` on the step. | The form posts, and the browser waits for the redirect |

- The browser's XHR follows the 303 redirect after the `POST`. htmx then swaps
  `main` from the redirected page, so the notice and the new repository step
  appear together.
- The "Creating repository…" label is in the button from the start but hidden.
  CSS shows it while the form has htmx's `htmx-request` class.

Alternatives:

- **A React and Vite single-page app** was compared during explore and rejected.
  It needs a second toolchain and test stack, Node in CI, and an SPA fallback
  route, and it would be about twice the work for four pages.
- **Plain JavaScript calling the JSON API** was rejected. With no build step it
  gets messy past a few screens, and it can only be tested in a real browser.
- **No JavaScript at all** was rejected. Creating a repository takes a few seconds,
  and a page that looks frozen for that long reads as broken.

### 2. A `web` package for the page controllers

A service page shows catalog data, the default template, and the repository form,
so it needs `catalog`, `templates`, and `provisioning` at once. `catalog` can't
depend on `templates`, because `templates` already depends on `catalog`. So the
page controllers live in a new `web` package that depends on all three:

```
web          --> catalog, templates, provisioning
provisioning --> catalog, templates
templates    --> catalog
catalog      --> (nothing)
```

`web` holds:

- `CatalogPages`: `GET /`, and `GET /services`
- `ServicePages`: the service page, register, and unregister
- `RepositoryPages`: `POST /services/{name}/repository`
- `PageExceptionHandler`
- `PageTimes`: date formatting
- the view records the templates read

This widens what the features make public:

- `ServiceCatalog.register`, `list`, and `unregister` become public, and so do
  `RegisterServiceRequest`, `ServiceNotFoundException`, and
  `DuplicateServiceNameException`.
- `ServiceCatalog`'s methods return `RegisteredService`, which gains
  `lifecycle`, `createdAt`, and `updatedAt`. `ServiceResponse` is built from it.
  `ServiceEntity` stays private to the catalog and never leaves `ServiceCatalog`.
  `update` keeps package-private visibility, because the UI doesn't edit services.
- `RepositoryProvisioner.provision` and `ProvisionRepositoryRequest` become
  public.

Only the catalog still changes services. The rule in docs/architecture.md becomes
"Other features change services only through `ServiceCatalog`".

Alternatives:

- **Page controllers in each feature package**, next to the REST controllers, was
  the plan in explore. It was rejected once the service page was laid out:
  - The catalog's page would need the default template's name and the
    repository form, which belong to packages the catalog can't depend on.
  - Workarounds, such as model attributes added to the catalog's pages by an
    advice in `templates`, hide the dependency instead of removing it.
- **Keeping the entity-returning methods and adding public copies** was rejected,
  because it means two versions of every catalog operation.

### 3. Routes

| Method and path | Controller | Responds with |
| --- | --- | --- |
| `GET /` | `CatalogPages` | 302 Found to `/services` |
| `GET /services?owner=&page=` | `CatalogPages` | the catalog page |
| `GET /services/{name}` | `ServicePages` | the service page, or 404 |
| `GET /register` | `ServicePages` | the register form |
| `POST /register` | `ServicePages` | 303 to the service page, or the form with 400 or 409 |
| `GET /services/{name}/unregister` | `ServicePages` | the confirmation page, or 404 |
| `POST /services/{name}/unregister` | `ServicePages` | 303 to `/services`, or 404 |
| `POST /services/{name}/repository` | `RepositoryPages` | 303 to the service page, or 404 |

- **The register form is at `/register`.** `new` and `register` are both valid
  service names, so `/services/new` would hide the page of a service named `new`.
- **Paths under `/services/{name}/` are actions on a service.** No service name
  is shadowed, because every action path has a second segment.
- **Page size and numbering:** the catalog shows 20 services per page, the API's
  default.
  - The `page` parameter is read as a string and counted from 1. Anything that
    isn't a positive whole number becomes 1.
  - A number past the last page is clamped. `CatalogPages` asks the catalog again
    for the last page, which costs one extra query on a request that's already
    wrong.
- **Redirects:**
  - After a `POST`, the response is 303 See Other, from a small helper that builds
    a `RedirectView` with that status. Spring's `redirect:` prefix sends 302,
    and 303 is what tells a browser to follow with `GET`.
  - `GET /` uses a plain `redirect:`, which sends 302.

### 4. Forms reuse the API's request records and messages

- **Binding:**
  - `POST /register` binds to `RegisterServiceRequest` with `@Valid` and a
    `BindingResult`.
  - `POST /services/{name}/repository` binds to `ProvisionRepositoryRequest`.
  - Spring binds form fields to a record's constructor, so the same constraints
    and messages apply as for JSON.
- **Empty fields:** a `StringTrimmerEditor(true)` in the page controllers'
  `@InitBinder` turns empty strings into `null`. An empty name then fails
  `@NotNull` rather than `@Pattern`, and an empty description is stored as no
  description, the same as when the API omits it.
- **Messages:** a field error is shown as follows.
  - A `NotNull` error has its own sentence: "Enter a name for the service" or
    "Enter the team that owns the service".
  - Any other error is the field's label followed by the constraint's message, for
    example "Name must be 1 to 63 lowercase letters…".
  - The mapping is a small method in `web`, unit tested.
- **Description length:** `@Size` on `description` gets the message "must be at
  most 300 characters" (a constant in `CatalogConstraints`). Bean Validation's
  default reads "size must be between 0 and 300".
- **Duplicate names:** a `DuplicateServiceNameException` from `register` becomes
  a `rejectValue("name", "duplicate")`. The form is shown again with 409, and the
  template renders this error code's link to the existing service.
- **Status codes:** the form comes back with 400 or 409 through
  `ModelAndView.setStatus`. Typed values stay, because the form is rendered from
  the bound record.
- **Error summary:** when there are errors, the summary at the top is
  `tabindex="-1"` with `autofocus`, so it takes focus without script. Each entry
  links to `#<field>`.

Alternatives:

- **Separate form classes for the pages** were rejected. Their constraints would
  repeat the API's and drift from them.
- **Showing raw constraint messages** was rejected. "must not be null" doesn't
  tell anyone what to do.

### 5. Creating a repository: post, then redirect, including on failure

`RepositoryPages` handles `POST /services/{name}/repository`:

1. `ProvisionRepositoryRequest` fails validation: it redirects with the error,
   such as "Visibility must be private or public".
2. Otherwise it calls `RepositoryProvisioner.provision(name, template, isPrivate)`.
3. Success: 303 to `/services/{name}`, with the flash attribute
   `notice = "Created repository octo-dev/orders-api."`.
4. `ServiceNotFoundException`: it's rethrown and becomes the 404 page.
5. Any other `ErrorResponseException` (409, 502, or 503): 303 to
   `/services/{name}`, with flash attributes holding the exception's `detail` and
   the visibility that was chosen.

`ServicePages` reads those flash attributes, then shows the error inside the
repository step and keeps the chosen visibility selected.

- **One rendering path:** only `ServicePages` renders a service page, so the
  provisioning code never builds catalog views.
- **No accidental retries:** reloading the page after a failure doesn't send the
  `POST` again, which would retry creating a repository on GitHub.
- **The cost:** the page shown after a failure has status 200. The failure's own
  status is visible only on the 303, and the specs place the error on the
  redirected page.
- **The 503 link:** a `detail` that comes from a 503 also shows the README link
  "How to create the token". The flash attributes carry the status for this.
- **Sessions:** flash attributes live in the HTTP session. A session cookie is
  created only when a flash attribute is set. Sessions are tracked with the
  cookie only (`server.servlet.session.tracking-modes=cookie`). Otherwise Tomcat
  puts `;jsessionid=…` in the redirect URL of a browser that has no cookie yet.

Alternative: showing the service page again from `RepositoryPages` with the
failure's status was rejected. It would need a second copy of the service page's
model, and reloading the page would send the `POST` again.

### 6. Errors: pages render HTML, and `/api/` keeps problem details

Errors take three paths:

| Where the error happens | Who handles it | Result |
| --- | --- | --- |
| In a page controller, or the request check before it | `web.PageExceptionHandler`: a `@ControllerAdvice(basePackageClasses = …)` for the `web` package, at highest precedence | `ServiceNotFoundException` renders `error/4xx` with "No service named `x` is registered." and 404. Another `ErrorResponse` renders `error/4xx` with its status and detail. Anything else is logged and renders `error/5xx` with 500 and no internals. |
| Outside any controller and outside `/api/`, such as an unknown path or a wrong method | `ApiExceptionHandler`, which now calls `response.sendError(status)` and returns no body for paths that don't start with `/api/` | Spring Boot's error controller renders `templates/error/4xx.html` or `5xx.html` for browsers |
| Under `/api/` | `ApiExceptionHandler`, unchanged | Problem details, as today |

- Controller advice is checked in order, and the first one that applies to the
  handler wins. So page controllers never reach `ApiExceptionHandler`, and REST
  controllers never reach `PageExceptionHandler`.
- The `sendError` branch is a few lines in `handleExceptionInternal` and
  `handleUnexpected`. It's the documented way to hand an error to the servlet
  container's error page.
- **Testing:**
  - MockMvc doesn't follow error dispatches. So the unknown-path scenario is
    tested in two steps: `GET /does-not-exist` returns 404 without a problem
    details body, and `GET /error` with the servlet's error attributes renders the
    HTML page.
  - The 500 page is tested in a `@WebMvcTest` slice, with a test-only page
    controller in the `web` package that throws.

Alternatives:

- **Limiting `ApiExceptionHandler` with `@RestControllerAdvice(annotations = …)`**
  was rejected. Unknown paths have no controller, so `/api/v1/does-not-exist`
  would lose its problem details. That breaks the platform-api spec.
- **Turning on `server.error.include-message`** to pass details to Spring Boot's
  page was rejected. It shows exception messages on every error page, including
  unexpected ones.

### 7. Refusing requests from other sites

`api.CrossSiteRequestInterceptor` is a `HandlerInterceptor` registered for every
path by a `WebMvcConfigurer` in `api`. For `POST`, `PUT`, `PATCH`, and `DELETE`
requests:

- `Sec-Fetch-Site: same-origin` or `none` passes, and `cross-site` or `same-site`
  is refused. Every current browser sends this header.
- Without that header, `WebUtils.isSameOrigin(request)` from `spring-web` compares
  `Origin` with the request's own scheme, host, and port. It passes requests with
  no `Origin`, such as curl, scripts, and tests.
- A refusal throws `CrossSiteRequestException`, an `ErrorResponseException` with
  status 403 and the detail "DevPlane accepts changes only from its own pages
  and from clients that aren't web browsers."
  - `ApiExceptionHandler` turns it into problem details under `/api/`.
  - `PageExceptionHandler` turns it into the HTML 403 page for pages.
  - Interceptors run before the controller, so nothing changes.
- `same-site` is refused because another app on `localhost`, on a different port,
  counts as the same site.

Alternatives:

- **Spring Security's CSRF tokens** were rejected. That's a framework, sessions,
  and a token in every form. The API would also need an exemption, which keeps
  its hole open. Fetch Metadata and `Origin` checks need none of that.
- **`SameSite` cookies** don't help. The attack carries no cookies, because
  DevPlane has no login.
- **CORS settings** don't stop form posts or `no-cors` fetches, which skip the
  preflight.

### 8. Templates, styles, and assets

- **Templates** in `src/main/resources/templates/`:
  - `layout.html` holds fragments: `head(title)`, the top bar, the skip link, and
    the notice area.
  - Shared fragments: `fragments/icons.html` (Lucide paths, with the ISC
    attribution in a comment), `fragments/lifecycle.html`, and
    `fragments/path.html`. The path diagram's steps are a list, so a later phase
    can add one.
  - Pages: `catalog.html`, `service.html`, `register.html`, `unregister.html`,
    `error/4xx.html`, and `error/5xx.html`.
  - Fragments are included with `th:replace`, which needs no layout-dialect
    dependency. `th:text` escapes everything, so service names and descriptions
    can't inject markup.
- **Styles:** one stylesheet, `static/css/devplane.css`, starts from
  `docs/mockups/style.css` without the mockup-only state rules. Plain CSS with
  custom properties needs no build step.
- **Fonts:** `static/fonts/` holds the Atkinson Hyperlegible Next 400 and 600 and
  Mono 400 latin `woff2` files from Fontsource (about 35KB), with `OFL.txt`. They
  load with `font-display: swap`. The page works offline, and no font CDN sees
  who uses DevPlane.
- **htmx:** the `org.webjars.npm:htmx.org:2.0.11` WebJar, with
  `org.webjars:webjars-locator-lite`, which Spring Boot manages. Spring Boot then
  serves it at the version-free path `/webjars/htmx.org/dist/htmx.min.js`, so a
  version bump touches only `build.gradle.kts`.
  - htmx 2.0.11 is the last 2.x release. 4.0.0 came out in September 2026 with
    new defaults. Our use is a handful of attributes, so upgrading can wait for
    its own change.
  - Vendoring the file was the alternative. It works, but nothing would track its
    version or license.
- **Times:** `PageTimes` formats every time.
  - **Exact:** "Oct 21, 2026, 16:40", in the JVM's default time zone. On the
    developer's laptop, that's their own zone.
  - **Relative:** "5 hours ago", "Yesterday", or "2 weeks ago", from the
    application's `Clock`, so tests control it.
  - The Gradle test tasks set `user.timezone=UTC`, so exact times are the same on
    every machine.
- **View records:** controllers map `RegisteredService` to small view records,
  such as `ServiceRow`, with display-ready strings. Templates only print them.

### 9. Testing

- **jsoup:** page tests use `MockMvcTester` in the shared `@IntegrationTest`
  context, and parse the HTML with jsoup to assert on elements and attributes:
  rows, links, `for`/`id` pairs, `aria-invalid`, and `aria-describedby`.
  - A test helper turns an `MvcTestResult` into a jsoup `Document`.
  - Each page has a test class, named for the scenarios it covers, as in the
    existing tests.
- **The no-token scenario** runs in a class that uses `MissingGitHubTokenTests`'
  configuration, so it shares that context.
- **Browser-only behavior** is checked by hand in the screenshot check task: the
  in-place filter, the repository progress, focus outlines, and layout at 390px.
- **New test dependencies:**
  - `org.jsoup:jsoup:1.23.2`
  - `spring-boot-starter-thymeleaf-test`, which puts Thymeleaf in `@WebMvcTest`
    slices. This follows the project's pattern of one test starter per starter.

Alternatives:

- **Comparing strings in the response body** was rejected. It's brittle, and it
  can't check attributes.
- **HtmlUnit through `MockMvcWebClientBuilder`** was rejected. It's heavier, and
  its JavaScript engine can't be relied on with htmx.
- **Playwright for Java** was rejected for this change. It would download about
  200MB of browsers in CI. It's worth reconsidering if the UI grows.

### Additions to the UI guidelines

The implementation adds these to docs/ui-guidelines.md:

- **Forms** are at most 480px wide.
- **Pages without a mockup** follow the guidelines' rules: the register form, the
  confirmation page, and the error pages.

Anything else that turns out to be needed is added to the guidelines in the same
task that uses it.

### Later roadmap items this must not block

- **The public site on GitHub Pages:** a static page outside the app. It may copy
  `devplane.css`, but it doesn't depend on the app's templates.
- **Editing a service:** the register form's fields become a fragment the edit
  form reuses. `ServiceCatalog.update` then becomes public like the other
  methods.
- **Deployments (Phase 2):** the path diagram gains a "Deployment" step, and the
  catalog may gain a column.
- **Health in the catalog (Phase 3):** a column in the catalog table.
- **The MCP server (Phase 4):** it uses the API. The cross-site check lets clients
  without browser headers through.

## Risks / Trade-offs

- [Making the catalog's operations public lets any package register or unregister
  services] → They still run the catalog's rules, and the entity stays private.
  docs/architecture.md names `web` and the REST controllers as the callers.
- [Thymeleaf expressions fail only when a page renders] → Every page and state is
  rendered by a test.
- [Behavior that needs a browser isn't automated] → The screenshot check covers it,
  plus a manual pass with JavaScript off.
- [A proxy that rewrites `Host` would make the `Origin` check refuse DevPlane's
  own forms] → DevPlane runs on `127.0.0.1` without a proxy. Running behind one
  would need Spring's forwarded-header support.
- [DNS rebinding: a site whose name resolves to `127.0.0.1` looks same-origin] →
  Out of scope, and recorded as future work: check the `Host` header against
  `localhost` and `127.0.0.1`.
- [htmx 2 while 4.0 is current] → 2.x is maintained, the use is small, and
  upgrading is a separate change.
- [Flash attributes create an HTTP session] → It's local and single-user, and
  nothing relies on the session for security.

## Migration Plan

There's no database change. API clients see two differences:

- Requests from browsers on other sites that change data get 403.
- The description length message changes wording.

To roll back, revert the change.
