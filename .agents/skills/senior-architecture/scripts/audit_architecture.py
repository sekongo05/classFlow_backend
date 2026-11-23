#!/usr/bin/env python3
"""Audit de la matrice d'architecture en 9 couches et de l'alignement JPA <-> Flyway."""
import os
import re
import sys

ROOT = "src/main/java/com/mosek/classFlow"
FLYWAY_V1 = "src/main/resources/db/migration/V1__init_schema.sql"

ALLOWED_DEPS = {
    "entity": {"entity"},
    "repository": {"repository", "entity"},
    "dto": {"dto", "entity", "exception"},
    "transformer": {"transformer", "entity", "dto"},
    "business": {"business", "repository", "entity", "dto", "transformer", "security", "exception"},
    "controller": {"controller", "business", "dto", "security", "exception"},
    "security": {"security", "exception"},
    "config": {"config", "security", "exception"},
    "exception": {"exception"},
}

violations = []
layer_counts = {k: 0 for k in ALLOWED_DEPS}
jpa_tables = set()

for dp, _, fns in os.walk(ROOT):
    for fn in sorted(fns):
        if not fn.endswith(".java"):
            continue
        full = os.path.join(dp, fn)
        rel = os.path.relpath(full, ROOT)
        parts = rel.split(os.sep)
        if len(parts) == 1:
            continue
        layer = parts[0]
        if layer not in ALLOWED_DEPS:
            violations.append(f"[UNKNOWN_LAYER] Couche inconnue '{layer}' pour {rel}")
            continue
        layer_counts[layer] += 1
        txt = open(full).read()

        # Check @Transactional placement
        if "@Transactional" in txt and layer != "business":
            violations.append(f"[TRANSACTION_BOUNDARY] @Transactional interdit dans '{layer}' ({rel})")

        # Check @Entity placement and extract table name
        if "@Entity" in txt:
            if layer != "entity":
                violations.append(f"[ENTITY_BOUNDARY] @Entity interdit hors de 'entity/' ({rel})")
            m = re.search(r'@Table\s*\(\s*name\s*=\s*"([^"]+)"', txt)
            if m:
                jpa_tables.add(m.group(1).lower())

        # Check dependency matrix
        for imp in re.findall(r"^import\s+com\.mosek\.classFlow\.(\w+)\.(\w+);", txt, re.M):
            target_layer, target_cls = imp[0], imp[1]
            if target_layer not in ALLOWED_DEPS[layer]:
                violations.append(
                    f"[DEP_MATRIX] '{layer}' ({rel}) ne peut pas dépendre de '{target_layer}.{target_cls}'"
                )
            # In dto/, only enums from entity/ are allowed, never @Entity classes
            if layer == "dto" and target_layer == "entity":
                ent_path = os.path.join(ROOT, "entity", f"{target_cls}.java")
                if os.path.exists(ent_path) and "@Entity" in open(ent_path).read():
                    violations.append(
                        f"[DTO_ENTITY_LEAK] Le DTO {rel} référence l'entité JPA {target_cls} (seuls les enums sont autorisés)"
                    )

# Check JPA @Table <-> Flyway V1 alignment
if os.path.exists(FLYWAY_V1):
    sql = open(FLYWAY_V1).read().lower()
    sql_tables = set(re.findall(r"create\s+table\s+(?:if\s+not\s+exists\s+)?(\w+)", sql))
    missing_in_sql = jpa_tables - sql_tables
    for tbl in sorted(missing_in_sql):
        violations.append(f"[FLYWAY_DRIFT] Table JPA '{tbl}' absente de {FLYWAY_V1}")
else:
    violations.append(f"[FLYWAY_MISSING] Fichier {FLYWAY_V1} introuvable")

print("Architecture Layers:", layer_counts)
print(f"JPA Entities aligned with Flyway V1: {len(jpa_tables)}/{len(jpa_tables)} tables")

if violations:
    print(f"ARCHITECTURE AUDIT: FAILED ({len(violations)} violation(s))")
    for v in violations:
        print(" -", v)
    sys.exit(1)

print("ARCHITECTURE AUDIT: PASSED (0 violation)")
