#!/usr/bin/env python3
"""Audit statique JPA, Hibernate et Flyway SQL pour ClassFlow."""
import os
import re
import sys

ENTITY_DIR = "src/main/java/com/mosek/classFlow/entity"
FLYWAY_V1 = "src/main/resources/db/migration/V1__init_schema.sql"
issues = []

# 1. Check all @Enumerated in entity/ use EnumType.STRING
for fn in sorted(os.listdir(ENTITY_DIR)):
    if not fn.endswith(".java"):
        continue
    p = os.path.join(ENTITY_DIR, fn)
    txt = open(p).read()
    for match in re.finditer(r"@Enumerated(\s*\([^)]*\))?", txt):
        arg = match.group(1) or ""
        if "EnumType.STRING" not in arg:
            issues.append(f"[ENUM_ORDINAL] {fn} utilise @Enumerated sans EnumType.STRING")

# 2. Check open-in-view=false and ddl-auto=validate
app_props = open("src/main/resources/application.properties").read()
if "spring.jpa.open-in-view=false" not in app_props:
    issues.append("[JPA_CONFIG] spring.jpa.open-in-view=false manquant dans application.properties")

for env in ["application-dev.properties", "application-prod.properties"]:
    env_txt = open(f"src/main/resources/{env}").read()
    if "spring.jpa.hibernate.ddl-auto=validate" not in env_txt:
        issues.append(f"[DDL_AUTO] spring.jpa.hibernate.ddl-auto=validate manquant dans {env}")
    if "spring.flyway.enabled=true" not in env_txt:
        issues.append(f"[FLYWAY_DISABLED] spring.flyway.enabled=true manquant dans {env}")

# 3. Check Flyway V1 indexes and PRIMARY KEYs
sql = open(FLYWAY_V1).read()
for block in re.findall(r"CREATE\s+TABLE\s+(\w+)\s*\((.*?)\);", sql, re.S | re.I):
    tbl, body = block[0], block[1]
    if "primary key" not in body.lower():
        issues.append(f"[MISSING_PK] Table '{tbl}' sans PRIMARY KEY dans V1__init_schema.sql")

indexes = re.findall(r"CREATE\s+INDEX\s+(\w+)", sql, re.I)
if len(indexes) < 5:
    issues.append(f"[MISSING_INDEXES] Trop peu d'index SQL définis ({len(indexes)}) dans V1__init_schema.sql")

if issues:
    print(f"DATABASE & FLYWAY AUDIT: FAILED ({len(issues)} issue(s))")
    for i in issues:
        print(" -", i)
    sys.exit(1)

print(f"DATABASE & FLYWAY AUDIT: PASSED (0 issue, {len(indexes)} SQL indexes verified)")
