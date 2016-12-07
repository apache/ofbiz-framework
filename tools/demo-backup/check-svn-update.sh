#!/bin/sh

# Restart each morning at 3
# crontab -e
# add : 0 3 * * * nice -n 19 ionice -c2 -n7 /home/ofbizDemo/check-svn-update.sh > /home/ofbizDemo/cronlog-svn-update.log 2>&1

./all-manual.sh