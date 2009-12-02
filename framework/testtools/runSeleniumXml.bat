rem set JAVA_HOME=%JDK_15%
set OFBIZ_HOME=../..
set CP=./build/lib/ofbiz-testtools.jar
set CP=%CP%;./lib/selenium-java-client-driver.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/httpclient-4.0.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/jdom-1.1.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/scripting/jython-nooro.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/junit.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/commons/commons-lang-2.3.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/log4j-1.2.15.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/javolution-5.2.3.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/build/lib/ofbiz-base.jar

echo %CP%

"%JAVA_HOME%/bin/java.exe" -Dselenium.config=./config/seleniumXml.properties -cp %CP% org.ofbiz.testtools.seleniumxml.SeleniumXml %1
