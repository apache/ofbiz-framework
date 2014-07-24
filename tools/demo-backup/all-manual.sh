#!/bin/sh

cd /home/ofbizDemo/trunk
    svn up
    ./ant stop
    ./ant clean-all
    ./ant load-demo
    ./ant svninfo
    ./ant start-batch

cd /home/ofbizDemo/branch13.7
    svn up
    ./ant stop -Dportoffset=10000
    ./ant clean-all
    ./ant load-demo
    ./ant svninfo
    ./ant start-batch -Dportoffset=10000


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
    nohup tools/startofbiz.sh &

cd /home/ofbizDemo