#!/bin/sh
#####################################################################
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
# ****************************************************************************
# This script is used to start WebLogic Server.
#
# To create your own start script for your domain, you can initialize the 
# environment by calling $WL_HOME/common/bin/commEnv.sh. It sets following
# variables: 
# WL_HOME        - The root directory of your WebLogic installation.
# JAVA_HOME      - Location of the version of Java used to start WebLogic
#                  Server. 
# JAVA_VENDOR    - Vendor of the JVM (i.e. BEA, HP, IBM, Sun, etc.)
# PATH           - JDK and WebLogic directories are added to system path.
# WEBLOGIC_CLASSPATH 
#                - Classpath needed to start WebLogic Server.
# LD_LIBRARY_PATH, LIBPATH and SHLIB_PATH
#                - Directories to locate native libraries.
# JAVA_VM        - The java arg specifying the VM to run.  (i.e.
#                  -server, -hotspot, etc.)
# MEM_ARGS       - The variable to override the standard memory arguments
#                  passed to java.
# CLASSPATHSEP   - CLASSPATH deliminter.
# PATHSEP        - Path deliminter.
# POINTBASE_HOME - Point Base home directory.
# POINTBASE_CLASSPATH 
#                - Classpath needed to start PointBase.
#
# Other variables used in this script include:
# SERVER_NAME    - Name of the weblogic server.
# ADMIN_URL      - If this variable is set, the server started will be a
#                  managed server, and will look to the url specified (i.e.
#                  http://localhost:7001) as the admin server.
# WLS_USER       - cleartext user for server startup.
# WLS_PW         - cleartext password for server startup.
# PRODUCTION_MODE      - Set to true for production mode servers, false for
#                  development mode.
# JAVA_OPTIONS   - Java command-line options for running the server. (These
#                  will be tagged on to the end of the JAVA_VM and MEM_ARGS)
#
# If you want to start the examples server using the JRockit JVM, edit
# $WL_HOME/common/bin/commEnv.sh to specify the correct values for
# JAVA_HOME and JAVA_VENDOR.
#
# For additional information, refer to the WebLogic Server Administration 
# Guide (http://e-docs.bea.com/wls/docs81/adminguide/startstop.html).
# ****************************************************************************

# set up WL_HOME, the root directory of your WebLogic installation
WL_HOME="C:/bea/weblogic81"

# set up common environment
# Set Production Mode.  When this is set to true, the server starts up in
# production mode.  When set to false, the server starts up in development
# mode.  If it is not set, it will default to false.
PRODUCTION_MODE=""

# Set JAVA_VENDOR to java virtual machine you want to run on server side.
JAVA_VENDOR="Sun"

# Set JAVA_HOME to java virtual machine you want to run on server side.
JAVA_HOME="C:/bea/jdk141_05"

. "$WL_HOME/common/bin/commEnv.sh"

# Set SERVER_NAME to the name of the server you wish to start up.
SERVER_NAME=examplesServer

# Set JAVA_VM to java virtual machine you want to run on server side.
# JAVA_VM=""

# Set JAVA_OPTIONS to the java flags you want to pass to the vm.  If there 
# are more than one, include quotes around them.  For instance: 
# JAVA_OPTIONS="-Dweblogic.attribute=value -Djava.attribute=value"
JAVA_OPTIONS=""


# ****************************************************************************
# PointBase and examples domain specific configuration
# Start PointBase 4.4.  PointBase will be killed when the server is shutdown.
unset POINTBASE_PID


SAMPLES_HOME="C:/bea/weblogic81/samples"
EXAMPLES_CONFIG="$SAMPLES_HOME/domains/examples"

EXAMPLES_HOME="$SAMPLES_HOME/server/examples"
EXAMPLES_BUILD="$EXAMPLES_HOME/build"

APPLICATIONS="$EXAMPLES_CONFIG/applications"
CLIENT_CLASSES="$EXAMPLES_BUILD/clientclasses"
SERVER_CLASSES="$EXAMPLES_BUILD/serverclasses"
COMMON_CLASSES="$EXAMPLES_BUILD/common"
EX_WEBAPP_CLASSES="$EXAMPLES_BUILD/examplesWebApp/WEB-INF/classes"

<#noparse>
CLASSPATH="${WL_HOME}/server/lib/webservices.jar${CLASSPATHSEP}${POINTBASE_CLASSPATH}${CLASSPATHSEP}${CLIENT_CLASSES}${CLASSPATHSEP}${SERVER_CLASSES}${CLASSPATHSEP}${COMMON_CLASSES}${CLASSPATHSEP}${CLIENT_CLASSES}/utils_common.jar"
export CLASSPATH

"$JAVA_HOME/bin/java" ${JAVA_OPTIONS} com.pointbase.net.netServer /port:9092 /d:3 /noconsole /pointbase.ini="pointbase.ini" > "pointbase.log" 2>&1 &
POINTBASE_PID=${!}
</#noparse>

# trap SIGINT, this function is defined in commEnv.sh
trapSIGINT

echo
echo "POINTBASE DATABASE HAS BEEN STARTED, IT'S PID IS $POINTBASE_PID!"
echo
#****************************************************************************


# Reset number of open file descriptors in the current process
# This function is defined in commEnv.sh
resetFd

# Start WebLogic server
CLASSPATH="$WEBLOGIC_CLASSPATH$CLASSPATHSEP$CLASSPATH"

# -=-=-=-=-=-=-=-=- Start OFBiz Classpath Here -=-=-=-=-=-=-=-=-
<#list classpathDirs as dir>
CLASSPATH="$CLASSPATH$CLASSPATHSEP${dir}"
</#list>
<#list classpathJars as jar>
CLASSPATH="$CLASSPATH$CLASSPATHSEP${jar}"
</#list>
# -=-=-=-=-=-=-=-=- End OFBiz Classpath Here -=-=-=-=-=-=-=-=-
 
echo CLASSPATH="$CLASSPATH"
echo
echo PATH="$PATH"
echo
echo "***************************************************"
echo "*  To start WebLogic Server, use a username and   *"
echo "*  password assigned to an admin-level user.  For *"
echo "*  server administration, use the WebLogic Server *"
echo "*  console at http://<hostname>:<port>/console    *"
echo "***************************************************"


"$JAVA_HOME/bin/java" $JAVA_VM $MEM_ARGS $JAVA_OPTIONS         \
  -Dweblogic.Name=$SERVER_NAME                                 \
  -Dweblogic.ProductionModeEnabled=$PRODUCTION_MODE            \
  -Djava.security.policy="$WL_HOME/server/lib/weblogic.policy" \
  -Dofbiz.home="${env.get("ofbiz.home")}"                      \
   weblogic.Server 
  

if [ "$POINTBASE_PID" != "" ]; then
  kill -9 $POINTBASE_PID
  unset POINTBASE_PID
fi
