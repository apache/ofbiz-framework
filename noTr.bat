git  apply -R Node.patch
git update-index --no-assume-unchanged "buildSrc/src/main/groovy/ofbiz-node-conventions.gradle"
cd plugins
git  apply -R example\vite-react.patch
git update-index --no-assume-unchanged "example/build.gradle"
cd ..