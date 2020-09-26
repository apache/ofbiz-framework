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
package org.apache.ofbiz.base.util;

import java.util.Map;

/**
 * Contains a simple script (scriptlet).
 * <p>A scriptlet is a small script that is commonly found in a scripting XML file.
 * The scriptlet is composed of two parts: the prefix - which is the script language
 * followed by a colon (":"), and the script. Example: <code>groovy:return foo.bar();</code>.
 * </p>
 */
public final class Scriptlet {

    private final String language;
    private final String script;

    public Scriptlet(String script) {
        Assert.notNull("script", script);
        int colonPos = script.indexOf(":");
        if (colonPos == -1) {
            throw new IllegalArgumentException("Invalid script: " + script);
        }
        this.language = script.substring(0, colonPos);
        this.script = script.substring(colonPos + 1);
    }

    /**
     * Executes the scriptlet and returns the result.
     * @param context The script bindings
     * @return The scriptlet result
     * @throws Exception
     */
    public Object executeScript(Map<String, Object> context) throws Exception {
        return ScriptUtil.evaluate(language, script, null, context);
    }

    @Override
    public String toString() {
        return this.language.concat(":").concat(this.script);
    }
}
