#!/bin/sh
#####################################################################
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
#####################################################################

export OFBIZ_HOME=../..
export CP=./build/lib/ofbiz-testtools.jar
export CP=$CP:./lib/selenium-java-client-driver.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/httpclient-4.0.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/jdom-1.1.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/scripting/jython-nooro.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/junit.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/commons/commons-lang-2.3.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/log4j-1.2.15.jar
export CP=$CP:$OFBIZ_HOME/framework/base/lib/javolution-5.4.3.jar
export CP=$CP:$OFBIZ_HOME/framework/base/build/lib/ofbiz-base.jar

echo $CP

if [ -f "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

"$JAVA" -Dselenium.config=./config/seleniumXml.properties  -cp $CP org.ofbiz.testtools.seleniumxml.SeleniumXml $1;





