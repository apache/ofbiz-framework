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

# set the parent directory as OFBiz Home
OFBIZ_HOME="$( cd -P "$( dirname "$0" )" && pwd )"/..

# console log file
OFBIZ_LOG=runtime/logs/console.log

# delete the last log
rm -f $OFBIZ_LOG

# Allows to run from Jenkins. See http://wiki.jenkins-ci.org/display/JENKINS/ProcessTreeKiller. Cons: the calling Jenkins job does not terminate if the log is not enabled, pros: this allows to monitor the log in Jenkins
#BUILD_ID=dontKillMe
# JLR post Gradle comment, not sure this is still true...

# start ofbiz
(cd "$OFBIZ_HOME" && exec gradlew ofbiz)
