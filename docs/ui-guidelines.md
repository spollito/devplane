# DevPlane UI Guidelines

These rules apply to every page of DevPlane's web UI, and to its public site on
GitHub Pages, with the exceptions listed under [Public site](#public-site). Check a
page against them before it's done, and add new values here before using them in a
page: a color, size, or component that isn't in this document doesn't belong in
the UI yet.

The UI is a working tool for the developer who runs DevPlane. It should look like
a drawing of the platform: lines, labels, and the real state of each service.
Nothing is there to decorate.

## Principles

- **Draw, don't decorate.** Structure comes from 1px lines and spacing, never from
  shadows, gradients, or background effects.
- **Identifiers look like identifiers.** Service names, owners, repositories, and
  template names are set in the monospace face, exactly as DevPlane stores them.
  Nothing else is.
- **Blue means "you can act here".** The accent marks links, the primary button,
  and keyboard focus. It is never used to decorate.
- **Show only what's true.** Every number, status, and date on a page comes from
  DevPlane. A feature appears only once it works, never as a disabled placeholder.

## Design values

The values below are CSS custom properties, defined once in the UI's stylesheet.
Pages use the properties, never raw values.

### Color

Dark theme only for now. A light theme would redefine these properties and nothing
else.

| Property | Value | Use | Contrast on `--bg` |
| --- | --- | --- | --- |
| `--bg` | `#0e131b` | Page background, inputs | |
| `--surface` | `#141a24` | Table header, top bar, notices | |
| `--surface-hover` | `#19212d` | Hovered table row | |
| `--line` | `#242d3b` | Dividers and region borders (decorative) | 1.3:1 |
| `--line-control` | `#5c6779` | Borders of inputs, secondary buttons, and diagram steps | 3.3:1 |
| `--text` | `#e3e7ed` | Body text, headings | 15.0:1 |
| `--text-muted` | `#9ba4b1` | Hints, metadata, table headers | 7.4:1 |
| `--accent` | `#6b9bff` | Links, focus ring, the next step in the path diagram | 6.9:1 |
| `--accent-strong` | `#2f6bf0` | Primary button background | 4.0:1 |
| `--accent-strong-hover` | `#2a60d9` | Primary button background on hover | |
| `--on-accent` | `#ffffff` | Text on the primary button | 4.7:1 on `--accent-strong`, 5.6:1 on `--accent-strong-hover` |
| `--production` | `#3fbf7f` | Lifecycle marker | 8.0:1 |
| `--experimental` | `#e0b03c` | Lifecycle marker | 9.3:1 |
| `--deprecated` | `#8a94a3` | Lifecycle marker | 6.1:1 |
| `--error` | `#ff7a70` | Error text and borders | 7.3:1 |
| `--success` | `#4cc38a` | Success notice marker and border | 8.4:1 |

All text pairs stay at 4.5:1 or more on `--surface` and `--surface-hover` too.
Check any new pair with a contrast checker and add it to this table.

### Type

Two faces from one family, served by DevPlane itself (no font CDN, so the UI works
offline), with their SIL Open Font License alongside:

- **Atkinson Hyperlegible Next** for everything except identifiers. It was designed
  for legibility and tells `l`, `1`, and `I`, and `0` and `O`, apart.
- **Atkinson Hyperlegible Mono** for identifiers: service names, owners,
  repositories, template names, and file names such as `orders-api.zip`.

Fallbacks are `system-ui, sans-serif` and `ui-monospace, monospace`.

| Step | Size / line height | Weight | Use |
| --- | --- | --- | --- |
| Small | 12px / 16px | 400, 600 | Hints, metadata, table headers |
| Body | 14px / 20px | 400, 600 | Everything else, including buttons and inputs |
| Heading | 16px / 24px | 600 | Section headings (`h2`) |
| Title | 20px / 28px | 600 | Page title (`h1`), one per page |

These four steps are all there is. Monospace runs wider than the sans, so it's set
1px smaller than the text around it (13px in body text, 19px in a title), and
never below 12px. Put the monospace class on an inline element inside the heading
or cell, not on the heading itself, so "1px smaller" is measured from the right
size. Identifiers never wrap inside a sentence or a cell, because a name split at
its hyphen reads as two names. A page title may wrap a long name. Body text lines
stay under 80 characters.

### Spacing

A 4px base: `--space-1` 4px, `--space-2` 8px, `--space-3` 12px, `--space-4` 16px,
`--space-5` 24px, `--space-6` 32px, `--space-7` 48px. Related things sit closer
together than unrelated things. A label is 4px from its input; sections are 32px
apart.

### Radius, borders, and elevation

- **Regions have square corners:** tables, notices, the top bar, and the steps of
  the path diagram.
- **Controls have a 3px radius:** buttons, inputs, and selects. The rounded corner
  tells you that you can click or type there.
- **Borders are 1px.** `--line` separates regions, and `--line-control` outlines
  controls. A dashed `--accent` border marks the next step in the path diagram and
  nothing else.
- **There is no elevation.** No `box-shadow`, no overlays, and no modal dialogs.

### Layout

- A 48px top bar holds the `DevPlane` wordmark, the navigation (`Services`), and an
  `API docs` link to `/swagger-ui.html` on the right.
- Content sits in one left-aligned column, at most 1120px wide, with 24px side
  padding (16px below 640px).
- Each page starts with breadcrumbs (except the catalog) and the page title. If the
  page has a primary action, such as "Register service" on the catalog, it sits to
  the right of the title, or wraps below it on narrow screens.
- There is one breakpoint, at 640px. Pages must not scroll sideways at 390px.
- Forms are at most 480px wide, with their buttons in a row below the last field:
  the primary action first, then Cancel.
- Pages without a mockup (the register form, the unregister confirmation, and the
  error pages) are built from the components in this document.

### Motion

Motion only answers an action. Nothing animates on page load or on hover, beyond
a color change.

- While a repository is being created, the dashed outline of its step moves slowly
  around it, like a line being drawn.
- Color changes on hover and focus take 100ms.
- With `prefers-reduced-motion: reduce`, nothing moves, and the pending text still
  says what's happening.

### Icons

Inline SVG, 16px, with Lucide's default stroke in `currentColor`, taken from
[Lucide](https://lucide.dev) (ISC license). Use them only where they help
recognition: an external link, a download, and a notice's status. Every icon sits
next to a text label, and icons are hidden from screen readers with
`aria-hidden="true"`.

## Components

### Buttons and links

| Kind | Looks like | Use |
| --- | --- | --- |
| Primary | `--accent-strong` background (`--accent-strong-hover` on hover), `--on-accent` text | The one main action in a page or region, such as "Register service" or "Create repository" |
| Secondary | Transparent, `--line-control` border, `--text` | Other actions: "Download project", "Cancel", pagination |
| Danger | Transparent, `--error` border and text | Destructive actions: "Unregister service" |
| Link | `--accent` text, underlined on hover and focus | Navigation, a table row's main link, and links within text |
| Secondary link | `--text`, underlined on hover and focus | Other links in a table row or summary, such as owners and repositories |

- Buttons are 32px tall, with 12px side padding and a 14px label at weight 600.
- A page or region has at most one primary button.
- A table row has one `--accent` link, its name, so the row's main link stands
  out. Other links in the row are secondary links.
- A link that leaves DevPlane (GitHub) shows the external-link icon and opens in the
  same tab.
- "Download project" is a link to `GET /api/v1/services/{name}/project` with the
  `download` attribute, styled as a secondary button.

### Form fields

Each field has, in this order: a visible label, an optional hint, an error message
when there is one, and the control.

- **Label:** 14px, weight 600, tied to the control with `for`. The label is never a
  placeholder, and placeholders aren't used.
- **Hint:** 12px, `--text-muted`, linked with `aria-describedby`. It states the
  rule, for example "Lowercase letters, digits, and hyphens. Starts with a letter.
  Can't be changed later."
- **Control:** 32px tall, `--bg` background, 1px `--line-control` border, 3px
  radius, monospace when it holds an identifier.
- **Optional fields** say "(optional)" in the label. Required fields are unmarked.
- **Choices:** radio buttons when there are at most four options, each with a
  one-line description. A choice that has only one option, such as the template
  while only `java-rest-api` exists, is shown as plain text, not as a control.

### Showing errors

DevPlane's validation and errors are the same for the API and the UI. The API
returns them as problem details, and a page shows the same information next to the
thing it's about. The status code and the raw response are never shown.

| Error | Where it appears |
| --- | --- |
| Invalid field (an entry in problem details' `errors`) | Under that field's label: the field's label followed by the message, for example "Name must be 1 to 63 lowercase letters…". The control gets an `--error` border and `aria-invalid="true"`. |
| Empty required field | Under the field: "Enter a name for the service" |
| Duplicate service name (409) | Under the name field: "A service named `orders-api` already exists," with a link to it |
| Failed action with no field (409, 502, or 503 from provisioning) | An error notice inside the region where the action was taken, titled with what failed ("Couldn't create the repository"), followed by DevPlane's `detail` and what to do next |
| Unknown service (404) | A page: "No service named `orders-api`," with a link back to Services |
| Unexpected error (500) | A page: "DevPlane couldn't finish this request. The DevPlane log has the details." |

When a form has any errors, an error summary at the top of the form lists them as
links to their fields, and receives focus. The values the developer typed are kept.

DevPlane writes every `detail` itself, so a detail is safe to show. Messages from
GitHub or Java exceptions never appear in the UI.

### Notices

A notice is a bordered region (1px, square corners, `--surface` background) with a
status icon and one or two sentences.

- **Success** (`role="status"`, `--success` border): appears at the top of the page
  after a redirect, such as "Registered `orders-api`."
- **Error** (`role="alert"`, `--error` border): appears where the failed action was.

Notices stay until the page changes. They don't time out.

### Tables

The catalog list is a real `<table>` with `<th scope="col">` headers.

- **Header row:** `--surface` background, 12px `--text-muted`, weight 600, sentence
  case.
- **Body rows:** 36px tall, divided by 1px `--line`, with a `--surface-hover`
  background on hover. Only the links inside a row are clickable, not the row
  itself.
- **Columns:** Name, Description, Owner, Lifecycle, Repository, and Updated.
  - **Name** links to the service.
  - **Description** is cut to one line with an ellipsis, and the full text is in
    its `title`.
  - **Owner** links to the list filtered by that owner.
  - **Repository** shows `owner/name`, linked to GitHub, or "None" in
    `--text-muted`.
  - **Updated** is relative ("2 days ago") in a `<time>` element, with the exact
    date and time in its `title`.
- **Below 640px,** Description, Repository, and Updated are hidden, and the owner
  moves under the name.
- **Paging:** "21–40 of 57" on the left and Previous and Next on the right, hidden
  when everything fits on one page.

### Lifecycle markers

A 10px square drawn in the stage's color, followed by the stage's name in
`--text`. The shape differs too, so the stage never depends on color alone:

| Stage | Marker | Meaning of the shape |
| --- | --- | --- |
| Experimental | Hollow square, `--experimental` | Drawn, not yet built |
| Production | Filled square, `--production` | Built and in use |
| Deprecated | Square with a diagonal stroke, `--deprecated` | Struck out |

Markers are not pills or badges: they have no background and no rounded corners.

### Service path diagram

The diagram shows where a service's code comes from and where it lives, as an
ordered list of steps drawn as connected boxes. It appears on the service page, and
on the empty catalog as an introduction to DevPlane. The steps are:

1. **Catalog entry**: when the service was registered
2. **Project**: generated on each download from a template, with "Download
   project"
3. **Repository**: on GitHub once it's created, with its link. Until then, it
   holds the repository options and "Create repository".

Rules:

- A step is drawn in one of three ways:
  - **Done:** a solid `--line-control` border.
  - **Next:** the step that needs the developer has a dashed `--accent` border, and
    its action sits inside it. Only one step is dashed at a time.
  - **Not reached yet:** a solid `--line` border. Its text says what will happen,
    so the faint border carries no meaning on its own.
- Steps connect with a 1px arrow, left to right, and stack top to bottom below
  640px.
- The diagram shows only steps DevPlane performs today. A later phase adds its step
  (such as a deployment) when that phase is built, never before.

### Empty, loading, and pending states

- **Empty:** say what's missing, and offer the action that fills it.
  - **The empty catalog introduces DevPlane.** It's the first page a new user
    sees, and the app has no landing page of its own. (The public site
    introduces DevPlane to people who haven't installed it.) It shows:
    - the heading "No services are registered yet"
    - one sentence on what DevPlane does
    - the path diagram, with "Catalog entry" as the next step holding "Register
      service", and Project and Repository not reached yet

    The page header's "Register service" button is hidden in this state, so the
    page keeps one primary button.
  - **A filtered list that matches nothing** says "`payments-team` owns no
    services" and links to the unfiltered list.
- **Loading:** pages are rendered on the server and arrive complete, so there are no
  spinners or skeleton screens.
- **Pending:** an action that waits on GitHub disables its button and its options,
  changes the button's label to say what's happening ("Creating repository…"),
  says how long it takes ("This takes a few seconds."), and marks its region
  `aria-busy="true"`. The page still works when JavaScript is off, with a normal
  form submit.

### Confirmations

A destructive action goes to a confirmation page, not a modal dialog. The page says
exactly what will happen and what won't. For example, unregistering says that the
service leaves the catalog and that its GitHub repository is kept. It has a danger
button naming the action and the object ("Unregister `orders-api`"), and a Cancel
link back.

## Public site

DevPlane's public site is one static page on GitHub Pages that explains what
DevPlane does and where it's going, for people who will read about it rather than
run it. It uses the same tokens, fonts, buttons, links, and top bar as the app. It
is written for reading rather than working, so these rules apply only to it.

- **Reading scale:**

  | Step | Size / line height | Use |
  | --- | --- | --- |
  | Title | 56px / 60px (40px / 44px below 640px) | The `h1`, once |
  | Lede | 18px / 28px | The paragraph under the title |
  | Section heading | 24px / 32px | Each section's `h2` |
  | Body | 16px / 26px | Paragraphs |

  These are the only sizes beyond the app's four. Sections are divided by a
  `--line` rule, with 48px of space on each side.
- **Figures are technical drawings:**
  - Inline SVG, with 1px-weight lines, inside a `--line` frame with a dot grid.
  - Each figure has a caption that starts with its number in monospace ("Fig. 1")
    and says what the figure shows.
  - Drawings use the same notation as the path diagram. Solid lines mean built,
    dashed `--accent` lines mean in progress, and dotted `--line-control` lines
    mean planned.
  - Text inside a drawing never renders below 12px at the size the drawing is
    shown.
  - Each drawing has a text legend that says the same thing. Screen readers read
    the legend, and it replaces the drawing's labels below 640px.
- **One moving part:** the layers in the first figure drift apart over the first
  70% of the viewport's height as the page scrolls. This uses CSS scroll-driven
  animation, and runs only where the browser supports it and reduced motion isn't
  set. Links within the page scroll smoothly, except with reduced motion.
- **Only true things:**
  - The site describes what DevPlane does today, and the README's roadmap with
    each item's real state.
  - The change that ticks a roadmap item also updates the site.
  - The site shows no testimonials, logos, or numbers DevPlane doesn't produce.

## Writing

- Use sentence case for everything: titles, labels, buttons, and table headers.
  Don't use all caps.
- A button says what happens, as a verb and its object: "Register service",
  "Download project", "Create repository", "Unregister service". Never use
  "Submit", "OK", or "Click here".
- An action keeps its name through the whole flow. "Create repository" produces
  "Created repository `octo-dev/orders-api`."
- Use DevPlane's own words, and use them the same way everywhere:

  | Word | Means |
  | --- | --- |
  | Service | A catalog entry |
  | Register / unregister | Adding a service to the catalog, or removing it |
  | Owner | The team that owns a service |
  | Lifecycle | Experimental, production, or deprecated |
  | Template | What a project is generated from |
  | Project | The generated code, downloaded as a zip |
  | Repository | The service's GitHub repository |

- Errors say what happened and what to do next. They don't apologize, and they're
  never vague.
- Don't write "please", "successfully", "simply", exclamation marks, or marketing
  words such as "seamless", "powerful", or "AI-powered".
- Dates on the service page are exact ("Oct 21, 2026, 14:05"). Dates in the table
  are relative, with the exact date in the `title`.

## Accessibility minimums

- **Contrast:** text has at least 4.5:1 against its background. Control borders,
  focus rings, and the shapes that carry meaning (lifecycle markers, and the
  borders of done and next diagram steps) have at least 3:1 (WCAG 2.2 AA).
- **Focus:** every interactive element shows a 2px `--accent` outline, offset by
  2px, when it receives keyboard focus (`:focus-visible`). An outline is never
  removed without this replacement.
- **Labels:** every control has a visible `<label>`. Hints and errors are linked
  with `aria-describedby`.
- **Keyboard:** every action works with the keyboard alone, in reading order. The
  first focusable element is a "Skip to content" link.
- **Structure:** each page has `header`, `nav`, and `main` landmarks, one `h1`, and
  headings in order. Lists are lists, and tables are tables.
- **Meaning:** color is never the only signal. Lifecycle markers differ in shape,
  and errors have text.
- **Targets:** buttons and links are at least 24 by 24px.
- **Zoom:** pages work at 200% zoom and at 390px wide, without scrolling sideways.
- **Motion:** respect `prefers-reduced-motion`.
- **Language:** `<html lang="en">`, and a `<title>` that names the page first, such
  as "orders-api · DevPlane".

## Never

- Purple or indigo gradients, or any gradient
- Gradient text, glow, or text shadows
- Emoji used as icons
- Content chopped into rounded cards with drop shadows
- `box-shadow`, `backdrop-filter`, glassmorphism, or blurred backgrounds
- Made-up numbers: stats, charts, counters, or progress that DevPlane doesn't
  measure
- Avatars, notification bells, search boxes, or menus for features DevPlane
  doesn't have
- Disabled placeholders for later-phase features (deployments, metrics, AI)
- Marketing copy, buzzwords, or text that tells the developer how to feel
- All-caps labels, or eyebrow labels above headings
- More than one accent color
- Placeholder text instead of a label
- A spinner without text
- Modal dialogs
- Animations on page load or scroll (except the public site's one moving part)
- The default look of a component library (Bootstrap, Material, and so on)
- Large display type. In the app, the largest text is the 20px page title. The
  public site's sizes are listed under Public site.

## Screenshot check

Every page change ends with a screenshot check against this document:

1. Run DevPlane (`./gradlew bootRun`) with services that cover the states being
   checked: an empty catalog, several pages of services, all three lifecycle
   stages, and a service with and without a repository.
2. Take full-page screenshots with Playwright at 1280px and 390px wide, for every
   state the change touches, including error states and keyboard focus on a
   control.
3. Check each screenshot against the Never list, then for:
   - colors and sizes that aren't in the Design values
   - more than one primary button in a region
   - sideways scrolling at 390px
   - text that breaks the Writing rules
4. Fix what's wrong and take the screenshots again. Screenshots are for review and
   aren't committed. The Playwright MCP server saves them in `.playwright-mcp/`.
