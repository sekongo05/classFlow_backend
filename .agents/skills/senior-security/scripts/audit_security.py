#!/usr/bin/env python3
"""Audit statique Sécurité & Anti-fuite d'erreurs pour ClassFlow."""
import os
import re
import sys

ROOT = "src/main/java/com/mosek/classFlow"
PUBLIC_CONTROLLERS = {"HealthController.java"}
PUBLIC_AUTH_METHODS = {"login", "registerStudent", "refresh"}
EXPLICIT_MESSAGE_HANDLERS = {"handleClassFlow", "handleValidation", "handleConstraintViolation"}

issues = []

# 1. Check SecurityConfig
sec_cfg_path = os.path.join(ROOT, "config", "SecurityConfig.java")
if os.path.exists(sec_cfg_path):
    sec_txt = open(sec_cfg_path).read()
    if "/api/v1/test" in sec_txt:
        issues.append("[SECURITY_CONFIG] Endpoint de test /api/v1/test exposé dans SecurityConfig.java")
    if "SessionCreationPolicy.STATELESS" not in sec_txt:
        issues.append("[SECURITY_CONFIG] La session HTTP doit être STATELESS")

# 2. Check all controllers have @PreAuthorize on class or non-public methods
ctrl_dir = os.path.join(ROOT, "controller")
for fn in sorted(os.listdir(ctrl_dir)):
    if not fn.endswith(".java") or fn in PUBLIC_CONTROLLERS:
        continue
    p = os.path.join(ctrl_dir, fn)
    txt = open(p).read()
    class_header = txt.split("class ")[0]
    if "@PreAuthorize" in class_header:
        continue
    pattern = re.compile(
        r"(@(?:Get|Post|Put|Patch|Delete)Mapping[^\n]*\n(?:\s*@[\w()\"',.\s]+\n)*)\s*public\s+[\w<>?,\s]+\s+(\w+)\s*\(",
        re.M
    )
    for match in pattern.finditer(txt):
        annotations_block, method_name = match.group(1), match.group(2)
        if fn == "AuthController.java" and method_name in PUBLIC_AUTH_METHODS:
            continue
        if "@PreAuthorize" not in annotations_block:
            issues.append(f"[MISSING_PREAUTHORIZE] {fn}#{method_name} n'a pas de @PreAuthorize")

# 3. Check application.yml error shielding
app_yml = open("src/main/resources/application.yml").read()
for prop in ["include-stacktrace: never", "include-message: never", "include-exception: false"]:
    if prop not in app_yml:
        issues.append(f"[ERROR_SHIELD] Propriété manquante dans application.yml : {prop}")

# 4. Check GlobalExceptionHandler never leaks ex.getMessage() in technical handlers
geh_path = os.path.join(ROOT, "exception", "GlobalExceptionHandler.java")
if not os.path.exists(geh_path):
    issues.append("[ERROR_SHIELD] GlobalExceptionHandler.java introuvable")
else:
    geh_txt = open(geh_path).read()
    handler_sig = re.compile(r"public\s+ResponseEntity<ApiErrorResponse>\s+(\w+)\s*\([^)]*\)\s*\{")
    for m in handler_sig.finditer(geh_txt):
        method_name = m.group(1)
        if method_name in EXPLICIT_MESSAGE_HANDLERS:
            continue
        idx = m.end()
        depth = 1
        while idx < len(geh_txt) and depth > 0:
            if geh_txt[idx] == "{":
                depth += 1
            elif geh_txt[idx] == "}":
                depth -= 1
            idx += 1
        body = geh_txt[m.end():idx - 1]
        for ret in re.findall(r"return\s+[^;]+;", body, re.S):
            if any(leak in ret for leak in [".getMessage(", ".getLocalizedMessage(", ".toString("]):
                issues.append(f"[ERROR_LEAK] {method_name} pourrait exposer un message technique au client")

if issues:
    print(f"SECURITY AUDIT: FAILED ({len(issues)} issue(s))")
    for issue in issues:
        print(" -", issue)
    sys.exit(1)

print("SECURITY AUDIT: PASSED (0 vulnerability)")
