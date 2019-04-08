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

# args:
#  $1 = loop count
#  $2 = loop type

# example:
#  git rebase -i $most_recent_git_svn_commit_hash
#  # select commits to work on/test with 'edit'
#  tools/git-rebase-runner.sh 5 all-tests
#
#  this will do:
#   ant clean
#   ant load-demo
#   ant run-tests
#   ant clean
#   git rebase --continue
#  in a loop 5 times, storing the output into runtime/git-rebase/logs.
#  You can use this to verify a series of commits before commiting
#  upstream in bulk with git svn dcommit $hash

top_dir="$(cd "$0/../..";echo "$PWD")"
. "$top_dir/tools/functions.sh"

cd "$top_dir"

loops="$1"
shift
cleanup_worker=""
loop_worker=""
case "$1" in
	(all-tests)
		cleanup_worker=standard_cleanup
		loop_worker=fulltestsuite_worker
		;;
	(install)
		cleanup_worker=standard_cleanup
		loop_worker=install_worker
		;;
	(*)
		echo "Invalid loop type given($1)!" 1>&2
		exit 1
		;;
esac
git_rebase_runner $loops $cleanup_worker $loop_worker
