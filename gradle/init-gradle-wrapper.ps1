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
    Write-Host "Options:"
    Write-Host "  -Help     Show this message and exit."
    Write-Host "  -Upgrade  After downloading/verifying the jar, run"
    Write-Host "            '.\gradlew wrapper' to regenerate gradlew.bat"
    Write-Host "            to match the new Gradle version."
    Write-Host ""
    Write-Host "Workflow for Gradle version upgrades (e.g. from a Dependabot PR):"
    Write-Host "  1. gradle\init-gradle-wrapper.ps1 -Upgrade"
    Write-Host "  2. Commit any changes to gradlew and gradlew.bat"
    exit 0
}

md -force gradle/wrapper

# Parse the Gradle version from gradle-wrapper.properties
$distributionUrl = (Get-Content "gradle\wrapper\gradle-wrapper.properties" | Where-Object { $_ -match "^distributionUrl=" }) -replace "^distributionUrl=", ""
$release = [regex]::Match($distributionUrl, 'gradle-(\d+(?:\.\d+)+)-').Groups[1].Value
Write-Host "Gradle version: $release"

$gradleWrapperJar = "gradle\wrapper\gradle-wrapper.jar"
$gradleWrapperUri = "https://github.com/gradle/gradle/raw/v$release/gradle/wrapper/gradle-wrapper.jar"
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

# Download gradle-wrapper.jar from the Gradle GitHub repository
If ($ExecutionContext.SessionState.LanguageMode -eq "ConstrainedLanguage") {
    Set-ItemProperty 'hklm:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' -name "__PSLockdownPolicy" -Value 8
    Invoke-WebRequest -outf $gradleWrapperJar $gradleWrapperUri
    Set-ItemProperty 'hklm:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment' -name "__PSLockdownPolicy" -Value 4
} else {
    Invoke-WebRequest -outf $gradleWrapperJar $gradleWrapperUri
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
