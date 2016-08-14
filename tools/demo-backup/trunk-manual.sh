#!/bin/sh

cd /home/ofbizDemo/trunk && svn up
./gradlew terminateOfbiz
./gradlew cleanAll 
./gradlew loadDefault 
./gradlew svnInfoFooter 
./gradlew ofbizBackground 
