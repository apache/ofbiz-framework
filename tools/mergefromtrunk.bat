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
rem interactive DOS version of mergefromtrunk.sh.
rem to use : launch and pass the trunk version number to merge in release

rem since we have now svn:mergeinfo changing root ("."), we need to update before merging
cd ..
svn up

rem version to merge
set /p version=version to merge :
set /a prevRev=%version% - 1

rem build the comment
echo "Applied fix from trunk for revision: %version%" > comment.tmp
svn log https://svn.apache.org/repos/asf/ofbiz/trunk -r %version% > log.tmp
copy comment.tmp + log.tmp = comment.tmp
del log.tmp
rem keep the comment.tmp file svn ignored. In case of trouble always happier to keep trace.  It will be overidden in next backport.

rem commit the backport to release with comment fom file
echo on
svn merge -r %prevRev%:%version% https://svn.apache.org/repos/asf/ofbiz/trunk
echo off

:menu
echo y) tests
echo n) exit

echo Do you want to run tests (else the commit will be done automatically using the comment grabed from trunk by the merge)?
choice /c:yn
if errorlevel = 2 goto commit
if errorlevel = 1 goto tests

:commit
echo on
svn commit -F comment.tmp
goto exit

:tests
echo on
ant clean-all
ant load-demo
ant run-tests
echo off

echo You can now do the commit by hand if all is OK. The comment grabbed from trunk by the merge is in the file comment.tmp at root

:exit
pause
