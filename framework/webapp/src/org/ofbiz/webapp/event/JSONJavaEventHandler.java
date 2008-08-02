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
package org.ofbiz.webapp.event;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;
import net.sf.json.JSONObject;

import org.ofbiz.base.util.Debug;

/**
 * JSONJavaEventHandler - JSON Object Wrapper around the JavaEventHandler
 */
public class JSONJavaEventHandler implements EventHandler {

    public static final String module = JSONJavaEventHandler.class.getName();
    protected EventHandler service;

    public void init(ServletContext context) throws EventHandlerException {
        this.service = new JavaEventHandler();
        this.service.init(context);
    }

    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        // call into the java handler for parameters parsing and invocation
        String respCode = service.invoke(eventPath, eventMethod, request, response);

        // pull out the service response from the request attribute
        Map<String, Object> attrMap = getAttributesAsMap(request);

        // create a JSON Object for return
        JSONObject json = JSONObject.fromMap(attrMap);
        String jsonStr = json.toString();
        if (jsonStr == null) {
            throw new EventHandlerException("JSON Object was empty; fatal error!");
        }

        // set the X-JSON content type
        response.setContentType("application/x-json");
        // jsonStr.length is not reliable for unicode characters 
        try {
            response.setContentLength(jsonStr.getBytes("UTF8").length);
        } catch (UnsupportedEncodingException e) {
            throw new EventHandlerException("Problems with Json encoding", e);
        }

        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            throw new EventHandlerException("Unable to get response writer", e);
        }

        return respCode;
    }

    private Map<String, Object> getAttributesAsMap(HttpServletRequest request) {
        Map<String, Object> attrMap = FastMap.newInstance();
        Enumeration en = request.getAttributeNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            Object val = request.getAttribute(name);
            if (val instanceof java.sql.Timestamp) {
                val = val.toString();
            }
            if (val instanceof String || val instanceof Number || val instanceof Map || val instanceof List) {
                if (Debug.verboseOn()) Debug.logVerbose("Adding attribute to JSON output: " + name, module);
                attrMap.put(name, val);
            }
        }

        return attrMap;
    }
}
