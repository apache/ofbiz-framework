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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.base.util.GeneralException;

/**
 * Uses XSL-FO formatted templates to generate PDF views
 * This handler will use JPublish to generate the XSL-FO
 */
public class SimpleContentViewHandler implements ViewHandler {
    
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
    	String dataResourceId = request.getParameter("dataResourceId");
        String contentRevisionSeqId = request.getParameter("contentRevisionSeqId");
        String mimeTypeId = request.getParameter("mimeTypeId");
        ByteWrapper byteWrapper = null;
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
            Debug.logInfo("SCVH(0a)- dataResourceId:" + dataResourceId, module);
            GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
            if (UtilValidate.isEmpty(dataResourceId)) {
                if (UtilValidate.isEmpty(contentRevisionSeqId)) {
                   GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                   dataResourceId = content.getString("dataResourceId");
                   Debug.logInfo("SCVH(0b)- dataResourceId:" + dataResourceId, module);
                } else {
                   GenericValue contentRevisionItem = delegator.findByPrimaryKeyCache("ContentRevisionItem", UtilMisc.toMap("contentId", rootContentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId));
                   if (contentRevisionItem == null) {
                       throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + rootContentId
                                                      + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                   }
                   Debug.logInfo("SCVH(1)- contentRevisionItem:" + contentRevisionItem, module);
                   Debug.logInfo("SCVH(2)-contentId=" + rootContentId
                           + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                   dataResourceId = contentRevisionItem.getString("newDataResourceId");
                   Debug.logInfo("SCVH(3)- dataResourceId:" + dataResourceId, module);
                }
    		}
			GenericValue dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
    		byteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, dataResourceId, https, webSiteId, locale, rootDir);
    		ByteArrayInputStream bais = new ByteArrayInputStream(byteWrapper.getBytes());
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
            	charset = "ISO-8859-1";
            }

            // setup content type
            String contentType2 = UtilValidate.isNotEmpty(mimeTypeId) ? mimeTypeId + "; charset=" +charset : contentType;

            UtilHttp.streamContentToBrowser(response, bais, byteWrapper.getLength(), contentType2);
    	} catch(GenericEntityException e) {
            throw new ViewHandlerException(e.getMessage());
    	} catch(IOException e) {
            throw new ViewHandlerException(e.getMessage());
    	} catch(GeneralException e) {
            throw new ViewHandlerException(e.getMessage());
    	}
     }
}
