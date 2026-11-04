# Design: Harden the web UI

## Context

See proposal.md for why, and the specs for the behavior. What exists today:

- `api.CrossSiteRequestInterceptor` refuses requests from other sites, but a form
  inside a framed DevPlane page is `same-origin` to the browser. No response sets
  `X-Frame-Options` or a Content Security Policy. There are no servlet filters yet.
- `devplane.css` sets `.id { white-space: nowrap }`, from a rule I wrote into the
  guidelines and mockups. `.table td` is also `nowrap`, `.cell-description` uses
  `max-width: 0; width: 100%`, and `.summary` is a `max-content` grid.
- The `.path` grid's column minimums add up to 830px, but it only stacks below 640px.
  With ordinary names, the service page is 854px wide at viewports from 641px to 830px
  (measured).
- Spring Data's `PageableUtils.getOffsetAsInteger` throws "Page offset exceeds
  Integer.MAX_VALUE" for a larger offset. That's what becomes the 500. The cutoffs
  measured match it: page 107374184 on the catalog page (numbered from 1), and page
  107374183 on the API (numbered from 0).
- `CatalogPages.pageNumber` only reads up to nine digits, so a longer number counts as
  invalid and shows the first page.
- The register form's error summary already takes focus with `tabindex="-1"` and
  `autofocus`, and htmx 2.0.11 focuses `[autofocus]` in swapped content (its
  `processFocus` function).

The layout decisions below were tried before writing this design, by injecting CSS
into the running app with the longest valid data.

## Goals / Non-Goals

**Goals:**

- One fix for each cause, in one place, so the API and pages can't disagree.
- Ordinary pages look the same as the approved mockups at 1280px and 390px.
- The next screenshot check can't miss this class of bug.

**Non-Goals:**

- A full Content Security Policy
- Automated browser tests. Layout can't be asserted from MockMvc, so this design adds
  the data and widths to the screenshot check instead.

## Decisions

### 1. A servlet filter sends the framing headers

`api.FrameDenyFilter`, a `OncePerRequestFilter` bean, sets
`X-Frame-Options: DENY` and `Content-Security-Policy: frame-ancestors 'none'` on the
response before calling the chain.

- **A filter, not an interceptor:** the cross-site check's interceptor doesn't run for
  every path that serves HTML. A filter covers pages, Swagger UI, static files, and
  unknown paths, with one rule and no list of paths.
- **Error pages:** `sendError` clears the response's body but keeps its headers, so the
  error page dispatched afterward still has them. MockMvc doesn't run that dispatch, so
  a task checks it with curl against the running app.
- **Both headers:** `frame-ancestors` is the current standard. `X-Frame-Options` covers
  browsers that don't support it.
- **Every response:** the headers do nothing on JSON, so there's no reason to carve the
  API out.

Alternatives:

- **Spring Security's headers support** was rejected: a framework for two headers,
  and `add-web-ui` already declined it.
- **A full policy** (`default-src` and so on) was rejected. It would have to allow
  htmx's `hx-on` handlers and Swagger UI's inline scripts, which is a change of its own.

### 2. Identifiers stay whole when they fit, and break when they don't

The `.id` rule becomes:

```css
.id {
	display: inline-block;
	max-width: 100%;
	white-space: normal;
	overflow-wrap: anywhere;
}
```

An inline-block moves to the next line as a unit, so `java-rest-api` never splits at a
hyphen inside a sentence. An identifier wider than its container breaks inside its own
box. A short name measured one line, and the page didn't scroll at any width.

Supporting changes, each found by the prototype:

| Rule | Change | Why |
| --- | --- | --- |
| `a:hover .id`, `a:focus-visible .id` | Underline explicitly | Text decoration doesn't reach inside an inline-block |
| `.button` | `min-height: 32px`, 5px vertical padding, `white-space: normal`, `max-width: 100%` | "Unregister `<63 characters>`" must wrap. Ordinary buttons stay 32px. |
| `.summary` | `flex-wrap` instead of a `max-content` grid | A long owner overlapped the lifecycle stage |
| `.facts` | Second column `minmax(0, 1fr)` | Lets a long name shrink |
| `.breadcrumbs li` | `display: flex`, with the `/` as a flex item | The `/` stayed alone on its own line |
| `.repo-link` | `max-width: 100%` | Keeps the link and icon inside the step |

Alternatives:

- **Letting names wrap normally** was rejected. `java-rest-api` splits at its hyphen in
  sentences, which is why `nowrap` was added.
- **Truncating with an ellipsis** was rejected. Identifiers are what people read and
  copy, and two long names can differ only at the end.
- **Non-breaking hyphens** were rejected. Copying the text would produce a different
  character, and so a wrong name.

### 3. The catalog table uses fixed column widths

Wrapping alone isn't enough. With automatic table layout, `overflow-wrap: anywhere`
shrinks identifiers' minimum width to almost nothing, and the description column's
`width: 100%` takes the space. Measured at 1280px, `orders-api`'s row was 97px tall and
the longest names' rows 477px.

So `.table` becomes `table-layout: fixed`:

| Column | Width |
| --- | --- |
| Name | 24% |
| Description | The rest (auto) |
| Owner | 14% |
| Lifecycle | 132px |
| Repository | 18% |
| Updated | 120px |

`.cell-description` drops `max-width: 0; width: 100%`, and keeps its ellipsis.

Measured at 1280px with these widths, ordinary rows are 38px and show "Order management
REST API" in full. The rows with the longest names are 137px, and nothing overflows.

Alternative: an element that scrolls sideways around the table was rejected. It would
hide columns and still break the guidelines' rule.

### 4. One breakpoint, at 960px

The existing 640px breakpoint moves to 960px, for the app only. Below it, the path
diagram stacks, the table hides Description, Repository, and Updated, and the owner
moves under the name. `.path`'s columns become `minmax(0, …fr)`, so they can't force a
minimum width above 960px.

Measured with the longest names at 1280, 1024, 961, 960, 768, 641, 390, and 320px, no
page scrolled sideways, overlapped, or clipped an identifier.

- **The trade-off:** a tablet loses three catalog columns it would otherwise have room
  for. Those columns are on each service's page, and one number is simpler than a third
  layout.
- **The public site** keeps its own sizes in the guidelines' Public site section.

Alternative: squeezing three diagram columns into a tablet's width was rejected,
because the repository step's form gets too narrow.

### 5. The catalog handles offsets the query can't reach

`ServiceCatalog.list` checks first:

```java
if (pageable.getOffset() > Integer.MAX_VALUE) {
	long total = (owner != null) ? repository.countByOwner(owner) : repository.count();
	return new PageImpl<>(List.of(), pageable, total);
}
```

- **One place:** both the API and the catalog page go through it. The API returns the
  empty page, as for any page past the end. The catalog page's existing code then asks
  for the last page, because the requested page is past `totalPages`.
- `ServiceRepository` gains `countByOwner`.
- `CatalogPages.pageNumber` reads any run of digits. One too long for an `int` becomes
  `Integer.MAX_VALUE`, so "a number past the last page" holds at any length.

Alternatives:

- **Clamping in each controller** was rejected: two copies of the rule, and other
  callers of `list` stay exposed.
- **Answering 400** was rejected. The controller's documented behavior is to clamp, and
  the web-ui spec says to show the last page.

### 6. Notices take focus

The success notice in `layout.html` and the repository error in `fragments/path.html`
gain `tabindex="-1"` and `autofocus`, the same pattern as the register form's error
summary.

- Without JavaScript, the browser honors `autofocus` when the redirected page loads.
- With htmx, its `processFocus` focuses `[autofocus]` in the swapped `main`. The
  "Focus follows the update in place" scenario is checked in a browser.
- Both notices already have a live-region role, so screen readers announce them.

Alternative: an `hx-on` handler that moves focus was rejected. It would need more
script, and it wouldn't help with JavaScript off.

### 7. An SVG icon

`static/favicon.svg` is the wordmark's three planes, drawn in `--text` on a `--bg`
square, so it stays visible on light and dark browser tabs. `layout.html`'s head links
it with `rel="icon"`, which every page includes.

Alternatives:

- **A `.ico` file** was rejected: a binary to maintain, for browsers old enough to
  ignore SVG icons. They get a harmless 404.
- **An empty `data:` icon** was rejected. It silences the request, but a real icon
  helps find the tab.

### 8. Testing

- **Server tests**, in the existing classes:
  - the framing headers on each path in the scenario
  - pages rendering a 63-character name and owner, with a repository
  - huge page numbers on both the page and the API, including with an owner filter
  - the notices' `tabindex` and `autofocus`, and no `autofocus` without a notice
  - the icon, and every page's link to it
- **Browser checks**, as the screenshot check: everything about layout, the refusal to
  frame, and focus after the update in place.

### Additions to the UI guidelines

- **Type:** replaces "Identifiers never wrap" with the rule in decision 2.
- **Layout:** one breakpoint at 960px. The padding and table rules that say 640px change
  with it, and the path diagram's stacking rule too.
- **Buttons:** at least 32px tall, growing only when a label has to wrap.
- **Tables:** the column widths in decision 3.
- **Notices:** a notice that reports an action's result takes focus.
- **Screenshot check:** the data must include 63-character names and owners. Widths
  become 1280px, 768px, and 390px. "No sideways scrolling" is measured by comparing
  `documentElement.scrollWidth` with `clientWidth`, not judged by eye.

### Later roadmap items this must not block

- **The public site on GitHub Pages:** it can't embed DevPlane in a frame, and doesn't
  need to. It may reuse `devplane.css`, and inherits these fixes.
- **Deployments and health columns (Phases 2 and 3):** the fixed widths need rebalancing
  when a column is added. That's a one-line change.

## Risks / Trade-offs

- [Layout regressions aren't caught automatically] → The screenshot check now names the
  data and widths that would have caught these. A browser test is worth revisiting if
  this recurs.
- [Fixed widths truncate descriptions sooner at wide-but-not-full widths, about 150px
  at 961px] → Descriptions keep their ellipsis and `title`, and show in full on the
  service page.
- [Tablets lose three catalog columns] → Accepted in decision 4.
- [A notice's focus outline appears on a redirected page] → It matches the register
  form's error summary, and shows the keyboard user where they are.
- [Any very long unbroken 63-character name breaks anywhere rather than at a hyphen] →
  Accepted: it has no hyphen to break at.

## Migration Plan

No database change. Roll back by reverting. API clients see only that very large `page`
values return 200 instead of 500, and two more response headers.
