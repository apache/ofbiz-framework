#!/bin/sh

#################################
# JBoss Deployment              #
#                               #
# Copy to ofbiz.ear directory   #
# in the JBoss deploy directory #
#                               #
#################################

if [ -f "./META-INF/application.xml" ]; then
  rm -rf META-INF
  echo "removed META-INF"
fi
if [ -f "./lib/ofbiz-base.jar" ]; then
  rm -rf lib
  echo "removed libs"
  rm *.war
  echo "removed wars"
fi

# move log4j.xml and jndi.properties
if [ -f "${ofbizHome}/framework/base/config/log4j.xml" ]; then
  mv ${ofbizHome}/framework/base/config/log4j.xml ${ofbizHome}/framework/base/config/_log4j.xml.bak
  echo "moved ${ofbizHome}/framework/base/config/log4j.xml"
fi
if [ -f "${ofbizHome}/framework/base/config/jndi.properties" ]; then
  mv ${ofbizHome}/framework/base/config/jndi.properties ${ofbizHome}/framework/base/config/_jndi.properties.bak
  echo "moved ${ofbizHome}/framework/base/config/jndi.properties"
fi

# copy all lib files
mkdir lib
<#list classpathJars as jar>
<#if (!jar.contains("j2eespec") && !jar.contains("geronimo") && !jar.contains("catalina"))>
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

# link the web applications
<#list webApps as webapp>
ln -s ${webapp.getLocation()} .${webapp.getContextRoot()}.war
</#list>
echo "linked webapp directories"

# create the application meta data
mkdir META-INF
cp ${targetDirectory}/application.xml ./META-INF
echo "installed application.xml"

# replace jboss bsh.jar with the ofbiz version
if [ -f "../../lib/bsh.jar" ]; then
  cp ${ofbizHome}/framework/base/lib/scripting/bsh-2.0b4.jar ../../lib/bsh.jar
  echo "updated bsh.jar"
fi

# revert log4j.xml and jndi.properties
if [ -f "${ofbizHome}/framework/base/config/_log4j.xml.bak" ]; then
  mv ${ofbizHome}/framework/base/config/_log4j.xml.bak ${ofbizHome}/framework/base/config/log4j.xml
  echo "fixed ${ofbizHome}/framework/base/config/log4j.xml"
fi
if [ -f "${ofbizHome}/framework/base/config/_jndi.properties.bak" ]; then
  mv ${ofbizHome}/framework/base/config/_jndi.properties.bak ${ofbizHome}/framework/base/config/jndi.properties
  echo "fixed ${ofbizHome}/framework/base/config/jndi.properties"
fi

echo "\n"
echo "make sure run.sh includes -Dofbiz.home=${ofbizHome} as part of the JAVA_OPTS variable"