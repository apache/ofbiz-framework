Three instances of OFBiz run on the OFBiz demo VM2 at https://ofbiz-vm2.apache.org.

* trunk: the trunk version
* stable: the last stable version (currently 16.11)
* old: the previous stable version (currently 13.07)
 
This is the second instance of VM we use hence the 2 in its domain name.
The root of https://ofbiz-vm2.apache.org is the so called bigfiles directory 
which is actually at /home/ofbizDemo/big-files

We own 3 Apache sub domains

* https://demo-trunk.ofbiz.apache.org
* https://demo-stable.ofbiz.apache.org
* https://demo-old.ofbiz.apache.org

Trunk and stable use a Let's Encrypt certificate
Because of technical issues we currently use a self-signed certificate for the old version

The Puppet configuration is at 
https://github.com/apache/infrastructure-puppet/blob/deployment/data/nodes/ofbiz-vm2.apache.org.yaml


>_Note_: **Only run the ofbiz demos using the 'ofbizDemo' user, never run as root.** 
    
    To do this sudo to the ofbizDemo user:

    sudo -s -u ofbizDemo -H

    sudo uses OTP (One Time Password), so you not only need to be registered as a sudoer (ask Infra) but also to use a tool like Donkey on Ubuntu (jleroux: I use that) to generate the OTP
    Then you can start/stop as required.

    To check if the demos are being run as the ofbizDemo user:

    ps aux | grep ofbizDemo

    The first column on the left tell you the username the demo is
    being run as - it should say ofbizDemo (UID) or 9997 (GID) !

    Type 'exit' to exit the ofbizDemo user and return to your normal user.

Also note that the demos are usually updated and started/stopped
automatically using the check-svn-update.sh script in this
directory, it is run by an ofbiz cron job every 24 hours at 3 AM.
You should therefore only need to start/stop manually if there is
a problem.

If you want to restart only a single instance you can respectively use

trunk-manual-nicely.sh
stable-manual-nicely.sh
old-manual-nicely.sh

