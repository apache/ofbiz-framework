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

rem Syntax: pullAllPluginsSource

rem Whatever, create anew
if EXIST plugins\ (
    cmd /c rd  /s/q plugins
)

rem Get the branch used in framework
git branch --show-current > temp.txt
set /p branch=<temp.txt
del temp.txt

git clone --depth 1 --single-branch --branch %branch% https://github.com/apache/ofbiz-plugins.git plugins

rem Remove .git, in this case it's useless information
cd plugins
cmd /c rd /s/q .git
cd ..
