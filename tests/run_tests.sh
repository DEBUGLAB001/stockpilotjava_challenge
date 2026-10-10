#!/usr/bin/env bash
cd "$(dirname "$0")/.."

# 1. Recompile any files modified by the candidate quietly
mvn -q test-compile > /dev/null 2>&1

# 2. Run the tests using maven test quietly, filtering JVM tool options & warnings
mvn test -B -q 2>&1 | grep -v "Picked up JAVA_TOOL_OPTIONS" | grep -v "OpenJDK 64-Bit Server VM warning" | grep -v "Java HotSpot" || true
exit_code=${PIPESTATUS[0]}

# Pass through non-zero exit code if tests failed
if [ -n "$exit_code" ]; then
    exit "$exit_code"
fi

