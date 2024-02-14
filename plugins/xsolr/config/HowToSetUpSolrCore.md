#Setting up SOLR Core

This document will explain how to set up SOLR core ZCPCORE on your SOlR instance.

Prerequisites
SOLR v 8.6.3, download from [here](https://archive.apache.org/dist/lucene/solr/8.6.3/)

Copy 

`{PROJECT-DIR}\plugins\xsolr\config\solr\ZCPCORE`

to 

`{SOLR-HOME}\solr-8.11.2\server\solr\`

or (If you have the environment variable set up)
`%SOLR_HOME%`

Start solr server

`{SOLR-HOME}\solr-8.11.2\bin\ start.cmd`

Test connectivity at 
[http://localhost:8983/solr](http://localhost:8983/solr)

Check to make sure your core exists at
[Core admin]()http://localhost:8983/solr/#/~cores/)

If it does not click on `Add Core` and put `ZCPCORE` for the `name` and `instanceDir` fields


Stopping solr server

`{SOLR-HOME}\solr-8.11.2\bin\ .\start.cmd stop -all`


# Index documents
Run all solr indexing services to index your documents in SOLR


## Installing SOLR v 8.6.3
`sudo su`

`cd /tmp`

`wget https://archive.apache.org/dist/lucene/solr/8.6.3/solr-8.6.3.tgz`

`tar xzf solr-8.6.3.tgz solr-8.6.3/bin/install_solr_service.sh --strip-components=2`

`./install_solr_service.sh solr-8.6.3.tgz`

### validate the status
`service solr status`

### Copy configuration
`cd /var/solr/data`

`cp -r /home/ofbiz/app/mmo-ofbiz/plugins/xsolr/config/solr/ZCPCORE .`

`chmod 777 -R ZCPCORE`

Remove temporary files for solr from /tmp
`cd /tmp`

`rm install_solr_service.sh solr-8.6.3.tgz`

Make sure tunnelling is done for Port 38080 and open a new ssh connection, then hit the url below to create the core

http://localhost:8983/solr/admin/cores?action=CREATE&name=ZCPCORE&instanceDir=ZCPCORE


### Updating solr schema

To update solr schema copy the file

`managed-schema`

from

`solr/ZCPCORE/conf/`

to 

`<SOLR-HOME>/server/solr/ZCPCORE/conf/`

NOTE: Replace SOLR-HOME with your actual path

Go to 

`http://localhost:8983/solr/#/~cores/ZCPCORE`

Select the `ZCPCORE` then click on `Reload` to reload the schema