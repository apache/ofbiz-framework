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

if ((Test-Path -Path ((Get-Item -Path ".\").FullName + "\gradle\wrapper\gradle-wrapper.jar")) -and (Test-Path -Path ((Get-Item -Path ".\").FullName + "\gradle\wrapper\gradle-wrapper.properties"))) {
    Write-Host "The Gradle Wrapper has already been downloaded.";
    exit
}

# HTTPS is not used because it gets complicated with Powershell and .Net framework versions depending on Windows versions
Invoke-WebRequest -outf gradle\wrapper\gradle-wrapper.jar http://dl.bintray.com/apacheofbiz/GradleWrapper/v3.2.1/gradle-wrapper.jar
Invoke-WebRequest -outf gradle\wrapper\gradle-wrapper.properties http://dl.bintray.com/apacheofbiz/GradleWrapper/v3.2.1/gradle-wrapper.properties
Invoke-WebRequest -outf gradlew.bat http://dl.bintray.com/apacheofbiz/GradleWrapper/v3.2.1/gradlew.bat

