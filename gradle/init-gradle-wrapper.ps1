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

param(
    [switch]$Help,
    [switch]$Upgrade
)

if ($Help) {
    Write-Host "Usage: gradle\init-gradle-wrapper.ps1 [-Help] [-Upgrade]"
    Write-Host ""
    Write-Host "Downloads and verifies gradle-wrapper.jar for Apache OFBiz."
    Write-Host "The jar is not committed to the repository; run this script"
    Write-Host "before using gradlew.bat for the first time."
    Write-Host ""
    Write-Host "The jar is extracted from the Gradle distribution (about 140 MB)."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Help     Show this message and exit."
    Write-Host "  -Upgrade  After downloading/verifying the jar, run"
    Write-Host "            '.\gradlew wrapper' to regenerate gradlew.bat"
    Write-Host "            to match the new Gradle version."
    Write-Host ""
    Write-Host "Workflow for Gradle version upgrades (e.g. from a Dependabot PR):"
    Write-Host "  1. Apply the version change (a Dependabot commit only edits"
    Write-Host "     gradle\wrapper\gradle-wrapper.properties)"
    Write-Host "  2. gradle\init-gradle-wrapper.ps1 -Upgrade"
    Write-Host "  3. Commit any changes to gradlew and gradlew.bat (patch releases"
    Write-Host "     usually have none)"
    exit 0
}

md -force gradle/wrapper

# Parse the Gradle version from gradle-wrapper.properties
$distributionUrl = (Get-Content "gradle\wrapper\gradle-wrapper.properties" | Where-Object { $_ -match "^distributionUrl=" }) -replace "^distributionUrl=", ""
$release = [regex]::Match($distributionUrl, 'gradle-(\d+(?:\.\d+)+)-').Groups[1].Value
Write-Host "Gradle version: $release"

$gradleWrapperJar = "gradle\wrapper\gradle-wrapper.jar"
$gradleDistributionUri = "https://services.gradle.org/distributions/gradle-$release-bin.zip"
$gradleDistributionSha256Uri = "$gradleDistributionUri.sha256"
$gradleWrapperSha256Uri = "https://services.gradle.org/distributions/gradle-$release-wrapper.jar.sha256"

function Get-ExpectedSha256 {
    try {
        return Invoke-RestMethod -Uri $gradleWrapperSha256Uri -ErrorAction Stop
    } catch {
        return $null
    }
}

function Get-ActualSha256 {
    return (Get-FileHash $gradleWrapperJar -Algorithm SHA256).Hash.ToLower()
}

# If gradle-wrapper.jar already exists, verify its checksum before deciding to skip or re-download
if (Test-Path $gradleWrapperJar) {
    Write-Host "gradle-wrapper.jar found, verifying checksum..."
    $expected = Get-ExpectedSha256
    if ($null -eq $expected) {
        Write-Host "Warning: could not reach checksum service, skipping verification"
        exit 0
    }
    $actual = Get-ActualSha256
    if ($actual -eq $expected) {
        Write-Host "Checksum OK."
        if ($Upgrade) {
            Write-Host "Running '.\gradlew wrapper' to regenerate gradlew and gradlew.bat..."
            & .\gradlew wrapper
        }
        exit 0
    } else {
        Write-Host "Checksum mismatch, re-downloading..."
        Remove-Item $gradleWrapperJar
    }
}

# Get gradle-wrapper.jar from the Gradle distribution.
#
# The wrapper jar is not published on its own. The jar whose checksum Gradle publishes is the one nested in the
# distribution: gradle-<version>/lib/plugins/gradle-wrapper-main-<version>.jar contains gradle-wrapper.jar. The
# gradle-wrapper.jar in the git tag of gradle/gradle is only the bootstrap wrapper Gradle builds itself with and
# does not always match the published checksum (see https://github.com/gradle/gradle/issues/39471).
function Get-TextFromUri($uri) {
    $content = (Invoke-WebRequest -Uri $uri -UseBasicParsing -ErrorAction Stop).Content
    if ($content -is [byte[]]) {
        $content = [System.Text.Encoding]::ASCII.GetString($content)
    }
    return $content.Trim()
}

# Copy one entry of a zip (or jar) file to a destination file
function Export-ZipEntry($zipFile, $entryName, $destinationFile) {
    $archive = [System.IO.Compression.ZipFile]::OpenRead($zipFile)
    try {
        $entry = $archive.GetEntry($entryName)
        if ($null -eq $entry) {
            return $false
        }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destinationFile, $true)
        return $true
    } finally {
        $archive.Dispose()
    }
}

function Save-WrapperJarFromDistribution {
    $workDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ofbiz-gradle-wrapper-" + [System.Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $workDir | Out-Null
    try {
        $zip = Join-Path $workDir "gradle.zip"
        Write-Host "Downloading gradle-$release-bin.zip (about 140 MB) to extract the wrapper jar..."
        # The progress bar makes Invoke-WebRequest very slow for large files in Windows PowerShell
        $savedProgressPreference = $ProgressPreference
        $ProgressPreference = 'SilentlyContinue'
        try {
            Invoke-WebRequest -OutFile $zip -Uri $gradleDistributionUri -UseBasicParsing -ErrorAction Stop
        } finally {
            $ProgressPreference = $savedProgressPreference
        }

        $expectedDistribution = Get-TextFromUri $gradleDistributionSha256Uri
        $actualDistribution = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLower()
        if ($actualDistribution -ne $expectedDistribution) {
            Write-Host "Error: checksum mismatch for $gradleDistributionUri"
            Write-Host "Expected: $expectedDistribution"
            Write-Host "Actual:   $actualDistribution"
            return $false
        }

        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $nestedEntry = "gradle-$release/lib/plugins/gradle-wrapper-main-$release.jar"
        $nestedJar = Join-Path $workDir "gradle-wrapper-main.jar"
        if (-not (Export-ZipEntry $zip $nestedEntry $nestedJar)) {
            Write-Host "Error: could not extract $nestedEntry from the Gradle distribution."
            return $false
        }
        if (-not (Export-ZipEntry $nestedJar "gradle-wrapper.jar" (Join-Path $workDir "gradle-wrapper.jar"))) {
            Write-Host "Error: could not extract gradle-wrapper.jar from $nestedEntry."
            return $false
        }
        Move-Item -Force (Join-Path $workDir "gradle-wrapper.jar") $gradleWrapperJar
        return $true
    } catch {
        Write-Host "Error: $($_.Exception.Message)"
        return $false
    } finally {
        Remove-Item -Recurse -Force $workDir -ErrorAction SilentlyContinue
    }
}

Write-Host "Getting gradle-wrapper.jar from the Gradle $release distribution..."
If ($ExecutionContext.SessionState.LanguageMode -eq "ConstrainedLanguage") {
    Set-ItemProperty 'hklm:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' -name "__PSLockdownPolicy" -Value 8
    $obtained = Save-WrapperJarFromDistribution
    Set-ItemProperty 'hklm:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' -name "__PSLockdownPolicy" -Value 4
} else {
    $obtained = Save-WrapperJarFromDistribution
}
if (-not $obtained) {
    if (Test-Path $gradleWrapperJar) {
        Remove-Item $gradleWrapperJar
    }
    Write-Host "Could not get gradle-wrapper.jar from $gradleDistributionUri"
    Write-Host "Please check the logs, fix the problem and run the script again."
    exit 1
}

# Verify the downloaded jar against the expected checksum published by Gradle
# See: https://docs.gradle.org/current/userguide/gradle_wrapper.html#wrapper_checksum_verification
Write-Host "Verifying checksum..."
$expected = Get-ExpectedSha256
if ($null -eq $expected) {
    Remove-Item $gradleWrapperJar
    Write-Host "Error: could not fetch checksum from $gradleWrapperSha256Uri"
    exit 1
}
$actual = Get-ActualSha256
if ($actual -eq $expected) {
    Write-Host "Checksum OK."
    if ($Upgrade) {
        Write-Host "Running '.\gradlew wrapper' to regenerate gradlew and gradlew.bat..."
        & .\gradlew wrapper
    }
} else {
    Remove-Item $gradleWrapperJar
    Write-Host "Error: checksum mismatch"
    Write-Host "Expected: $expected"
    Write-Host "Actual:   $actual"
    exit 1
}

Start-Sleep -s 3
