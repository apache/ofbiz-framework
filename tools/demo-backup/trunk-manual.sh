#!/bin/sh

cd /home/ofbizDemo/trunk
svn up
./gradlew pullAllPluginsSource
./gradlew terminateOfbiz
./gradlew cleanAll
./gradlew loadDefault
./gradlew svnInfoFooter
./gradlew ofbizBackground
