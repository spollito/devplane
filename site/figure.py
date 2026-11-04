"""Draws the exploded isometric figure of DevPlane's four layers on the splash page.

Run `python3 figure.py` in this folder. It rewrites the drawing between the
`figure:start` and `figure:end` markers in index.html. To change a layer's
state, edit LAYERS below: "built", "next" (in progress), or "planned".
"""
import re
from pathlib import Path

W, H, T, CX = 190, 110, 6, 220
CYS = [125, 225, 325, 425]
RIGHT_X = CX + W

def p(cy, a, b):
    return (CX + (a - b) * W, cy - H + (a + b) * H)

def pts(points):
    return " ".join(f"{x:.1f},{y:.1f}" for x, y in points)

def quad(cy, a0, b0, a1, b1):
    return f'<polygon points="{pts([p(cy, a0, b0), p(cy, a1, b0), p(cy, a1, b1), p(cy, a0, b1)])}"/>'

def line(cy, a0, b0, a1, b1):
    (x1, y1), (x2, y2) = p(cy, a0, b0), p(cy, a1, b1)
    return f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}"/>'

def detail(key, cy):
    if key == "interface":  # a small catalog table, seen from above
        parts = [quad(cy, 0.2, 0.22, 0.8, 0.78), line(cy, 0.2, 0.34, 0.8, 0.34)]
        parts += [line(cy, 0.2, b, 0.8, b) for b in (0.48, 0.62)]
        parts.append(line(cy, 0.42, 0.34, 0.42, 0.78))
        return parts
    if key == "api":  # three modules: catalog, templates, provisioning
        spans = [(0.12, 0.34), (0.41, 0.63), (0.7, 0.92)]
        parts = [quad(cy, a0, 0.66, a1, 0.9) for a0, a1 in spans]
        parts += [line(cy, 0.34, 0.78, 0.41, 0.78), line(cy, 0.63, 0.78, 0.7, 0.78)]
        return parts
    if key == "deploy":  # cluster nodes
        spans = [(0.12, 0.34), (0.41, 0.63), (0.7, 0.92)]
        return [quad(cy, 0.66, b0, 0.9, b1) for b0, b1 in spans]
    if key == "ops":  # a metric line over a baseline
        trace = [(0.1, 0.84), (0.2, 0.74), (0.3, 0.8), (0.4, 0.66), (0.5, 0.76), (0.6, 0.7), (0.7, 0.82), (0.8, 0.72), (0.9, 0.78)]
        return [line(cy, 0.1, 0.92, 0.9, 0.92), f'<polyline points="{pts([p(cy, a, b) for a, b in trace])}"/>']
    return []

MARKERS = {
    "built": '<rect x="0" y="0" width="8" height="8" class="marker marker--built"/>',
    "next": '<rect x="0.5" y="0.5" width="7" height="7" class="marker marker--next"/>',
    "planned": '<rect x="0.5" y="0.5" width="7" height="7" class="marker marker--planned"/>',
}

LAYERS = [
    ("interface", "built", "Interface", "Web UI and API docs", "Built"),
    ("api", "built", "Platform API", "Catalog, templates, repositories", "Built"),
    ("deploy", "planned", "Deployment", "Containers, CI/CD, Kubernetes", "Planned for phase 2"),
    ("ops", "planned", "Operations", "Metrics, logs, AI diagnostics", "Planned for phases 3 and 4"),
]

out = ['<svg class="layers" viewBox="0 -12 690 584" aria-hidden="true">',
       '<defs><pattern id="dots" width="16" height="16" patternUnits="userSpaceOnUse"><circle cx="1" cy="1" r="0.9" class="grid-dot"/></pattern></defs>',
       '<rect x="0" y="-12" width="690" height="584" fill="url(#dots)"/>',
       '<g class="guides">',
       f'<line x1="{CX - W}" y1="{CYS[0] - 24}" x2="{CX - W}" y2="{CYS[-1] + 24}"/>',
       f'<line x1="{RIGHT_X}" y1="{CYS[0] - 24}" x2="{RIGHT_X}" y2="{CYS[-1] + 24}"/>',
       f'<line x1="{CX}" y1="{CYS[0] + H}" x2="{CX}" y2="{CYS[-1] + H + T + 24}"/>',
       '</g>']

# Painter's order: the bottom layer first, so each layer hides what's behind it.
for index in reversed(range(len(LAYERS))):
    key, state, name, what, status = LAYERS[index]
    cy = CYS[index]
    top, right, bottom, left = p(cy, 0, 0), p(cy, 1, 0), p(cy, 1, 1), p(cy, 0, 1)
    down = lambda pt: (pt[0], pt[1] + T)
    out.append(f'<g class="layer layer--{state} layer--{index + 1}">')
    out.append(f'<polygon class="layer__side" points="{pts([left, bottom, down(bottom), down(left)])}"/>')
    out.append(f'<polygon class="layer__side" points="{pts([bottom, right, down(right), down(bottom)])}"/>')
    out.append(f'<polygon class="layer__top" points="{pts([top, right, bottom, left])}"/>')
    out.append('<g class="layer__detail">' + "".join(detail(key, cy)) + '</g>')
    out.append('<g class="callout">')
    out.append(f'<circle cx="{RIGHT_X}" cy="{cy}" r="2.5"/>')
    out.append(f'<line x1="{RIGHT_X + 4}" y1="{cy}" x2="{RIGHT_X + 42}" y2="{cy}"/>')
    tx = RIGHT_X + 50
    out.append(f'<text x="{tx}" y="{cy - 12}" class="callout__name">{name}</text>')
    out.append(f'<text x="{tx}" y="{cy + 8}" class="callout__what">{what}</text>')
    out.append(f'<g transform="translate({tx},{cy + 19})">{MARKERS[state]}</g>')
    out.append(f'<text x="{tx + 14}" y="{cy + 28}" class="callout__status">{status}</text>')
    out.append('</g></g>')

out.append('</svg>')

page = Path(__file__).with_name("index.html")
html = page.read_text()
pattern = re.compile(r"(<!-- figure:start[^>]*-->\n).*?(\n<!-- figure:end -->)", re.DOTALL)
if not pattern.search(html):
    raise SystemExit("index.html has no figure:start and figure:end markers")
page.write_text(pattern.sub(lambda m: m.group(1) + "\n".join(out) + m.group(2), html, count=1))
