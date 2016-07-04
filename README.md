Apache OFBiz
============

Welcome to __Apache OFBiz__! A powerful top level Apache software project.
OFBiz is an Enterprise Resource Planning (ERP) System written in Java and
houses a large set of libraries, entities, services and features to run 
all aspects of your business.

For more details about OFBiz please visit the OFBiz Documentation page:

[OFBiz documentation](http://ofbiz.apache.org/documentation.html)

[OFBiz License](http://www.apache.org/licenses/LICENSE-2.0)

System requirements
-------------------

The only requirement to run OFBiz is to have the Java Development Kit (JDK) 
version 8 installed on your system (not just the JRE, but the full JDK) which
you can download from the below link.

[JDK download](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

>_Note_: if you are using eclipse, make sure of running the appropriate eclipse
command `gradlew eclipse`. This command will prepare OFBiz for eclipse with
the correct classpath and settings.

Quick start
-----------

To quickly install and fire-up OFBiz, please follow the below instructions
from the command line at the OFBiz top level directory (folder)

### Prepare OFBiz:

MS Windows:
`gradlew cleanAll loadDefault`

Unix-like OS:
`./gradlew cleanAll loadDefault`

### Start OFBiz:

MS Windows:
`gradlew ofbiz`

Unix-like OS:
`./gradlew ofbiz`

### Visit OFBiz through your browser:

[Order Back Office](https://localhost:8443/ordermgr)

[Accounting Back Office](https://localhost:8443/accounting)

[Administrator interface](https://localhost:8443/webtools)

You can log in with the user __admin__ and password __ofbiz__.

>_Note_: the default configuration uses an embedded Java database
(Apache Derby) and embedded application server components such as
Tomcat, Geronimo (transaction manager), etc.

* * * * * * * * * * * *

Build system syntax
-------------------

All build tasks are executed using the __Gradle__ build system which
is embedded in OFBiz. To execute build tasks go to OFBiz top-level
directory (folder) and execute tasks from there.

### Operating System Syntax

The syntax for tasks differ slightly between windows and Unix-like systems

- __Windows__: `gradlew <tasks-in-here>`

- __Unix-like__: `./gradlew <tasks-in-here>`

For the rest of this document, we will use the windows syntax, if you are on
a Unix-like system, you need to add the `./` to gradlew

### Types of tasks in Gradle

There are two types of tasks designed for OFBiz in Gradle:

- __Standard tasks__: To execute general standard Gradle tasks

- __OFBiz server tasks__: To execute OFBiz startup commands. These
  tasks start with one of the following words:
  - __ofbiz__ : standard server commands
  - __ofbizDebug__ : server commands running in remote debug mode
  - __ofbizSecure__ : server commands running in secure mode using notsoserial API
  - __ofbizBackground__ ; server commands running in a background forked process
  - __ofbizBackgroundSecure__ : server commands running in a background forked
    process in secure mode using the notsoserial API

Tips: 

- OFBiz __server commands__ require __"quoting"__ the 
  commands. For example: `gradlew "ofbiz --help"`

- Shortcuts to task names can be used by writing the
  first letter of every word in a task name. However, you
  cannot use the shortcut form for OFBiz server tasks.
  Example: `gradlew loadAdminUserLogin -PuserLoginId=myadmin` = `gradlew lAUL -PuserLoginId=myadmin`

#### Example standard tasks

`gradlew build`

`gradlew cleanAll loadDefault testIntegration`

#### Example OFBiz server tasks

`gradlew "ofbiz --help"`

`gradlew "ofbizSecure --start --portoffset 10000"`

`gradlew "ofbizDebug --test"`

`gradlew "ofbizBackground --start --portoffset 10000"`

`gradlew "ofbiz --shutdown --portoffset 10000"`

`gradlew ofbiz` (default is --start)

#### Example mixed tasks (standard and OFBiz server)

`gradlew cleanAll loadDefault "ofbiz --start"`

* * * * * * * * * * * *

Quick reference
---------------

You can use the below common list of tasks as a quick reference
for controlling the system. This document uses the windows task
syntax, if you are on a Unix-like system, you need to add the 
`./` to gradlew i.e. `./gradlew`

* * * * * * * * * * * *

### Help tasks

#### List OFBiz server commands

List all available commands to control the OFBiz server

`gradlew "ofbiz --help"`

#### List build tasks

List all available tasks from the build system

`gradlew tasks`

#### List build projects

List all available projects in the build system

`gradlew projects`

#### Gradle build system help

Show usage and options for the Gradle build system

`gradlew --help`

* * * * * * * * * * * *

### Server command tasks

#### Start OFBiz

`gradlew "ofbiz --start"`

start is the default server task so this also works:

`gradlew ofbiz`

#### Shutdown OFBiz

`gradlew "ofbiz --shutdown"`

#### Get OFBiz status

`gradlew "ofbiz --status"`

#### Start OFBiz in remote debug mode

Starts OFBiz in remote debug mode and waits for debugger
or IDEs to connect on port __5005__

`gradlew "ofbizDebug --start"`

OR

`gradlew ofbizDebug`

#### Start OFBiz in secure mode

Starts OFBiz in secure mode using the notsoserial API to prevent
Java serialization security issues

`gradlew "ofbizSecure --start"`

OR

`gradlew ofbizSecure`

#### Start OFBiz on a different port

Start OFBiz of the network port offsetted by the range
provided in the argument to --portoffset

`gradlew "ofbiz --start --portoffset 10000"`

#### Start OFBiz in the background

Start OFBiz in the background by forking it to a new
process and redirecting the output to __runtime/logs/console.log__

`gradlew "ofbizBackground --start"`

OR

`gradlew ofbizBackground`

You can also offset the port, for example:

`gradlew "ofbizBackground --start --portoffset 10000"`

#### Start OFBiz in the background in secure mode

Start OFBiz in secure mode in the background by forking it to a new
process and redirecting the output to __runtime/logs/console.log__

`gradlew "ofbizBackgroundSecure --start"`

OR

`gradlew ofbizBackgroundSecure`

* * * * * * * * * * * *

### Data loading tasks

OFBiz contains the following data reader types:

- __seed__: OFBiz and External Seed Data - to be maintained along with source and
  updated whenever a system deployment is updated
- __seed-initial__: OFBiz and External Seed Data - to be maintained along with 
  source like other seed data, but only loaded initially and not updated
  when a system is updated except manually reviewing each line
- __demo__: OFBiz Only Demo Data
- __ext__: External General Data (custom)
- __ext-test__: External Test Data (custom)
- __ext-demo__: External Demo Data (custom)

you can choose which data readers to pass in the following syntax:

`gradlew "ofbiz --load-data readers=<readers-here-comma-separated>"`

Example:

`gradlew "ofbiz --load-data readers=seed,seed-initial,ext,ext-demo"`

#### Load default OFBiz data

Load default data; meant for generic OFBiz development, 
testing, demonstration, etc purposes

`gradlew loadDefault`

OR

`gradlew "ofbiz --load-data"`

#### Load seed data

Load ONLY the seed data (not seed-initial, demo, ext* or anything else);
meant for use after an update of the code to reload the seed data
as it is generally maintained along with the code and needs to be
in sync for operation

`gradlew "ofbiz --load-data readers=seed"`

#### load ext data

Load seed, seed-initial and ext data; meant for manual/generic
testing, development, or going into production with a derived
system based on stock OFBiz where the ext data basically
replaces the demo data

`gradlew "ofbiz --load-data readers=seed,seed-initial,ext"`

#### load ext test data

Load seed, seed-initial, ext and ext-test data; meant for
automated testing with a derived system based on stock OFBiz

`gradlew "ofbiz --load-data readers=seed,seed-initial,ext,ext-test"`

#### load data from an entity file

Load data from an XML file holding entity data.

`gradlew "ofbiz --load-data file=foo/bar/FileNameHere.xml"`

#### create a new tenant

Create a new tenant in your environment, create the delegator, load
initial data with admin-user and password (needs multitenant=Y in 
general.properties). The following project parameters are passed:

- tenantId: mandatory
- tenantName: mandatory, name of the tenant
- domainName: optional, default is org.ofbiz
- tenantReaders: optional, default value is seed,seed-initial,demo
- dbPlatform: optional, D(Derby), M(MySQL), O(Oracle), P(PostgreSQL) (default D) 
- dbIp: optional, ip address of the database
- dbUser: optional, username of the database
- dbPassword: optional, password of the database

`gradlew createTenant -PtenantId=mytenant -PtenantName="My Name"`

`gradlew createTenant -PtenantId=mytenant -PtenantName="My Name" -PdomainName=com.example -PtenantReaders=seed,seed-initial,ext -PdbPlatform=M -PdbIp=127.0.0.1 -PdbUser=mydbuser -PdbPassword=mydbpass`

If run successfully, the system creates a new tenant having:

- delegator: default#${tenandId} (e.g. default#mytenant)
- admin user: ${tenantId}-admin (e.g. mytenant-admin)
- admin user password: ofbiz

#### load data for a specific tenant

Load data for one specific tenant in a multitenant environment. Note
that you must set multitenant=Y in general.properties and the
following project parameters are passed:

- tenantId (mandatory)
- tenantReaders (optional)
- tenantComponent (optional)

`gradlew loadTenant -PtenantId=mytenant`

`gradlew loadTenant -PtenantId=mytenant -PtenantReaders=seed,seed-initial,demo -PtenantComponent=base`

* * * * * * * * * * * *

### Testing tasks

#### Execute all unit tests

`gradlew test`

#### Execute all integration tests

`gradlew testIntegration`

OR

`gradlew 'ofbiz --test'`

#### Execute an integration test case

run a test case, in this example the componnet is "entity" and the case
name is "entity-tests"

`gradlew "ofbiz --test component=entity --test case=entity-tests"`

#### Execute an integration test case in debug mode

listens on port __5005__

`gradlew "ofbizDebug --test component=entity --test case=entity-tests"`

#### Execute an integration test suite

listens on port __5005__

`gradlew "ofbiz --test component=widget --test suitename=org.ofbiz.widget.test.WidgetMacroLibraryTests"`

#### Execute an integration test suite in debug mode

listens on port __5005__

`gradlew "ofbizDebug --test component=widget --test suitename=org.ofbiz.widget.test.WidgetMacroLibraryTests"`

* * * * * * * * * * * *

### Miscellaneous tasks

#### Launch a graphical user interface of Gradle

This is a very convenient feature of Gradle which
allows the user to interact with Gradle through a
swing GUI. You can save frequently used commands
in a list of favorites for frequent reuse.

`gradlew --gui`

#### Run all tests on a clean system

`gradlew cleanAll loadDefault testIntegration`

#### Clean all generated artifacts

`gradlew cleanAll`

#### Refresh the generated artifacts

`gradlew clean build`

#### Create a custom component in hot-deploy

`gradlew createComponent -PcomponentName=Custom -PcomponentResourceName=Custom -PwebappName=customweb -PbasePermission=OFBTOOLS,CUSTOM_SECURITY`

#### Create an admin user account

Create an admin user with login name MyUserName and default password
with value "ofbiz". Upon first login OFBiz will request changing the
default password

`gradlew loadAdminUserLogin -PuserLoginId=MyUserName`

#### Setup eclipse project for OFBiz

Thanks to some gradle magic, setting up OFBiz on eclipse is very
easy. All you need is to execute one command and then you can
import the project to eclipse. This command will generate
the necessary __.classpath__ and __.project__ files for eclipse.

`gradlew eclipse`