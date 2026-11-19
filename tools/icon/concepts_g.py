"""
Round 3: variations of concept G "Liste mit Neider" - two finished tasks, one unfinished task
glaring at them. G0 is the original; everything else varies colour, layout, gaze and attitude.

    python tools/icon/concepts_g.py   ->  tools/icon/concepts/sheet_g.png
"""

import os
import subprocess

from concepts import CHROME, OUT, tile

GREEN, GREEN_DARK, LIME = "#22B573", "#138A55", "#9BE15D"
YELLOW, PURPLE, PURPLE_DARK = "#FFC93C", "#5B3FD9", "#3E24B8"
NAVY, NAVY_DARK, WHITE, INK = "#18233A", "#0E1626", "#FFFFFF", "#13202B"
CORAL, CORAL_DARK, TEAL, TEAL_DARK = "#FF6B5A", "#E2483A", "#1BA6A6", "#0E7C80"
CREAM, CREAM_DARK = "#FFF6E3", "#F4E3BE"

_n = [0]


def eye(cx, cy, w, h, look=(1, 0), lid=0.42, white=WHITE, pupil=INK, lid_color=None, brow=0.0):
    """Side-eye with a heavy flat lid; look=(x, y) pushes the pupil, brow>0 frowns."""
    _n[0] += 1
    top, bottom = cy - h / 2, cy + h / 2
    almond = (f"M{cx - w / 2},{cy} Q{cx},{top - h * 0.45} {cx + w / 2},{cy} "
              f"Q{cx},{bottom + h * 0.45} {cx - w / 2},{cy}Z")
    lid_y = top + h * lid
    px, py = cx + look[0] * w * 0.2, cy + h * 0.12 + look[1] * h * 0.18
    s = f'<clipPath id="e{_n[0]}"><path d="{almond}"/></clipPath><path d="{almond}" fill="{white}"/>'
    s += f'<g clip-path="url(#e{_n[0]})"><circle cx="{px}" cy="{py}" r="{h * 0.36}" fill="{pupil}"/>'
    if lid_color:
        s += f'<rect x="{cx - w}" y="{top - h}" width="{2 * w}" height="{lid_y - top + h}" fill="{lid_color}"/>'
    s += "</g>"
    if brow:
        # inner end low, outer end high -> grumpy frown
        base = lid_y - h * 0.18
        left_y, right_y = (base - brow, base + brow * 0.35) if cx < 54 else (base + brow * 0.35, base - brow)
        s += (f'<path d="M{cx - w * 0.6},{left_y} L{cx + w * 0.6},{right_y}" '
              f'stroke="{pupil}" stroke-width="{h * 0.17}" stroke-linecap="round" fill="none"/>')
        s += f'<path d="M{cx - w / 2 - h * 0.05},{lid_y} L{cx + w / 2 + h * 0.05},{lid_y}" stroke="{pupil}" stroke-width="{h * 0.12}" stroke-linecap="round"/>'
    else:
        s += f'<path d="M{cx - w / 2 - h * 0.05},{lid_y} L{cx + w / 2 + h * 0.05},{lid_y}" stroke="{pupil}" stroke-width="{h * 0.16}" stroke-linecap="round"/>'
    return s


def done_row(y, h, bar, check, line, x0=22, x1=86, strike=False, opacity=0.95):
    s = f'<rect x="{x0}" y="{y}" width="{x1 - x0}" height="{h}" rx="{h / 2}" fill="{bar}" opacity="{opacity}"/>'
    cy = y + h / 2
    s += (f'<path d="M{x0 + 6},{cy} L{x0 + 10},{cy + 4} L{x0 + 17},{cy - 4}" fill="none" stroke="{check}" '
          f'stroke-width="{h * 0.26}" stroke-linecap="round" stroke-linejoin="round"/>')
    s += f'<rect x="{x0 + 22}" y="{cy - 2}" width="{(x1 - x0) - 34}" height="4" rx="2" fill="{line}" opacity="0.4"/>'
    if strike:
        s += f'<path d="M{x0 + 22},{cy} L{x1 - 12},{cy}" stroke="{line}" stroke-width="2.4" stroke-linecap="round"/>'
    return s


def jealous_row(y, h, color, look=(-1, 0), x0=22, x1=86, eye_w=13, eye_h=9, brow=0.0, white=WHITE, rotate=0):
    cx = (x0 + x1) / 2
    s = f'<g transform="rotate({rotate} {cx} {y + h / 2})">'
    s += f'<rect x="{x0}" y="{y}" width="{x1 - x0}" height="{h}" rx="{h / 2}" fill="{color}"/>'
    s += eye(cx - eye_w * 0.62, y + h / 2, eye_w, eye_h, look, 0.4, white, lid_color=color, brow=brow)
    s += eye(cx + eye_w * 0.62, y + h / 2, eye_w, eye_h, look, 0.4, white, lid_color=color, brow=brow)
    return s + "</g>"


def variant(bg, bar=WHITE, check=GREEN, line=GREEN, jealous=YELLOW, rows=(30, 50), row_h=14,
            jy=70, jh=18, look=(-1, 0), eye_w=13, eye_h=9, brow=0.0, strike=False, done_opacity=0.95,
            jx=None, rotate=0, jealous_first=False, bx=(22, 86)):
    jx = jx or bx
    def build(uid):
        body = ""
        done = "".join(done_row(y, row_h, bar, check, line, bx[0], bx[1], strike=strike, opacity=done_opacity) for y in rows)
        mine = jealous_row(jy, jh, jealous, look, jx[0], jx[1], eye_w, eye_h, brow, rotate=rotate)
        body = (mine + done) if jealous_first else (done + mine)
        return tile(bg, body, uid)
    return build


VARIANTS = [
    ("G0 Original", variant((PURPLE, PURPLE_DARK))),
    ("G1 Grün", variant((GREEN, GREEN_DARK), check=GREEN, line=GREEN, jealous=YELLOW)),
    ("G2 Nachtblau", variant((NAVY, NAVY_DARK), check=GREEN, line=GREEN, jealous=LIME)),
    ("G3 Koralle", variant((CORAL, CORAL_DARK), check=CORAL, line=CORAL, jealous=YELLOW)),
    ("G4 Petrol", variant((TEAL, TEAL_DARK), check=TEAL, line=TEAL, jealous=YELLOW)),
    ("G5 Hell", variant((CREAM, CREAM_DARK), bar=WHITE, check=GREEN, line=INK, jealous=PURPLE)),
    ("G6 Blick nach oben", variant((PURPLE, PURPLE_DARK), look=(-0.6, -1))),
    ("G7 Wütende Brauen", variant((PURPLE, PURPLE_DARK), brow=3.0, eye_w=14, eye_h=10)),
    ("G8 Zwei Zeilen, große Augen", variant((PURPLE, PURPLE_DARK), rows=(32,), row_h=16, jy=56, jh=26,
                                            eye_w=19, eye_h=14)),
    ("G9 Neider oben", variant((PURPLE, PURPLE_DARK), rows=(54, 74), jy=28, jh=20, look=(-0.5, 1),
                              jealous_first=True)),
    ("G10 Durchgestrichen", variant((PURPLE, PURPLE_DARK), strike=True, done_opacity=0.75)),
    ("G11 Ragt heraus", variant((PURPLE, PURPLE_DARK), jx=(22, 94), rotate=-6)),
    ("G12 Grün + Brauen", variant((GREEN, GREEN_DARK), check=GREEN, line=GREEN, jealous=YELLOW, brow=3.0,
                                  eye_w=14, eye_h=10, look=(-0.6, -1))),
    ("G13 Nachtblau, 2 Zeilen", variant((NAVY, NAVY_DARK), check=GREEN, line=GREEN, jealous=LIME, rows=(32,),
                                        row_h=16, jy=56, jh=26, eye_w=19, eye_h=14, brow=3.5)),
    ("G14 Hell + Grün", variant((CREAM, CREAM_DARK), bar=WHITE, check=GREEN, line=INK, jealous=GREEN,
                                look=(-0.6, -1))),
    ("G15 Koralle, ragt heraus", variant((CORAL, CORAL_DARK), check=CORAL, line=CORAL, jealous=YELLOW,
                                         jx=(22, 94), rotate=-6, brow=3.0)),
]


def main():
    os.makedirs(OUT, exist_ok=True)
    cells = ""
    for i, (title, fn) in enumerate(VARIANTS):
        doc = fn(f"g{i}")
        cells += (f'<div class="cell"><div class="big">{doc}</div><div class="row">'
                  f'<div class="small">{doc}</div><div class="small round">{doc}</div><span>{title}</span></div></div>')
    html = f"""<!doctype html><html><head><meta charset="utf-8"><style>
      body {{ margin:0; background:#ececec; font-family: Segoe UI, sans-serif; color:#222; }}
      .grid {{ display:grid; grid-template-columns: repeat(4, 240px); gap:22px; padding:22px; }}
      .big {{ width:240px; height:240px; border-radius:53px; overflow:hidden; }}
      .big svg, .small svg {{ transform: scale(1.5); transform-origin: center; }}
      .row {{ display:flex; align-items:center; gap:8px; margin-top:6px; }}
      .small {{ width:48px; height:48px; border-radius:11px; overflow:hidden; flex:none; }}
      .round {{ border-radius:50%; }}
      span {{ font-size:14px; }}
    </style></head><body><div class="grid">{cells}</div></body></html>"""
    path = os.path.join(OUT, "sheet_g.html")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(html)
    png = os.path.join(OUT, "sheet_g.png")
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={png}",
                    "--window-size=1080,1340", "file:///" + path.replace("\\", "/")], check=True, capture_output=True)
    print("written", png)


if __name__ == "__main__":
    main()
