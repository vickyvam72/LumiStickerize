"""Bangun res/values*/strings.xml dari tabel teks 5 bahasa.
Jalankan: python3 tools/strings/build.py"""
import os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from part_a import A
from part_b import B
from part_c import C

FOLDERS = ["values", "values-in", "values-ms", "values-es", "values-pt"]  # en, id, ms, es, pt
table = {}
for part in (A, B, C):
    for k, v in part.items():
        assert k not in table, f"duplicate key {k}"
        table[k] = v

root = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main")
problems = []
for k, v in table.items():
    if len(v) != 5: problems.append(f"{k}: {len(v)} languages")
    ph = [sorted(re.findall(r"%\d\$[sd]", x)) for x in v]
    if any(p != ph[0] for p in ph): problems.append(f"{k}: placeholders differ {ph}")

# semua kunci yang dipakai di kode harus ada
used = set()
for dp, _, files in os.walk(os.path.join(root, "java")):
    for f in files:
        if f.endswith(".kt"):
            used |= set(re.findall(r"R\.string\.([a-z0-9_]+)", open(os.path.join(dp, f), encoding="utf-8").read()))
missing = used - set(table)
unused = set(table) - used
if missing: problems.append(f"missing keys: {sorted(missing)}")
if problems:
    print("\n".join(problems)); sys.exit(1)

def esc(s):
    s = s.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if s[:1] in "@?": s = "\\" + s
    return s

for i, folder in enumerate(FOLDERS):
    os.makedirs(os.path.join(root, "res", folder), exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             "<!-- Dibuat otomatis oleh tools/strings/build.py. Ubah tabelnya di sana, bukan file ini. -->",
             "<resources>"]
    for k, v in table.items():
        lines.append(f'    <string name="{k}">{esc(v[i])}</string>')
    lines.append("</resources>")
    open(os.path.join(root, "res", folder, "strings.xml"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
print(f"{len(table)} strings x {len(FOLDERS)} languages; unused: {sorted(unused)}")
