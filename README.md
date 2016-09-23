<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
Apache OFBiz®
============

Welcome to __Apache OFBiz®__! A powerful top level Apache software project.
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

>_Note_: if you are using Eclipse, make sure of running the appropriate Eclipse
command `gradlew eclipse` before creating the project in Eclipse.
This command will prepare OFBiz for Eclipse with the correct classpath and settings 
by creating the.classpath and .project files.

Security
-------------------

You can trust the OFBiz Project Management Committee members and committers do their best to keep OFBiz secure from external exploits, and fix vulnerabilities as soon as they are known. Despite these efforts, if ever you find and want to report a security issue, please report at: security @ ofbiz.apache.org, before disclosing them in a public forum.

>_Note_: Be sure to read this Wiki page if ever you plan to use RMI, JNDI, JMX or Spring and maybe other Java classes OFBiz does not use Out Of The Box (OOTB): [The infamous Java serialization vulnerability](https://cwiki.apache.org/confluence/display/OFBIZ/The+infamous+Java+serialization+vulnerability)

You can find more information about security in OFBiz at [Keeping OFBiz secure](https://cwiki.apache.org/confluence/display/OFBIZ/Keeping+OFBiz+secure) 


Quick start
-----------

To quickly install and fire-up OFBiz, please follow the below instructions
from the command line at the OFBiz top level directory (folder)

### Prepare OFBiz:

__Note__: Depending on your Internet connection speed it might take a long
time for this step to complete if you are using OFBiz for the first time
as it needs to download all dependencies. So please be patient!

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
Apache Tomcat®, Apache Geronimo (transaction manager), etc.

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
  - __ofbizBackground__ ; server commands running in a background forked process

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

#### Force OFBiz shutdown

Terminate all running OFBiz server instances by calling
the appropriate operating system kill command. Use this
command to force OFBiz termination if the --shutdown
command does not work. Usually this is needed when in the
middle of data loading or testing in OFBiz.

Warning: Be careful in using this command as force termination
might lead to inconsistent state / data

`gradlew terminateOfbiz`

#### Start OFBiz in remote debug mode

Starts OFBiz in remote debug mode and waits for debugger
or IDEs to connect on port __5005__

`gradlew "ofbizDebug --start"`

OR

`gradlew ofbizDebug`

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

Loads default data set; meant for initial loading of generic OFBiz data. 
Can be applied for development, testing, demonstration, etc. purposes. 
Be aware that executing this task can result in your data being overwritten in your database of choice. 
Use with caution in production environments. 
The default data set is defined by datasource using the read-data attribute, 
followed by the name of the data set, into the datasource element of the 'entityengine.xml' file.


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
- tenantName: optional, default is value of tenantId
- domainName: optional, default is org.apache.ofbiz
- tenantReaders: optional, default value is seed,seed-initial,demo
- dbPlatform: optional, D(Derby), M(MySQL), O(Oracle), P(PostgreSQL) (default D) 
- dbIp: optional, ip address of the database
- dbUser: optional, username of the database
- dbPassword: optional, password of the database

`gradlew createTenant -PtenantId=mytenant`

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

`gradlew "ofbiz --test component=widget --test suitename=org.apache.ofbiz.widget.test.WidgetMacroLibraryTests"`

#### Execute an integration test suite in debug mode

listens on port __5005__

`gradlew "ofbizDebug --test component=widget --test suitename=org.apache.ofbiz.widget.test.WidgetMacroLibraryTests"`

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

#### Create an admin user account

Create an admin user with login name MyUserName and default password
with value "ofbiz". Upon first login OFBiz will request changing the
default password

`gradlew loadAdminUserLogin -PuserLoginId=MyUserName`

#### Compile Java using Xlint output

Xlint prints output of all warnings detected by the compiler

`gradlew -PXlint build`

#### Run OWASP tool to identify dependency vulnerabilities (CVEs)

The below command activates a gradle plugin (OWASP) and Identifies
and reports known vulnerabilities (CVEs) in OFBiz library dependencies.
This command takes a long time to execute because it needs to download
all plugin dependencies and the CVE identification process is also
time consuming

`gradlew -PenableOwasp dependencyCheck`

#### Setup eclipse project for OFBiz

Thanks to some gradle magic, setting up OFBiz on eclipse is very
easy. All you need is to execute one command and then you can
import the project to eclipse. This command will generate
the necessary __.classpath__ and __.project__ files for eclipse.

`gradlew eclipse`

* * * * * * * * * * *

OFBiz plugin system
-------------------

OFBiz provides an extension mechanism through plugins. Plugins are standard
OFBiz components that reside in the specialpurpose directory. Plugins can be
added manually or fetched from a maven repository. The standard tasks for
managing plugins are listed below.

>_Note_: OFBiz plugin versions follow [Semantic Versioning 2.0.0](http://semver.org/)

### Pull (download and install) a plugin automatically

Download a plugin with all its dependencies (plugins) and install them one-by-one
starting with the dependencies and ending with the plugin itself.

`gradlew pullPlugin -PdependencyId="org.apache.ofbiz.plugin:myplugin:0.1.0"`

If the plugin resides in a custom maven repository (not jcenter or localhost) then
you can use specify the repository using below command:

`gradlew pullPlugin -PrepoUrl="http://www.example.com/custom-maven" -PdependencyId="org.apache.ofbiz.plugin:myplugin:0.1.0"`

If you need username and password to access the custom repository:

`gradlew pullPlugin -PrepoUrl="http://www.example.com/custom-maven" -PrepoUser=myuser -PrepoPassword=mypassword -PdependencyId="org.apache.ofbiz.plugin:myplugin:0.1.0"`

### Install a plugin

If you have a plugin called mycustomplugin and want to install it in OFBiz follow the
below instructions:

- Extract the plugin if it is compressed
- Place the extracted directory into /specialpurpose
- Run the below command

`gradlew installPlugin -PpluginId=myplugin`

The above commands achieve the following:

- add the plugin to /specialpurpose/component-load.xml
- executes the task "install" in the plugin's build.gradle file if it exists

### Uninstall a plugin

If you have an existing plugin called mycustomplugin and you wish to uninstall
run the below command

`gradlew uninstallPlugin -PpluginId=myplugin`

The above commands achieve the following:

- executes the task "uninstall" in the plugin's build.gradle file if it exists
- removes the plugin from /specialpurpose/component-load.xml

### Remove a plugin

Calls __uninstallPlugin__ on an existing plugin and then delete it from the file-system

`gradlew removePlugin -PpluginId=myplugin` 

### Create a new plugin

Create a new plugin. The following project parameters are passed:

- pluginId: mandatory
- pluginResourceName: optional, default is the Capitalized value of pluginId
- webappName: optional, default is the value of pluginId
- basePermission: optional, default is the UPPERCASE value of pluginId

`gradlew createPlugin -PpluginId=myplugin`

`gradlew createPlugin -PpluginId=myplugin -PpluginResourceName=MyPlugin -PwebappName=mypluginweb -PbasePermission=MYSECURITY`

The above commands achieve the following:

- create a new plugin in /specialpurpose/myplugin
- add the plugin to /specialpurpose/component-load.xml

### Push a plugin to a repository

This task publishes an OFBiz plugin into a maven package and then uploads it to
a maven repository. Currently, pushing is limited to localhost maven repository
(work in progress). To push a plugin the following parameters are passed:

- pluginId: mandatory
- groupId: optional, defaults to org.apache.ofbiz.plugin
- pluginVersion: optional, defaults to 0.1.0-SNAPSHOT
- pluginDescription: optional, defaults to "Publication of OFBiz plugin ${pluginId}"

`gradlew pushPlugin -PpluginId=myplugin`

`gradlew pushPlugin -PpluginId=mycompany -PpluginGroup=com.mycompany.ofbiz.plugin -PpluginVersion=1.2.3 -PpluginDescription="Introduce special functionality X"`


* * * * * * * * * * * *

Useful Tips
-----------

### Gradle tab-completion on Unix-like systems:

To get tab completion (auto complete gradle commands by pressing tab)
you can download the script from the below link and place it in the
appropriate location for your system.

[Gradle tab completion](https://gist.github.com/nolanlawson/8694399)

For example, on debian based systems, you can use the following command:

`sudo curl -L -s https://gist.github.com/nolanlawson/8694399/raw/gradle-tab-completion.bash -o /etc/bash_completion.d/gradle-tab-completion.bash`

Crypto notice
-------------

This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  BEFORE using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See <http://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS
Export Administration Regulations, Section 740.13) for both object
code and source code.

The following provides more details on the included cryptographic
software:

- Various classes in OFBiz, including DesCrypt, HashCrypt, and
 BlowFishCrypt use libraries from the Sun Java JDK API including
 java.security.* and javax.crypto.* (the JCE, Java Cryptography
 Extensions API)
- Other classes such as HttpClient and various related ones use
 the JSSE (Java Secure Sockets Extension) API
