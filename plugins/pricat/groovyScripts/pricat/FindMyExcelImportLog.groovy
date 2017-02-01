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
import org.apache.ofbiz.base.util.*;
import java.io.FileInputStream;

module = "FindMyExcelImport.groovy";

context.borderStyle = "2px inset /*begin-color ThreeDHighlight*/#ffffff/*end-color*/";
context.borderSimpleStyle = "2px solid /*begin-color ThreeDFace*/#f0f0f0/*end-color*/";

sequenceNum = request.getParameter("sequenceNum");

if (sequenceNum == null) {
    context.logFileContent = "No sequenceNum parameter found.";
    return; 
}

historyEntry = delegator.findOne("ExcelImportHistory", [sequenceNum : Long.valueOf(sequenceNum), userLoginId : userLogin.userLoginId], false);
if (historyEntry == null) {
    context.logFileContent = "No import history found.";
    return;
}

logFile = FileUtil.getFile("runtime/pricat/" + userLogin.userLoginId + "/" + sequenceNum + ".log");
if (!logFile.exists()) {
    context.logFileContent = "No log file found.";
}

FileInputStream fis = new FileInputStream(logFile);
InputStreamReader isr = new InputStreamReader(fis);
BufferedReader br = new BufferedReader(isr);
logFileContent = "";
while((s = br.readLine())!=null){
    logFileContent += s;
}
context.logFileContent = logFileContent;

