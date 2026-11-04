# Proposal: Harden the web UI

## Why

A review of the merged web UI found problems that the tests and screenshot checks
missed, because they used short names and only two screen widths:

- A page framed by another site can be clicked, and its `POST` passes the
  cross-site check, because the browser labels it `same-origin`. In a reproduction,
  one click inside a framed page unregistered a service.
- Valid long names make pages scroll sideways and overlap. So does the service page
  at tablet widths (641px to 830px measured), even with short names.
- A huge page number returns a 500 on both the catalog page and the API.

Fixing these now means the public site and Phase 2 build on a UI that holds up.

Roadmap: adds no roadmap item. This is follow-up work on the completed Phase 1 item
"Web UI for browsing the catalog and creating services".

## What Changes

- **Framing:** every response sends `X-Frame-Options: DENY` and
  `Content-Security-Policy: frame-ancestors 'none'`, so another site can't embed a
  DevPlane page and trick a click on its actions.
- **Long names and tablet widths:**
  - An identifier stays on one line when it fits, and breaks only when it doesn't.
  - No page scrolls sideways or overlaps at any width from 320px up, with the longest
    valid names, owners, and repositories.
  - The catalog table uses fixed column widths.
  - The UI's one breakpoint moves from 640px to 960px, so tablets get the stacked
    layout.
- **Huge page numbers:**
  - The catalog page shows the last page, however large the number is.
  - The API returns an empty page with the correct totals, like any other page past
    the end.
- **Keyboard focus:** a notice that reports an action's result takes focus, so the
  next Tab continues from the result instead of the top of the page. Today focus
  falls to the top after "Create repository".
- **Icon:** every page links an icon, so browsers stop requesting a missing
  `/favicon.ico`.
- **Docs:**
  - The README's stale Status and Architecture text is corrected.
  - `docs/architecture.md` records the DNS-rebinding gap as known future work.
  - `docs/ui-guidelines.md` changes its identifier and breakpoint rules, and the
    screenshot check now requires maximum-length names and a 768px width.

**Out of scope**

- The DNS-rebinding `Host` check. It stays future work, now recorded in
  `docs/architecture.md`.
- The public site on GitHub Pages: its own change
- A full Content Security Policy. Only `frame-ancestors` is set, so scripts and
  Swagger UI are unaffected.
- Browser-automated tests. Layout stays covered by the screenshot check, as
  `add-web-ui` decided.
- Who merges pull requests, and branch protection
- Anything from Phase 2 onward

## Capabilities

### New Capabilities

None. Each fix belongs to an existing capability.

### Modified Capabilities

- `web-ui`: adds requirements that pages can't be framed, fit the longest valid names,
  move focus to an action's result, and have an icon. "Page through the catalog"
  gains a scenario for page numbers too large to reach.
- `service-catalog`: "List services" says what a page past the end returns, including
  page numbers too large to reach.

## Impact

- **Code:**
  - A new servlet filter in the `api` package sends the framing headers.
  - `ServiceCatalog.list` handles an offset too large for the database query, and
    `ServiceRepository` gains `countByOwner`.
  - `CatalogPages` reads digits too long for an `int` as a very large page.
- **Resources:**
  - `static/css/devplane.css`: identifier rule, table columns, breakpoint, buttons,
    summary, breadcrumbs, and the diagram's columns
  - `layout.html` and `fragments/path.html`: notice focus and the icon link
  - a new `static/favicon.svg`
- **API:** no endpoint changes. Very large `page` values return 200 with empty `items`
  instead of 500. Every response gains the two framing headers.
- **Dependencies:** none
- **Docs:** README.md, docs/architecture.md, and docs/ui-guidelines.md
