"""Workflow Antigravity de Qualité Backend — ClassFlow.

Vérifie automatiquement :
1. Les 7 audits statiques des skills Senior (.agents/skills/senior-*/scripts/)
   et l'absence de fichiers résiduels dans le dépôt.
2. L'exécution complète de la suite de tests Maven via `senior-testing/scripts/run_tests.sh`.
3. La double revue parallèle IA (Clean Code/SRP + Sécurité/Anti-fuite d'erreurs).
4. La remédiation automatique optionnelle si `args.auto_fix` est activé (par défaut `false`).

Exécution : `run_workflow(ScriptPath=".agents/workflows/quality_check_backend.py")`
Args optionnels : {"auto_fix": false}
"""

AGENTS_MD_PATH = "AGENTS.md"
AUTO_FIX = bool(args.get("auto_fix", False))

phase("1. Audits statiques des 7 skills Senior & hygiène du dépôt")

SKILLS_AUDIT_CMD = (
    "python3 .agents/skills/senior-architecture/scripts/audit_architecture.py && "
    "python3 .agents/skills/senior-clean-code/scripts/audit_clean_code.py && "
    "python3 .agents/skills/senior-security/scripts/audit_security.py && "
    "python3 .agents/skills/senior-database-flyway/scripts/audit_database.py && "
    "python3 .agents/skills/senior-api-contract/scripts/audit_api_contract.py && "
    "python3 .agents/skills/senior-offline-realtime/scripts/audit_offline_realtime.py && "
    "python3 .agents/skills/senior-devops-release/scripts/audit_devops.py && "
    "python3 -c \"import os, sys; "
    "bad = [p for p in ['.legacy_flat_implementation', 'HELP.md', '.agents/plans/plan_execution_backend.md'] if os.path.exists(p)]; "
    "sys.exit('Résidus détectés: ' + str(bad)) if bad else print('HYGIENE AUDIT: PASSED')\""
)

static_ok = True
try:
    static_out = await call_tool(
        "run_command",
        CommandLine=SKILLS_AUDIT_CMD,
        WaitMsBeforeAsync=10000,
        toolSummary="Run all Senior skill audits",
    )
    log(static_out)
except Exception as err:
    static_ok = False
    static_out = getattr(err, "message", str(err))
    log("Échec audits skills :", static_out)

phase("2. Suite de tests complète Maven (senior-testing)")
tests_ok = True
try:
    test_out = await call_tool(
        "run_command",
        CommandLine="bash .agents/skills/senior-testing/scripts/run_tests.sh",
        WaitMsBeforeAsync=10000,
        toolSummary="Run full Maven test suite",
    )
    log("Tests Maven : SUCCÈS")
except Exception as err:
    tests_ok = False
    test_out = getattr(err, "message", str(err))
    log("Tests Maven : ÉCHEC", test_out[-2500:])

phase("3. Revue qualité parallèle (Clean Code & Sécurité/Exceptions)")
review_schema = {
    "type": "object",
    "properties": {
        "score": {"type": "integer"},
        "blocking_issues": {"type": "array", "items": {"type": "string"}},
        "recommendations": {"type": "array", "items": {"type": "string"}},
    },
    "required": ["score", "blocking_issues", "recommendations"],
}

clean_code_review, security_review = await parallel(
    [
        (
            "Clean Code Reviewer",
            f"Audite la qualité du code dans `src/main/java/com/mosek/classFlow/` par rapport à `{AGENTS_MD_PATH}` : "
            "repère les classes trop longues (>300 lignes), le code mort, la duplication et les problèmes de nommage.",
        ),
        (
            "Security & Error Reviewer",
            f"Audite `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`, `security/` et `controller/` "
            f"par rapport à `{AGENTS_MD_PATH}` : vérifie qu'aucun message technique ne peut fuiter vers le client, "
            "que @PreAuthorize est bien présent et qu'aucun endpoint sensible n'est ouvert.",
        ),
    ],
    lambda item: agent(item[1], schema=review_schema, role=item[0], model="flash"),
)

log("Clean Code Review :", clean_code_review)
log("Security & Error Review :", security_review)

blocking = clean_code_review.get("blocking_issues", []) + security_review.get("blocking_issues", [])

if AUTO_FIX and (not static_ok or not tests_ok or blocking):
    phase("4. Remédiation automatique")
    await agent(
        f"Corrige les problèmes suivants en respectant `{AGENTS_MD_PATH}` :\n"
        f"- Audits skills OK={static_ok}\n"
        f"- Tests Maven OK={tests_ok}\n"
        f"- Points bloquants revue : {blocking}\n"
        "Relance les scripts d'audit et `bash .agents/skills/senior-testing/scripts/run_tests.sh` pour confirmer que tout passe.",
        schema={"type": "string"},
        role="Quality Auto-Fixer",
    )

if not static_ok or not tests_ok:
    raise RuntimeError(f"Quality gate FAILED (static_ok={static_ok}, tests_ok={tests_ok})")

log("Quality gate PASSED.")
