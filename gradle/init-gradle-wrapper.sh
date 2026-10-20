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
        # wget exits with a non-zero status on HTTP errors; there is no output to inspect with -q
        wget -q -O "$2" "$1"
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

# Find a tool able to extract single entries from a zip file: unzip or, as a fallback, the jar tool
# of the JDK (which OFBiz needs anyway; some images, like the official eclipse-temurin ones, have no unzip)
findExtractTool() {
    if [ -n "$(whereIsBinary unzip)" ]; then
        echo "unzip"
    elif [ -n "$(whereIsBinary jar)" ]; then
        echo "jar"
    elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
        echo "$JAVA_HOME/bin/jar"
    fi
}

# Extract one entry from a zip (or jar) file: $1 = file, $2 = entry, $3 = existing destination directory.
# The file and the directory must be given as absolute paths.
extractFromZip() {
    if [ "$EXTRACT_TOOL" = "unzip" ]; then
        unzip -q -o "$1" "$2" -d "$3"
    else
        (cd "$3" && "$EXTRACT_TOOL" xf "$1" "$2")
    fi
}

# Get gradle-wrapper.jar from the Gradle distribution and put it in $1.
#
# The wrapper jar is not published on its own. The jar whose checksum Gradle publishes is the one nested in the
# distribution: gradle-<version>/lib/plugins/gradle-wrapper-main-<version>.jar contains gradle-wrapper.jar. The
# gradle-wrapper.jar in the git tag of gradle/gradle is only the bootstrap wrapper Gradle builds itself with and
# does not always match the published checksum (see https://github.com/gradle/gradle/issues/39471).
downloadWrapperJarFromDistribution() {
    WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/ofbiz-gradle-wrapper.XXXXXX") || return 1
    trap 'rm -rf "$WORK_DIR"' EXIT
    trap 'rm -rf "$WORK_DIR"; exit 1' INT TERM

    echo "Downloading gradle-$RELEASE-bin.zip (about 140 MB) to extract the wrapper jar..."
    if ! downloadFile "$GRADLE_DISTRIBUTION_URI" "$WORK_DIR/gradle.zip"; then
        echo "Download of $GRADLE_DISTRIBUTION_URI failed."
        return 1
    fi

    EXPECTED_DISTRIBUTION_SHA256=$(downloadToStdout "$GRADLE_DISTRIBUTION_SHA256_URI")
    if [ -z "$EXPECTED_DISTRIBUTION_SHA256" ]; then
        echo "Error: could not fetch checksum from $GRADLE_DISTRIBUTION_SHA256_URI"
        return 1
    fi
    ACTUAL_DISTRIBUTION_SHA256=$(computeSha256 "$WORK_DIR/gradle.zip")
    if [ -z "$ACTUAL_DISTRIBUTION_SHA256" ]; then
        echo "Warning: sha256sum or shasum not found, the downloaded distribution could not be verified"
    elif [ "$ACTUAL_DISTRIBUTION_SHA256" != "$EXPECTED_DISTRIBUTION_SHA256" ]; then
        echo "Error: checksum mismatch for $GRADLE_DISTRIBUTION_URI"
        echo "Expected: $EXPECTED_DISTRIBUTION_SHA256"
        echo "Actual:   $ACTUAL_DISTRIBUTION_SHA256"
        return 1
    fi

    NESTED_JAR="gradle-$RELEASE/lib/plugins/gradle-wrapper-main-$RELEASE.jar"
    if ! extractFromZip "$WORK_DIR/gradle.zip" "$NESTED_JAR" "$WORK_DIR"; then
        echo "Error: could not extract $NESTED_JAR from the Gradle distribution."
        return 1
    fi
    mkdir "$WORK_DIR/wrapper"
    if ! extractFromZip "$WORK_DIR/$NESTED_JAR" "gradle-wrapper.jar" "$WORK_DIR/wrapper"; then
        echo "Error: could not extract gradle-wrapper.jar from $NESTED_JAR."
        return 1
    fi
    mv "$WORK_DIR/wrapper/gradle-wrapper.jar" "$1"
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
            echo "The jar is extracted from the Gradle distribution (about 140 MB),"
            echo "which needs unzip or the jar tool of a JDK."
            echo ""
            echo "Options:"
            echo "  --help     Show this message and exit."
            echo "  --upgrade  After downloading/verifying the jar, run"
            echo "             './gradlew wrapper' to regenerate gradlew and"
            echo "             gradlew.bat to match the new Gradle version."
            echo ""
            echo "Workflow for Gradle version upgrades (e.g. from a Dependabot PR):"
            echo "  1. Apply the version change (a Dependabot commit only edits"
            echo "     gradle/wrapper/gradle-wrapper.properties)"
            echo "  2. sh gradle/init-gradle-wrapper.sh --upgrade"
            echo "  3. Commit any changes to gradlew and gradlew.bat (patch releases"
            echo "     usually have none)"
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

GRADLE_DISTRIBUTION_URI="https://services.gradle.org/distributions/gradle-$RELEASE-bin.zip"
GRADLE_DISTRIBUTION_SHA256_URI="$GRADLE_DISTRIBUTION_URI.sha256"
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

# Ensure a tool to extract the jar from the distribution is available
EXTRACT_TOOL=$(findExtractTool)
if [ -z "$EXTRACT_TOOL" ]; then
    echo "unzip or the jar tool of a JDK (found through JAVA_HOME) not found, please install one of them or install yourself gradle (for more information see README.md or https://gradle.org/install)"
    exit 1
fi

echo "Getting gradle-wrapper.jar from the Gradle $RELEASE distribution..."
if ! downloadWrapperJarFromDistribution "$GRADLE_WRAPPER_JAR"; then
    rm -f "$GRADLE_WRAPPER_JAR"
    echo "Could not get gradle-wrapper.jar from $GRADLE_DISTRIBUTION_URI"
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
