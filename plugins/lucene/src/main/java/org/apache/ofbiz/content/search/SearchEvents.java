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
package org.apache.ofbiz.content.search;

import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * SearchEvents Class
 */
public class SearchEvents {

    public static final String module = SearchEvents.class.getName();

    public static String indexContentTree(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> result;
        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        serviceInMap.put("userLogin", userLogin);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String siteId = (String)paramMap.get("contentId");
        serviceInMap.put("contentId", siteId);
        try {
            result = dispatcher.runSync("indexContentTree", serviceInMap);
        } catch (GenericServiceException e) {
            String errorMsg = "Error calling the indexContentTree service." + e.toString();
            Debug.logError(e, errorMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg + e.toString());
            return "error";
        }
        String errMsg = ServiceUtil.getErrorMessage(result);
        if (UtilValidate.isEmpty(errMsg)) {
            return "success";
        } else {
            ServiceUtil.setMessages(request, errMsg, null, null);
            return "error";
        }
    }
}
