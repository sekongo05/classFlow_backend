#!/usr/bin/env python3
"""Audit des invariants Temps Réel (WebSocket), Mode Secours et Synchronisation Hors Ligne."""
import sys

att_txt = open("src/main/java/com/mosek/classFlow/business/AttendanceService.java").read()
sync_txt = open("src/main/java/com/mosek/classFlow/business/SyncService.java").read()
ws_txt = open("src/main/java/com/mosek/classFlow/config/WebSocketConfig.java").read()

issues = []

if "/topic/sessions/" not in att_txt or "convertAndSend" not in att_txt:
    issues.append("[WEBSOCKET] Diffusion /topic/sessions/{id}/attendance absente de AttendanceService")

if 'enableSimpleBroker("/topic")' not in ws_txt:
    issues.append("[WEBSOCKET] Broker STOMP /topic absent de WebSocketConfig")

if "getSheetVersion() + 1" not in att_txt:
    issues.append("[SHEET_VERSIONING] Incrémentation de sheetVersion après correction absente de AttendanceService")

if "AttendanceMode.EMERGENCY" not in att_txt:
    issues.append("[EMERGENCY_MODE] Marquage AttendanceMode.EMERGENCY absent de AttendanceService")

if "findByClientOperationId" not in sync_txt or "DUPLICATE_IGNORED" not in sync_txt:
    issues.append("[SYNC_IDEMPOTENCY] Vérification d'idempotence clientOperationId absente de SyncService")

if issues:
    print(f"OFFLINE & REALTIME AUDIT: FAILED ({len(issues)} issue(s))")
    for i in issues:
        print(" -", i)
    sys.exit(1)

print("OFFLINE & REALTIME AUDIT: PASSED (0 issue)")
