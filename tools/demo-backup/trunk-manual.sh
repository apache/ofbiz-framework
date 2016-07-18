#!/bin/sh

cd /home/ofbizDemo/trunk && svn up
./gradlew "ofbiz --shutdown" 
./gradlew cleanAll 
./gradlew loadDefault 
./gradlew svnInfoFooter 
./gradlew "ofbizBackgroundSecure --start" 
