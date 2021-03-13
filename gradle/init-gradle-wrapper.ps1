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

if ((Test-Path -Path ((Get-Item -Path ".\").FullName + "\gradle\wrapper\gradle-wrapper.jar"))) {
    Write-Host "The Gradle Wrapper has already been downloaded.";
    exit
}

# HTTPS is not used because it gets complicated with Powershell and .Net framework versions depending on Windows versions
Invoke-WebRequest -outf gradle\wrapper\gradle-wrapper.jar https://github.com/gradle/gradle/raw/v4.5.1/gradle/wrapper/gradle-wrapper.jar
