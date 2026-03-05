/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.base.util.string

import groovy.io.FileType
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.ScriptUtil
import org.junit.Test

import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern

class FlexibleStringExpanderBaseCodeTests {

    private static final String MODULE = FlexibleStringExpanderBaseCodeTests.getName()
    private final Pattern pattern = Pattern.compile('\\$\\{groovy:.*}')

    @Test
    void testEveryGroovyScriptletFromXmlFiles() {
        Pattern filterWidgetXmlFiles = ~/\.\/(framework|application|plugins).*\/widget\/.*(Screens|Menus|Forms)\.xml$/
        new File('.').traverse(type: FileType.FILES, filter: filterWidgetXmlFiles) { it ->
            assert parseXmlFile(it).isEmpty()
        }
    }

    /** Resolve all scriptlet on file on retrieve all identity as unsafe
     *
     * @param file
     * @return List unsafe scriptlet
     */
    private List parseXmlFile(File file) {
        String text = file.getText()
        Matcher matcher = pattern.matcher(text)
        List matchedScriptlet = []
        for (MatchResult matchResult : matcher.results().toList()) {
            String scriptlet = text.substring(matchResult.start() + 9, matchResult.end() - 1)
            if (!ScriptUtil.checkIfScriptIsSafe(scriptlet)) {
                matchedScriptlet << scriptlet
            }
        }
        if (matchedScriptlet) {
            Debug.logInfo("Unsafe scriptlet found on file ${file.getName()} : ", MODULE)
            Debug.logInfo('*************************************', MODULE)
            Debug.logInfo('* ' + matchedScriptlet.join('\n* '), MODULE)
            Debug.logInfo('*************************************', MODULE)
        }
        return matchedScriptlet
    }

}
