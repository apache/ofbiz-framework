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

# Syntax: ./setup-worktree-plugins.sh [plugins-branch]
#
# plugins/ is listed in .gitignore and is itself an independent git
# repository (see pullPluginSource.sh / pullAllPluginsSource.sh). Because
# it's gitignored, `git worktree add` for this repo does not populate it --
# a freshly created linked worktree gets no plugins/ directory at all, which
# later fails the build (missing-plugins-dir builds silently skip all plugin
# subprojects; a wrong-branch plugins/ instead fails with a misleading
# Groovy-version dependency conflict).
#
# Run this script from inside a linked worktree of *this* repo (i.e. after
# `git worktree add ../some-path -b some-branch`) to give that worktree a
# plugins/ checkout on a matching branch, without re-cloning: it adds a
# linked worktree of the *existing* plugins/ git repository found via the
# main checkout.
#
# The matching plugins/ branch is found by name first, then (for a
# feature/ticket branch with no plugins/ branch of the same name) by
# walking this repo's actual branch ancestry to find the branch it was cut
# from -- no assumption is made about branch-naming conventions.

set -e

if [ -e "plugins" ]; then
    echo "ERROR: plugins/ already exists here ($(pwd)/plugins) -- refusing to overwrite it. Remove it first if you want this script to (re)create it." >&2
    exit 1
fi

git_common_dir=$(git rev-parse --git-common-dir 2>/dev/null) || {
    echo "ERROR: not inside a git repository." >&2
    exit 1
}
main_worktree=$(cd "$(dirname "$git_common_dir")" && pwd)
plugins_repo="$main_worktree/plugins"

if [ ! -e "$plugins_repo/.git" ]; then
    echo "ERROR: no plugins/ git repository found at $plugins_repo -- expected the main ofbiz checkout to have a working plugins/ git checkout to branch off of." >&2
    exit 1
fi

branch_exists() {
    git -C "$plugins_repo" show-ref --verify --quiet "refs/heads/$1" ||
        git -C "$plugins_repo" show-ref --verify --quiet "refs/remotes/origin/$1"
}

ofbiz_branch=$(git branch --show-current)
plugins_branch=""

if [ -n "$1" ]; then
    plugins_branch=$1
    if ! branch_exists "$plugins_branch"; then
        echo "ERROR: plugins branch '$plugins_branch' not found (checked refs/heads and refs/remotes/origin in $plugins_repo)." >&2
        exit 1
    fi
elif [ -z "$ofbiz_branch" ]; then
    echo "ERROR: this worktree is in detached HEAD state, so the matching plugins branch can't be inferred. Pass it explicitly: ./setup-worktree-plugins.sh <branch>" >&2
    exit 1
elif branch_exists "$ofbiz_branch"; then
    plugins_branch=$ofbiz_branch
else
    # No plugins branch shares this worktree's exact branch name -- it's
    # presumably a feature/ticket branch. Rather than guess from naming
    # conventions (which vary per team/fork), walk this repo's own branch
    # ancestry, nearest first, to find the branch it was actually cut from,
    # and use that name in plugins/ instead. Keep walking further back
    # (e.g. past an intermediate release branch to trunk) until one of
    # them has a matching plugins branch.
    candidates=$(
        for ref in $(git for-each-ref --format='%(refname:short)' refs/heads refs/remotes/origin); do
            if [ "$ref" = "$ofbiz_branch" ] || [ "$ref" = "origin/$ofbiz_branch" ] || [ "$ref" = "origin/HEAD" ]; then
                continue
            fi
            if git merge-base --is-ancestor "$ref" HEAD 2>/dev/null; then
                distance=$(git rev-list --count "$ref"..HEAD 2>/dev/null) && echo "$distance $ref"
            fi
        done | sort -n
    )
    old_ifs=$IFS
    IFS='
'
    for line in $candidates; do
        ref=${line#* }
        candidate=${ref#origin/}
        if branch_exists "$candidate"; then
            plugins_branch=$candidate
            break
        fi
    done
    IFS=$old_ifs
fi

if [ -z "$plugins_branch" ]; then
    echo "ERROR: could not determine a plugins/ branch matching ofbiz branch '$ofbiz_branch'." >&2
    echo "Pass one explicitly: ./setup-worktree-plugins.sh <branch>" >&2
    echo "Branches available in $plugins_repo:" >&2
    git -C "$plugins_repo" branch -a --format='  %(refname:short)' >&2
    exit 1
fi

echo "Setting up plugins/ as a linked worktree of $plugins_repo on branch '$plugins_branch'..."

# If that branch is already checked out elsewhere (e.g. in the main
# checkout this was branched from), git refuses a second worktree on the
# same branch -- fall back to a detached checkout of the same commit,
# which is fine for building since we only need matching source, not to
# commit from inside this worktree's plugins/.
if worktree_add_output=$(git -C "$plugins_repo" worktree add "$(pwd)/plugins" "$plugins_branch" 2>&1); then
    echo "$worktree_add_output"
elif echo "$worktree_add_output" | grep -q "already checked out"; then
    echo "Branch '$plugins_branch' is already checked out elsewhere; using a detached checkout of the same commit instead."
    git -C "$plugins_repo" worktree add --detach "$(pwd)/plugins" "$plugins_branch"
else
    echo "$worktree_add_output" >&2
    exit 1
fi

echo "Done: plugins/ is ready on '$plugins_branch'."
