#!/bin/sh
#####################################################################
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
#####################################################################

# This shell script tries to be as close to POSIX as possible.
# external dependencies:
#   mkdir
#   tee
#   git rev-parse
#   git rebase --continue

set -x
# Simulate mkdir -p, which isn't POSIX; this is modelled after
# 'install', which is what standard unix makefiles tend to use for the
# purpose
_install_dir() {
    # POSIX doesn't support the shell 'local' keyword, so simulate
    # that with ()
    (
        IFS="/"
        set -- $1
        while [ $# -gt 0 ]; do
            [ -d "$1" ] || mkdir "$1"
            cd "$1"
            shift
        done
    )
}
# create the entire directory path.  Multiple directories can be given.
install_dir() {
    while [ $# -gt 0 ]; do
        _install_dir "$1"
        shift
    done
}

# args:
#  $1: how many loops
#  $2-$#: command to call thru each loop
git_rebase_runner() {
    install_dir runtime/git-rebase/logs
    (
        set -- runtime/git-rebase/logs/*
        if [ -e "$1" ]; then
            rm "$@"
        fi
    )
    total_loops="$1"
    cleaner="$2"
    shift 2
    if [ -e runtime/git-rebase/hook.sh ]; then
        runtime/git-rebase/hook.sh start $total_loops || break
    fi
    eval "$cleaner"
    while [ $total_loops -gt 0 ]; do
        git_rebase_runner_success=0
        total_loops=$(($total_loops - 1))
        hash="$(git rev-parse HEAD)"
        local_log="runtime/git-rebase/logs/$hash.log"
        [ -e "$local_log" ] && rm "$local_log"
        # POSIX tee does not support -a, so we have to run everything
        # inside a single redirection.
        {
            if [ -e runtime/git-rebase/hook.sh ]; then
                runtime/git-rebase/hook.sh pre-run $total_loops || break
            fi
            eval "$@" || break
            eval "$cleaner" || break
            if [ -e runtime/git-rebase/hook.sh ]; then
                runtime/git-rebase/hook.sh post-run $total_loops || break
            fi
        } 2>&1 | tee "runtime/git-rebase/logs/$hash.log"
        git rebase --continue || break
        git_rebase_runner_success=1
    done
    if [ -e runtime/git-rebase/hook.sh ]; then
        runtime/git-rebase/hook.sh stop $total_loops || break
    fi
    # POSIX [ doesn't deal well when one of the arguments is empty; this
    # could occur if $total_loops = 0.
    if [ "z$git_rebase_runner_success" = "z0" ]; then
        eval "$cleaner"
        while [ $total_loops -gt 0 ]; do
            total_loops=$(($total_loops - 1))
            git rebase --continue
        done
    fi
}

run_gradlew() {
    # POSIX [ doesn't deal well when one of the arguments is empty, and
    # [ doesn't support [ !
    if [ "z$USE_LOCAL_ANT" = "z" ]; then
        gradlew "$@"
    else
        ./gradlew "$@"
    fi
}

standard_cleanup() {
    run_gradlew cleanAll
}
install_worker() {
    run_gradlew loadDefault
}
fulltestsuite_worker() {
    run_gradlew loadDefault
    run_gradlew testIntegration
}
#git_rebase_runner 3 fulltestsuite_cleanup fulltestsuite_worker
