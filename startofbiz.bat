
"%JAVA_HOME%\bin\java" -Xms256M -Xmx512M -jar ofbiz.jar > framework\logs\console.log

REM This one is for more of a debugging mode
REM "%JAVA_HOME%\bin\java" -Xms256M -Xmx512M -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar ofbiz.jar > framework\logs\console.log

