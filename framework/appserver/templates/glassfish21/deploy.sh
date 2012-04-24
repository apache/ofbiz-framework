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


#################################
# Glassfish Deployment          #
#                               #
# Run with Glassfish Home and   #
# domain directory path         #
#################################

# Check args.

if [ $# -ne 2 ]; then
  echo "Input required .."
  echo "Syntax: $0 <glassfish-home> <glassfish-domain-directory-path>"
  exit 1
fi

# Clean up ..

if [ -f "./META-INF/application.xml" ]; then
  rm -rf META-INF
  echo "removed META-INF"
fi
if [ -f "./lib/ofbiz-base.jar" ]; then
  rm -rf lib
  echo "removed libs"
  rm *.war
  echo "removed wars"
  rm ofbiz.ear
  echo "removed ear"
fi

GLASSFISH_HOME=$1
DOMAIN_HOME=$2

# setup JavaDB drivers.
if [ ! -f "${ofbizHome}/framework/entity/lib/jdbc/derbyclient.jar" ]; then
    cp $GLASSFISH_HOME/javadb/lib/derbyclient.jar ${ofbizHome}/framework/entity/lib/jdbc/derbyclient.jar
fi

# setup log4j.xml (copy to domain1/config folder)
if [ ! -f "$DOMAIN_HOME/config/log4j.xml" ]; then
    cp ${ofbizHome}/framework/base/config/log4j.xml $DOMAIN_HOME/config
fi

# setup the OFBIZ_HOME by updating domain.xml
if [ ! -f "$DOMAIN_HOME/config/domain.xml_bak" ]; then
  cp $DOMAIN_HOME/config/domain.xml $DOMAIN_HOME/config/domain.xml_bak
  patch  $DOMAIN_HOME/config/domain.xml ${targetDirectory}/domain.xml.patch
  if [ $? ne 0] ; then
    echo "Patching domain.xml failed.."
    exit 1;
  fi
  echo "patched domain.xml"
fi

# setup entityengine.xml
if [ -f "${ofbizHome}/framework/entity/config/entityengine.xml" ]; then
    cp ${ofbizHome}/framework/entity/config/entityengine.xml ${ofbizHome}/framework/entity/config/entityengine.xml_bak
    patch  framework/entity/config/entityengine.xml ${targetDirectory}/entityengine.xml.patch
    if [ $? ne 0] ; then
      echo "Patching entityengine.xml failed.."
      exit 1;
    fi
    echo "patched entityengine.xml"
fi

# setup url.properties (SSL port)
if [ -f "${ofbizHome}/framework/webapp/config/url.properties" ]; then
    mv ${ofbizHome}/framework/webapp/config/url.properties ${ofbizHome}/framework/webapp/config/url.properties_bak
    cp ${targetDirectory}/url.properties ${ofbizHome}/framework/webapp/config/url.properties
    echo "Copied url.properties"
fi

# copy all lib files
mkdir lib
<#list classpathJars as jar>
<#if (!jar.contains("j2eespec") && !jar.contains("catalina") && !jar.contains("mx4j") && !jar.contains("derby-") && !jar.contains("commons-el") && !jar.contains("avalon-framework") && !jar.contains("mail-1.4.jar"))>
cp ${jar} ./lib
</#if>
</#list>
echo "installed ofbiz libraries"

<#list classpathDirs as dir>
<#if (dir != ofbizHome)>
<#assign jarname = dir.substring(ofbizHome.length()+1)/>
<#assign jarname = jarname.replaceAll("/", ".")/>
jar cvf ./lib/${jarname}.jar -C ${dir} .
</#if>
</#list>
echo "\n\n"
echo "packaged and installed ofbiz configuration directories"

# WAR the web applications
<#list webApps as webapp>
jar -cvf ${webapp.getName()}.war -C ${webapp.getLocation()} .
</#list>
echo "WARred webapp directories"

# create the application meta data
mkdir META-INF
cp ${targetDirectory}/application.xml ./META-INF
echo "installed application.xml"

# build EAR
jar -cvf ofbiz.ear *

# copy the EAR to autodeploy dir
cp ofbiz.ear $DOMAIN_HOME/autodeploy/ofbiz.ear
echo "Copied EAR to autodeploy"

# revert entityengine.xml
if [ -f "${ofbizHome}/framework/entity/config/entityengine.xml_bak" ]; then
    mv ${ofbizHome}/framework/entity/config/entityengine.xml_bak ${ofbizHome}/framework/entity/config/entityengine.xml
    echo "restored entityengine.xml"
fi

# revert url.properties
if [ -f "${ofbizHome}/framework/webapp/config/url.properties_bak" ]; then
    mv ${ofbizHome}/framework/webapp/config/url.properties_bak ${ofbizHome}/framework/webapp/config/url.properties
    echo "restored url.properties"
fi


echo "\n"
