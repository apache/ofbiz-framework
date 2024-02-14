# Upgrading Gradle version

To upgrade the gradle version simply update the `distributionUrl` in `gradle-wrapper.properties` (placed 
in current directory)to point to the newest version and run all other commands as is, all future 
commands will be updated.

## Change log

- Dec 29, MSS upgraded gradle version to version 7.3.3 by changing the distributionUrl to the one below
in `gradle-wrapper.properties` (placed in current directory)

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-all.zip
```