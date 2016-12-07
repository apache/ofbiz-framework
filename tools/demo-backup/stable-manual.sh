#!/bin/sh

cd /home/ofbizDemo/branch16.11
svn up
./gradlew "ofbizBackground --stop --portoffset 10000"
./gradlew cleanAll
./gradlew "ofbiz --load-data --portoffset 10000"
./gradlew svnInfoFooter
./gradlew "ofbizBackground --start --portoffset 10000"
