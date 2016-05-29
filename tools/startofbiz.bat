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

%~d0
set OFBIZ_HOME=%~p0..\

rem ### Console log file
rem set OFBIZ_LOG=runtime\logs\console.log

rem ### Delete the last log
rem del %OFBIZ_LOG%

rem ###VM args block ####################################################
rem set MEMIF=-Xms128M -Xmx512M
rem # RMI settings
rem set DEBUG=-Dsun.rmi.server.exceptionTrace=true
rem # Automatic IP address for Windows
rem ipconfig | find "IP." | find /v "::" | find /v "0.0.0.0" > tmp.tmp
rem for /f "tokens=2* delims=:" %%a in (tmp.tmp)  do for %%b IN (%%a) do set IPADDR=%%b
rem del tmp.tmp
rem set RMIIF=-Djava.rmi.server.hostname=%IPADDR%
rem # Not needed anymore, for history
rem set MISC=-Duser.language=en
rem set VMARGS=%MEMIF% %MISC% %DEBUG% %RMIIF%
rem ####################################################################

rem ### Worldpay Config
rem set VMARGS=-Xbootclasspath/p:applications\accounting\lib\cryptix.jar %VMARGS%


rem ### Different ways of launching OFBiz ##############################
rem ### start ofbiz with previous set VMARGS
rem "%JAVA_HOME%\bin\java" %VMARGS% -jar ofbiz.jar > %OFBIZ_LOG%

rem ### This one is for more of a debugging mode
rem "%JAVA_HOME%\bin\java" -Xms128M -Xmx512M -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar ofbiz.jar > runtime\logs\console.log

rem ### Simple easy to read line
cd %OFBIZ_HOME%
echo on
"%JAVA_HOME%\bin\java" -Xms128M -Xmx512M -jar ofbiz.jar
echo off
rem ### If you would prefer the console output to be logged rather than displayed switch out the above line for this one
rem "%JAVA_HOME%\bin\java" -Xms128M -Xmx512M -jar ofbiz.jar > runtime\logs\console.log

