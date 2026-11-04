# Web UI

## ADDED Requirements

### Requirement: Pages can't be framed by other sites
The system SHALL send `X-Frame-Options: DENY` and `Content-Security-Policy: frame-ancestors 'none'` with every response, including error pages and the API documentation page, so that another site can't embed a page and trick a click on its actions.

#### Scenario: Every page sends the headers
- **GIVEN** `orders-api` is registered
- **WHEN** a browser requests `/services`, `/services/orders-api`, `/services/orders-api/unregister`, `/register`, `/does-not-exist`, and `/swagger-ui/index.html`
- **THEN** each response has the header `X-Frame-Options: DENY` and the header `Content-Security-Policy: frame-ancestors 'none'`

#### Scenario: A browser refuses to frame a page
- **GIVEN** `orders-api` is registered
- **WHEN** a page on another site puts `/services/orders-api/unregister` in an `iframe`
- **THEN** the browser doesn't show DevPlane's page in the frame, so there is nothing to click
- **AND** `orders-api` is still registered

### Requirement: Pages fit the longest valid names
The system SHALL lay out every page so that it doesn't scroll sideways and nothing overlaps, at any width from 320px up, with the longest valid service names, owners, and repositories. An identifier SHALL stay on one line when it fits its container, and SHALL break only when it doesn't. Pages SHALL show identifiers in full, never cut off.

#### Scenario: The longest valid names
- **GIVEN** a service with a 63-character name and a 63-character owner has a repository, and another has a 63-character name and none
- **WHEN** the catalog, both service pages, and both unregister pages are opened at 1280px, 768px, and 390px wide
- **THEN** no page scrolls sideways, nothing overlaps, and each identifier is shown in full
- **AND** the same holds for the register form with a duplicate 63-character name, and for the notices after registering and unregistering such a service

#### Scenario: Short identifiers stay whole
- **GIVEN** `orders-api` is registered
- **WHEN** its page is opened at 1280px and 390px wide
- **THEN** `orders-api` and `java-rest-api` each sit on one line, including inside sentences

#### Scenario: Ordinary rows stay one line
- **GIVEN** `orders-api` is registered with owner `payments-team` and a short description
- **WHEN** the catalog is opened at 1280px wide
- **THEN** its row is one line tall, and its description is readable

#### Scenario: Tablet widths with ordinary names
- **GIVEN** `orders-api` is registered
- **WHEN** the catalog and its service page are opened at 768px wide
- **THEN** neither page scrolls sideways

#### Scenario: Pages render the longest names
- **GIVEN** a service with a 63-character name and a 63-character owner has a repository
- **WHEN** a browser requests `/services`, its service page, and its unregister page
- **THEN** each response status is 200 OK, and each page contains the whole name

### Requirement: Focus moves to an action's result
The system SHALL move keyboard focus to the notice that reports an action's result, with or without JavaScript, so that the next Tab continues from the result. A page that shows no notice SHALL not move focus.

#### Scenario: A success notice takes focus
- **GIVEN** no service named `orders-api` is registered
- **WHEN** a browser sends `POST /register` with valid fields for `orders-api`, and follows the redirect
- **THEN** the page's "Registered `orders-api`." notice has `tabindex="-1"` and `autofocus`

#### Scenario: A failure notice takes focus
- **GIVEN** DevPlane runs without a GitHub token, and `orders-api` is registered
- **WHEN** a browser sends `POST /services/orders-api/repository`, and follows the redirect
- **THEN** the "Couldn't create the repository" notice has `tabindex="-1"` and `autofocus`

#### Scenario: No notice, no focus change
- **GIVEN** `orders-api` is registered
- **WHEN** a browser requests `/services/orders-api` directly
- **THEN** no element on the page has `autofocus`

#### Scenario: Focus follows the update in place
- **GIVEN** JavaScript is on, and `orders-api` has no repository
- **WHEN** the developer presses "Create repository", and the page updates in place
- **THEN** keyboard focus is on the notice that reports the result, not at the top of the page

### Requirement: Pages have an icon
The system SHALL serve an icon at `GET /favicon.svg` with the content type `image/svg+xml`, and every page SHALL link to it, so that browsers don't request a missing `/favicon.ico`.

#### Scenario: Serves and links the icon
- **GIVEN** `orders-api` is registered
- **WHEN** a browser requests `/favicon.svg`
- **THEN** the response status is 200 OK and the content type is `image/svg+xml`
- **AND** `/services`, `/services/orders-api`, `/register`, and `/services/orders-api/unregister` each have a `link` with `rel="icon"` pointing to it

#### Scenario: No failed icon request
- **WHEN** Chrome opens `/services` with an empty cache
- **THEN** its console shows no failed request for an icon

## MODIFIED Requirements

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

#### Scenario: Shows the last page for huge page numbers
- **GIVEN** 25 services are registered
- **WHEN** a browser requests `/services?page=107374184`, `/services?page=2147483647`, and `/services?page=99999999999`
- **THEN** each response status is 200 OK, and each shows the 21st through 25th services
