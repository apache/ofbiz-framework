#!/bin/bash
# Root shim: runs the monolith build wrapper with the monolith as project dir.
# The pre-push hook and reflexive commands expect the wrapper at the repo root.
cd "$(cd "$(dirname "$0")" && pwd)/monolith" || exit 1
exec ./gradlew "$@"
