#!/bin/sh 

###############################################################################
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
###############################################################################

# Check args.

if [ $# -ne 2 ]; then
  echo "Input required ..."
  echo "Syntax: $0 <glassfish-home> <glassfish-domain-directory-path>"
  exit 1
fi

GLASSFISH_HOME=$1
DOMAIN_HOME=$2
WORK_DIR=$PWD

# Setup JavaDB JDBC driver.
cd ../ 
cp $GLASSFISH_HOME/javadb/lib/derbyclient.jar ${ofbizHome}/framework/entity/lib/jdbc/derbyclient.jar
echo "Installed JDBC driver for JavaDB"

# Generate seed and demo data.
cd ${ofbizHome}
cp framework/entity/config/entityengine.xml framework/entity/config/entityengine.xml_orig
patch framework/entity/config/entityengine.xml ${targetDirectory}/entityengine.xml.patch 
if [ $? ne 0] ; then
 echo "Patching entityengine.xml failed.."
 exit 1;
fi

 # Start JavaDB server.
 cd $GLASSFISH_HOME
 export DERBY_HOME=$PWD/javadb
 export DERBY_OPTS=-Dderby.system.home=$PWD/domains/domain1/config
 sh javadb/bin/startNetworkServer &
echo "Started JavaDB server"

 # Load the seed and demo data.
 cd ${ofbizHome} 
 ./ant load-demo
echo "Loaded seed and demo data"

 # Restore entityengine.xml
 mv framework/entity/config/entityengine.xml_orig framework/entity/config/entityengine.xml


# Setup work area (where the WARs and EAR to be built.
cp ${targetDirectory}/deploy.sh $WORK_DIR  

# Run the deployment script.
echo "Running the deployment script"
cd $WORK_DIR  
chmod +x deploy.sh  
./deploy.sh $GLASSFISH_HOME $DOMAIN_HOME > run.log  


# Finally start glassfish server.
#echo "Starting glassfish server .."
#cd $GLASSFISH_HOME
#bin/asadmin start-domain &

cd ..
echo "done"
