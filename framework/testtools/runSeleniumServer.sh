#!/bin/sh
# location of java executable
if [ -f "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

"$JAVA" -jar ../../framework/testtools/lib/selenium-server.jar  -singleWindow  -trustAllSSLCertificates -timeout 240
exit 0

