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
package org.apache.ofbiz.webapp.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;

/**
 * Generic Script Event Handler. This event handler uses the javax.script package (JSR-223) to invoke scripts or script functions.
 * <p>The script event handler will put the following artifacts in the script engine's bindings:</p>
 * <ul>
 *   <li><code>parameters</code> - a <code>Map</code> containing servlet context, session, request attributes and parameters</li>
 *   <li><code>request</code> - a <code>HttpServletRequest</code> instance</li>
 *   <li><code>response</code> - a <code>HttpServletResponse</code> instance</li>
 *   <li><code>session</code> - a <code>HttpSession</code> instance</li>
 *   <li><code>dispatcher</code> - a <code>LocalDispatcher</code> instance</li>
 *   <li><code>delegator</code> - a <code>Delegator</code> instance</li>
 *   <li><code>security</code> - a <code>Security</code> instance</li>
 *   <li><code>locale</code> - a <code>Locale</code> instance</li>
 *   <li><code>timeZone</code> - a <code>TimeZone</code> instance</li>
 *   <li><code>userLogin</code> - a UserLogin <code>GenericValue</code></li>
 * </ul>
 * <p>If the event element includes an invoke attribute, then the matching script function/method will be called
 * with a single argument - the bindings <code>Map</code>.</p>
 */
public final class ScriptEventHandler implements EventHandler {

    public static final String module = ScriptEventHandler.class.getName();
    private static final Set<String> protectedKeys = createProtectedKeys();

    private static Set<String> createProtectedKeys() {
        Set<String> newSet = new HashSet<>();
        newSet.add("request");
        newSet.add("response");
        newSet.add("session");
        newSet.add("dispatcher");
        newSet.add("delegator");
        newSet.add("security");
        newSet.add("locale");
        newSet.add("timeZone");
        newSet.add("userLogin");
        /* Commenting out for now because some scripts write to the parameters Map - which should not be allowed.
        newSet.add(ScriptUtil.PARAMETERS_KEY);
        */
        return Collections.unmodifiableSet(newSet);
    }

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("request", request);
            context.put("response", response);
            HttpSession session = request.getSession();
            context.put("session", session);
            context.put("dispatcher", request.getAttribute("dispatcher"));
            context.put("delegator", request.getAttribute("delegator"));
            context.put("security", request.getAttribute("security"));
            context.put("locale", UtilHttp.getLocale(request));
            context.put("timeZone", UtilHttp.getTimeZone(request));
            context.put("userLogin", session.getAttribute("userLogin"));
            context.put(ScriptUtil.PARAMETERS_KEY, UtilHttp.getCombinedMap(request, UtilMisc.toSet("delegator", "dispatcher", "security", "locale", "timeZone", "userLogin")));
            Object result = null;
            try {
                ScriptContext scriptContext = ScriptUtil.createScriptContext(context, protectedKeys);
                result = ScriptUtil.executeScript(event.path, event.invoke, scriptContext, null);
                if (result == null) {
                    result = scriptContext.getAttribute(ScriptUtil.RESULT_KEY);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error running event " + event.path + ": ", module);
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            if (result instanceof Map) {
                Map<String, Object> resultMap = UtilGenerics.cast(result);
                String successMessage = (String)resultMap.get("_event_message_");
                if (successMessage != null) {
                    request.setAttribute("_EVENT_MESSAGE_", successMessage);
                }
                String errorMessage = (String)resultMap.get("_error_message_");
                if (errorMessage != null) {
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                }
                return (String)resultMap.get("_response_code_");
            }
            if (result != null && !(result instanceof String)) {
                throw new EventHandlerException("Event did not return a String result, it returned a " + result.getClass().getName());
            }
            return (String) result;
        } catch (Exception e) {
            throw new EventHandlerException("Error running event " + event.path, e);
        }
    }
}
