#!/bin/sh

cd /home/ofbizDemo/trunk
svn up
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
./ant stop
./ant clean-all
./ant load-demo
./ant svninfo
./ant start-batch

