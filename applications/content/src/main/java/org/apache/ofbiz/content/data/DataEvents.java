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
package org.apache.ofbiz.content.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * DataEvents Class
 */
public class DataEvents {

    private static final String MODULE = DataEvents.class.getName();
    private static final String ERR_RESOURCE = "ContentErrorUiLabels";

    public static String uploadImage(HttpServletRequest request, HttpServletResponse response) {
        return DataResourceWorker.uploadAndStoreImage(request, "dataResourceId", "imageData");
    }

    /**
     * Streams any binary content data to the browser.
     * <p>Supersedes {@link org.apache.ofbiz.content.data.DataEvents#serveImage(HttpServletRequest, HttpServletResponse) DataEvents#serveImage()}</p>
     */
    public static String serveObjectData(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        String userAgent = request.getHeader("User-Agent");

        Map<String, Object> httpParams = UtilHttp.getParameterMap(request);
        String contentId = (String) httpParams.get("contentId");
        if (UtilValidate.isEmpty(contentId)) {
            String errorMsg = "Required parameter contentId not found!";
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        // get the permission service required for streaming data; default is always the genericContentPermission
        String permissionService = EntityUtilProperties.getPropertyValue("content", "stream.permission.service",
                "genericContentPermission", delegator);

        // This is counterintuitive but it works, for OFBIZ-11840
        // It could be improved by checking for possible events associated with svg
        // As listed at https://portswigger.net/web-security/cross-site-scripting/cheat-sheet
        if (contentId.contains("<svg")) {
            return "success";
        }

        // get the content record
        GenericValue content;
        try {
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        // make sure content exists
        if (content == null) {
            String errorMsg = "No content found for Content ID: " + contentId;
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        // make sure there is a DataResource for this content
        String dataResourceId = content.getString("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            String errorMsg = "No Data Resource found for Content ID: " + contentId;
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        // get the data RESOURCE
        GenericValue dataResource;
        try {
            dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        // make sure the data RESOURCE exists
        if (dataResource == null) {
            String errorMsg = "No Data Resource found for ID: " + dataResourceId;
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        // see if data RESOURCE is public or not
        String isPublic = dataResource.getString("isPublic");
        if (UtilValidate.isEmpty(isPublic)) {
            isPublic = "N";
        }

        // not public check security
        if (!"Y".equalsIgnoreCase(isPublic)) {
            // do security check
            Map<String, ? extends Object> permSvcCtx = UtilMisc.toMap("userLogin", userLogin, "locale", locale, "mainAction",
                    "VIEW", "contentId", contentId);
            Map<String, Object> permSvcResp;
            try {
                permSvcResp = dispatcher.runSync(permissionService, permSvcCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            if (ServiceUtil.isError(permSvcResp)) {
                String errorMsg = ServiceUtil.getErrorMessage(permSvcResp);
                Debug.logError(errorMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                return "error";
            }

            // no service errors; now check the actual response
            Boolean hasPermission = (Boolean) permSvcResp.get("hasPermission");
            if (!hasPermission) {
                String errorMsg = (String) permSvcResp.get("failMessage");
                Debug.logError(errorMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                return "error";
            }
        }

        // get objects needed for data processing
        String contextRoot = (String) request.getAttribute("_CONTEXT_ROOT_");
        String webSiteId = (String) session.getAttribute("webSiteId");
        String dataName = dataResource.getString("dataResourceName");

        // get the mime type
        String mimeType = DataResourceWorker.getMimeType(dataResource);

        // hack for IE and mime types
        if (UtilValidate.isNotEmpty(userAgent) && userAgent.indexOf("MSIE") > -1) {
            Debug.logInfo("Found MSIE changing mime type from - " + mimeType, MODULE);
            mimeType = "application/octet-stream";
        }

        // for local resources; use HTTPS if we are requested via HTTPS
        String https = "false";
        String protocol = request.getProtocol();
        if ("https".equalsIgnoreCase(protocol)) {
            https = "true";
        }

        // get the data RESOURCE stream and content length
        Map<String, Object> resourceData;
        try {
            resourceData = DataResourceWorker.getDataResourceStream(dataResource, https, webSiteId, locale, contextRoot, false);
        } catch (IOException | GeneralException e) {
            Debug.logError(e, "Error getting DataResource stream", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        // get the stream data
        InputStream stream = null;
        Long length = null;

        if (resourceData != null) {
            stream = (InputStream) resourceData.get("stream");
            length = (Long) resourceData.get("length");
        }
        Debug.logInfo("Got RESOURCE data stream: " + length + " bytes", MODULE);

        // stream the content to the browser
        if (stream != null && length != null) {
            try {
                UtilHttp.streamContentToBrowser(response, stream, length.intValue(), mimeType, dataName);
            } catch (IOException e) {
                Debug.logError(e, "Unable to write content to browser", MODULE);
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                // this must be handled with a special error string because the output stream has been already used and we will not be able
                // to return the error page;
                // the "io-error" should be associated to a response of type "none"
                return "io-error";
            }
        } else {
            String errorMsg = "No data is available.";
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        return "success";
    }

    /**
     * Streams ImageDataResource data to the output.
     * <p>Superseded by {@link org.apache.ofbiz.content.data.DataEvents#serveObjectData(HttpServletRequest, HttpServletResponse)
     * DataEvents#serveObjectData}</p>
     */
    @Deprecated
    public static String serveImage(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ServletContext application = session.getServletContext();

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, Object> parameters = UtilHttp.getParameterMap(request);

        Debug.logInfo("Img UserAgent - " + request.getHeader("User-Agent"), MODULE);

        String dataResourceId = (String) parameters.get("imgId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            String errorMsg = "Error getting image record from db: " + " dataResourceId is empty";
            Debug.logError(errorMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg);
            return "error";
        }

        try {
            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
            if (!"Y".equals(dataResource.getString("isPublic"))) {
                // now require login...
                GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
                if (userLogin == null) {
                    String errorMsg = "You must be logged in to download the Data Resource with ID [" + dataResourceId + "]";
                    Debug.logError(errorMsg, MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                    return "error";
                }

                // make sure the logged in user can download this content; otherwise is a pretty big security hole for DataResource records...
                // TODO: should we restrict the roleTypeId?
                long contentAndRoleCount = EntityQuery.use(delegator).from("ContentAndRole")
                        .where("partyId", userLogin.get("partyId"),
                                "dataResourceId", dataResourceId)
                        .queryCount();
                if (contentAndRoleCount == 0) {
                    String errorMsg = "You do not have permission to download the Data Resource with ID [" + dataResourceId
                            + "], ie you are not associated with it.";
                    Debug.logError(errorMsg, MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                    return "error";
                }
            }

            String mimeType = DataResourceWorker.getMimeType(dataResource);

            // hack for IE and mime types
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.indexOf("MSIE") > -1) {
                Debug.logInfo("Found MSIE changing mime type from - " + mimeType, MODULE);
                mimeType = "application/octet-stream";
            }

            if (mimeType != null) {
                response.setContentType(mimeType);
            }
            OutputStream os = response.getOutputStream();
            Map<String, Object> resourceData = DataResourceWorker.getDataResourceStream(dataResource, "",
                    application.getInitParameter("webSiteId"), UtilHttp.getLocale(request), application.getRealPath("/"), false);
            os.write(IOUtils.toByteArray((InputStream) resourceData.get("stream")));
            os.flush();
        } catch (GeneralException | IOException e) {
            String errMsg = "Error downloading digital product content: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }


    /** Dual create and edit event.
     *  Needed to make permission criteria available to services.
     */
    public static String persistDataResource(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = null;
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String dataResourceId;
        GenericValue dataResource = delegator.makeValue("DataResource");
        dataResource.setPKFields(paramMap);
        dataResource.setNonPKFields(paramMap);
        Map<String, Object> serviceInMap = UtilMisc.makeMapWritable(dataResource);
        serviceInMap.put("userLogin", userLogin);
        String mode = (String) paramMap.get("mode");
        Locale locale = UtilHttp.getLocale(request);

        try {
            if (mode != null && "UPDATE".equals(mode)) {
                result = dispatcher.runSync("updateDataResource", serviceInMap);
                if (ServiceUtil.isError(result)) {
                    String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "dataEvents.error_call_update_service", locale);
                    String errorMsg = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMsg, MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
            } else {
                mode = "CREATE";
                result = dispatcher.runSync("createDataResource", serviceInMap);
                if (ServiceUtil.isError(result)) {
                    String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "dataEvents.error_call_create_service", locale);
                    String errorMsg = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMsg, MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
                dataResourceId = (String) result.get("dataResourceId");
                dataResource.set("dataResourceId", dataResourceId);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }

        String returnStr = "success";
        if ("CREATE".equals(mode)) {
            // Set up return message to guide selection of follow on view
            request.setAttribute("dataResourceId", result.get("dataResourceId"));
            String dataResourceTypeId = (String) serviceInMap.get("dataResourceTypeId");
            if (dataResourceTypeId != null) {
                if ("ELECTRONIC_TEXT".equals(dataResourceTypeId)
                        || "IMAGE_OBJECT".equals(dataResourceTypeId)) {
                    returnStr = dataResourceTypeId;
                }
            }
        }

        return returnStr;
    }
}
