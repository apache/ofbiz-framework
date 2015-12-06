#!/bin/sh

# check trunk for updates finally no longer done, restart each morning at 3

cd /home/ofbizDemo/trunk
#svn st -u | grep '*'

#if [ $? = 0 ]; then
    svn up
    ./ant stop
    ./ant clean-all
    ./ant load-demo
    ./ant svninfo
    #./ant start  > console.log
    ./ant start-batch-secure
#fi

# check branch for updates

cd /home/ofbizDemo/branch13.7
#svn st -u | grep '*'

#if [ $? = 0 ]; then
    svn up
    ./ant stop -Dportoffset=10000
    ./ant clean-all
    ./ant load-demo
    ./ant svninfo
#    #./ant start -Dportoffset=10000 > console.log
    ./ant start-batch-secure -Dportoffset=10000
#fi

cd /home/ofbizDemo/branch12.4
#svn st -u | grep '*'

#if [ $? = 0 ]; then
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
#fi
