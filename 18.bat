rem git update-index --no-assume-unchanged build.gradle
rem git update-index --no-assume-unchanged gradle/wrapper/gradle-wrapper.properties
rem git stash
call notr
git switch release18.12
cd plugins
call 18