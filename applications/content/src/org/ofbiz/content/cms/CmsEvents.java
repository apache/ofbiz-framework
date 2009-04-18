/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.content.cms;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.website.WebSiteWorker;
import org.ofbiz.widget.html.HtmlFormRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;


/**
 * CmsEvents
 */
public class CmsEvents {

    public static final String module = CmsEvents.class.getName();

    public static String cms(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ServletContext servletContext = request.getSession().getServletContext();
        HttpSession session = request.getSession();

        String webSiteId = (String) session.getAttribute("webSiteId");
        if (webSiteId == null) {
            webSiteId = WebSiteWorker.getWebSiteId(request);
            if (webSiteId == null) {
                request.setAttribute("_ERROR_MESSAGE_", "Not able to run CMS application; no webSiteId defined for WebApp!");
                return "error";
            }
        }

        // is this a default request or called from a defined request mapping
        String targetRequest = (String) request.getAttribute("targetRequestUri");
        String actualRequest = (String) request.getAttribute("thisRequestUri");
        if (targetRequest != null) {
            targetRequest = targetRequest.replaceAll("\\W", "");
        } else {
            targetRequest = "";
        }
        if (actualRequest != null) {
            actualRequest = actualRequest.replaceAll("\\W", "");
        } else {
            actualRequest = "";
        }

        // place holder for the content id
        String contentId = null;
        String mapKey = null;

        String pathInfo = request.getPathInfo();
        if (targetRequest.equals(actualRequest)) {
            // was called directly -- path info is everything after the request
            String[] pathParsed = pathInfo.split("/", 3);
            if (pathParsed != null && pathParsed.length > 2) {
                pathInfo = pathParsed[2];
            } else {
                pathInfo = null;
            }
        } // if called through the default request, there is no request in pathinfo

        // if path info is null; check for a default content
        if (pathInfo == null) {
            List<GenericValue> defaultContents = null;
            try {
                defaultContents = delegator.findByAnd("WebSiteContent", UtilMisc.toMap("webSiteId", webSiteId,
                        "webSiteContentTypeId", "DEFAULT_PAGE"), UtilMisc.toList("-fromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            defaultContents = EntityUtil.filterByDate(defaultContents);
            GenericValue defaultContent = EntityUtil.getFirst(defaultContents);
            if (defaultContent != null) {
                pathInfo = defaultContent.getString("contentId");
            }
        }

        // check for path alias first
        if (pathInfo != null) {
            // clean up the pathinfo for parsing
            pathInfo = pathInfo.trim();
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            if (pathInfo.endsWith("/")) {
                pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
            }
            Debug.log("Path INFO for Alias: " + pathInfo, module);

            GenericValue pathAlias = null;
            try {
                pathAlias = delegator.findByPrimaryKeyCache("WebSitePathAlias", UtilMisc.toMap("webSiteId", webSiteId, "pathAlias", pathInfo));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (pathAlias != null) {
                String alias = pathAlias.getString("aliasTo");
                contentId = pathAlias.getString("contentId");
                mapKey = pathAlias.getString("mapKey");
                if (contentId == null && UtilValidate.isNotEmpty(alias)) {
                    if (!alias.startsWith("/")) {
                       alias = "/" + alias;
                    }

                    RequestDispatcher rd = request.getRequestDispatcher(request.getServletPath() + alias);
                    try {
                        rd.forward(request, response);
                    } catch (ServletException e) {
                        Debug.logError(e, module);
                        return "error";
                    } catch (IOException e) {
                        Debug.logError(e, module);
                        return "error";
                    }

                    return null; // null to not process any views
                }
            }

            // process through CMS -- using the mapKey (for now)
            Locale locale = UtilHttp.getLocale(request);

            // get the contentId/mapKey from URL
            if (contentId == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Current PathInfo: " + pathInfo, module);
                if (pathInfo.indexOf("/") != -1) {
                    String[] pathSplit = pathInfo.split("/");
                    if (Debug.verboseOn()) Debug.logVerbose("Split pathinfo: " + pathSplit.length, module);
                    if (pathSplit != null && pathSplit.length > 0) {
                        contentId = pathSplit[0];
                        if (pathSplit.length > 1) {
                            mapKey = pathSplit[1];
                        }
                    }
                } else {
                    contentId = pathInfo;
                }
            }

            // verify the request content is associated with the current website
            boolean websiteOk;
            try {
                websiteOk = verifyContentToWebSite(delegator, webSiteId, contentId);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                throw new GeneralRuntimeException(e.getMessage(), e);
            }

            if (websiteOk) {
                // create the template map
                MapStack<String> templateMap = MapStack.create();
                ScreenRenderer.populateContextForRequest(templateMap, null, request, response, servletContext);
                templateMap.put("formStringRenderer", new HtmlFormRenderer(request, response));

                // make the link prefix
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                templateMap.put("_REQUEST_HANDLER_", rh);

                // NOTE DEJ20080817: this is done in the ContentMapFacade class now to avoid problems with the jsessionid being in the middle of the URL and such
                //String contextLinkPrefix = rh.makeLink(request, response, "", true, false, true);
                //templateMap.put("_CONTEXT_LINK_PREFIX_", contextLinkPrefix);

                Writer writer;
                try {
                    // use UtilJ2eeCompat to get this setup properly
                    boolean useOutputStreamNotWriter = false;
                    if (servletContext != null) {
                        useOutputStreamNotWriter = UtilJ2eeCompat.useOutputStreamNotWriter(servletContext);
                    }
                    if (useOutputStreamNotWriter) {
                        ServletOutputStream ros = response.getOutputStream();
                        writer = new OutputStreamWriter(ros, "UTF-8");
                    } else {
                        writer = response.getWriter();
                    }

                    // render
                    if (UtilValidate.isEmpty(mapKey)) {
                        ContentWorker.renderContentAsText(dispatcher, delegator, contentId, writer, templateMap, locale, "text/html", true);
                    } else {
                        ContentWorker.renderSubContentAsText(dispatcher, delegator, contentId, writer, mapKey, templateMap, locale, "text/html", true);
                    }

                } catch (IOException e) {
                    throw new GeneralRuntimeException("Error in the response writer/output stream: " + e.toString(), e);
                } catch (GeneralException e) {
                    throw new GeneralRuntimeException("Error rendering content: " + e.toString(), e);
                }

                return "success";
            } else {
                String contentName = null;
                String siteName = null;
                try {
                    GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                    if (UtilValidate.isNotEmpty(content)) {
                        contentName = content.getString("contentName");
                    }
                    siteName = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId)).getString("siteName");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                request.setAttribute("_ERROR_MESSAGE_", "Content: " + contentName + " [" + contentId + "] is not a publish point for the current website: "+ siteName + " [" + webSiteId + "]");
                return "error";
            }
        }
        String siteName = null;
        try {
            siteName = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId)).getString("siteName");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        request.setAttribute("_ERROR_MESSAGE_", "Not able to find a page to display for website: "+ siteName + " [" + webSiteId + "] not even a default page!");
        return "error";
        // throw an unknown request error
        //throw new GeneralRuntimeException("Unknown request; this request does not exist or cannot be called directly.");
    }

    protected static boolean verifyContentToWebSite(GenericDelegator delegator, String webSiteId, String contentId) throws GeneralException {
        // first check if the passed in contentId is a publish point for the web site
        List<GenericValue> publishPoints = null;
        try {
            publishPoints = delegator.findByAndCache("WebSiteContent",
                    UtilMisc.toMap("webSiteId", webSiteId, "contentId", contentId, "webSiteContentTypeId", "PUBLISH_POINT"),
                    UtilMisc.toList("-fromDate"));
        } catch (GenericEntityException e) {
            throw e;
        }

        publishPoints = EntityUtil.filterByDate(publishPoints);
        if (UtilValidate.isNotEmpty(publishPoints)) {
            if (Debug.verboseOn()) Debug.logVerbose("Found publish points: " + publishPoints, module);
            return true;
        } else {
            // the passed in contentId is not a publish point for the web site;
            // however we will publish its content if it is a node of one of the trees that have a publish point as the root
            List<GenericValue> topLevelContentValues = delegator.findByAndCache("WebSiteContent",
                UtilMisc.toMap("webSiteId", webSiteId, "webSiteContentTypeId", "PUBLISH_POINT"), UtilMisc.toList("-fromDate"));
            topLevelContentValues = EntityUtil.filterByDate(topLevelContentValues);
            if (topLevelContentValues != null) {
                for (GenericValue point: topLevelContentValues) {
                    if (verifySubContent(delegator, contentId, point.getString("contentId"))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected static boolean verifySubContent(GenericDelegator delegator, String contentId, String contentIdFrom) throws GeneralException {
        List<GenericValue> contentAssoc = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdFrom, "contentIdTo", contentId, "contentAssocTypeId", "SUB_CONTENT"));
        contentAssoc = EntityUtil.filterByDate(contentAssoc);
        if (contentAssoc == null || contentAssoc.size() == 0) {
            List<GenericValue> assocs = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdFrom));
            assocs = EntityUtil.filterByDate(assocs);
            if (assocs != null) {
                for (GenericValue assoc: assocs) {
                    if (verifySubContent(delegator, contentId, assoc.getString("contentIdTo"))) {
                        return true;
                    }
                }
            }
        } else {
            if (Debug.verboseOn()) Debug.logVerbose("Found assocs: " + contentAssoc, module);
            return true;
        }

        return false;
    }
}
