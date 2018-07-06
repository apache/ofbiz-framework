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
package org.apache.ofbiz.common;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.junit.Before;
import org.junit.Test;

import groovy.util.GroovyScriptEngine;

public class GetLocaleListTests {
    private Map<String, Object> params;
    private GroovyScriptEngine engine;

    @Before
    public void setUp() throws Exception {
        params = new HashMap<>();
        engine = new GroovyScriptEngine(System.getProperty("user.dir"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> runScript() throws Exception {
        Map<String, Object> gContext = new HashMap<>();
        gContext.put(ScriptUtil.PARAMETERS_KEY, params);
        engine.run("framework/common/groovyScripts/GetLocaleList.groovy", GroovyUtil.getBinding(gContext));
        return (List<Map<String, String>>) gContext.get("locales");
    }

    static private List<String> localeStrings(List<Map<String, String>> locales) {
        return locales.stream()
            .map(m -> m.get("localeString"))
            .collect(Collectors.toList());
    }

    @Test
    public void frenchLocaleName() throws Exception {
        params.put("localeName", "fr");
        List<Map<String, String>> res = runScript();
        assertThat(localeStrings(res), hasItems("en_ZA", "fr", "fr_BE", "fr_CA", "fr_FR", "fr_LU", "fr_CH"));
    }

    @Test
    public void frenchLocaleString() throws Exception {
        params.put("localeString", "fr");
        List<Map<String, String>> res = runScript();
        assertThat(localeStrings(res),
                both(hasItems("fr", "fr_BE", "fr_CA", "fr_FR", "fr_LU", "fr_CH")).and(not(hasItem("en_ZA"))));
    }

    @Test
    public void frenchNoDuplicates() throws Exception {
        params.put("localeName", "fr");
        params.put("localeString", "fr");
        List<Map<String, String>> res = runScript();
        assertThat(localeStrings(res), hasItems("en_ZA", "fr", "fr_BE", "fr_CA", "fr_FR", "fr_LU", "fr_CH"));
        assertEquals(new HashSet<String>(localeStrings(res)).size(), localeStrings(res).size());
    }
}
