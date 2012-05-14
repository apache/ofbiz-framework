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

package org.ofbiz.content.content;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.UrlServletHelper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.webapp.control.ContextFilter;
import org.owasp.esapi.errors.EncodingException;

public class ContentUrlFilter extends ContextFilter {
    public final static String module = ContentUrlFilter.class.getName();
    
    public static final String CONTROL_MOUNT_POINT = "control";
    protected static String defaultLocaleString = null;
    protected static String redirectUrl = null;
    public static String defaultViewRequest = "contentViewInfo";
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)  throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");
        
        //Get ServletContext
        ServletContext servletContext = config.getServletContext();
        //Set request attribute and session
        UrlServletHelper.setRequestAttributes(request, delegator, servletContext);
        String urlContentId = null;
        StringBuffer pathInfoBuffer = UtilHttp.getFullRequestUrl(httpRequest);
        String pathInfo = pathInfoBuffer.toString();
        if (UtilValidate.isNotEmpty(pathInfo)) {
            String alternativeUrl = pathInfo.substring(pathInfo.lastIndexOf("/"));
            if (alternativeUrl.endsWith("-content")) {
                try {
                    List<GenericValue> contentDataResourceViews = delegator.findByAnd("ContentDataResourceView", UtilMisc.toMap("drObjectInfo", alternativeUrl), null, false);
                    if (contentDataResourceViews.size() > 0) {
                        contentDataResourceViews = EntityUtil.orderBy(contentDataResourceViews, UtilMisc.toList("createdDate DESC"));
                        GenericValue contentDataResourceView = EntityUtil.getFirst(contentDataResourceViews);
                        List<GenericValue> contents = EntityUtil.filterByDate(delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentAssocTypeId", "ALTERNATIVE_URL", "contentIdTo", contentDataResourceView.getString("contentId")), null, false));
                        if (contents.size() > 0) {
                            GenericValue content = EntityUtil.getFirst(contents);
                            urlContentId = content.getString("contentId");
                        }
                    }
                } catch (Exception e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            if (UtilValidate.isNotEmpty(urlContentId)) {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append("/" + CONTROL_MOUNT_POINT);
                urlBuilder.append("/" + config.getInitParameter("viewRequest") + "?contentId=" + urlContentId);
                
                //Set view query parameters
                UrlServletHelper.setViewQueryParameters(request, urlBuilder);
                Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", module);
                RequestDispatcher dispatch = request.getRequestDispatcher(urlBuilder.toString());
                dispatch.forward(request, response);
                return;
            }
            
            //Check path alias
            UrlServletHelper.checkPathAlias(request, httpResponse, delegator, pathInfo);
        }
        // we're done checking; continue on
        chain.doFilter(request, response);
    }
    
    public static String makeContentAltUrl(HttpServletRequest request, HttpServletResponse response, String contentId, String viewContent) {
        if (UtilValidate.isEmpty(contentId)) {
            return null;
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String url = null;
        try {
            List<EntityCondition> expr = FastList.newInstance();
            expr.add(EntityCondition.makeCondition("caContentAssocTypeId", EntityOperator.EQUALS, "ALTERNATIVE_URL"));
            expr.add(EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null));
            expr.add(EntityCondition.makeCondition("contentIdStart", EntityOperator.EQUALS, contentId));
            Set<String> fieldsToSelect = UtilMisc.toSet("contentIdStart", "drObjectInfo", "dataResourceId", "caFromDate", "caThruDate", "caCreatedDate");
            List<GenericValue> contentAssocDataResources = delegator.findList("ContentAssocDataResourceViewTo", EntityCondition.makeCondition(expr), fieldsToSelect, UtilMisc.toList("-caFromDate"), null, true);
            if (contentAssocDataResources.size() > 0) {
                GenericValue contentAssocDataResource = EntityUtil.getFirst(contentAssocDataResources);
                url = contentAssocDataResource.getString("drObjectInfo");
                try {
                    url = StringUtil.defaultWebEncoder.decodeFromURL(url);
                    String mountPoint = request.getContextPath();
                    if (!(mountPoint.equals("/")) && !(mountPoint.equals(""))) {
                        url = mountPoint + url;
                    }
                } catch (EncodingException e) {
                    Debug.logError(e, module);
                }
            }
        } catch (Exception e) {
            Debug.logWarning("[Exception] : " + e.getMessage(), module);
        }
         
        if (UtilValidate.isEmpty(url)) {
            if (UtilValidate.isEmpty(viewContent)) {
                viewContent = defaultViewRequest;
            }
            url = makeContentUrl(request, response, contentId, viewContent);
        }
        return url;
    }
    
    public static String makeContentUrl(HttpServletRequest request, HttpServletResponse response, String contentId, String viewContent) {
        if (UtilValidate.isEmpty(contentId)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(request.getSession().getServletContext().getContextPath());
        if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(CONTROL_MOUNT_POINT);
        
        if (UtilValidate.isNotEmpty(viewContent)) {
            urlBuilder.append("/" + viewContent);
        } else {
            urlBuilder.append("/" + defaultViewRequest);
        }
        urlBuilder.append("?contentId=" + contentId);
        return urlBuilder.toString();
    }
}
