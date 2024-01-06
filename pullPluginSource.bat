@echo off
rem #####################################################################
rem Licensed to the Apache Software Foundation (ASF) under one
rem or more contributor license agreements.  See the NOTICE file
rem distributed with this work for additional information
rem regarding copyright ownership.  The ASF licenses this file
rem to you under the Apache License, Version 2.0 (the
rem "License"); you may not use this file except in compliance
rem with the License.  You may obtain a copy of the License at
rem
rem http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing,
rem software distributed under the License is distributed on an
rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem KIND, either express or implied.  See the License for the
rem specific language governing permissions and limitations
rem under the License.
rem #####################################################################

rem Syntax: eg pullPluginSource bi

rem Remove plugins dir in case of all plugins present (no .git)
if EXIST plugins\ (
    if NOT EXIST plugins\.git\ (
        cmd /c rd/s/q plugins
    )
)

rem Get the branch used in framework
git branch --show-current > temp.txt
set /p branch=<temp.txt
del temp.txt

rem Clone and set if new else simply add
if NOT EXIST plugins\.git\ (
    git clone --depth 1 --sparse --single-branch --branch %branch% https://github.com/apache/ofbiz-plugins.git plugins
    cd plugins
    git sparse-checkout set %1
) else (
    cd plugins
    git sparse-checkout add %1
)

cd ..