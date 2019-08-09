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

# Variable for location
OFBIZ_HOME="$(pwd)"
GRADLE_OFBIZ_PATH="$OFBIZ_HOME/gradle"
GRADLE_WRAPPER_OFBIZ_PATH="$GRADLE_OFBIZ_PATH/wrapper"

# version and uri to download the wrapper
RELEASE="5.0.0"
GRADLE_WRAPPER_URI="https://dl.bintray.com/apacheofbiz/GradleWrapper/v$RELEASE/"
GRADLE_WRAPPER_URI_BACKUP="https://github.com/gradle/gradle/raw/v$RELEASE/gradle/wrapper/"

# Embded checksum shasum to control the download
SHASUM_GRADLE_WRAPPER_FILES="1d23286bcb9e7d3debff18c1b892b9dbb9a4ec6c  gradle/wrapper/gradle-wrapper.jar
f9c2ad227ef1fe774cb0e141abfc431b05fc9fd4  gradle/wrapper/gradle-wrapper.properties"

GRADLE_WRAPPER_JAR="gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="gradle-wrapper.properties"
GRADLE_WRAPPER_FILES="$GRADLE_WRAPPER_JAR $GRADLE_WRAPPER_PROPERTIES"

whereIsBinary() {
    whereis $1 | grep /
}

# Resolve the command to use for calling and realize the download
downloadFile() {
   if [ -n "$(whereIsBinary curl)" ]; then
       GET_CMD="curl -L -o $GRADLE_WRAPPER_OFBIZ_PATH/$1 -s -w %{http_code} $2/$1";
       if [ "$($GET_CMD)" = "200" ]; then
           return 0;
       fi
   elif [ -n "$(whereIsBinary wget)" ]; then
       GET_CMD="wget -q -O $GRADLE_WRAPPER_OFBIZ_PATH/$1 --server-response $2/$1";
       GET_CMD="$GET_CMD"' 2>&1 > /dev/null | grep "HTTP/.* 200"';
       if [ -n "$($GET_CMD)" ]; then
           return 0;
       fi
   fi
   return 1
}

# Call and if not succes try to use backup
resolveFile() {
   downloadFile $1 $GRADLE_WRAPPER_URI;
   if [ $? -eq 1 ]; then
       downloadFile $1 $GRADLE_WRAPPER_URI_BACKUP;
   fi
}

echo " === Prepare operation ===";
# Control that we work the script on a good directory
if [ ! -d "$GRADLE_OFBIZ_PATH" ]; then
    echo "Location seems to be uncorrected, please take care to run 'sh gradle/init-gradle-wrapper.sh' at the Apache OFBiz home";
    exit 1;
fi

# check if we have on binary to download missing wrapper
if [ -z "$(whereIsBinary curl)" ] && [ -z "$(whereIsBinary wget)" ]; then
   echo "No command curl or wget found, please install one or install yourself gradle (more information see README.adoc or https://gradle.org/install)";
   exit 1
fi

if [ ! -r "$GRADLE_WRAPPER_OFBIZ_PATH/$GRADLE_WRAPPER_JAR" ]; then
    echo "$GRADLE_WRAPPER_OFBIZ_PATH/$GRADLE_WRAPPER_JAR not found, we download it"

    for fileToDownload in $GRADLE_WRAPPER_FILES; do
         echo " === Download $fileToDownload ===";
         resolveFile $fileToDownload
    done
    if [ ! $? -eq 0 ]; then
        rm -f $GRADLE_WRAPPER_OFBIZ_PATH/*
        echo "\nDownload files $GRADLE_WRAPPER_FILES from $GRADLE_WRAPPER_URI failed.\nPlease check the log to found the reason and run the script again."
    fi
    echo " === Control downloaded files ==="
    if [ -n "$(whereIsBinary shasum)" ]; then
        echo "$SHASUM_GRADLE_WRAPPER_FILES" | shasum -c -;
        exit 0;
    fi

    echo " Warning: shasum not found, skip the control process"
    exit 1;
fi
echo " Nothing todo"
