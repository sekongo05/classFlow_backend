#!/usr/bin/env bash
set -euo pipefail

JAVA_MAJOR=0
if command -v java >/dev/null 2>&1; then
  JAVA_MAJOR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed -E 's/^1\.//; s/^([0-9]+).*/\1/' || echo 0)
fi

if [ "${JAVA_MAJOR:-0}" -lt 21 ] && [ -d "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

MAVEN_OFFLINE_FLAG="${MAVEN_OFFLINE_FLAG:--o}"
if [ "${CLASSFLOW_MAVEN_ONLINE:-false}" = "true" ]; then
  MAVEN_OFFLINE_FLAG=""
fi

./mvnw ${MAVEN_OFFLINE_FLAG:+$MAVEN_OFFLINE_FLAG} -Daether.syncContext.named.factory=noop test "$@"
