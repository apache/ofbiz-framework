/*
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
*/

import org.apache.tools.ant.BuildException

def resolveFile = { name, base ->
    return project.resolveFile(project.replaceProperties(name), base)
}

def getAttribute = { name, defaultValue ->
    def value = attributes[name]
    if (value == null) {
        if (!defaultValue) throw new BuildException("No default for attribute($name)")
        value = defaultValue
    }
    return value
}

def uptodate = { left, right ->
    def uptodateTask = project.createTask('uptodate')
    uptodateTask.srcfile = left
    uptodateTask.targetFile = right
    return uptodateTask.eval()
}

def basedir = project.baseDir
def ant = new AntBuilder(self)
def javacchome = resolveFile('${ofbiz.home.dir}/framework/base/lib/javacc', basedir)
def src = getAttribute('src', 'src')
def dir = getAttribute('dir', basedir)
def file = getAttribute('file', basedir)
def srcfile = resolveFile("$src/$dir/${file}.jjt", basedir)
def srcpaths = [
    jjtree:     resolveFile(getAttribute('gendir', '${build.dir}/gen-src') + '/jjtree/', basedir),
    javacc:     resolveFile(getAttribute('gendir', '${build.dir}/gen-src') + '/javacc/', basedir),
]
def dirs = [
    jjtree:     resolveFile(dir, srcpaths.jjtree),
    javacc:     resolveFile(dir, srcpaths.javacc),
]
def gen = [
    jjfile:     new File(dirs.jjtree, project.replaceProperties("${file}.jj")),
    javafile:   new File(dirs.javacc, project.replaceProperties("${file}.java")),
]
def srcpath = project.getReference('src-path')
def foundpath = [
    jjtree:     false,
    javacc:     false,
]
srcpath.each {
    foundpath.jjtree |= it.file == srcpaths.jjtree
    foundpath.javacc |= it.file == srcpaths.javacc
}
if (!foundpath.jjtree) srcpath.append(ant.path{pathelement(location: srcpaths.jjtree)})
if (!foundpath.javacc) srcpath.append(ant.path{pathelement(location: srcpaths.javacc)})

if (!uptodate(srcfile, gen.jjfile)) {
    ant.delete(dir:dirs.jjtree)
    ant.mkdir(dir:dirs.jjtree)
    ant.jjtree(
        target:             srcfile,
        javacchome:         javacchome,
        outputdirectory:    dirs.jjtree,
    )
}
if (!uptodate(gen.jjfile, gen.javafile)) {
    ant.delete(dir:dirs.javacc)
    ant.mkdir(dir:dirs.javacc)

    ant.javacc(
        target:             gen.jjfile,
        javacchome:         javacchome,
        outputdirectory:    dirs.javacc,
    )
    ant.delete(dir:resolveFile('${build.classes}/' + dir, basedir))
}
