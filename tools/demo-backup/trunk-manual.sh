#!/bin/sh

cd /home/ofbizDemo/trunk
svn up
./ant stop
./ant clean-all
./ant load-demo
./ant svninfo
./ant start-batch-secure

