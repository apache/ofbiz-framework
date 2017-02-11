Dependency-Check
================

Dependency-Check is a utility that attempts to detect publicly disclosed vulnerabilities contained within project dependencies. It does this by determining if there is a Common Platform Enumeration (CPE) identifier for a given dependency. If found, it will generate a report linking to the associated CVE entries.

Documentation and links to production binary releases can be found on the [github pages](http://jeremylong.github.io/DependencyCheck/). Additionally, more information about the architecture and ways to extend dependency-check can be found on the [wiki].

Current Releases
-------------
### Jenkins Plugin

For instructions on the use of the Jenkins plugin please see the [OWASP Dependency-Check Plugin page](https://wiki.jenkins-ci.org/display/JENKINS/OWASP+Dependency-Check+Plugin).

### Command Line

More detailed instructions can be found on the
[dependency-check github pages](http://jeremylong.github.io/DependencyCheck/dependency-check-cli/).
The latest CLI can be downloaded from bintray's
[dependency-check page](https://bintray.com/jeremy-long/owasp/dependency-check).

On *nix
```
$ ./bin/dependency-check.sh -h
$ ./bin/dependency-check.sh --app Testing --out . --scan [path to jar files to be scanned]
```
On Windows
```
> bin/dependency-check.bat -h
> bin/dependency-check.bat --app Testing --out . --scan [path to jar files to be scanned]
```
On Mac with [Homebrew](http://brew.sh)
```
$ brew update && brew install dependency-check
$ dependency-check -h
$ dependency-check --app Testing --out . --scan [path to jar files to be scanned]
```

### Maven Plugin

More detailed instructions can be found on the [dependency-check-maven github pages](http://jeremylong.github.io/DependencyCheck/dependency-check-maven).
The plugin can be configured using the following:

```xml
<project>
    <build>
        <plugins>
            ...
            <plugin>
              <groupId>org.owasp</groupId>
              <artifactId>dependency-check-maven</artifactId>
              <executions>
                  <execution>
                      <goals>
                          <goal>check</goal>
                      </goals>
                  </execution>
              </executions>
            </plugin>
            ...
        </plugins>
        ...
    </build>
    ...
</project>
```

### Ant Task

For instructions on the use of the Ant Task, please see the [dependency-check-ant github page](http://jeremylong.github.io/DependencyCheck/dependency-check-ant).

Development Usage
-------------
The following instructions outline how to compile and use the current snapshot. While every intention is to maintain a stable snapshot it is recommended
that the release versions listed above be used.

The repository has some large files due to test resources. The team has tried to cleanup the history as much as possible.
However, it is recommended that you perform a shallow clone to save yourself time:

```bash
git clone --depth 1 git@github.com:jeremylong/DependencyCheck.git
```

On *nix
```
$ mvn install
$ ./dependency-check-cli/target/release/bin/dependency-check.sh -h
$ ./dependency-check-cli/target/release/bin/dependency-check.sh --app Testing --out . --scan ./src/test/resources
```
On Windows
```
> mvn install
> dependency-check-cli/target/release/bin/dependency-check.bat -h
> dependency-check-cli/target/release/bin/dependency-check.bat --app Testing --out . --scan ./src/test/resources
```

Then load the resulting 'DependencyCheck-Report.html' into your favorite browser.

Mailing List
------------

Subscribe: [dependency-check+subscribe@googlegroups.com] [subscribe]

Post: [dependency-check@googlegroups.com] [post]

Archive: [google group](https://groups.google.com/forum/#!forum/dependency-check)

Copyright & License
-

Dependency-Check is Copyright (c) 2012-2015 Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the Apache 2.0 license. See the [LICENSE.txt](https://github.com/jeremylong/DependencyCheck/dependency-check-cli/blob/master/LICENSE.txt) file for the full license.

Dependency-Check makes use of several other open source libraries. Please see the [NOTICE.txt] [notices] file for more information.


  [wiki]: https://github.com/jeremylong/DependencyCheck/wiki
  [subscribe]: mailto:dependency-check+subscribe@googlegroups.com
  [post]: mailto:dependency-check@googlegroups.com
  [notices]: https://github.com/jeremylong/DependencyCheck/blob/master/NOTICES.txt
