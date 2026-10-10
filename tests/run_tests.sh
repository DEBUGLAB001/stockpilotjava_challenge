#!/usr/bin/env bash
cd "$(dirname "$0")/.."

# 1. Recompile any files modified by the candidate quietly
mvn -q test-compile > /dev/null 2>&1

# 2. Run the tests quietly and extract ONLY the strict JSON block
OUTPUT=$(mvn test -B -q 2>&1 || true)

# Extract only the JSON block (from the first '{' of the report to the matching '}')
echo "$OUTPUT" | awk '/^\{/{flag=1} flag{print} /^\}/{if(flag){flag=0}}' | head -n 35

# 3. Determine pass/fail from JSON (if "Failed": 0, exit 0, else exit 1)
if echo "$OUTPUT" | grep -q '"Failed": 0'; then
    exit 0
else
    exit 1
fi
