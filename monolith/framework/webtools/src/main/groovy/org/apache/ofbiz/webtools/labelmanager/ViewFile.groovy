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
package org.apache.ofbiz.webtools.labelmanager

import org.apache.ofbiz.base.component.ComponentConfig
import org.apache.ofbiz.base.util.UtilXml
import org.w3c.dom.Document

// ViewFile's only real caller (ViewReferences.ftl) only ever links to files that
// LabelReferences.java found while scanning for label usage, and for .xml that scan
// is always <component-root>/minilang/*.xml. Restricting to that exact domain - rather
// than the generic applications/themes/plugins/runtime content allowlist, which excludes
// framework/ entirely - keeps framework-rooted label references viewable while still
// blocking framework config files such as entityengine.xml.
boolean isUnderComponentMinilangDir(File file) {
    try {
        String canonicalFilePath = file.getCanonicalPath()
        for (ComponentConfig config : ComponentConfig.getAllComponents()) {
            String canonicalMinilangDir = config.rootLocation().resolve('minilang').toFile().getCanonicalPath()
            if (canonicalFilePath == canonicalMinilangDir || canonicalFilePath.startsWith(canonicalMinilangDir + File.separator)) {
                return true
            }
        }
    } catch (IOException ignored) {
        return false
    }
    return false
}

fileString = ''
if (parameters.fileName) {
    file = new File(parameters.fileName)
    if (parameters.fileName.endsWith('.xml') && isUnderComponentMinilangDir(file)) {
        Document document = UtilXml.readXmlDocument(file.toURL(), false)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        UtilXml.writeXmlDocument(document, os, 'UTF-8', true, true, 4)
        os.close()
        fileString = os.toString()
    }
    rows = fileString.split(System.getProperty('line.separator'))
    context.rows = rows.size()
}
context.fileString = fileString
