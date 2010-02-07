rem #####################################################################
rem Licensed to the Apache Software Foundation (ASF) under one
rem or more contributor license agreements.  See the NOTICE file
rem distributed with this work for additional information
rem regarding copyright ownership.  The ASF licenses this file
rem to you under the Apache License, Version 2.0 (the
rem "License"); you may not use this file except in compliance
rem with the License.  You may obtain a copy of the License at
rem
rem http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing,
rem software distributed under the License is distributed on an
rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem KIND, either express or implied.  See the License for the
rem specific language governing permissions and limitations
rem under the License.
rem #####################################################################

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
