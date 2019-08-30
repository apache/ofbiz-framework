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
RELEASE="3.2.1"
GRADLE_WRAPPER_URI="https://dl.bintray.com/apacheofbiz/GradleWrapper/v$RELEASE/"
GRADLE_WRAPPER_URI_BACKUP="https://github.com/gradle/gradle/raw/v$RELEASE/gradle/wrapper/"

# checksum to verify the downloaded file
SHASUM_GRADLE_WRAPPER_FILES="12478d9829998a5433231ad971bae52978279a3d  gradle/wrapper/gradle-wrapper.jar
05d4ab69d3f2143e017710b0917b740f75a75c07  gradle/wrapper/gradle-wrapper.properties
aaa5fb4c074407cb4d7f8c89a80342f3130880c3  gradlew"

GRADLE_WRAPPER_JAR="gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="gradle-wrapper.properties"
GRADLE_WRAPPER_FILES="$GRADLE_WRAPPER_JAR $GRADLE_WRAPPER_PROPERTIES"
GRADLE_WRAPPER_SCRIPT="gradlew"

whereIsBinary() {
    whereis $1 | grep /
}

# Perform the download using curl or wget
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

# Download the file from the main URI; if the download fails then use the backup URI
resolveFile() {
   downloadFile $1 $GRADLE_WRAPPER_URI;
   if [ $? -eq 1 ]; then
       downloadFile $1 $GRADLE_WRAPPER_URI_BACKUP;
   fi
}

echo " === Prepare operation ===";
# Verify that the script is executed from the right location
if [ ! -d "$GRADLE_OFBIZ_PATH" ]; then
    echo "Location seems to be incorrect, please run 'sh gradle/init-gradle-wrapper.sh' from the Apache OFBiz home";
    exit 1;
fi
if [ ! -d "$GRADLE_WRAPPER_OFBIZ_PATH" ]; then
    mkdir $GRADLE_WRAPPER_OFBIZ_PATH;
fi

# check if we have on binary to download missing wrapper
if [ -z "$(whereIsBinary curl)" ] && [ -z "$(whereIsBinary wget)" ]; then
   echo "curl or wget not found, please install one of them or install yourself gradle (for more information see README.md or https://gradle.org/install)";
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
        echo "\nDownload files $GRADLE_WRAPPER_FILES from $GRADLE_WRAPPER_URI failed.\nPlease check the logs, fix the problem and run the script again."
    fi

    if [ ! -r "$GRADLE_WRAPPER_SCRIPT" ]; then
         echo " === Download script wrapper ==="
         resolveFile $GRADLE_WRAPPER_SCRIPT
         mv "$GRADLE_WRAPPER_OFBIZ_PATH/$GRADLE_WRAPPER_SCRIPT" .
         chmod u+x $GRADLE_WRAPPER_SCRIPT
    fi

    echo " === Control downloaded files ==="
    if [ -n "$(whereIsBinary shasum)" ]; then
        echo "$SHASUM_GRADLE_WRAPPER_FILES" | shasum -c -;
        exit 0;
    fi

    echo " Warning: shasum not found, the downloaded files could not be verified"
    exit 1;
fi
echo " Nothing to be done"
