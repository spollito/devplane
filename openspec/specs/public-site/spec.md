# public-site Specification

## Purpose
The public site is how people who won't run DevPlane learn what it does and try it. It's
a static site on GitHub Pages: a splash page, and a demo that runs entirely in the
visitor's browser.

## Requirements

### Requirement: Publish the site from main
The system SHALL publish the site as static files at `https://spollito.github.io/devplane/` on every push to `main`. A pull request SHALL build and check the site without publishing it, and a failed build SHALL leave the published site as it was.

#### Scenario: Publishes from main
- **WHEN** a commit reaches `main`
- **THEN** the site's workflow builds the site and deploys it to GitHub Pages
- **AND** `https://spollito.github.io/devplane/` responds 200 OK with the splash page

#### Scenario: A pull request doesn't publish
- **WHEN** a pull request's workflow runs
- **THEN** it builds the site and runs the demo's tests, and doesn't deploy

#### Scenario: A failed build doesn't publish
- **GIVEN** the demo's tests fail on `main`
- **WHEN** the site's workflow runs
- **THEN** the deploy doesn't run, and the published site is unchanged

### Requirement: The site works without other hosts
The system SHALL serve every file the site needs from the site itself, including its fonts and stylesheets, and all its links between its own pages and files SHALL work under the `/devplane/` path. The site SHALL set no cookies, and SHALL send nothing to any server, other than requesting its own files.

#### Scenario: Requests stay on the site
- **WHEN** a browser opens the splash page and uses the demo
- **THEN** every request goes to `spollito.github.io`, and each one succeeds
- **AND** the site sets no cookies

### Requirement: The splash page explains DevPlane
The system SHALL show a splash page at the site's root that says what DevPlane does today, its vision, how it's built, and how to run it, with a "Try the demo" link and a link to the source. Its drawing of DevPlane's layers SHALL show the Interface and Platform API layers as built, and the Deployment and Operations layers as planned. The page SHALL be readable with JavaScript turned off.

#### Scenario: Shows the page
- **WHEN** a browser opens the site's root
- **THEN** the page's `h1` is "DevPlane", and it has the sections "What it does today", "Where it's going", "How it's built", and "Run it locally"
- **AND** it has a "Try the demo" link to `demo/`, and a link to `https://github.com/spollito/devplane`

#### Scenario: Shows each layer's state
- **WHEN** a browser opens the site's root
- **THEN** the drawing and its text legend show Interface and Platform API as built, and Deployment and Operations as planned

#### Scenario: Works without JavaScript
- **GIVEN** JavaScript is turned off
- **WHEN** a browser opens the site's root
- **THEN** all of the page's text and links work

### Requirement: The roadmap matches the README
The system SHALL list on the splash page every roadmap item from `README.md`, in the same order and wording, marking an item as built exactly when the README ticks it. The build SHALL fail when the two differ.

#### Scenario: In step
- **WHEN** `./gradlew test` runs and the splash page's roadmap matches the README
- **THEN** the check passes

#### Scenario: A ticked item isn't updated on the site
- **GIVEN** a roadmap item is ticked in the README but not marked as built on the splash page
- **WHEN** `./gradlew test` runs
- **THEN** the build fails, and names the item

### Requirement: The demo says it's a demo
The system SHALL show on every demo screen that this is a demo with sample data, that its data stays in the visitor's browser, and that nothing is created on GitHub. The demo SHALL link back to the splash page and to the instructions for running the real app.

#### Scenario: Every screen is labeled
- **WHEN** a visitor opens the catalog, a service, the register form, and the unregister page in the demo
- **THEN** each screen shows the demo notice, with links to the splash page and to running DevPlane locally

#### Scenario: Needs JavaScript
- **GIVEN** JavaScript is turned off
- **WHEN** a browser opens `demo/`
- **THEN** the page says the demo needs JavaScript, and links to the splash page

### Requirement: Browse the sample catalog
The system SHALL show in the demo a catalog of sample services, sorted by name, using the real app's table and stylesheet, and SHALL filter it by owner. Sample repositories SHALL be shown as plain text, with no link, because they don't exist.

#### Scenario: Shows the sample catalog
- **WHEN** a visitor opens the demo
- **THEN** the table lists the sample services sorted by name, each with its description, owner, lifecycle stage, repository or "None", and last update, each name linking to its demo page

#### Scenario: Filters by owner
- **WHEN** a visitor types `payments-team` into the "Owner" field
- **THEN** the table lists only that owner's services

#### Scenario: Owner with no services
- **WHEN** a visitor types `billing-team` into the "Owner" field, and it owns no services
- **THEN** the page reads "`billing-team` owns no services", with a way to show all services

#### Scenario: Sample repositories aren't links
- **GIVEN** a sample service has a sample repository
- **WHEN** a visitor views the catalog or that service
- **THEN** the repository is plain text, not a link

#### Scenario: An empty demo catalog
- **GIVEN** the visitor has unregistered every service
- **WHEN** the demo shows the catalog
- **THEN** it shows the real app's empty-catalog introduction, with a "Register service" action

### Requirement: Register a service in the demo
The system SHALL let a visitor register a service in the demo with the same fields, rules, and error messages as the real app's register form: errors next to their fields, an error summary, and the values kept. A valid registration SHALL add the service and show its page with a "Registered" notice. The build SHALL fail when the demo's rules differ from the real app's.

#### Scenario: Registers a service
- **WHEN** a visitor registers `my-service` with owner `my-team`
- **THEN** the demo shows `my-service`'s page with the notice "Registered `my-service`."
- **AND** the catalog lists it

#### Scenario: Reports every invalid field
- **WHEN** a visitor submits the form with the name `My Service` and an empty owner
- **THEN** the form shows "Name must be 1 to 63 lowercase letters, digits, or hyphens, start with a letter, and not end with a hyphen" and "Enter the team that owns the service" next to their fields, with `Name` still holding `My Service`
- **AND** nothing is added to the catalog

#### Scenario: Rejects a duplicate name
- **GIVEN** `orders-api` is in the demo's catalog
- **WHEN** a visitor registers `orders-api`
- **THEN** the form shows "A service named `orders-api` already exists", linking to that service
- **AND** the existing service is unchanged

#### Scenario: Rules drift from the real app
- **GIVEN** the demo's identifier pattern, description limit, lifecycle stages, or messages differ from the real app's
- **WHEN** `./gradlew test` runs
- **THEN** the build fails

### Requirement: View a service in the demo
The system SHALL show each service's page in the demo with its details and its steps from catalog entry to project to repository, as the real app does. A name that isn't in the demo's catalog SHALL show a page that says so, with a link back to the catalog.

#### Scenario: Shows a service
- **GIVEN** `orders-api` is in the demo's catalog
- **WHEN** a visitor opens its page
- **THEN** the page shows its name, description, owner, lifecycle stage, and times, and the steps "Catalog entry", "Project", and "Repository" in that order

#### Scenario: Unknown service
- **WHEN** a visitor opens the demo's address for a service that isn't in its catalog
- **THEN** the page says that no service by that name is in the demo, and links to the catalog

### Requirement: Download the real example project
The system SHALL offer "Download project" on every demo service's page, linking to `downloads/orders-api.zip`, a project generated by DevPlane's own code when the site is built. The page SHALL say that the demo always downloads the `orders-api` example, and that the real app generates a project for each service.

#### Scenario: Downloads the example
- **WHEN** a visitor presses "Download project" on any demo service's page
- **THEN** the browser downloads `orders-api.zip`, and the page says it's the `orders-api` example

#### Scenario: The download is a real project
- **WHEN** `./gradlew siteBuild` runs
- **THEN** `build/site/downloads/orders-api.zip` holds an `orders-api/` folder with the files DevPlane generates from the `java-rest-api` template, and `gradlew` is marked executable

#### Scenario: A broken template fails the build
- **GIVEN** the template can't be generated
- **WHEN** `./gradlew siteBuild` runs
- **THEN** the task fails, and no site is published

### Requirement: Simulate creating a repository
The system SHALL let a visitor press "Create repository" in the demo, choosing private or public. It SHALL show the real app's progress state, then a result that says it was simulated and that nothing was created on GitHub, and SHALL say what the real app does instead. The demo SHALL send no request to GitHub, and SHALL show no link to a repository.

#### Scenario: Simulates the result
- **GIVEN** `my-service` has no repository in the demo
- **WHEN** a visitor presses "Create repository"
- **THEN** the button reads "Creating repository…" and is disabled for about a second
- **AND** the repository step then says that this was simulated and nothing was created on GitHub, with no link to a repository

#### Scenario: Sends nothing to GitHub
- **WHEN** a visitor presses "Create repository"
- **THEN** the browser sends no request to any other host

### Requirement: Unregister and reset in the demo
The system SHALL let a visitor unregister a demo service after a confirmation page, as the real app does, and SHALL offer a way to reset the demo to its sample data. Changes SHALL survive a reload when the browser allows local storage, and the demo SHALL keep working for the current visit when it doesn't.

#### Scenario: Unregisters after confirming
- **GIVEN** `orders-api` is in the demo's catalog
- **WHEN** a visitor opens its unregister page and confirms
- **THEN** the catalog shows "Unregistered `orders-api`.", and no longer lists it

#### Scenario: Resets the demo
- **GIVEN** the visitor has registered and unregistered services
- **WHEN** the visitor presses "Reset demo"
- **THEN** the catalog shows the sample services again

#### Scenario: Survives a reload
- **GIVEN** the visitor registered `my-service`
- **WHEN** the visitor reloads the page
- **THEN** `my-service` is still listed

#### Scenario: Storage is blocked
- **GIVEN** the browser refuses local storage
- **WHEN** a visitor uses the demo
- **THEN** every action works until the page is reloaded, and no error is shown

### Requirement: The site meets the UI guidelines
The system SHALL make the splash page and the demo follow `docs/ui-guidelines.md`: its design values, accessibility minimums, and Never list. Neither SHALL scroll sideways at any width from 320px up, including with the longest valid names in the demo. Text a visitor enters SHALL appear only as text, never as markup.

#### Scenario: Checked in a browser
- **WHEN** the splash page and each demo screen are opened at 1280px, 768px, and 390px wide, including with a 63-character name and owner in the demo
- **THEN** each passes the guidelines' screenshot check, with no sideways scrolling and a visible focus outline on every control

#### Scenario: Entered text stays text
- **WHEN** a visitor enters `<img src=x onerror=alert(1)>` in the demo's description field and registers the service
- **THEN** the page shows those characters as text, and no script runs
