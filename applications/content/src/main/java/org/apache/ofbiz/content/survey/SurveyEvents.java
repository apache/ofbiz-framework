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
package org.apache.ofbiz.content.survey;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.webapp.event.EventHandlerException;

/**
 * SurveyEvents Class
 */
public class SurveyEvents {

    public static final String module = SurveyEvents.class.getName();

    public static String createSurveyResponseAndRestoreParameters(HttpServletRequest request, HttpServletResponse response) {
        // Call createSurveyResponse as an event, easier to setup and ensures parameter security
        ConfigXMLReader.Event createSurveyResponseEvent = new ConfigXMLReader.Event("service", null, "createSurveyResponse", true);
        RequestHandler rh = (RequestHandler) request.getAttribute("_REQUEST_HANDLER_");
        ConfigXMLReader.ControllerConfig controllerConfig = rh.getControllerConfig();
        String requestUri = (String) request.getAttribute("thisRequestUri");
        RequestMap requestMap = null;
        try {
            requestMap = controllerConfig.getRequestMapMap().get(requestUri);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
        }
        String eventResponse = null;
        try {
            eventResponse = rh.runEvent(request, response, createSurveyResponseEvent, requestMap, null);
        } catch (EventHandlerException e) {
            Debug.logError(e, module);
            return "error";
        }
        if (!"success".equals(eventResponse)) {
            return eventResponse;
        }
        // Check for an incoming _ORIG_PARAM_MAP_ID_, if present get the session stored parameter map and insert it's entries as request attributes
        Map<String, Object> combinedMap = UtilHttp.getCombinedMap(request);
        if (combinedMap.containsKey("_ORIG_PARAM_MAP_ID_")) {
            String origParamMapId = (String) combinedMap.get("_ORIG_PARAM_MAP_ID_");
            UtilHttp.restoreStashedParameterMap(request, origParamMapId);
        }
        return "success";
    }
}
