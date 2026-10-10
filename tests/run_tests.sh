#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."

# 1. Recompile any files modified by the candidate
mvn -q test-compile 1>&2

# 2. Run the tests using maven test quietly
exec mvn test -B -q
