"""Gabungkan paket tema jadi app/src/main/assets/themes/<bahasa>.json.
Jalankan: python3 tools/themes/build.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from part1 import THEMES_1
from part2 import THEMES_2
from part3 import THEMES_3

LANGS = ["en", "id", "ms", "es", "pt"]
themes = THEMES_1 + THEMES_2 + THEMES_3
out_dir = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "themes")
os.makedirs(out_dir, exist_ok=True)
ids = set()
problems = []
for tid, emoji, names, items in themes:
    assert tid not in ids, tid
    ids.add(tid)
    if len(items) != 10: problems.append(f"{tid}: {len(items)} items")
    for it in items:
        if len(it) != 10: problems.append(f"{tid}: item size {len(it)} {it[0]}")
        for li, lang in enumerate(LANGS):
            c, p = it[li * 2], it[li * 2 + 1]
            if len(c) > 25: problems.append(f"{tid}/{lang}: caption too long ({len(c)}): {c}")
            if len(p) > 120: problems.append(f"{tid}/{lang}: pose too long: {p}")
    for lang in LANGS:
        if lang not in names: problems.append(f"{tid}: missing name {lang}")
if problems:
    print("\n".join(problems)); sys.exit(1)
for li, lang in enumerate(LANGS):
    data = [{"id": tid, "emoji": emoji, "name": names[lang],
             "items": [{"c": it[li * 2], "p": it[li * 2 + 1]} for it in items]}
            for tid, emoji, names, items in themes]
    with open(os.path.join(out_dir, f"{lang}.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
print(f"{len(themes)} themes x {len(LANGS)} languages written")
