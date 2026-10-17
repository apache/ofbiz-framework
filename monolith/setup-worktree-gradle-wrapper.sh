#!/usr/bin/env sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Syntax: ./setup-worktree-gradle-wrapper.sh
#
# gradle/wrapper/gradle-wrapper.jar is listed in .gitignore -- it's not
# committed, by design (see gradle/init-gradle-wrapper.sh), to keep a binary
# out of version control. Every fresh checkout downloads and SHA-256-verifies
# it once. But `git worktree add` only checks out tracked files, so a freshly
# created linked worktree gets no gradle-wrapper.jar at all, and `./gradlew`
# fails immediately with "Unable to access jarfile .../gradle-wrapper.jar"
# until someone runs gradle/init-gradle-wrapper.sh (or copies the jar over
# by hand).
#
# Run this script from inside a linked worktree of this repo to fix that. If
# the main checkout already has a verified gradle-wrapper.jar for the same
# Gradle version, it's copied over directly (no network needed). Otherwise
# this falls back to gradle/init-gradle-wrapper.sh, which downloads and
# verifies the jar from Gradle's distribution service.

set -e

OFBIZ_HOME="$(pwd)"
WRAPPER_DIR="$OFBIZ_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
    echo "ERROR: $WRAPPER_PROPERTIES not found -- run this from the Apache OFBiz home." >&2
    exit 1
fi

if [ -r "$WRAPPER_JAR" ]; then
    echo "gradle-wrapper.jar already present at $WRAPPER_JAR -- nothing to do."
    exit 0
fi

git_common_dir=$(git rev-parse --git-common-dir 2>/dev/null) || {
    echo "ERROR: not inside a git repository." >&2
    exit 1
}
main_worktree=$(cd "$(dirname "$git_common_dir")" && pwd)
main_jar="$main_worktree/gradle/wrapper/gradle-wrapper.jar"
main_properties="$main_worktree/gradle/wrapper/gradle-wrapper.properties"

if [ -r "$main_jar" ] && [ -r "$main_properties" ] && cmp -s "$main_properties" "$WRAPPER_PROPERTIES"; then
    echo "Copying gradle-wrapper.jar from the main checkout ($main_worktree) -- same Gradle version, no download needed."
    cp "$main_jar" "$WRAPPER_JAR"
    echo "Done."
    exit 0
fi

echo "No matching gradle-wrapper.jar found in the main checkout; downloading and verifying instead."
exec "$OFBIZ_HOME/gradle/init-gradle-wrapper.sh" "$@"
