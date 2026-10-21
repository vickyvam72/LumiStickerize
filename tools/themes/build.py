"""Ubah tools/themes/tema.py jadi app/src/main/assets/tema.json.
Jalankan: python3 tools/themes/build.py"""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from tema import TEMA

problems = []
ids = set()
for tid, emoji, name, items in TEMA:
    if tid in ids: problems.append(f"id dobel: {tid}")
    ids.add(tid)
    if len(items) < 10: problems.append(f"{tid}: hanya {len(items)} kata (minimal 10)")
    caps = [c.lower() for c, _ in items]
    for c in set(caps):
        if caps.count(c) > 1: problems.append(f"{tid}: kata dobel '{c}'")
    for c, p in items:
        if len(c) > 25: problems.append(f"{tid}: kata terlalu panjang ({len(c)}): {c}")
        if len(p) > 120: problems.append(f"{tid}: pose terlalu panjang: {p}")
if problems:
    print("\n".join(problems)); sys.exit(1)

out = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "tema.json")
data = [{"id": tid, "emoji": emoji, "name": name, "items": [{"c": c, "p": p} for c, p in items]}
        for tid, emoji, name, items in TEMA]
with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
print(f"{len(TEMA)} tema, {sum(len(t[3]) for t in TEMA)} kata & pose")
