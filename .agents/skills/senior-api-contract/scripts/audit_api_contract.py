#!/usr/bin/env python3
"""Audit du contrat API REST : messages de validation en français et synchro Fiche Frontend."""
import os
import re
import sys

DTO_DIR = "src/main/java/com/mosek/classFlow/dto"
CTRL_DIR = "src/main/java/com/mosek/classFlow/controller"
FRONTEND_MD = "docs/Fiche_Integration_Frontend_ClassFlow.md"

CONSTRAINTS = [
    "NotBlank", "NotNull", "NotEmpty", "Email",
    "Size", "Min", "Max", "Positive", "PositiveOrZero"
]

issues = []

# 1. Verify all Bean Validation annotations in dto/ have an explicit French message
for fn in sorted(os.listdir(DTO_DIR)):
    if not fn.endswith(".java"):
        continue
    p = os.path.join(DTO_DIR, fn)
    lines = open(p).read().splitlines()
    for idx, line in enumerate(lines, 1):
        for c in CONSTRAINTS:
            for m in re.finditer(r"@" + c + r"\b(\s*\([^)]*\))?", line):
                args = m.group(1) or ""
                if "message" not in args:
                    issues.append(f"[MISSING_VALIDATION_MSG] {fn}:{idx} @{c} sans attribut message=\"...\"")

# 2. Verify all base @RequestMapping paths appear in Fiche_Integration_Frontend_ClassFlow.md
if os.path.exists(FRONTEND_MD):
    doc = open(FRONTEND_MD).read()
    for fn in sorted(os.listdir(CTRL_DIR)):
        if not fn.endswith(".java"):
            continue
        txt = open(os.path.join(CTRL_DIR, fn)).read()
        m = re.search(r'@RequestMapping\("([^"]+)"\)', txt)
        if m:
            base_path = m.group(1)
            if base_path not in doc:
                issues.append(f"[DOC_DRIFT] Le chemin '{base_path}' ({fn}) est absent de {FRONTEND_MD}")
else:
    issues.append(f"[DOC_MISSING] {FRONTEND_MD} est introuvable")

if issues:
    print(f"API CONTRACT AUDIT: FAILED ({len(issues)} issue(s))")
    for i in issues:
        print(" -", i)
    sys.exit(1)

print("API CONTRACT AUDIT: PASSED (0 issue)")
