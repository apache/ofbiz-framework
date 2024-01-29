
## Setting up Development environment

Install Postgresql Database server, version 16.1

Create a DB user
`CREATE USER zcpdbuser WITH ENCRYPTED PASSWORD 'zcpjieHJikLeg9ZpDexz3FM8gh' CREATEDB;`

Create 3 databases with the newly created user:

`CREATE DATABASE zcp WITH OWNER = zcpdbuser;`
`CREATE DATABASE zcptenant WITH OWNER = zcpdbuser;`
`CREATE DATABASE zcpolap WITH OWNER = zcpdbuser;`


#### Build and Load initial seed data

Build the source code

`./gradlew build`

Load seed and initial seed data

`./gradlew -x test "ofbiz --load-data readers=seed,seed-initial,ext,dev"`

Start server

`./gradlew -x test "ofbiz --start"`
