/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.ofbiz.base.util.FileUtil
import org.apache.ofbiz.base.util.UtilProperties

String ofbizLogDir = UtilProperties.getPropertyValue("debug", "log4j.appender.css.dir", "runtime/logs/")
if (!ofbizLogDir.startsWith("/")) {
    ofbizLogDir = System.getProperty("ofbiz.home") + "/" + ofbizLogDir
}
if (!ofbizLogDir.endsWith("/")) {
    ofbizLogDir = ofbizLogDir.concat("/")
}

File runTimeLogDir = FileUtil.getFile(ofbizLogDir)
File[] listLogFiles = runTimeLogDir.listFiles()
String ofbizLogRegExp = UtilProperties.getPropertyValue("debug", "log4j.appender.css.fileNameRegExp", "[(ofbiz)|(error)].*")
List listLogFileNames = []
for (int i = 0; i < listLogFiles.length; i++) {
    if (listLogFiles[i].isFile()) {
        logFileName = listLogFiles[i].getName()
        if (logFileName.matches(ofbizLogRegExp)) {
            listLogFileNames.add(logFileName)
        }
    }
}
context.listLogFileNames = listLogFileNames

if (parameters.logFileName) {
    List logLines = []
    try {
        File logFile = FileUtil.getFile(ofbizLogDir.concat(parameters.logFileName))
        logFile.eachLine { line ->
            if (parameters.searchString) {
                if (!line.contains(parameters.searchString)) {
                    return
                }
            }
            type = ''
            if (line.contains(" |I| ")) {
                type = 'INFO'
            } else if (line.contains(" |W| ")) {
                type = 'WARN'
            } else if (line.contains(" |E| ")) {
                type = 'ERROR'
            } else if (line.contains(" |D| ")) {
                type = 'DEBUG'
            }
            logLines.add([type: type, line:line])
        }
    } catch (Exception exc) {}
    context.logLines = logLines
}
