# Tasks

`harden-web-ui` should be merged before starting, so the site inherits its layout fixes.

## 1. Site build

- [x] 1.1 Make `devplane.css`'s font URLs relative (`../fonts/…`). Verify that
  `StaticAssetsTests` passes, and in a browser that the app still loads its fonts.
- [x] 1.2 Add the test-source class in the `templates` package that archives the
  `orders-api` example through `ProjectArchiver`, and the `siteDownload` `JavaExec` task.
  Verify "The download is a real project" by unzipping it: an `orders-api/` folder, and
  `gradlew` marked executable.
- [x] 1.3 Add the `siteBuild` `Sync` task, assembling `build/site/` from `site/` (without
  `figure.py` and `*.test.js`), the app's stylesheet and fonts, and the zip. Verify the
  folder's contents, and that `./gradlew siteBuild` fails when generation fails ("A broken
  template fails the build").

## 2. Splash page

- [x] 2.1 Create `site/index.html`, `site/site.css`, and `site/figure.py` from the approved
  mockups, with every link and asset relative and no Google Fonts. Apply the design's
  content changes: status, Interface as built, "Try the demo", "What it does today", and
  the top bar. Verify the scenarios "Shows the page", "Shows each layer's state", and
  "Works without JavaScript".
- [x] 2.2 Add the new Phase 1 item to the README roadmap. Use the README's exact wording in
  the splash page's roadmap. Add `SiteRoadmapTests`, and verify "In step" and "A ticked item
  isn't updated on the site".

## 3. Demo logic

- [x] 3.1 Write `site/demo/catalog.js`: sample data, the validation rules and messages as
  constants, list, filter, register (including duplicates), unregister, reset, simulated
  repositories, relative times, and storage that falls back to memory. Unit test it with
  `node --test`, including invalid fields, duplicates, and blocked storage.
- [x] 3.2 Add `SiteRulesTests` in the `catalog` test package. Verify "Rules drift from the
  real app" by changing a rule temporarily and seeing the build fail, then restoring it.

## 4. Demo screens

- [x] 4.1 Write `site/demo/index.html` with its `noscript` message, and `app.js`'s routing,
  its element helper, and the demo notice. Add the catalog screen: the table, filter,
  no-match state, and empty-catalog introduction, with sample repositories as plain text.
  Add the demo notice to docs/ui-guidelines.md. Verify in a browser "Every screen is
  labeled", "Needs JavaScript", and the "Browse the sample catalog" scenarios.
- [x] 4.2 Add the service screen: details, the path steps, "Download project" with its
  explanation, and the unknown-service page. Add the simulated "Create repository", with its
  progress state and result. Verify in a browser the "View a service in the demo" and
  "Simulate creating a repository" scenarios, and "Downloads the example".
- [x] 4.3 Add the register form, with errors next to fields and the error summary. Add the
  unregister confirmation, and "Reset demo". Verify in a browser the "Register a service in
  the demo" and "Unregister and reset in the demo" scenarios, and "Entered text stays text".

## 5. Publishing

- [x] 5.1 Add `.github/workflows/pages.yml` with its `build` and `deploy` jobs. Verify on the
  pull request that the workflow builds and runs the demo's tests without deploying ("A pull
  request doesn't publish").
- [ ] 5.2 Update `docs/ui-guidelines.md`'s Public site section as the design lists, and
  `docs/mockups/README.md` to say `site/` is the real site. Describe the site in
  docs/architecture.md, and link to it from the README. Verify `openspec validate --all
  --strict`, and that the links resolve once the site is live.

## 6. Integration checks

- [x] 6.1 Serve `build/site/` under a `/devplane/` path locally, and run the screenshot check
  on the splash page and every demo screen at 1280px, 768px, and 390px, including a
  63-character name and owner. Confirm the "Requests stay on the site" scenario from the
  browser's network log, compare the demo with the app's pages, and fix what the check finds.
- [x] 6.2 Ask the repository owner before enabling Pages, with GitHub Actions as its source.
  Once it's enabled, tick the roadmap item, run `./gradlew build`, and confirm CI passes on
  the pull request.
- [ ] 6.3 After merging, confirm "Publishes from main" at
  `https://spollito.github.io/devplane/`, and repeat the network check on the live site. Tick
  this task in the archive pull request.

## Workflow follow-up

- Run `/opsx:verify add-public-site`, then `/opsx:archive add-public-site` to merge this
  change's specs into `openspec/specs/`.
