#!/bin/sh

#################################
# JBoss Deployment              #
#                               #
# Copy to ofbiz.ear directory   #
# in the JBoss deploy directory #
#                               #
#################################
DERBY_VERSION="10.4.1.3"

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

# install derby
if [ ! -f "../../lib/derby-$DERBY_VERSION.jar" ]; then
  cp "${ofbizHome}/framework/entity/lib/jdbc/derby-$DERBY_VERSION.jar" ../../lib/
  echo "installed derby-$DERBY_VERSION"
fi

# install derby plugin
if [ ! -f "../../lib/derby-plugin.jar" ]; then
  cp ../../../../docs/examples/varia/derby-plugin.jar ../../lib/
  echo "installed derby-plugin.jar"
fi

# install derby datasource
if [ ! -f "../derby-ds.xml" ]; then
  cp ${ofbizHome}/framework/appserver/templates/jboss422/patches/derby*.xml ..
  echo "derby datasource configuration installed"
fi

# configure the jboss entity engine (patch) configuration
if [ ! -f "${ofbizHome}/framework/entity/config/entityengine-jboss422.xml" ]; then
  patch -i ${ofbizHome}/framework/appserver/templates/jboss422/patches/jboss-ee-cfg.patch -o ${ofbizHome}/framework/entity/config/entityengine-jboss422.xml ${ofbizHome}/framework/entity/config/entityengine.xml
  echo "created entityengine-jboss.xml"
fi

# move entityengine.xml, log4j.xml and jndi.properties
if [ -f "${ofbizHome}/framework/entity/config/entityengine-jboss422.xml" ]; then
  mv ${ofbizHome}/framework/entity/config/entityengine.xml ${ofbizHome}/framework/entity/config/entityengine.xml.jbak
  mv ${ofbizHome}/framework/entity/config/entityengine-jboss422.xml ${ofbizHome}/framework/entity/config/entityengine.xml
  echo "moved entityengine.xml"
fi
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
<#if (!jar.contains("j2eespec") && !jar.contains("geronimo") && !jar.contains("catalina") && !jar.contains("mx4j") && !jar.contains("derby-") && !jar.contains("commons-logging") &&!jar.contains("commons-collections") &&!jar.contains("commons-codec") && !jar.contains("commons-el") && !jar.contains("avalon-framework") && !jar.contains("bsh") && !jar.contains("bsf") && !jar.contains("antlr") && !jar.contains("mail-1.4.jar"))>
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

# revert entityengine.xml log4j.xml and jndi.properties
if [ -f "${ofbizHome}/framework/entity/config/entityengine.xml.jbak" ]; then
  mv ${ofbizHome}/framework/entity/config/entityengine.xml ${ofbizHome}/framework/entity/config/entityengine-jboss422.xml
  mv ${ofbizHome}/framework/entity/config/entityengine.xml.jbak ${ofbizHome}/framework/entity/config/entityengine.xml
  echo "fixed entityengine.xml"
fi
if [ -f "${ofbizHome}/framework/base/config/_log4j.xml.bak" ]; then
  mv ${ofbizHome}/framework/base/config/_log4j.xml.bak ${ofbizHome}/framework/base/config/log4j.xml
  echo "fixed ${ofbizHome}/framework/base/config/log4j.xml"
fi
if [ -f "${ofbizHome}/framework/base/config/_jndi.properties.bak" ]; then
  mv ${ofbizHome}/framework/base/config/_jndi.properties.bak ${ofbizHome}/framework/base/config/jndi.properties
  echo "fixed ${ofbizHome}/framework/base/config/jndi.properties"
fi

# setup the OFBIZ_HOME by updating run.conf
if [ ! -f "../../../../bin/run.conf.obak" ]; then
  mv ../../../../bin/run.conf ../../../../bin/run.conf.obak
  cp ${ofbizHome}/setup/jboss422/run.conf ../../../../bin/run.conf
  echo "modifed bin/run.conf (with backup)"
fi

echo "\n"
