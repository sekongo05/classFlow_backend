#!/usr/bin/env python3
"""Audit DevOps & Production Readiness pour ClassFlow."""
import re
import sys

prod_props = open("src/main/resources/application-prod.properties").read()
pom_xml = open("pom.xml").read()
sec_cfg = open("src/main/java/com/mosek/classFlow/config/SecurityConfig.java").read()

issues = []

# 1. Check application-prod.properties has no hardcoded default credentials
for var in ["${SPRING_DATASOURCE_URL}", "${SPRING_DATASOURCE_USERNAME}", "${SPRING_DATASOURCE_PASSWORD}"]:
    if var not in prod_props:
        issues.append(f"[PROD_ENV] {var} sans valeur par défaut en dur est requis dans application-prod.properties")

# 2. Check Java 21 target in pom.xml
if "<java.version>21</java.version>" not in pom_xml:
    issues.append("[POM_JAVA] <java.version>21</java.version> requis dans pom.xml")

# 3. Check healthcheck endpoint is permitted
if '"/api/v1/health"' not in sec_cfg:
    issues.append("[HEALTHCHECK] /api/v1/health doit être autorisé dans SecurityConfig.java")

if issues:
    print(f"DEVOPS & RELEASE AUDIT: FAILED ({len(issues)} issue(s))")
    for i in issues:
        print(" -", i)
    sys.exit(1)

print("DEVOPS & RELEASE AUDIT: PASSED (0 issue)")
