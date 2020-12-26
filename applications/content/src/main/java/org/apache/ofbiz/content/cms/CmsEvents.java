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

package org.apache.ofbiz.content.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.website.WebSiteWorker;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroFormRenderer;

import freemarker.template.TemplateException;


/**
 * CmsEvents
 */
public class CmsEvents {

    private static final String MODULE = CmsEvents.class.getName();

    public static String cms(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        ServletContext servletContext = request.getSession().getServletContext();
        HttpSession session = request.getSession();
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        Writer writer = null;
        Locale locale = UtilHttp.getLocale(request);

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

        String pathInfo = null;

        String displayMaintenancePage = (String) session.getAttribute("displayMaintenancePage");
        if (UtilValidate.isNotEmpty(displayMaintenancePage) && "Y".equalsIgnoreCase(displayMaintenancePage)) {
            try {
                writer = response.getWriter();
                GenericValue webSiteContent = EntityQuery.use(delegator).from("WebSiteContent").where("webSiteId", webSiteId,
                        "webSiteContentTypeId", "MAINTENANCE_PAGE").filterByDate().queryFirst();
                if (webSiteContent != null) {
                    ContentWorker.renderContentAsText(dispatcher, webSiteContent.getString("contentId"), writer, null, locale,
                            "text/html", null, null, true);
                    return "success";
                } else {
                    request.setAttribute("_ERROR_MESSAGE_", "Not able to display maintenance page for [" + webSiteId + "]");
                    return "error";
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            } catch (IOException e) {
                throw new GeneralRuntimeException("Error in the response writer/output stream while rendering content.", e);
            } catch (GeneralException e) {
                throw new GeneralRuntimeException("Error rendering content", e);
            }
        } else {
            // If an override view is present then use that in place of request.getPathInfo()
            String overrideViewUri = (String) request.getAttribute("_CURRENT_CHAIN_VIEW_");
            if (UtilValidate.isNotEmpty(overrideViewUri)) {
                pathInfo = overrideViewUri;
            } else {
                pathInfo = request.getPathInfo();
                if (targetRequest.equals(actualRequest) && pathInfo != null) {
                    // was called directly -- path info is everything after the request
                    String[] pathParsed = pathInfo.split("/", 3);
                    if (pathParsed.length > 2) {
                        pathInfo = pathParsed[2];
                    } else {
                        pathInfo = null;
                    }
                } // if called through the default request, there is no request in pathinfo
            }

            // if path info is null or path info is / (i.e application mounted on root); check for a default content
            if (pathInfo == null || "/".equals(pathInfo)) {
                GenericValue defaultContent = null;
                try {
                    defaultContent = EntityQuery.use(delegator).from("WebSiteContent")
                            .where("webSiteId", webSiteId, "webSiteContentTypeId", "DEFAULT_PAGE")
                            .orderBy("-fromDate").filterByDate().cache().queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
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

                GenericValue pathAlias = null;
                try {
                    pathAlias = EntityQuery.use(delegator).from("WebSitePathAlias").where("webSiteId", webSiteId, "pathAlias",
                            pathInfo).orderBy("-fromDate").cache().filterByDate().queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (pathAlias != null) {
                    String alias = pathAlias.getString("aliasTo");
                    contentId = pathAlias.getString("contentId");
                    mapKey = pathAlias.getString("mapKey");
                    if (contentId == null && UtilValidate.isNotEmpty(alias)) {
                        if (!alias.startsWith("/")) {
                            alias = "/" + alias;
                        }

                        String context = request.getContextPath();
                        String location = context + request.getServletPath();
                        GenericValue webSite = WebSiteWorker.getWebSite(request);
                        if (webSite != null && webSite.getString("hostedPathAlias") != null && !"ROOT".equals(pathInfo)) {
                            location += "/" + webSite.getString("hostedPathAlias");
                        }

                        String uriWithContext = request.getRequestURI();
                        String uri = uriWithContext.substring(context.length());
                        uri = uri.substring(0, uri.lastIndexOf('/'));

                        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                        response.setHeader("Location", location + uri + alias);
                        response.setHeader("Connection", "close");

                        return null; // null to not process any views
                    }
                }

                // get the contentId/mapKey from URL
                if (contentId == null) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Current PathInfo: " + pathInfo, MODULE);
                    }
                    String[] pathSplit = pathInfo.split("/");
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Split pathinfo: " + pathSplit.length, MODULE);
                    }
                    contentId = pathSplit[0];
                    if (pathSplit.length > 1) {
                        mapKey = pathSplit[1];
                    }
                }


                // verify the request content is associated with the current website
                int statusCode = -1;
                boolean hasErrorPage = false;

                if (contentId != null) {
                    try {
                        statusCode = verifyContentToWebSite(delegator, webSiteId, contentId);
                    } catch (GeneralException e) {
                        Debug.logError(e, MODULE);
                        throw new GeneralRuntimeException(e.getMessage(), e);
                    }
                } else {
                    statusCode = HttpServletResponse.SC_NOT_FOUND;
                }

                // We try to find a specific Error page for this website concerning the status code
                if (statusCode != HttpServletResponse.SC_OK) {
                    GenericValue errorContainer = null;
                    try {
                        errorContainer = EntityQuery.use(delegator).from("WebSiteContent")
                                .where("webSiteId", webSiteId, "webSiteContentTypeId", "ERROR_ROOT")
                                .orderBy("fromDate").filterByDate().cache().queryFirst();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }

                    if (errorContainer != null) {
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Found error containers: " + errorContainer, MODULE);
                        }

                        GenericValue errorPage = null;
                        try {
                            errorPage = EntityQuery.use(delegator).from("ContentAssocViewTo")
                                    .where("contentIdStart", errorContainer.getString("contentId"),
                                            "caContentAssocTypeId", "TREE_CHILD",
                                            "contentTypeId", "DOCUMENT",
                                            "caMapKey", String.valueOf(statusCode))
                                    .filterByDate().queryFirst();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                        if (errorPage != null) {
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Found error pages " + statusCode + " : " + errorPage, MODULE);
                            }
                            contentId = errorPage.getString("contentId");
                        } else {
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("No specific error page, falling back to the Error Container for " + statusCode, MODULE);
                            }
                            contentId = errorContainer.getString("contentId");
                        }
                        mapKey = null;
                        hasErrorPage = true;
                    }
                    // We try to find a generic content Error page concerning the status code
                    if (!hasErrorPage) {
                        try {
                            GenericValue errorPage = EntityQuery.use(delegator).from("Content").where("contentId",
                                    "CONTENT_ERROR_" + statusCode).cache().queryOne();
                            if (errorPage != null) {
                                if (Debug.verboseOn()) {
                                    Debug.logVerbose("Found generic page " + statusCode, MODULE);
                                }
                                contentId = errorPage.getString("contentId");
                                mapKey = null;
                                hasErrorPage = true;
                            }
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                }

                if (statusCode == HttpServletResponse.SC_OK || hasErrorPage) {
                    // create the template map
                    MapStack<String> templateMap = MapStack.create();
                    ScreenRenderer.populateContextForRequest(templateMap, null, request, response, servletContext);
                    templateMap.put("statusCode", statusCode);

                    // make the link prefix
                    templateMap.put("_REQUEST_HANDLER_", RequestHandler.from(request));

                    //Cache Headers
                    UtilHttp.setResponseBrowserProxyNoCache(response);
                    //Security Headers
                    UtilHttp.setResponseBrowserDefaultSecurityHeaders(response, null);

                    response.setStatus(statusCode);

                    try {
                        writer = response.getWriter();
                        // TODO: replace "screen" to support dynamic rendering of different output
                        if (visualTheme == null) {
                            String defaultVisualThemeId = EntityUtilProperties.getPropertyValue("general", "VISUAL_THEME", delegator);
                            visualTheme = ThemeFactory.getVisualThemeFromId(defaultVisualThemeId);
                        }
                        FormStringRenderer formStringRenderer = new MacroFormRenderer(visualTheme.getModelTheme()
                                .getFormRendererLocation("screen"), request, response);
                        templateMap.put("formStringRenderer", formStringRenderer);

                        // if use web analytics
                        List<GenericValue> webAnalytics = EntityQuery.use(delegator).from("WebAnalyticsConfig")
                                .where("webSiteId", webSiteId).queryList();
                        // render
                        if (UtilValidate.isNotEmpty(webAnalytics) && hasErrorPage) {
                            ContentWorker.renderContentAsText(dispatcher, contentId, writer, templateMap, locale, "text/html",
                                    null, null, true, webAnalytics);
                        } else if (UtilValidate.isEmpty(mapKey)) {
                            ContentWorker.renderContentAsText(dispatcher, contentId, writer, templateMap, locale, "text/html", null, null, true);
                        } else {
                            ContentWorker.renderSubContentAsText(dispatcher, contentId, writer, mapKey, templateMap, locale, "text/html", true);
                        }

                    } catch (TemplateException e) {
                        throw new GeneralRuntimeException(String.format(
                                "Error creating form renderer while rendering content [%s] with path alias [%s]", contentId, pathInfo), e);
                    } catch (IOException e) {
                        throw new GeneralRuntimeException(String.format(
                                "Error in the response writer/output stream while rendering content [%s] with path alias [%s]",
                                contentId, pathInfo), e);
                    } catch (GeneralException e) {
                        throw new GeneralRuntimeException(String.format(
                                "Error rendering content [%s] with path alias [%s]", contentId, pathInfo), e);
                    }

                    return "success";
                } else {
                    String contentName = null;
                    String siteName = null;
                    try {
                        GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
                        if (content != null && UtilValidate.isNotEmpty(content.getString("contentName"))) {
                            contentName = content.getString("contentName");
                        } else {
                            request.setAttribute("_ERROR_MESSAGE_", "Content: [" + contentId
                                    + "] is not a publish point for the current website: [" + webSiteId + "]");
                            return "error";
                        }
                        siteName = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache().queryOne().getString("siteName");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                    request.setAttribute("_ERROR_MESSAGE_", "Content: " + contentName + " [" + contentId
                            + "] is not a publish point for the current website: " + siteName + " [" + webSiteId + "]");
                    return "error";
                }
            }
        }
        String siteName = null;
        GenericValue webSite = null;
        try {
            webSite = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache().queryOne();
            if (webSite != null) {
                siteName = webSite.getString("siteName");
            }
            if (siteName == null) {
                siteName = "Not specified";
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (webSite != null) {
            request.setAttribute("_ERROR_MESSAGE_", "Not able to find a page to display for website: " + siteName + " ["
                    + webSiteId + "] not even a default page!");
        } else {
            request.setAttribute("_ERROR_MESSAGE_", "Not able to find a page to display, not even a default page AND the "
                    + "website entity record for WebSiteId:" + webSiteId + " could not be found");
        }
        return "error";
    }

    protected static int verifyContentToWebSite(Delegator delegator, String webSiteId, String contentId) throws GeneralException {
        // first check if the passed in contentId is a publish point for the web site
        List<GenericValue> publishPoints = null;
        boolean hadContent = false;
        try {
            publishPoints = EntityQuery.use(delegator).from("WebSiteContent")
                    .where("webSiteId", webSiteId, "contentId", contentId, "webSiteContentTypeId", "PUBLISH_POINT")
                    .orderBy("-fromDate").cache().queryList();
        } catch (GenericEntityException e) {
            throw e;
        }
        if (UtilValidate.isNotEmpty(publishPoints)) {
            hadContent = true;
        }
        publishPoints = EntityUtil.filterByDate(publishPoints);
        if (UtilValidate.isNotEmpty(publishPoints)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Found publish points: " + publishPoints, MODULE);
            }
            return HttpServletResponse.SC_OK;
        } else {
            // the passed in contentId is not a publish point for the web site;
            // however we will publish its content if it is a node of one of the trees that have a publish point as the root
            List<GenericValue> topLevelContentValues = EntityQuery.use(delegator).from("WebSiteContent")
                    .where("webSiteId", webSiteId, "webSiteContentTypeId", "PUBLISH_POINT")
                    .orderBy("-fromDate").cache().filterByDate().queryList();

            if (topLevelContentValues != null) {
                for (GenericValue point : topLevelContentValues) {
                    int subContentStatusCode = verifySubContent(delegator, contentId, point.getString("contentId"));
                    if (subContentStatusCode == HttpServletResponse.SC_OK) {
                        return HttpServletResponse.SC_OK;
                    } else if (subContentStatusCode == HttpServletResponse.SC_GONE) {
                        hadContent = true;
                    }
                }
            }
        }
        int responseCode;
        if (hadContent) {
            responseCode = HttpServletResponse.SC_GONE;
        } else {
            responseCode = HttpServletResponse.SC_NOT_FOUND;
        }
        Debug.logWarning("Could not verify contentId [" + contentId + "] to webSiteId [" + webSiteId + "], returning code: " + responseCode, MODULE);
        return responseCode;
    }

    protected static int verifySubContent(Delegator delegator, String contentId, String contentIdFrom) throws GeneralException {
        List<GenericValue> contentAssoc = EntityQuery.use(delegator).from("ContentAssoc")
                .where("contentId", contentIdFrom, "contentIdTo", contentId, "contentAssocTypeId", "SUB_CONTENT")
                .cache().queryList();

        boolean hadContent = false;
        if (UtilValidate.isNotEmpty(contentAssoc)) {
            hadContent = true;
        }
        contentAssoc = EntityUtil.filterByDate(contentAssoc);
        if (UtilValidate.isEmpty(contentAssoc)) {
            List<GenericValue> assocs = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentIdFrom)
                    .cache().filterByDate().queryList();
            if (assocs != null) {
                for (GenericValue assoc : assocs) {
                    int subContentStatusCode = verifySubContent(delegator, contentId, assoc.getString("contentIdTo"));
                    if (subContentStatusCode == HttpServletResponse.SC_OK) {
                        return HttpServletResponse.SC_OK;
                    } else if (subContentStatusCode == HttpServletResponse.SC_GONE) {
                        hadContent = true;
                    }
                }
            }
        } else {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Found assocs: " + contentAssoc, MODULE);
            }
            return HttpServletResponse.SC_OK;
        }
        if (hadContent) return HttpServletResponse.SC_GONE;
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
