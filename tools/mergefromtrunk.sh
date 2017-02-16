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
set -e

# this script requires a posix shell; namely, $(( math evaluation.

help() {
    cat << _EOF_
$0 [options]

mergefromtrunk.sh merge \$rev        Apply revision \$rev from trunk.
mergefromtrunk.sh test            Run test suite(clean-all, load-demo, run-tests).
mergefromtrunk.sh commit        Commit current fix to svn.
mergefromtrunk.sh abort            Abort current merge session.

-h | --help        Show this help.
_EOF_
}

cmd=""
rev=""
while [ $# -gt 0 ]; do
    case "$1" in
        (-h|--help)
            help
            exit 0
            ;;
        (-*)
            echo "Unknown arg ($1)." 1>&2
            help 1>&2
            exit 1
            ;;
        (*)
            if [ z = "z$cmd" ]; then
                cmd="$1"
            else
                case "$cmd" in
                    (merge)
                        rev="$1"
                        ;;
                    (*)
                        echo "Too many arguments." 1>&2
                        help 1>&2
                        exit 1
                        ;;
                esac
            fi
            ;;
    esac
    shift
done
case "$cmd" in
    (merge)
        if [ z = "z$rev" ]; then
            echo "Need a revision." 1>&2
            help 1>&2
            exit 1
        fi
        if [ -d runtime/merge-state ]; then
            echo "Merge session already started." 1>&2
            help 1>&2
            exit 1
        fi
        mkdir -p runtime/merge-state
        echo "$rev" > runtime/merge-state/revision
        # do not run any of the following commands in a complex
        # chained pipe; if one of the commands in the pipe fails,
        # it isn't possible to detect the failure.
        printf "Applied fix from trunk for revision: %s \n===\n\n" "$rev" > runtime/merge-state/log-message
        svn log https://svn.apache.org/repos/asf/ofbiz/ofbiz-framework/trunk -r "$rev" > runtime/merge-state/log.txt
        set -- $(wc -l runtime/merge-state/log.txt)
        head -n $(($1 - 1)) < runtime/merge-state/log.txt > runtime/merge-state/log.txt.head
        tail -n $(($1 - 4)) < runtime/merge-state/log.txt.head >> runtime/merge-state/log-message
        prevRev=$(($rev - 1))
        svn up
        svn merge -r "$prevRev:$rev" https://svn.apache.org/repos/asf/ofbiz/ofbiz-framework/trunk
        ;;
    (test)
        ./gradlew cleanAll loadDefault testIntegration
        ;;
    (commit)
        svn commit -F runtime/merge-state/log-message
        rm -rf runtime/merge-state
        ;;
    (abort)
        svn resolved . -R
        svn revert . -R
        rm -rf runtime/merge-state
        ;;
    ("")
        echo "Need a command and a revision." 1>&2
        help 1>&2
        exit 1
        ;;
    (*)
        echo "Unknown command($cmd)." 1>&2
        help 1>&2
        exit 1
        ;;
esac
