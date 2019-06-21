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

OFBIZ_HOME="$(pwd)"
GRADLE_OFBIZ_PATH="$OFBIZ_HOME/gradle"
GRADLE_WRAPPER_OFBIZ_PATH="$GRADLE_OFBIZ_PATH/wrapper"
RELEASE="5.0.0"
#GRADLE_WRAPPER_URI="https://github.com/gradle/gradle/blob/v${RELEASE}/gradle/wrapper"
GRADLE_WRAPPER_URI="https://svn.apache.org/repos/asf/ofbiz/tools/Buildbot/Gradle/Wrapper/trunk/"
GRADLE_WRAPPER_JAR="gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="gradle-wrapper.properties"
GRADLE_WRAPPER_FILES="$GRADLE_WRAPPER_JAR $GRADLE_WRAPPER_PROPERTIES"

whereIsBinary() {
    whereis $1 | grep /
}

if [ ! -d "$GRADLE_OFBIZ_PATH" ]; then
    echo "Location seems to be uncorrected, please take care to run 'sh gradle/init-gradle-wrapper.sh' at the Apache OFBiz home";
    exit -1;
fi

if [ ! -d "$GRADLE_WRAPPER_OFBIZ_PATH" ]; then
    mkdir $GRADLE_WRAPPER_OFBIZ_PATH
fi

if [ ! -r "$GRADLE_WRAPPER_OFBIZ_PATH/$GRADLE_WRAPPER_JAR" ]; then
    echo "$GRADLE_WRAPPER_OFBIZ_PATH/$GRADLE_WRAPPER_JAR not found, we download it"

    if [ ! -r "$GRADLE_ZIP_RELEASE" ]; then
        if [ -n "$(whereIsBinary curl)" ]; then
            GET_CMD="curl -L -o";
        elif [ -n "$(whereIsBinary wget)" ]; then
            GET_CMD="wget -O";
        else
           echo "No command curl or wget found, please install one or install yourself gradle (more information see https://gradle.org/install)";
           exit -1
        fi
        for fileToDownload in $GRADLE_WRAPPER_FILES; do
            $GET_CMD $GRADLE_WRAPPER_OFBIZ_PATH/$fileToDownload $GRADLE_WRAPPER_URI/$fileToDownload?raw=true;
        done
    fi
    if [ ! $? -eq 0 ]; then
        rm -f $GRADLE_WRAPPER_OFBIZ_PATH/*
        echo "\nDownload files $GRADLE_WRAPPER_FILES from $GRADLE_WRAPPER_URI failed.\nPlease check the log to found the reason and run the script again."
    fi
fi
