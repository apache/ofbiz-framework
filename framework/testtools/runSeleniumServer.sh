#!/bin/sh
# location of java executable
if [ -f "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

#"$JAVA" -jar ../../framework/testtools/lib/selenium-server.jar -firefoxProfileTemplate ./config/firefox_profile -singleWindow  -trustAllSSLCertificates -timeout 240
"$JAVA" -jar ../../framework/testtools/lib/selenium-server.jar -singleWindow -timeout 240
