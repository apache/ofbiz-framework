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
package org.apache.ofbiz.widget.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.webapp.ftl.ScriptTemplateListTransform;

public class ScriptTemplateUtil {

    private static String sessionKey = "ScriptTemplateMap";
    private static String requestKey = "ScriptTemplateList";
    private static int maxNumOfScriptInCache = 10;

    /**
     * add script src link for use by @see {@link ScriptTemplateListTransform}
     * @param context
     * @param filePath
     */
    public static void addScriptSrcToRequest(Map<String, Object> context, String filePath){
        HttpServletRequest request = (HttpServletRequest)context.get("request");
        Set<String> scriptTemplates = UtilGenerics.cast(request.getAttribute(requestKey));
        if (scriptTemplates==null){
            // use of LinkedHashSet to maintain insertion order
            scriptTemplates = new LinkedHashSet<String>();
            request.setAttribute(requestKey, scriptTemplates);
        }
        scriptTemplates.add(filePath);
    }

    /**
     * get the script src links collected from the "script-template" tags
     * @param request
     * @return
     */
    public static Set<String> getScriptSrcLinksFromRequest(HttpServletRequest request){
        Set<String> scriptTemplates = UtilGenerics.cast(request.getAttribute(requestKey));
        return scriptTemplates;
    }

    public static void putScriptInSession(Map<String, Object> context, String fileName, String fileContent){
        HttpSession session = (HttpSession)context.get("session");
        Map<String,String> scriptTemplateMap = UtilGenerics.cast(session.getAttribute(sessionKey));
        if (scriptTemplateMap==null){
            synchronized (session) {
                scriptTemplateMap = UtilGenerics.cast(session.getAttribute(sessionKey));
                if (scriptTemplateMap==null){
                    // use of LinkedHashMap to limit size of the map
                    scriptTemplateMap = new LinkedHashMap<String, String>() {
                        private static final long serialVersionUID = 1L;
                        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                            return size() > maxNumOfScriptInCache;
                        }
                    };
                    session.setAttribute(sessionKey, scriptTemplateMap);
                }
            }
        }
        scriptTemplateMap.put(fileName, fileContent);
    }

    public static String getScriptFromSession(HttpSession session, String fileName){
        Map<String,String> scriptTemplateMap = UtilGenerics.cast(session.getAttribute(sessionKey));
        if (scriptTemplateMap!=null){
            return scriptTemplateMap.get(fileName);
        }
        return null;
    }
}
