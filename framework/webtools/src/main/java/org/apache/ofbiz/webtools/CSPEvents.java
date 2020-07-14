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
package org.apache.ofbiz.webtools;

import java.io.IOException;

import org.apache.ofbiz.base.lang.JSON;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;

public class CSPEvents {
    
    public static final String module = CSPEvents.class.getName();
    
    private static final String[] onlyAttrs = new String[] {
        "violated-directive",
        "blocked-uri",
        "referrer",
        "script-sample",
        "source-file",
        "disposition",
        "original-policy",
        "line-number",
        "effective-directive",
        "document-uri",
        "status-code"
    };
    private static final Set<String> onlyKeys = new HashSet<String>(Arrays.asList((onlyAttrs)));

    public static String CSPReport(HttpServletRequest request, HttpServletResponse response) {
        // log csp related errors, reported by the browser

        try {
            Map<String, Object> attrMap = JSON.from(request.getInputStream()).toObject(Map.class);
            if (attrMap.containsKey("csp-report")) {
                Map<String, Object> repMap = JSON.from(attrMap.get("csp-report")).toObject(Map.class);
                Set<String> keys = new HashSet<String>(repMap.keySet());
                for (String attr : keys) {
                    if (!onlyKeys.contains(attr)) {
                        repMap.remove(attr);
                    }
                }
                if (repMap.size() > 0) {
                    JSON json = JSON.from(repMap);
                    Debug.logError(json.toString(), module);
                    response.setStatus(204);
                    return "success";
                }
            }
        } catch (IOException e) {
            Debug.logError(e, module);
        }
        response.setStatus(403);
        return "error";
    }
    
}
