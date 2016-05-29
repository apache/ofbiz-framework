@ECHO OFF
REM #####################################################################
REM # Licensed to the Apache Software Foundation (ASF) under one
REM # or more contributor license agreements.  See the NOTICE file
REM # distributed with this work for additional information
REM # regarding copyright ownership.  The ASF licenses this file
REM # to you under the Apache License, Version 2.0 (the
REM # "License"); you may not use this file except in compliance
REM # with the License.  You may obtain a copy of the License at
REM #
REM # http://www.apache.org/licenses/LICENSE-2.0
REM #
REM # Unless required by applicable law or agreed to in writing,
REM # software distributed under the License is distributed on an
REM # "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM # KIND, either express or implied.  See the License for the
REM # specific language governing permissions and limitations
REM # under the License.
REM #####################################################################

IF DEFINED JAVA_HOME (
  SET JAVA="%JAVA_HOME%\bin\java"
) ELSE (
  SET JAVA="java"
)

SET TOP=%~dp0
SET LAUNCHER_JAR=
SET BASE_LIB=%TOP%framework\base\lib
SET ANT_LIB=%BASE_LIB%\ant
FOR %%G IN (%BASE_LIB%\ant-*-ant-launcher.jar) DO SET LAUNCHER_JAR=%%G
REM ECHO %LAUNCHER_JAR%
IF [%LAUNCHER_JAR%] == [] (
  ECHO "Couldn't find ant-launcher.jar"
) ELSE (
  ECHO %JAVA% -jar "%LAUNCHER_JAR%" -lib "%ANT_LIB%" %1 %2 %3 %4 %5 %6
  %JAVA% -jar "%LAUNCHER_JAR%" -lib "%ANT_LIB%" %1 %2 %3 %4 %5 %6
)

