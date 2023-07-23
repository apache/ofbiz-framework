git update-index --no-assume-unchanged build.gradle
git update-index --no-assume-unchanged gradle/wrapper/gradle-wrapper.properties
git stash
git switch release22.01
cd plugins
call 22