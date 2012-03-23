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

rem Hot-deploy synchronisation script, update existing components, check out new ones
rem This is useful when you keep a link with Apache OFBiz repository and have your own repository for hot-deploy

setlocal EnableDelayedExpansion

rem SVN path of the hot-deploy folder, here is just a suggestion, to be adapted to you real repository
set SVN_PATH=.../implementation/trunk/ofbiz/hot-deploy/

rem Go to (local) working copy of the hot-deploy folder
pushd ../hot-deploy

rem Get all files and directories of hot-deploy folder via SVN
for /f "tokens=*" %%i in ('svn list !SVN_PATH!') do (
    rem %%i is now  a commponent name
    set DIRNAME=%%i
    
    rem Remove trailing slash if necessary
    if "!DIRNAME:~-1!" == "/" set DIRNAME=!DIRNAME:~0,-1!
    
    rem Check if directory already exists (i.e. if already checked out)
    if exist !DIRNAME! (
        echo Update file or directory !DIRNAME!...
        svn update !DIRNAME!
    ) else (
        echo Checkout directory !DIRNAME!...
        svn checkout !SVN_PATH!!DIRNAME! !DIRNAME!
    )
    
    rem Unset variable
    set DIRNAME=
)

rem Unset variable
set SVN_PATH=
popd

pause