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
package org.ofbiz.content.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;

/**
 * Uses XSL-FO formatted templates to generate PDF views
 * This handler will use JPublish to generate the XSL-FO
 */
public class SimpleContentViewHandler extends AbstractViewHandler {

    public static final String module = SimpleContentViewHandler.class.getName();
    protected ServletContext servletContext = null;

    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }
    /**
     * @see org.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        String contentId = request.getParameter("contentId");
        String rootContentId = request.getParameter("rootContentId");
        String mapKey = request.getParameter("mapKey");
        String contentAssocTypeId = request.getParameter("contentAssocTypeId");
        String fromDateStr = request.getParameter("fromDate");
        String dataResourceId = request.getParameter("dataResourceId");
        String contentRevisionSeqId = request.getParameter("contentRevisionSeqId");
        String mimeTypeId = request.getParameter("mimeTypeId");
        Locale locale = UtilHttp.getLocale(request);
        String rootDir = null;
        String webSiteId = null;
        String https = null;

        if (UtilValidate.isEmpty(rootDir)) {
            rootDir = servletContext.getRealPath("/");
        }
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = (String) servletContext.getAttribute("webSiteId");
        }
        if (UtilValidate.isEmpty(https)) {
            https = (String) servletContext.getAttribute("https");
        }
        try {
            if (Debug.verboseOn()) Debug.logVerbose("SCVH(0a)- dataResourceId:" + dataResourceId, module);
            Delegator delegator = (Delegator)request.getAttribute("delegator");
            if (UtilValidate.isEmpty(dataResourceId)) {
                if (UtilValidate.isEmpty(contentRevisionSeqId)) {
                    if (UtilValidate.isEmpty(mapKey) && UtilValidate.isEmpty(contentAssocTypeId)) {
                        GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                        dataResourceId = content.getString("dataResourceId");
                        if (Debug.verboseOn()) Debug.logVerbose("SCVH(0b)- dataResourceId:" + dataResourceId, module);
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
                        if (Debug.verboseOn()) Debug.logVerbose("SCVH(0b)- dataResourceId:" + dataResourceId, module);
                    }
                } else {
                    GenericValue contentRevisionItem = delegator.findByPrimaryKeyCache("ContentRevisionItem", UtilMisc.toMap("contentId", rootContentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId));
                    if (contentRevisionItem == null) {
                        throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + rootContentId
                                                       + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                    }
                    dataResourceId = contentRevisionItem.getString("newDataResourceId");
                    if (Debug.verboseOn()) Debug.logVerbose("SCVH(1)- contentRevisionItem:" + contentRevisionItem, module);
                    if (Debug.verboseOn()) Debug.logVerbose("SCVH(2)-contentId=" + rootContentId + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                    if (Debug.verboseOn()) Debug.logVerbose("SCVH(3)- dataResourceId:" + dataResourceId, module);
                }
            }
            GenericValue dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            // DEJ20080717: why are we rendering the DataResource directly instead of rendering the content?
            ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
            ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer.array());
            // hack for IE and mime types
            //String userAgent = request.getHeader("User-Agent");
            //if (userAgent.indexOf("MSIE") > -1) {
            //    Debug.log("Found MSIE changing mime type from - " + mimeTypeId, module);
            //    mimeTypeId = "application/octet-stream";
            //}
            // setup chararcter encoding and content type
            String charset = dataResource.getString("characterSetId");
            mimeTypeId = dataResource.getString("mimeTypeId");
            if (UtilValidate.isEmpty(charset)) {
                charset = servletContext.getInitParameter("charset");
            }
            if (UtilValidate.isEmpty(charset)) {
                charset = "UTF-8";
            }

            // setup content type
            String contentType2 = UtilValidate.isNotEmpty(mimeTypeId) ? mimeTypeId + "; charset=" +charset : contentType;
            String fileName = null;
            if (!UtilValidate.isEmpty(dataResource.getString("dataResourceName"))) {
                fileName = dataResource.getString("dataResourceName").replace(" ", "_"); // spaces in filenames can be a problem
            }
            UtilHttp.streamContentToBrowser(response, bais, byteBuffer.limit(), contentType2, fileName);
        } catch (GenericEntityException e) {
            throw new ViewHandlerException(e.getMessage());
        } catch (IOException e) {
            throw new ViewHandlerException(e.getMessage());
        } catch (GeneralException e) {
            throw new ViewHandlerException(e.getMessage());
        }
    }
}
