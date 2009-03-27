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
package org.ofbiz.webapp.event;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;

public class GroovyEventHandler implements EventHandler {

    public static final String module = GroovyEventHandler.class.getName();

    public void init(ServletContext context) throws EventHandlerException {
    }

    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        try {
            Map<String, Object> groovyContext = FastMap.newInstance();
            groovyContext.put("request", request);
            groovyContext.put("response", response);
            HttpSession session = request.getSession();
            groovyContext.put("session", session);

            groovyContext.put("dispatcher", request.getAttribute("dispatcher"));
            groovyContext.put("delegator", request.getAttribute("delegator"));
            groovyContext.put("security", request.getAttribute("security"));
            groovyContext.put("locale", UtilHttp.getLocale(request));
            groovyContext.put("timeZone", UtilHttp.getTimeZone(request));
            groovyContext.put("userLogin", session.getAttribute("userLogin"));
            groovyContext.put("parameters", UtilHttp.getCombinedMap(request, UtilMisc.toSet("delegator", "dispatcher", "security", "locale", "timeZone", "userLogin")));

            Object result = GroovyUtil.runScriptAtLocation(event.path + event.invoke, groovyContext);
            // check the result
            if (result != null && !(result instanceof String)) {
                throw new EventHandlerException("Event did not return a String result, it returned a " + result.getClass().getName());
            }
            return (String) result;
        } catch (Exception e) {
            throw new EventHandlerException("Groovy Event Error", e);
        }
    }
}
