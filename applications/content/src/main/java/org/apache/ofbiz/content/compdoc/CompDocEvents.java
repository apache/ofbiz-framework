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
package org.apache.ofbiz.content.compdoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.event.CoreEvents;
import org.apache.ofbiz.webapp.website.WebSiteWorker;


/**
 * CompDocEvents Class
 */
public class CompDocEvents {
    public static final String module = CompDocEvents.class.getName();

    /**
     *
     * @param request
     * @param response
     * @return
     *
     * Creates the topmost Content entity of a Composite Document tree.
     * Also creates an "empty" Composite Document Instance Content entity.
     * Creates ContentRevision/Item records for each, as well.
     */

    public static String persistRootCompDoc(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Locale locale = UtilHttp.getLocale(request);
        HttpSession session = request.getSession();
        //Security security = (Security)request.getAttribute("security");
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        String contentId = (String)paramMap.get("contentId");
        //String instanceContentId = null;

        if (UtilValidate.isNotEmpty(contentId)) {
            try {
                EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error running serviceName persistContentAndAssoc", module);
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.error_modelservice_for_srv_name", locale);
                request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + " [" + "persistContentAndAssoc" + "]: " + e.toString());
                return "error";
           }
        }

        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService("persistContentAndAssoc");
        } catch (GenericServiceException e) {
            String errMsg = "Error getting model service for serviceName, 'persistContentAndAssoc'. " + e.toString();
            Debug.logError(errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + "</li>");
            return "error";
        }
        Map<String, Object> persistMap = modelService.makeValid(paramMap, ModelService.IN_PARAM);
        persistMap.put("userLogin", userLogin);
        try {
            Map<String, Object> persistResult = dispatcher.runSync("persistContentAndAssoc", persistMap);
            if (ServiceUtil.isError(persistResult)) {
                String errMsg = "Error running serviceName, 'persistContentAndAssoc'. " + ServiceUtil.getErrorMessage(persistResult);
                request.setAttribute("_ERROR_MESSAGE_",  "<li>" + errMsg + "</li>");
                Debug.logError(errMsg, module);
                return "error";
            }
            contentId = (String)persistResult.get("contentId");
            //request.setAttribute("contentId", contentId);
            for (Entry<String, Object> entry : persistResult.entrySet()) {
                Object obj = entry.getValue();
                Object val = persistResult.get(obj);
                request.setAttribute(obj.toString(), val);
            }
            // Update ContentRevision and ContentRevisonItem
            Map<String, Object> contentRevisionMap = new HashMap<>();
            contentRevisionMap.put("itemContentId", contentId);
            contentRevisionMap.put("contentId", contentId);
            contentRevisionMap.put("userLogin", userLogin);
            Map<String, Object> result = dispatcher.runSync("persistContentRevisionAndItem", contentRevisionMap);
            if (ServiceUtil.isError(result)) {
                String errMsg = "Error running serviceName, 'persistContentRevisionAndItem'. " + ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_",  "<li>" + errMsg + "</li>");
                Debug.logError(errMsg, module);
                return "error";
            }
            for (Entry<String, Object> entry : result.entrySet()) {
                Object obj = entry.getValue();
                Object val = result.get(obj);
                request.setAttribute(obj.toString(), val);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error running serviceName, 'persistContentAndAssoc'. " + e.toString();
            Debug.logError(errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", "<li>" + errMsg + "</li>");
            return "error";
        }
        return "success";
    }

    public static String padNumberWithLeadingZeros(Long num, Integer padLen) {
        String s = UtilFormatOut.formatPaddedNumber(num, padLen);
        return s;
    }

    public static String genCompDocPdf(HttpServletRequest request, HttpServletResponse response) {
        String responseStr = "success";
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        ServletContext servletContext = session.getServletContext();
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String contentId = (String)paramMap.get("contentId");
        Locale locale = UtilHttp.getLocale(request);
        String webSiteId = WebSiteWorker.getWebSiteId(request);
        
        String rootDir = servletContext.getRealPath("/");
        String https = (String) servletContext.getAttribute("https");

        Map<String, Object> mapIn = new HashMap<>();
        mapIn.put("contentId", contentId);
        mapIn.put("locale", locale);
        mapIn.put("rootDir", rootDir);
        mapIn.put("webSiteId", webSiteId);
        mapIn.put("https", https);
        mapIn.put("userLogin", userLogin);

        Map<String, Object> results = null;
        try {
            results = dispatcher.runSync("renderCompDocPdf", mapIn);
            if (ServiceUtil.isError(results)) {
                String errorMessage = ServiceUtil.getErrorMessage(results);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, module);
                return "error";
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }

        ByteBuffer outByteBuffer = (ByteBuffer) results.get("outByteBuffer");

        // setup content type
        String contentType = "application/pdf; charset=ISO-8859-1";

        try (ByteArrayInputStream bais = new ByteArrayInputStream(outByteBuffer.array())) {
            UtilHttp.streamContentToBrowser(response, bais, outByteBuffer.limit(), contentType);
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }
        return responseStr;
    }
    public static String genContentPdf(HttpServletRequest request, HttpServletResponse response) {
        String responseStr = "success";
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        ServletContext servletContext = session.getServletContext();
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String contentId = (String)paramMap.get("contentId");
        Locale locale = UtilHttp.getLocale(request);
        String webSiteId = WebSiteWorker.getWebSiteId(request);

        String rootDir = servletContext.getRealPath("/");
        String https = (String) servletContext.getAttribute("https");

        Map<String, Object> mapIn = new HashMap<>();
        mapIn.put("contentId", contentId);
        mapIn.put("locale", locale);
        mapIn.put("rootDir", rootDir);
        mapIn.put("webSiteId", webSiteId);
        mapIn.put("https", https);
        mapIn.put("userLogin", userLogin);

        Map<String, Object> results = null;
        try {
            results = dispatcher.runSync("renderContentPdf", mapIn);
            if (ServiceUtil.isError(results)) {
                String errorMessage = ServiceUtil.getErrorMessage(results);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, module);
                return "error";
            }
        } catch (GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }

        ByteBuffer outByteBuffer = (ByteBuffer) results.get("outByteBuffer");

        // setup content type
        String contentType = "application/pdf; charset=ISO-8859-1";

        try (ByteArrayInputStream bais = new ByteArrayInputStream(outByteBuffer.array())) {
            UtilHttp.streamContentToBrowser(response, bais, outByteBuffer.limit(), contentType);
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }
        return responseStr;
    }
}
