#!/bin/sh

cd /home/ofbizDemo/branch12.4
svn up
tools/stopofbiz.sh
sleep 10
tools/stopofbiz.sh
sleep 10
tools/stopofbiz.sh
sleep 10
./ant clean-all
./ant load-demo
./ant svninfo
sleep 10
nohup tools/startofbiz-secure.sh &
