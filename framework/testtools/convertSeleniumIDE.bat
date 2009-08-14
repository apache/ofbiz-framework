set OFBIZ_HOME=../..
set CP=./build/lib/ofbiz-testtools.jar
set CP=%CP%;./lib/httpclient-4.0-beta1.jar
set CP=%CP%;./lib/selenium-java-client-driver.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/jdom-1.1.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/scripting/jython-nooro.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/junit.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/commons/commons-lang-2.3.jar
set CP=%CP%;%OFBIZ_HOME%/framework/base/lib/log4j-1.2.15.jar

rem echo %CP%

rem For Example:
rem convertSeleniumIDE.bat <recorded_script> <converted_script>

"%JAVA_HOME%/bin/java.exe" -cp %CP% org.ofbiz.testtools.seleniumxml.SeleniumIDEConverter %1 %2
