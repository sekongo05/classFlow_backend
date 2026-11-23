#!/usr/bin/env python3
"""Audit statique Senior Clean Code pour ClassFlow (SRP, hygiène, code mort, constantes)."""
import os
import re
import sys

ROOT = "src/main/java/com/mosek/classFlow"
MAX_LINES = 300
ROLE_LITERALS = ['"ADMIN".equals', '"TEACHER".equals', '"STUDENT".equals', '"DELEGATE".equals', '"DEPUTY_DELEGATE".equals']

files = [os.path.join(dp, fn) for dp, _, fns in os.walk(ROOT) for fn in fns if fn.endswith(".java")]
texts = {p: open(p).read() for p in files}
all_src = "\n".join(texts.values())
issues = []

for p, txt in sorted(texts.items()):
    rel = os.path.relpath(p, ROOT)
    parts = rel.split(os.sep)
    layer = parts[0] if len(parts) > 1 else "(root)"
    lines = txt.splitlines()

    if len(lines) > MAX_LINES:
        issues.append(f"[SRP] Classe trop longue ({len(lines)} > {MAX_LINES} lignes) : {rel}")

    for i, line in enumerate(lines, 1):
        if line.startswith("import ") and ".*" in line:
            issues.append(f"[HYGIENE] Wildcard import interdit : {rel}:{i}")
        elif line.startswith("import ") and not line.startswith("import static "):
            cls = line.strip()[:-1].split(".")[-1]
            body = "\n".join(l for l in lines if not l.startswith("import "))
            if not re.search(r"\b" + re.escape(cls) + r"\b", body):
                issues.append(f"[HYGIENE] Import inutilisé ({cls}) : {rel}:{i}")

    if layer == "dto":
        for rec in re.findall(r"public\s+record\s+(\w+)", txt):
            if len(re.findall(r"\b" + re.escape(rec) + r"\b", all_src)) <= 1:
                issues.append(f"[DEAD_CODE] DTO record non utilisé : {rel} -> {rec}")

    if any(m in txt for m in ROLE_LITERALS):
        issues.append(f"[MAGIC_STRING] Comparaison littérale de rôle détectée : {rel}")

if issues:
    print(f"CLEAN CODE AUDIT: FAILED ({len(issues)} issue(s))")
    for issue in issues:
        print(" -", issue)
    sys.exit(1)

print(f"CLEAN CODE AUDIT: PASSED (0 issue across {len(files)} files)")
