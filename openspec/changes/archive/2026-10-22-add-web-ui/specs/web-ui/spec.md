# Web UI

## Purpose

The web UI is how a developer uses DevPlane in a browser. It shows the catalog
and each service's path from catalog entry to repository, and it registers
services, creates their repositories, and unregisters them.

## ADDED Requirements

### Requirement: Pages are served at DevPlane's address
The system SHALL serve its web pages from the same address as the Platform API, and SHALL respond to `GET /` with 302 Found and a `Location` of `/services`, the catalog.

#### Scenario: Opens the catalog from the root
- **WHEN** a browser sends `GET /`
- **THEN** the response status is 302 Found and the `Location` header is `/services`

### Requirement: Pages work without JavaScript
The system SHALL make every action on its pages a link, or a button in an HTML form with a `method` and an `action`, so that every action works as a plain link or form submission when JavaScript is turned off.

#### Scenario: Every action is a link or a form
- **GIVEN** `orders-api` is registered without a repository
- **WHEN** a browser requests `/services`, `/services/orders-api`, `/register`, and `/services/orders-api/unregister`
- **THEN** every button on those pages is inside a `form` that has a `method` and an `action`
- **AND** every other action is an `a` element with an `href`

### Requirement: Pages meet the accessibility minimums
The system SHALL give every page a `<title>` that names the page first, a "Skip to content" link as its first focusable element, `header`, `nav`, and `main` landmarks, and exactly one `h1`. Every form control SHALL have a visible label tied to it. The pages SHALL also meet the accessibility minimums in `docs/ui-guidelines.md`, which a browser check confirms.

#### Scenario: Structure of every page
- **GIVEN** `orders-api` is registered
- **WHEN** a browser requests `/services`, `/services/orders-api`, `/register`, and `/services/orders-api/unregister`
- **THEN** each page's first link is "Skip to content", pointing at its `main` element
- **AND** each page has one `h1`, and `header`, `nav`, and `main` elements
- **AND** every `input` has a `label` whose `for` matches its `id`, or sits inside its `label`

#### Scenario: Checked in a browser
- **WHEN** each page is opened at 1280px and at 390px wide
- **THEN** the page meets the guidelines' accessibility minimums, including a visible focus outline on every control, and it doesn't scroll sideways

### Requirement: Browse the catalog
The system SHALL list registered services at `GET /services` as an HTML table sorted by name. Each row SHALL show the service's name linking to `/services/{name}`, its description, its owner, its lifecycle stage, its repository as `owner/name` linking to the repository's URL (or "None"), and when it was last updated.

#### Scenario: Lists services alphabetically
- **GIVEN** services named `payments-api`, `auth-service`, and `orders-api` are registered
- **WHEN** a browser sends `GET /services`
- **THEN** the response status is 200 OK and the page is HTML
- **AND** the table's rows are `auth-service`, `orders-api`, and `payments-api`, in that order, each linking to its service's page

#### Scenario: Shows each service's repository
- **GIVEN** `orders-api` has the repository `https://github.com/octo-dev/orders-api`, and `auth-service` has none
- **WHEN** a browser requests `/services`
- **THEN** the `orders-api` row shows `octo-dev/orders-api`, linking to `https://github.com/octo-dev/orders-api`
- **AND** the `auth-service` row shows "None"

#### Scenario: Shows a missing description
- **GIVEN** `orders-api` is registered without a description
- **WHEN** a browser requests `/services`
- **THEN** the `orders-api` row's description reads "No description"

### Requirement: Page through the catalog
The system SHALL show 20 services per page of `/services`, picking the page with the optional `page` query parameter, numbered from 1. When there is more than one page, it SHALL say which services it shows out of how many, and link to the previous and next pages. A missing or invalid page number SHALL show the first page, and a number past the last page SHALL show the last.

#### Scenario: Shows the second page
- **GIVEN** 25 services are registered
- **WHEN** a browser requests `/services?page=2`
- **THEN** the table lists the 21st through 25th services by name
- **AND** the page reads "21–25 of 25", links "Previous" to `/services?page=1`, and has no "Next" link

#### Scenario: Hides paging when everything fits
- **GIVEN** 3 services are registered
- **WHEN** a browser requests `/services`
- **THEN** the page reads "3 services" and has no "Previous" or "Next" link

#### Scenario: Corrects page numbers that don't exist
- **GIVEN** 25 services are registered
- **WHEN** a browser requests `/services?page=abc`, `/services?page=0`, and `/services?page=9`
- **THEN** each response status is 200 OK
- **AND** the first two show the 1st through 20th services, and the third shows the 21st through 25th

### Requirement: Filter the catalog by owner
The system SHALL list only the services owned by the team in the optional `owner` query parameter of `/services`, show that owner in the page's "Owner" field, and keep the owner in its paging links. Every owner name on the pages SHALL link to that owner's list. A blank `owner` SHALL list every service. With JavaScript on, changing the "Owner" field SHALL update the list without reloading the page.

#### Scenario: Lists one owner's services
- **GIVEN** `orders-api` and `payments-api` are owned by `payments-team`, and `auth-service` is owned by `identity-team`
- **WHEN** a browser requests `/services?owner=payments-team`
- **THEN** the table lists only `orders-api` and `payments-api`
- **AND** the "Owner" field holds `payments-team`

#### Scenario: Owner names link to their list
- **GIVEN** `orders-api` is owned by `payments-team`
- **WHEN** a browser requests `/services`
- **THEN** the `orders-api` row's owner links to `/services?owner=payments-team`

#### Scenario: Owner with no services
- **GIVEN** at least one service is registered, and none is owned by `billing-team`
- **WHEN** a browser requests `/services?owner=billing-team`
- **THEN** the response status is 200 OK and the page reads "`billing-team` owns no services"
- **AND** it links to `/services`

#### Scenario: Blank owner
- **GIVEN** 3 services are registered
- **WHEN** a browser requests `/services?owner=`
- **THEN** the table lists all 3 services

#### Scenario: Filters in place
- **GIVEN** JavaScript is on and services with several owners are registered
- **WHEN** the developer types `payments-team` into the "Owner" field
- **THEN** the table shows only `payments-team`'s services without the page reloading, and the address ends with `?owner=payments-team`

### Requirement: Introduce DevPlane in an empty catalog
The system SHALL show, in place of the table and the owner filter when no services are registered, the heading "No services are registered yet", one sentence on what DevPlane does, and the steps from catalog entry to project to repository. The page SHALL have exactly one "Register service" link, to `/register`, inside the catalog entry step.

#### Scenario: Empty catalog
- **GIVEN** no services are registered
- **WHEN** a browser requests `/services`
- **THEN** the page has no table and no "Owner" field, and its heading reads "No services are registered yet"
- **AND** it lists the steps "Catalog entry", "Project", and "Repository" in that order
- **AND** it has exactly one "Register service" link, pointing to `/register`

### Requirement: View a service's page
The system SHALL show a registered service at `GET /services/{name}`. The page SHALL show the service's name, its description (or "No description"), its owner linking to the owner's list, its lifecycle stage, and its registration and last update times. These SHALL be followed by its steps from catalog entry to project to repository. An unknown name SHALL get 404 Not Found and a page that says so.

#### Scenario: Shows a service
- **GIVEN** `orders-api` is registered with owner `payments-team`, description `Order management REST API`, and lifecycle `experimental`
- **WHEN** a browser sends `GET /services/orders-api`
- **THEN** the response status is 200 OK, and the page's `h1` is `orders-api`
- **AND** the page shows `Order management REST API`, `payments-team` linking to `/services?owner=payments-team`, "Experimental", and the registration and update times
- **AND** it lists the steps "Catalog entry", "Project", and "Repository" in that order

#### Scenario: Unknown service
- **WHEN** a browser requests `/services/payments-api` and no service by that name is registered
- **THEN** the response status is 404 Not Found and the page is HTML
- **AND** the page says that no service named `payments-api` is registered, and links to `/services`

### Requirement: Download a service's project from its page
The system SHALL show, in the project step of a service's page, the template the project is generated from and a "Download project" link to `/api/v1/services/{name}/project`.

#### Scenario: Links to the download
- **GIVEN** `orders-api` is registered
- **WHEN** a browser requests `/services/orders-api`
- **THEN** the project step names the `java-rest-api` template
- **AND** it has a "Download project" link to `/api/v1/services/orders-api/project`

### Requirement: Show a service's repository
The system SHALL show, in the repository step of the page of a service that has a repository, the repository as `owner/name` linking to its URL, when it was created, and the template it was created from. The step SHALL offer no way to create another repository.

#### Scenario: Shows a provisioned repository
- **GIVEN** `orders-api`'s repository `https://github.com/octo-dev/orders-api` was created from the `java-rest-api` template
- **WHEN** a browser requests `/services/orders-api`
- **THEN** the repository step shows `octo-dev/orders-api` linking to `https://github.com/octo-dev/orders-api`, its creation time, and `java-rest-api`
- **AND** the page has no "Create repository" button

### Requirement: Create a service's repository from its page
The system SHALL offer, on the page of a service without a repository, a form that sends `POST /services/{name}/repository` with a `visibility` of `private` (selected by default) or `public`. It creates the repository from the default template. On success, the response SHALL be 303 See Other to `/services/{name}`, which then shows "Created repository `{owner}/{name}`." and the repository.

#### Scenario: Creates a private repository by default
- **GIVEN** `orders-api` is registered without a repository, and the GitHub token belongs to `octo-dev`
- **WHEN** a browser requests `/services/orders-api`
- **THEN** the repository step's `visibility` field has `private` selected
- **WHEN** the browser sends that form as `POST /services/orders-api/repository` with `visibility=private`
- **THEN** the response status is 303 See Other and the `Location` header is `/services/orders-api`
- **AND** the page at that address reads "Created repository `octo-dev/orders-api`." and links to `https://github.com/octo-dev/orders-api`
- **AND** the GitHub repository is private

#### Scenario: Creates a public repository
- **GIVEN** `orders-api` is registered without a repository
- **WHEN** a browser sends `POST /services/orders-api/repository` with `visibility=public`
- **THEN** the response status is 303 See Other
- **AND** the GitHub repository is public

### Requirement: Explain why a repository wasn't created
When creating a repository from a page fails, the system SHALL respond 303 See Other to `/services/{name}` and record nothing. Failures include an invalid visibility, an existing repository, a name already taken on GitHub, a missing GitHub token, and a GitHub error. That page SHALL show "Couldn't create the repository" and the reason inside the repository step. For an unknown service, the system SHALL respond 404 Not Found with the unknown-service page.

#### Scenario: No GitHub token
- **GIVEN** DevPlane runs without a GitHub token, and `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/repository` with `visibility=private`
- **THEN** the response status is 303 See Other, and the `Location` header is `/services/orders-api`
- **AND** the page at that address shows "Couldn't create the repository" in the repository step, with a reason that mentions `DEVPLANE_GITHUB_TOKEN`
- **AND** `orders-api` still has no repository

#### Scenario: GitHub rejects the token
- **GIVEN** `orders-api` is registered, and GitHub rejects the configured token
- **WHEN** a browser sends `POST /services/orders-api/repository` with `visibility=private`
- **THEN** the page it's redirected to says that GitHub rejected the token
- **AND** `orders-api` still has no repository

#### Scenario: Invalid visibility
- **GIVEN** `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/repository` with `visibility=internal`
- **THEN** the page it's redirected to says that the visibility must be private or public
- **AND** nothing is sent to GitHub

#### Scenario: The service already has a repository
- **GIVEN** `orders-api` has a repository
- **WHEN** a browser sends `POST /services/orders-api/repository` with `visibility=private`
- **THEN** the page it's redirected to says that `orders-api` already has a repository
- **AND** nothing is sent to GitHub, and the repository is unchanged

#### Scenario: Unknown service
- **WHEN** a browser sends `POST /services/payments-api/repository` and no service by that name is registered
- **THEN** the response status is 404 Not Found, with the page that says no service named `payments-api` is registered

### Requirement: Show progress while a repository is created
The system SHALL, while a repository is being created with JavaScript on, disable the "Create repository" button and the visibility options. The button SHALL read "Creating repository…", and the step SHALL say that this takes a few seconds. When the request finishes, the page SHALL update in place to show the result.

#### Scenario: Waits for GitHub
- **GIVEN** JavaScript is on, and `orders-api` has no repository
- **WHEN** the developer presses "Create repository"
- **THEN** until DevPlane responds, the button is disabled and reads "Creating repository…", and the visibility options are disabled
- **AND** when DevPlane responds, the repository step shows the repository or the reason it wasn't created, without a full page reload

### Requirement: Register a service from a form
The system SHALL show a form at `GET /register` with the fields `name`, `owner`, an optional `description`, and `lifecycle`. `lifecycle` offers `experimental` (selected by default), `production`, and `deprecated`. Sending the form as `POST /register` SHALL register the service by the Platform API's rules. The response SHALL be 303 See Other to `/services/{name}`, which then shows "Registered `{name}`."

#### Scenario: Registers a service
- **GIVEN** no service named `orders-api` is registered
- **WHEN** a browser sends `POST /register` with `name=orders-api`, `owner=payments-team`, `description=Order management REST API`, and `lifecycle=experimental`
- **THEN** the response status is 303 See Other and the `Location` header is `/services/orders-api`
- **AND** the page at that address reads "Registered `orders-api`."
- **AND** `GET /api/v1/services/orders-api` returns the service with those details

#### Scenario: Offers experimental by default
- **WHEN** a browser requests `/register`
- **THEN** the form's `lifecycle` field has `experimental` selected

### Requirement: Show form errors next to their fields
The system SHALL answer a registration with invalid fields with 400 Bad Request and the form again, keeping the values entered. Each invalid field SHALL show its error under its label, and an error summary at the top of the form SHALL link to each invalid field. An empty name or owner SHALL read "Enter …". Other errors SHALL use the Platform API's validation message after the field's label.

#### Scenario: Reports every invalid field
- **WHEN** a browser sends `POST /register` with `name=Orders API`, an empty `owner`, and `lifecycle=experimental`
- **THEN** the response status is 400 Bad Request and the form is shown again, with `Orders API` still in the name field
- **AND** the name field's error reads "Name must be 1 to 63 lowercase letters, digits, or hyphens, start with a letter, and not end with a hyphen"
- **AND** the owner field's error reads "Enter the team that owns the service"
- **AND** both fields are marked `aria-invalid="true"` and described by their errors, and the error summary links to both
- **AND** no service is registered

#### Scenario: Rejects an overlong description
- **WHEN** a browser sends `POST /register` with a valid name and owner and a 301-character description
- **THEN** the response status is 400 Bad Request
- **AND** the description field's error reads "Description must be at most 300 characters"

### Requirement: Reject a duplicate service name in the form
The system SHALL answer a registration whose name is already registered with 409 Conflict and the form again. The form SHALL show "A service named `{name}` already exists" under the name field, with a link to that service's page. The existing service SHALL be unchanged.

#### Scenario: Name already registered
- **GIVEN** `orders-api` is registered with owner `payments-team`
- **WHEN** a browser sends `POST /register` with `name=orders-api`, `owner=checkout-team`, and `lifecycle=experimental`
- **THEN** the response status is 409 Conflict
- **AND** the name field's error reads "A service named `orders-api` already exists", linking to `/services/orders-api`
- **AND** `orders-api`'s owner is still `payments-team`

### Requirement: Unregister a service after confirming
The system SHALL show at `GET /services/{name}/unregister` a page that asks to confirm, says the service will be removed from the catalog, and says that any GitHub repository stays on GitHub. Its form SHALL send `POST /services/{name}/unregister`, which unregisters the service and responds 303 See Other to `/services`. The catalog then shows "Unregistered `{name}`." Both SHALL respond 404 Not Found with the unknown-service page for an unknown name.

#### Scenario: Asks before unregistering
- **GIVEN** `orders-api` has the repository `https://github.com/octo-dev/orders-api`
- **WHEN** a browser requests `/services/orders-api/unregister`
- **THEN** the page says that `orders-api` will be removed from the catalog, and that `octo-dev/orders-api` stays on GitHub
- **AND** it has an "Unregister `orders-api`" button in a form that sends `POST /services/orders-api/unregister`, and a "Cancel" link to `/services/orders-api`
- **AND** `orders-api` is still registered

#### Scenario: Unregisters the service
- **GIVEN** `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/unregister`
- **THEN** the response status is 303 See Other and the `Location` header is `/services`
- **AND** the page at that address reads "Unregistered `orders-api`.", and `orders-api` is no longer registered

#### Scenario: Unknown service
- **WHEN** a browser requests `/services/payments-api/unregister`, or sends `POST /services/payments-api/unregister`, and no service by that name is registered
- **THEN** each response status is 404 Not Found, with the page that says no service named `payments-api` is registered

### Requirement: Pages show errors as HTML
The system SHALL answer a browser request for a path outside `/api/` that doesn't exist with 404 Not Found and an HTML page linking to `/services`. It SHALL answer an unexpected error while showing a page with 500 Internal Server Error and an HTML page that shows no exception message, class, or stack trace. Paths under `/api/v1` SHALL keep answering with problem details.

#### Scenario: Unknown page
- **WHEN** a browser sends `GET /does-not-exist` with an `Accept` header of `text/html`
- **THEN** the response status is 404 Not Found and the page is HTML, with a link to `/services`

#### Scenario: Unexpected error
- **WHEN** an unexpected error occurs while DevPlane shows a page
- **THEN** the response status is 500 Internal Server Error and the page is HTML
- **AND** the page shows no exception message, exception class, or stack trace

#### Scenario: API paths keep problem details
- **WHEN** a browser sends `GET /api/v1/does-not-exist` with an `Accept` header of `text/html`
- **THEN** the response status is 404 Not Found and the body is a problem details document

### Requirement: Pages refuse form submissions from other sites
The system SHALL reject, with 403 Forbidden and an HTML page, any `POST` to a page that a browser sends from another site, and change nothing. A request comes from another site when its `Sec-Fetch-Site` header is `cross-site` or `same-site`. Without that header, it comes from another site when its `Origin` header isn't DevPlane's own address.

#### Scenario: Rejects a form from another site
- **GIVEN** `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/unregister` with the `Origin` header `https://example.com`
- **THEN** the response status is 403 Forbidden and the page is HTML
- **AND** `orders-api` is still registered

#### Scenario: Rejects a cross-site fetch
- **WHEN** a browser sends `POST /register` with valid fields and the `Sec-Fetch-Site` header `cross-site`
- **THEN** the response status is 403 Forbidden, and no service is registered

#### Scenario: Accepts DevPlane's own forms
- **GIVEN** `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/unregister` with the `Sec-Fetch-Site` header `same-origin`
- **THEN** the response status is 303 See Other, and `orders-api` is no longer registered
