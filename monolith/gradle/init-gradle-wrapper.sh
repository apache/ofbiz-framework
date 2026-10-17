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

# Paths
OFBIZ_HOME="$(pwd)"
GRADLE_WRAPPER_OFBIZ_PATH="$OFBIZ_HOME/gradle/wrapper"
GRADLE_WRAPPER_PROPERTIES="$GRADLE_WRAPPER_OFBIZ_PATH/gradle-wrapper.properties"
GRADLE_WRAPPER_JAR="$GRADLE_WRAPPER_OFBIZ_PATH/gradle-wrapper.jar"

whereIsBinary() {
    whereis $1 | grep /
}

# Perform the download using curl or wget, output to stdout
downloadToStdout() {
    if [ -n "$(whereIsBinary curl)" ]; then
        curl -L -s "$1"
    elif [ -n "$(whereIsBinary wget)" ]; then
        wget -q -O - "$1"
    fi
}

# Download a file to a given destination path
downloadFile() {
    if [ -n "$(whereIsBinary curl)" ]; then
        HTTP_CODE=$(curl -L -o "$2" -s -w '%{http_code}' "$1")
        [ "$HTTP_CODE" = "200" ]
    elif [ -n "$(whereIsBinary wget)" ]; then
        wget -q -O "$2" "$1" 2>&1 | grep -q 'HTTP/1.1 200 OK'
        [ $? -eq 0 ]
    else
        return 1
    fi
}

# Compute SHA256 of a file
computeSha256() {
    if [ -n "$(whereIsBinary sha256sum)" ]; then
        sha256sum "$1" | cut -d' ' -f1
    elif [ -n "$(whereIsBinary shasum)" ]; then
        shasum -a 256 "$1" | cut -d' ' -f1
    fi
}

UPGRADE=false

for arg in "$@"; do
    case "$arg" in
        --help)
            echo "Usage: sh gradle/init-gradle-wrapper.sh [--help] [--upgrade]"
            echo ""
            echo "Downloads and verifies gradle-wrapper.jar for Apache OFBiz."
            echo "The jar is not committed to the repository; run this script"
            echo "before using ./gradlew for the first time."
            echo ""
            echo "Options:"
            echo "  --help     Show this message and exit."
            echo "  --upgrade  After downloading/verifying the jar, run"
            echo "             './gradlew wrapper' to regenerate gradlew and"
            echo "             gradlew.bat to match the new Gradle version."
            echo ""
            echo "Workflow for Gradle version upgrades (e.g. from a Dependabot PR):"
            echo "  1. sh gradle/init-gradle-wrapper.sh --upgrade"
            echo "  2. Commit any changes to gradlew and gradlew.bat"
            exit 0
            ;;
        --upgrade)
            UPGRADE=true
            ;;
        *)
            echo "Unknown option: $arg"
            echo "Run 'sh gradle/init-gradle-wrapper.sh --help' for usage."
            exit 1
            ;;
    esac
done

# Verify that the script is executed from the right location
if [ ! -f "$GRADLE_WRAPPER_PROPERTIES" ]; then
    echo "gradle/wrapper/gradle-wrapper.properties not found."
    echo "Please run 'sh gradle/init-gradle-wrapper.sh' from the Apache OFBiz home."
    exit 1
fi

# Parse the Gradle version from gradle-wrapper.properties
RELEASE=$(grep "^distributionUrl=" "$GRADLE_WRAPPER_PROPERTIES" | sed 's/.*gradle-\([0-9.]*\)-.*/\1/')
if [ -z "$RELEASE" ]; then
    echo "Could not determine Gradle version from $GRADLE_WRAPPER_PROPERTIES"
    exit 1
fi
echo "Gradle version: $RELEASE"

GRADLE_WRAPPER_URI="https://github.com/gradle/gradle/raw/v$RELEASE/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_SHA256_URI="https://services.gradle.org/distributions/gradle-$RELEASE-wrapper.jar.sha256"

# If gradle-wrapper.jar already exists, verify its checksum before deciding to skip or re-download
if [ -r "$GRADLE_WRAPPER_JAR" ]; then
    echo "gradle-wrapper.jar found, verifying checksum..."
    EXPECTED_SHA256=$(downloadToStdout "$GRADLE_WRAPPER_SHA256_URI")
    if [ -z "$EXPECTED_SHA256" ]; then
        echo "Warning: could not reach checksum service, skipping verification"
        exit 0
    fi
    ACTUAL_SHA256=$(computeSha256 "$GRADLE_WRAPPER_JAR")
    if [ -z "$ACTUAL_SHA256" ]; then
        echo "Warning: sha256sum or shasum not found, cannot verify existing gradle-wrapper.jar"
        exit 0
    fi
    if [ "$ACTUAL_SHA256" = "$EXPECTED_SHA256" ]; then
        echo "Checksum OK."
        if [ "$UPGRADE" = true ]; then
            echo "Running './gradlew wrapper' to regenerate gradlew and gradlew.bat..."
            ./gradlew wrapper
        fi
        exit 0
    else
        echo "Checksum mismatch, re-downloading..."
        rm -f "$GRADLE_WRAPPER_JAR"
    fi
fi

# Ensure curl or wget is available
if [ -z "$(whereIsBinary curl)" ] && [ -z "$(whereIsBinary wget)" ]; then
    echo "curl or wget not found, please install one of them or install yourself gradle (for more information see README.md or https://gradle.org/install)"
    exit 1
fi

echo "Downloading gradle-wrapper.jar..."
if ! downloadFile "$GRADLE_WRAPPER_URI" "$GRADLE_WRAPPER_JAR"; then
    rm -f "$GRADLE_WRAPPER_JAR"
    echo "Download of gradle-wrapper.jar from $GRADLE_WRAPPER_URI failed."
    echo "Please check the logs, fix the problem and run the script again."
    exit 1
fi

echo "Verifying checksum..."
EXPECTED_SHA256=$(downloadToStdout "$GRADLE_WRAPPER_SHA256_URI")
if [ -z "$EXPECTED_SHA256" ]; then
    rm -f "$GRADLE_WRAPPER_JAR"
    echo "Error: could not fetch checksum from $GRADLE_WRAPPER_SHA256_URI"
    exit 1
fi

ACTUAL_SHA256=$(computeSha256 "$GRADLE_WRAPPER_JAR")
if [ -z "$ACTUAL_SHA256" ]; then
    echo "Warning: sha256sum or shasum not found, the downloaded file could not be verified"
    exit 0
fi

if [ "$ACTUAL_SHA256" = "$EXPECTED_SHA256" ]; then
    echo "Checksum OK."
    if [ "$UPGRADE" = true ]; then
        echo "Running './gradlew wrapper' to regenerate gradlew and gradlew.bat..."
        ./gradlew wrapper
    fi
else
    rm -f "$GRADLE_WRAPPER_JAR"
    echo "Error: checksum mismatch"
    echo "Expected: $EXPECTED_SHA256"
    echo "Actual:   $ACTUAL_SHA256"
    exit 1
fi
