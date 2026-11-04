# UI Mockups

Static HTML mockups that were reviewed and approved while writing
[../ui-guidelines.md](../ui-guidelines.md). DevPlane doesn't serve or test these
files. They're the record of how the web UI and the public site were meant to look.
Where a mockup and the guidelines disagree, the guidelines win.

The real public site is in [`site/`](../../site), which started from `landing.html`,
`landing.css`, and `figure.py`. Change it there, not here.

Open a file in a browser to view it. The fonts load from Google Fonts here, while
the real UI serves its own.

| File | Shows |
| --- | --- |
| `catalog.html` | The catalog list. Add `?state=empty` for the empty catalog introduction, or `?state=no-match` for an owner filter that matches nothing. |
| `service.html` | A service page. Add `?state=creating`, `?state=error`, or `?state=created` for the repository step's other states. |
| `landing.html` | The public site's splash page, as approved |
| `style.css` | The design values and components that every page shares |
| `landing.css` | The public site's additions |
| `figure.py` | Draws the landing page's layer figure. Run `python3 figure.py` after changing a layer's state. |

The `?state=` switches and the sample services are mockup-only.
