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
package org.apache.ofbiz.content.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.view.AbstractViewHandler;
import org.apache.ofbiz.webapp.view.ViewHandlerException;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

public class SimpleContentViewHandler extends AbstractViewHandler {

    public static final String module = SimpleContentViewHandler.class.getName();
    private String rootDir = null;
    private String https = null;

    public void init(ServletContext context) throws ViewHandlerException {
        rootDir = context.getRealPath("/");
        https = (String) context.getAttribute("https");
    }
    /**
     * @see org.apache.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        String contentId = request.getParameter("contentId");
        String rootContentId = request.getParameter("rootContentId");
        String mapKey = request.getParameter("mapKey");
        String contentAssocTypeId = request.getParameter("contentAssocTypeId");
        String fromDateStr = request.getParameter("fromDate");
        String dataResourceId = request.getParameter("dataResourceId");
        String contentRevisionSeqId = request.getParameter("contentRevisionSeqId");
        String mimeTypeId = request.getParameter("mimeTypeId");
        Locale locale = UtilHttp.getLocale(request);
        String webSiteId = WebSiteWorker.getWebSiteId(request);

        try {
            if (Debug.verboseOn()) Debug.logVerbose("dataResourceId:" + dataResourceId, module);
            Delegator delegator = (Delegator)request.getAttribute("delegator");
            if (UtilValidate.isEmpty(dataResourceId)) {
                if (UtilValidate.isEmpty(contentRevisionSeqId)) {
                    if (UtilValidate.isEmpty(mapKey) && UtilValidate.isEmpty(contentAssocTypeId)) {
                        if (UtilValidate.isNotEmpty(contentId)) {
                            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
                            dataResourceId = content.getString("dataResourceId");
                        }
                        if (Debug.verboseOn()) Debug.logVerbose("dataResourceId:" + dataResourceId, module);
                    } else {
                        Timestamp fromDate = null;
                        if (UtilValidate.isNotEmpty(fromDateStr)) {
                            try {
                                fromDate = UtilDateTime.stringToTimeStamp(fromDateStr, null, locale);
                            } catch (ParseException e) {
                                fromDate = UtilDateTime.nowTimestamp();
                            }
                        }
                        List<String> assocList = null;
                        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
                            assocList = UtilMisc.toList(contentAssocTypeId);
                        }
                        GenericValue content = ContentWorker.getSubContent(delegator, contentId, mapKey, null, null, assocList, fromDate);
                        dataResourceId = content.getString("dataResourceId");
                        if (Debug.verboseOn()) Debug.logVerbose("dataResourceId:" + dataResourceId, module);
                    }
                } else {
                    GenericValue contentRevisionItem = EntityQuery.use(delegator).from("ContentRevisionItem").where("contentId", rootContentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId).cache().queryOne();
                    if (contentRevisionItem == null) {
                        throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + rootContentId
                                                       + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                    }
                    dataResourceId = contentRevisionItem.getString("newDataResourceId");
                    if (Debug.verboseOn()) Debug.logVerbose("contentRevisionItem:" + contentRevisionItem, module);
                    if (Debug.verboseOn()) Debug.logVerbose("contentId=" + rootContentId + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                    if (Debug.verboseOn()) Debug.logVerbose("dataResourceId:" + dataResourceId, module);
                }
            }
            if (UtilValidate.isNotEmpty(dataResourceId)) {
                GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
                // DEJ20080717: why are we rendering the DataResource directly instead of rendering the content?
                ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer.array());
                // setup character encoding and content type
                String charset = dataResource.getString("characterSetId");
                if (UtilValidate.isEmpty(charset)) {
                    charset = encoding;
                }
                if (UtilValidate.isEmpty(mimeTypeId)) {
                    mimeTypeId = DataResourceWorker.getMimeType(dataResource);
                    if ("text/html".equalsIgnoreCase(mimeTypeId)) {
                        mimeTypeId = "application/octet-stream";
                    }
                }
                // setup content type
                String contentType2 = UtilValidate.isNotEmpty(mimeTypeId) ? mimeTypeId + "; charset=" +charset : contentType;
                String fileName = null;
                if (UtilValidate.isNotEmpty(dataResource.getString("dataResourceName"))) {
                    fileName = dataResource.getString("dataResourceName").replace(" ", "_"); // spaces in filenames can be a problem
                }

                // see if data resource is public or not
                String isPublic = dataResource.getString("isPublic");
                if (UtilValidate.isEmpty(isPublic)) {
                    isPublic = "N";
                }
                // get the permission service required for streaming data; default is always the genericContentPermission
                String permissionService = EntityUtilProperties.getPropertyValue("content", "stream.permission.service", "genericContentPermission", delegator);

                // not public check security
                if (!"Y".equalsIgnoreCase(isPublic)) {
                    // do security check
                    Map<String, ? extends Object> permSvcCtx = UtilMisc.toMap("userLogin", userLogin, "locale", locale, "mainAction", "VIEW", "contentId", contentId);
                    Map<String, Object> permSvcResp;
                    try {
                        permSvcResp = dispatcher.runSync(permissionService, permSvcCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                        throw new ViewHandlerException(e.getMessage());
                    }
                    if (ServiceUtil.isError(permSvcResp)) {
                        String errorMsg = ServiceUtil.getErrorMessage(permSvcResp);
                        Debug.logError(errorMsg, module);
                        request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                        throw new ViewHandlerException(errorMsg);
                    }

                    // no service errors; now check the actual response
                    Boolean hasPermission = (Boolean) permSvcResp.get("hasPermission");
                    if (!hasPermission.booleanValue()) {
                        String errorMsg = (String) permSvcResp.get("failMessage");
                        Debug.logError(errorMsg, module);
                        request.setAttribute("_ERROR_MESSAGE_", errorMsg);
                        throw new ViewHandlerException(errorMsg);
                    }
                }
                UtilHttp.streamContentToBrowser(response, bais, byteBuffer.limit(), contentType2, fileName);
            }
        } catch (GenericEntityException e) {
            throw new ViewHandlerException(e.getMessage());
        } catch (IOException e) {
            throw new ViewHandlerException(e.getMessage());
        } catch (GeneralException e) {
            throw new ViewHandlerException(e.getMessage());
        }
    }
}
