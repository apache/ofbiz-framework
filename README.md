
## Setting up Development environment

Install Postgresql Database server, version 16.1

Create a DB user
- `CREATE USER zcpdbuser WITH ENCRYPTED PASSWORD 'zcpjieHJikLeg9ZpDexz3FM8gh' CREATEDB;`

Create 3 databases with the newly created user:

- `CREATE DATABASE zcp WITH OWNER = zcpdbuser;`
- `CREATE DATABASE zcptenant WITH OWNER = zcpdbuser;`
- `CREATE DATABASE zcpolap WITH OWNER = zcpdbuser;`


## Install and Configure Solr

Install Solr 8.11.2

Create solr core with name ZCPCORE

`solr create -c ZCPCORE`


#### Build and Load initial seed data

Build the source code

`./gradlew build`

Load seed and initial seed data

`./gradlew -x test "ofbiz --load-data readers=seed,seed-initial,ext,dev"`

Start server

`./gradlew -x test "ofbiz --start"`


Starts OFBiz in remote debug mode and waits for debugger
or IDEs to connect on port **5005**

`./gradlew "ofbizDebug --start"`


## Admin Account Details

username: app-super-admin
password: cAH38PsURvpE@!*w
