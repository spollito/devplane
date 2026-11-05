# Tasks

## 1. Framing

- [x] 1.1 Add `api.FrameDenyFilter`, a `OncePerRequestFilter` bean that sets
  `X-Frame-Options: DENY` and `Content-Security-Policy: frame-ancestors 'none'`.
  Verify the "Every page sends the headers" scenario with an integration test. Describe
  the filter in docs/architecture.md, next to the cross-site check.
- [x] 1.2 With `./gradlew bootRun`, check with curl that the 404 page at
  `/does-not-exist` has both headers after its error dispatch. Then repeat the review's
  reproduction for "A browser refuses to frame a page": a page on another origin frames
  `/services/{name}/unregister`. Confirm the frame is refused, no `POST` can be sent, and
  the service is still registered.

## 2. Huge page numbers

- [x] 2.1 In `ServiceCatalog.list`, return an empty `PageImpl` with the correct total
  when the offset is above `Integer.MAX_VALUE`, and add `ServiceRepository.countByOwner`.
  Verify the service-catalog scenarios "Page past the last" and "Page number too large to
  reach", with and without an owner.
- [x] 2.2 Make `CatalogPages.pageNumber` read any run of digits, and treat one too long
  for an `int` as `Integer.MAX_VALUE`. Verify "Shows the last page for huge page numbers",
  and that "Corrects page numbers that don't exist" still passes.

## 3. Long names and tablet widths

- [x] 3.1 Change `.id` and the supporting rules from the design's decision 2: link
  underlines, `.button`, `.summary`, `.facts`, `.breadcrumbs li`, and `.repo-link`. Update
  the Type and Buttons rules in docs/ui-guidelines.md. Verify in a browser that
  `java-rest-api` stays on one line in sentences ("Short identifiers stay whole"), and
  that ordinary buttons are still 32px tall.
- [x] 3.2 Give `.table` `table-layout: fixed` with the widths from decision 3, and move
  the app's breakpoint from 640px to 960px, with `.path`'s columns as `minmax(0, …fr)`.
  Update the Layout and Tables rules and the path diagram's stacking rule in
  docs/ui-guidelines.md, leaving the Public site section's sizes alone. Verify in a
  browser "Ordinary rows stay one line", and "Tablet widths with ordinary names" at 768px.
- [x] 3.3 Add the "Pages render the longest names" test, and change the screenshot check
  in docs/ui-guidelines.md: its data includes 63-character names and owners, its widths
  are 1280px, 768px, and 390px, and sideways scrolling is measured by comparing
  `documentElement.scrollWidth` with `clientWidth`.

## 4. Focus and icon

- [x] 4.1 Give the notices in `layout.html` and `fragments/path.html` `tabindex="-1"` and
  `autofocus`, and add the rule to docs/ui-guidelines.md. Verify the scenarios "A success
  notice takes focus", "A failure notice takes focus" (in the no-token context), and "No
  notice, no focus change". In a browser with JavaScript on, verify "Focus follows the
  update in place".
- [x] 4.2 Add `static/favicon.svg`, and link it from `layout.html`'s head. Verify "Serves
  and links the icon". In a browser with an empty cache, verify "No failed icon request".

## 5. Docs and integration checks

- [x] 5.1 Correct the README: the Status section (Phase 1 is complete), and "Architecture
  documentation will live in". Add a known-gaps note to docs/architecture.md saying that
  DevPlane doesn't check the `Host` header, so DNS rebinding isn't guarded against yet.
  Verify that the links in the changed sections resolve.
- [x] 5.2 Run the updated screenshot check on every page and state at 1280px, 768px, and
  390px, using the longest valid data, including "The longest valid names". Check keyboard
  focus on a control and on each notice. Measure `scrollWidth` on each page. Fix what the
  check finds.
- [x] 5.3 Run `./gradlew build` and `openspec validate --all --strict`, and confirm CI
  passes on the pull request. This change completes no roadmap item, so no box is ticked.

## Workflow follow-up

- Run `/opsx:verify harden-web-ui`, then `/opsx:archive harden-web-ui` to merge this
  change's specs into `openspec/specs/`.
