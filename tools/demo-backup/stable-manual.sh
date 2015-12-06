#!/bin/sh

cd /home/ofbizDemo/branch13.7
svn up
./ant stop -Dportoffset=10000
./ant clean-all
./ant load-demo
./ant svninfo
./ant start-batch-secure -Dportoffset=10000
