#!/bin/sh
export OFBIZ_HOME=../..
export CP=./build/lib/ofbiz-testtools.jar
export CP=$CP:./lib/selenium-java-client-driver.jar
export CP=$CP:$OFBIZ_HOME/framework/base/build/lib/ofbiz-base.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/httpclient-4.0.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/jdom-1.1.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/scripting/jython-nooro.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/junit.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/commons/commons-lang-2.3.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/log4j-1.2.15.jar

# echo $CP

# For Example:
# convertSeleniumIDE.bat <recorded_script> <converted_script>

if [ -f "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

tempFile=`mktemp tempFileXXX.html` 
sed '2d' $1 > $tempFile

"$JAVA" -cp $CP org.ofbiz.testtools.seleniumxml.SeleniumIDEConverter "$tempFile" "$2"
rm $tempFile
exit 0
