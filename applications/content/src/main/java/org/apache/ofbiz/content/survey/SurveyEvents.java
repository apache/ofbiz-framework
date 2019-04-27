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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.service.*;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.webapp.event.EventHandlerException;

/**
 * SurveyEvents Class
 */
public class SurveyEvents {

    public static final String module = SurveyEvents.class.getName();

    public static String createSurveyResponseAndRestoreParameters(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        DispatchContext dctx = dispatcher.getDispatchContext();

        Map<String, Object> combinedMap = UtilHttp.getCombinedMap(request);

        try {
            Map<String, Object> surveyResponseMap = dctx.makeValidContext("createSurveyResponse", ModelService.IN_PARAM, combinedMap);
            Map<String, Object> surveyResponseResult = dispatcher.runSync("createSurveyResponse", surveyResponseMap);
            if (ServiceUtil.isError(surveyResponseResult)) {
                Debug.logError(ServiceUtil.getErrorMessage(surveyResponseResult), module);
                return "error";
            }
            request.setAttribute("surveyResponseId", surveyResponseResult.get("surveyResponseId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return "error";
        }

        // Check for an incoming _ORIG_PARAM_MAP_ID_, if present get the session stored parameter map and insert it's entries as request attributes
        if (combinedMap.containsKey("_ORIG_PARAM_MAP_ID_")) {
            String origParamMapId = (String) combinedMap.get("_ORIG_PARAM_MAP_ID_");
            UtilHttp.restoreStashedParameterMap(request, origParamMapId);
        }
        return "success";
    }
}
