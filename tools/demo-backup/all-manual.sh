#!/bin/sh

cd /home/ofbizDemo/trunk
    svn up
    ./gradlew terminateOfbiz
    ./gradlew cleanAll 
    ./gradlew loadDefault 
    ./gradlew svnInfoFooter 
    ./gradlew ofbizBackground

cd /home/ofbizDemo/branch13.7
    svn up
    ./ant stop -Dportoffset=10000
    ./ant clean-all
    ./ant load-demo
    ./ant svninfo
    ./ant start-batch-secure -Dportoffset=10000


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

cd /home/ofbizDemo