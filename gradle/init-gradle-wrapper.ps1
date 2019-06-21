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

if (! (Test-Path -Path ((Get-Item -Path ".\").FullName + "\gradle\"))) {
    Write-Host "Location seems to be incorrect, please run the 'sh gradle/init-gradle-wrapper.ps1' script from Apache OFBiz root.";
    exit
}

# This uses  PowerShell Invoke-WebRequest command (aliased as wget here)
# https is not used because we don't want users to be asked for a credential (not sure about that, maybe https is OK)
wget -outf gradle\wrapper\gradle-wrapper.jar https://svn.apache.org/repos/asf/ofbiz/tools/Buildbot/Gradle/Wrapper/trunk/gradle-wrapper.jar
wget -outf gradle\wrapper\gradle-wrapper.properties https://svn.apache.org/repos/asf/ofbiz/tools/Buildbot/Gradle/Wrapper/trunk/gradle-wrapper.properties
