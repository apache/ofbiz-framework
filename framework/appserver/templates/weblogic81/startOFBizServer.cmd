@rem ################################################################
@rem Licensed to the Apache Software Foundation (ASF) under one
@rem or more contributor license agreements.  See the NOTICE file
@rem distributed with this work for additional information
@rem regarding copyright ownership.  The ASF licenses this file
@rem to you under the Apache License, Version 2.0 (the
@rem "License"); you may not use this file except in compliance
@rem with the License.  You may obtain a copy of the License at
@rem 
@rem http://www.apache.org/licenses/LICENSE-2.0
@rem 
@rem Unless required by applicable law or agreed to in writing,
@rem software distributed under the License is distributed on an
@rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@rem KIND, either express or implied.  See the License for the
@rem specific language governing permissions and limitations
@rem under the License.
@rem ###################################################################
@rem This script is used to start WebLogic Server.
@rem
@rem To create your own start script for your domain, you can initialize the 
@rem environment by calling %WL_HOME%/common/bin/commEnv.cmd. 
@rem
@rem commEnv.cmd initializes following variables: 
@rem WL_HOME        - The root directory of your WebLogic installation.
@rem JAVA_HOME      - Location of the version of Java used to start WebLogic
@rem                  Server. 
@rem JAVA_VENDOR    - Vendor of the JVM (i.e. BEA, HP, IBM, Sun, etc.)
@rem PATH           - JDK and WebLogic directories are added to system path.
@rem WEBLOGIC_CLASSPATH 
@rem                - Classpath needed to start WebLogic Server.
@rem JAVA_VM        - The java arg specifying the VM to run.  (i.e.
@rem                  -server, -hotspot, etc.)
@rem MEM_ARGS       - The variable to override the standard memory arguments
@rem                  passed to java.
@rem POINTBASE_HOME - Point Base home directory.
@rem POINTBASE_CLASSPATH 
@rem                - Classpath needed to start PointBase.
@rem Other variables used in this script include:
@rem SERVER_NAME    - Name of the weblogic server.
@rem ADMIN_URL      - If this variable is set, the server started will be a
@rem                  managed server, and will look to the url specified (i.e.
@rem                  http://localhost:7001) as the admin server.
@rem WLS_USER       - cleartext user for server startup.
@rem WLS_PW         - cleartext password for server startup.
@rem PRODUCTION_MODE - Set to true for production mode servers, false for
@rem                  development mode.
@rem JAVA_OPTIONS   - Java command-line options for running the server. (These
@rem                  will be tagged on to the end of the JAVA_VM and 
@rem                  MEM_ARGS)
@rem
@rem If you want to start the examples server using the JRockit JVM, edit
@rem %WL_HOME%/common/bin/commEnv.cmd to specify the correct values for
@rem JAVA_HOME and JAVA_VENDOR.
@rem
@rem For additional information, refer to the WebLogic Server Administration 
@rem Guide (http://e-docs.bea.com/wls/docs81/adminguide/startstop.html).
@rem *************************************************************************

echo off
SETLOCAL

set WL_HOME=C:\bea\weblogic81
@rem Set Production Mode.  When this is set to true, the server starts up in
@rem production mode.  When set to false, the server starts up in development
@rem mode.  If it is not set, it will default to false.
set PRODUCTION_MODE=

@rem Set JAVA_VENDOR to java virtual machine you want to run on server side.
set JAVA_VENDOR=Sun

@rem Set JAVA_HOME to java virtual machine you want to run on server side.
set JAVA_HOME=C:\bea\jdk141_05

call "%WL_HOME%\common\bin\commEnv.cmd"

@rem Set SERVER_NAME to the name of the server you wish to start up.
set SERVER_NAME=examplesServer


@rem Set JAVA_OPTIONS to the java flags you want to pass to the vm. i.e.: 
@rem set JAVA_OPTIONS=-Dweblogic.attribute=value -Djava.attribute=value
set JAVA_OPTIONS=

@rem Set MEM_ARGS to the memory args you want to pass to java.  For instance:
@rem if "%JAVA_VENDOR%"=="BEA" set MEM_ARGS=-Xms32m -Xmx200m

@rem *************************************************************************
@rem PointBase and examples domain specific configuration
set SAMPLES_HOME=C:\bea\weblogic81\samples
set EXAMPLES_CONFIG=%SAMPLES_HOME%\domains\examples

set EXAMPLES_HOME=%SAMPLES_HOME%\server\examples
set EXAMPLES_BUILD=%EXAMPLES_HOME%\build

set APPLICATIONS=%EXAMPLES_CONFIG%\applications
set CLIENT_CLASSES=%EXAMPLES_BUILD%\clientclasses
set SERVER_CLASSES=%EXAMPLES_BUILD%\serverclasses
set COMMON_CLASSES=%EXAMPLES_BUILD%\common
set EX_WEBAPP_CLASSES=%EXAMPLES_BUILD%\examplesWebApp\WEB-INF\classes

@rem Add PointBase classes to the classpath, so we can start the examples 
@rem database.  Also add the examples directories specified above to the 
@rem classpath to be picked up by WebLogic Server.
set CLASSPATH=C:\bea\weblogic81\server\lib\webservices.jar;%POINTBASE_CLASSPATH%;%CLIENT_CLASSES%;%SERVER_CLASSES%;%COMMON_CLASSES%;%CLIENT_CLASSES%\utils_common.jar

@rem Start PointBase 4.4.
start "PointBase" cmd /c ""%JAVA_HOME%\bin\java" com.pointbase.net.netServer /port:9092 /d:3 /pointbase.ini="pointbase.ini"" > "pointbase.log" 2>&1
@rem *************************************************************************

@rem Call WebLogic Server

set CLASSPATH=%WEBLOGIC_CLASSPATH%;%CLASSPATH%

@rem -=-=-=-=-=-=-=-=- Start OFBiz Classpath Here -=-=-=-=-=-=-=-=-
<#list classpathDirs as dir>
set CLASSPATH=%CLASSPATH%;${dir}
</#list>
<#list classpathJars as jar>
set CLASSPATH=%CLASSPATH%;${jar}
</#list>
@rem -=-=-=-=-=-=-=-=- End OFBiz Classpath Here -=-=-=-=-=-=-=-=-

"%JAVA_HOME%\bin\java" %JAVA_VM% %MEM_ARGS% %JAVA_OPTIONS% -Dweblogic.Name=%SERVER_NAME% -Dweblogic.ProductionModeEnabled=%PRODUCTION_MODE% -Djava.security.policy="%WL_HOME%\server\lib\weblogic.policy" -Dofbiz.home="${env.get("ofbiz.home")}" weblogic.Server

ENDLOCAL
