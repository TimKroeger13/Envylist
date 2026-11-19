"""
Round 2: bold, colourful, iconic symbol concepts for Envylist (flat app-store style).
Each concept fuses "task / tick" with "envy / side-eye" into one memorable shape.

    python tools/icon/concepts.py   ->  tools/icon/concepts/sheet.png
"""

import os
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "concepts")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

GREEN = "#22B573"
GREEN_DARK = "#138A55"
LIME = "#9BE15D"
YELLOW = "#FFC93C"
PURPLE = "#5B3FD9"
NAVY = "#18233A"
CREAM = "#FFF6E3"
WHITE = "#FFFFFF"
INK = "#13202B"
CORAL = "#FF6B5A"


def eye(cx, cy, w, h, look=1.0, lid=0.42, white=WHITE, pupil=INK, lid_color=None):
    """Flat side-eye: almond, heavy straight lid, pupil pushed to the side."""
    top, bottom = cy - h / 2, cy + h / 2
    almond = (f"M{cx - w / 2},{cy} Q{cx},{top - h * 0.45} {cx + w / 2},{cy} "
              f"Q{cx},{bottom + h * 0.45} {cx - w / 2},{cy}Z")
    lid_y = top + h * lid
    clip = f"c{abs(hash((cx, cy, w))) % 99999}"
    px = cx + look * w * 0.2
    return f"""
    <clipPath id="{clip}"><path d="{almond}"/></clipPath>
    <path d="{almond}" fill="{white}"/>
    <g clip-path="url(#{clip})">
      <circle cx="{px}" cy="{cy + h * 0.12}" r="{h * 0.36}" fill="{pupil}"/>
      <rect x="{cx - w}" y="{top - h}" width="{2 * w}" height="{lid_y - top + h}" fill="{lid_color or 'none'}"/>
    </g>
    <path d="M{cx - w / 2 - h * 0.05},{lid_y} L{cx + w / 2 + h * 0.05},{lid_y}" stroke="{pupil}" stroke-width="{h * 0.16}" stroke-linecap="round"/>"""


def tile(bg, body, uid):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="100%" height="100%">
  <defs>
    <linearGradient id="{uid}-bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{bg[0]}"/><stop offset="1" stop-color="{bg[1]}"/>
    </linearGradient>
    <filter id="{uid}-soft" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="2.2" stdDeviation="2" flood-color="#000" flood-opacity="0.22"/>
    </filter>
  </defs>
  <rect width="108" height="108" fill="url(#{uid}-bg)"/>
  <g filter="url(#{uid}-soft)">{body}</g>
</svg>"""


# ------------------------------------------------------------------ concepts

def c_check_eye(uid):
    """A: the tick IS the lower lid of a suspicious eye."""
    body = eye(54, 50, 44, 26, 1, 0.42, lid_color=GREEN)
    body += f'<path d="M33,64 L46,76 L77,49" fill="none" stroke="{YELLOW}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>'
    return tile((GREEN, GREEN_DARK), body, uid)


def c_peek(uid):
    """B: two big eyes peek jealously over the edge of a task card."""
    body = eye(45, 47, 18, 14, 1, 0.38) + eye(63, 47, 18, 14, 1, 0.38)
    body += f'<rect x="29" y="55" width="50" height="27" rx="8" fill="{YELLOW}"/>'
    body += f'<path d="M37,68 L42,73 L51,63" fill="none" stroke="{INK}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>'
    body += f'<rect x="56" y="66" width="16" height="5" rx="2.5" fill="{INK}" opacity="0.35"/>'
    return tile((PURPLE, "#3E24B8"), body, uid)


def c_blob(uid):
    """C: a green envy blob with a deadpan side-eye."""
    body = f'<path d="M54,22 C78,22 86,40 85,58 C84,78 70,88 54,88 C36,88 23,78 23,57 C23,38 32,22 54,22 Z" fill="{LIME}"/>'
    body += eye(43, 52, 17, 13, 1, 0.45, lid_color=LIME) + eye(65, 52, 17, 13, 1, 0.45, lid_color=LIME)
    body += f'<path d="M47,70 Q54,67 61,70" fill="none" stroke="{INK}" stroke-width="3.5" stroke-linecap="round"/>'
    return tile((NAVY, "#0E1626"), body, uid)


def c_e_list(uid):
    """D: a bold E made of three list bars; the middle bar is a side-eye."""
    body = f'<rect x="28" y="26" width="13" height="56" rx="6.5" fill="{WHITE}"/>'
    body += f'<rect x="28" y="26" width="52" height="13" rx="6.5" fill="{WHITE}"/>'
    body += f'<rect x="28" y="69" width="52" height="13" rx="6.5" fill="{WHITE}"/>'
    body += eye(60, 54, 30, 15, 1, 0.42, white=YELLOW)
    return tile((GREEN, GREEN_DARK), body, uid)


def c_box_eye(uid):
    """E: a checkbox that glares sideways at its own tick."""
    body = f'<rect x="30" y="38" width="38" height="38" rx="10" fill="{WHITE}"/>'
    body += eye(49, 57, 24, 16, 1, 0.4, lid_color=WHITE)
    body += f'<path d="M62,42 L68,48 L80,32" fill="none" stroke="{YELLOW}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>'
    return tile((CORAL, "#E2483A"), body, uid)


def c_eyes_only(uid):
    """F: just the side-eye, as a pure, bold mark (works at any size)."""
    body = eye(38, 54, 30, 24, 1, 0.42, white=WHITE, lid_color=GREEN) + eye(70, 54, 30, 24, 1, 0.42, white=WHITE, lid_color=GREEN)
    return tile((GREEN, GREEN_DARK), body, uid)


def c_list_jealous(uid):
    """G: three tasks; two are ticked, the third one stares at them."""
    body = ""
    for i, y in enumerate((30, 50)):
        body += f'<rect x="22" y="{y}" width="64" height="14" rx="7" fill="{WHITE}" opacity="0.95"/>'
        body += f'<path d="M28,{y + 7} L32,{y + 11} L39,{y + 3}" fill="none" stroke="{GREEN}" stroke-width="3.6" stroke-linecap="round" stroke-linejoin="round"/>'
        body += f'<rect x="44" y="{y + 5}" width="34" height="4" rx="2" fill="{GREEN}" opacity="0.35"/>'
    body += f'<rect x="22" y="70" width="64" height="18" rx="9" fill="{YELLOW}"/>'
    body += eye(42, 79, 13, 9, -1, 0.4, white=WHITE, lid_color=YELLOW) + eye(58, 79, 13, 9, -1, 0.4, white=WHITE, lid_color=YELLOW)
    return tile((PURPLE, "#3E24B8"), body, uid)


def c_bubble(uid):
    """H: a speech bubble that side-eyes – the task that complains."""
    body = f'<path d="M30,26 H78 Q88,26 88,36 V64 Q88,74 78,74 H50 L36,86 L38,74 H30 Q20,74 20,64 V36 Q20,26 30,26 Z" fill="{WHITE}"/>'
    body += eye(42, 50, 18, 14, 1, 0.42, white=GREEN, pupil=INK, lid_color=WHITE)
    body += eye(66, 50, 18, 14, 1, 0.42, white=GREEN, pupil=INK, lid_color=WHITE)
    return tile((YELLOW, "#F3A712"), body, uid)


CONCEPTS = [
    ("A Haken-Auge", c_check_eye),
    ("B Augen über der Karte", c_peek),
    ("C Neid-Blob", c_blob),
    ("D E aus Listen + Auge", c_e_list),
    ("E Checkbox schielt", c_box_eye),
    ("F Nur der Seitenblick", c_eyes_only),
    ("G Liste mit Neider", c_list_jealous),
    ("H Meckernde Blase", c_bubble),
]


def main():
    os.makedirs(OUT, exist_ok=True)
    cells = ""
    for i, (title, fn) in enumerate(CONCEPTS):
        doc = fn(f"k{i}")
        with open(os.path.join(OUT, f"concept_{title[0]}.svg"), "w", encoding="utf-8") as fh:
            fh.write(doc)
        cells += (f'<div class="cell"><div class="big">{doc}</div><div class="row">'
                  f'<div class="small">{doc}</div><div class="small round">{doc}</div><span>{title}</span></div></div>')
    html = f"""<!doctype html><html><head><meta charset="utf-8"><style>
      body {{ margin:0; background:#ececec; font-family: Segoe UI, sans-serif; color:#222; }}
      .grid {{ display:grid; grid-template-columns: repeat(4, 280px); gap:24px; padding:24px; }}
      .big {{ width:280px; height:280px; border-radius:62px; overflow:hidden; }}
      .big svg {{ transform: scale(1.5); transform-origin: center; }}
      .row {{ display:flex; align-items:center; gap:10px; margin-top:8px; }}
      .small {{ width:48px; height:48px; border-radius:11px; overflow:hidden; flex:none; }}
      .small svg {{ transform: scale(1.5); transform-origin: center; }}
      .round {{ border-radius:50%; }}
      span {{ font-size:16px; }}
    </style></head><body><div class="grid">{cells}</div></body></html>"""
    path = os.path.join(OUT, "sheet.html")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(html)
    png = os.path.join(OUT, "sheet.png")
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={png}",
                    "--window-size=1240,780", "file:///" + path.replace("\\", "/")], check=True, capture_output=True)
    print("written", png)


if __name__ == "__main__":
    main()
