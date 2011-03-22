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
package org.ofbiz.product.category;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.StringUtil.StringWrapper;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.webapp.control.ContextFilter;
import org.ofbiz.webapp.website.WebSiteWorker;

public class CatalogUrlFilter extends ContextFilter {

    public final static String module = CatalogUrlFilter.class.getName();
    
    public static final String CONTROL_MOUNT_POINT = "control";
    public static final String PRODUCT_REQUEST = "product";
    public static final String CATEGORY_REQUEST = "category";
    
    protected static String defaultViewIndex = null;
    protected static String defaultViewSize = null;
    protected static String defaultLocaleString = null;
    protected static String redirectUrl = null;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");

        // check if multi tenant is enabled
        String useMultitenant = UtilProperties.getPropertyValue("general.properties", "multitenant");
        if ("Y".equals(useMultitenant)) {
            // get tenant delegator by domain name
            String serverName = request.getServerName();
            try {
                // if tenant was specified, replace delegator with the new per-tenant delegator and set tenantId to session attribute
                delegator = getDelegator(config.getServletContext());
                List<GenericValue> tenants = delegator.findList("Tenant", EntityCondition.makeCondition("domainName", serverName), null, UtilMisc.toList("-createdStamp"), null, false);
                if (UtilValidate.isNotEmpty(tenants)) {
                    GenericValue tenant = EntityUtil.getFirst(tenants);
                    String tenantId = tenant.getString("tenantId");
                    
                    // make that tenant active, setup a new delegator and a new dispatcher
                    String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
                    httpRequest.getSession().setAttribute("delegatorName", tenantDelegatorName);
                
                    // after this line the delegator is replaced with the new per-tenant delegator
                    delegator = DelegatorFactory.getDelegator(tenantDelegatorName);
                    config.getServletContext().setAttribute("delegator", delegator);
                }
                
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Unable to get Tenant", module);
            }
        }

        // set the web context in the request for future use
        request.setAttribute("servletContext", httpRequest.getSession().getServletContext());
        request.setAttribute("delegator", delegator);

        // set the webSiteId in the session
        if (UtilValidate.isEmpty(httpRequest.getSession().getAttribute("webSiteId"))){
            httpRequest.getSession().setAttribute("webSiteId", httpRequest.getSession().getServletContext().getAttribute("webSiteId"));
        }
        
        // set initial parameters
        String initDefaultViewIndex = config.getInitParameter("defaultViewIndex");
        String initDefaultViewSize = config.getInitParameter("defaultViewSize");
        String initDefaultLocalesString = config.getInitParameter("defaultLocaleString");
        String initRedirectUrl = config.getInitParameter("redirectUrl");
        defaultViewIndex = UtilValidate.isNotEmpty(initDefaultViewIndex) ? initDefaultViewIndex : "";
        defaultViewSize = UtilValidate.isNotEmpty(initDefaultViewSize) ? initDefaultViewSize : "";
        defaultLocaleString = UtilValidate.isNotEmpty(initDefaultLocalesString) ? initDefaultLocalesString : "";
        redirectUrl = UtilValidate.isNotEmpty(initRedirectUrl) ? initRedirectUrl : "";
        
        String pathInfo = httpRequest.getServletPath();
        
        if (UtilValidate.isNotEmpty(pathInfo)) {
            List<String> pathElements = StringUtil.split(pathInfo, "/");
            String alternativeUrl = pathElements.get(0);
            // get web site and default locale string
            String localeString = null;
            String webSiteId = WebSiteWorker.getWebSiteId(request);
            GenericValue webSite;
            try {
                webSite = delegator.findOne("WebSite", UtilMisc.toMap("webSiteId", webSiteId), true);
                if (UtilValidate.isNotEmpty(webSite)) {
                    GenericValue productStore = webSite.getRelatedOne("ProductStore");
                    if (UtilValidate.isNotEmpty(productStore)) {
                        localeString = productStore.getString("defaultLocaleString");
                    }
                } else {
                    localeString = defaultLocaleString;
                }
            } catch (GenericEntityException ex) {
                Debug.logWarning(ex, module);
            }
            
            // get view index, view size and view sort from path info
            String viewIndex = defaultViewIndex;
            String viewSize = defaultViewSize;
            String viewSort = null;
            String searchString = null;
            
            int queryStringIndex = pathInfo.indexOf("?");
            if (queryStringIndex >= 0) {
                List<String> queryStringTokens = StringUtil.split(pathInfo.substring(queryStringIndex + 1), "&");
                for (String queryStringToken : queryStringTokens) {
                    int equalIndex = queryStringToken.indexOf("=");
                    String name = queryStringToken.substring(0, equalIndex - 1);
                    String value = queryStringToken.substring(equalIndex + 1, queryStringToken.length() - 1);
                    
                    if ("viewIndex".equals(name)) {
                        viewIndex = value;
                    } else if ("viewSize".equals(name)) {
                        viewSize = value;
                    } else if ("viewSort".equals(name)) {
                        viewSort = value;
                    } else if ("searchString".equals(name)) {
                        searchString = value;
                    }
                }
            }
            
            String productId = null;
            String productCategoryId = null;
            
            try {
                // look for productId
                List<EntityCondition> productContentConds = FastList.newInstance();
                productContentConds.add(EntityCondition.makeCondition(
                      EntityCondition.makeCondition("drObjectInfo", alternativeUrl)
                      , EntityOperator.OR
                      , EntityCondition.makeCondition("drObjectInfo", "/" + alternativeUrl)));
                productContentConds.add(EntityCondition.makeCondition("localeString", localeString));
                productContentConds.add(EntityCondition.makeCondition("productContentTypeId", "ALTERNATIVE_URL"));
                productContentConds.add(EntityUtil.getFilterByDateExpr());
                List<GenericValue> productContentInfos = delegator.findList("ProductContentAndInfo", EntityCondition.makeCondition(productContentConds), null, UtilMisc.toList("-fromDate"), null, true);
                if (UtilValidate.isNotEmpty(productContentInfos)) {
                    GenericValue productContentInfo = EntityUtil.getFirst(productContentInfos);
                    productId = productContentInfo.getString("productId");
                }
                
                // look for productCategoryId
                List<EntityCondition> productCategoryContentConds = FastList.newInstance();
                productCategoryContentConds.add(EntityCondition.makeCondition(
                        EntityCondition.makeCondition("drObjectInfo", alternativeUrl)
                        , EntityOperator.OR
                        , EntityCondition.makeCondition("drObjectInfo", "/" + alternativeUrl)));
                productContentConds.add(EntityCondition.makeCondition("localeString", localeString));
                productCategoryContentConds.add(EntityCondition.makeCondition("prodCatContentTypeId", "ALTERNATIVE_URL"));
                productCategoryContentConds.add(EntityUtil.getFilterByDateExpr());
                List<GenericValue> productCategoryContentInfos = delegator.findList("ProductCategoryContentAndInfo", EntityCondition.makeCondition(productCategoryContentConds), null, UtilMisc.toList("-fromDate"), null, true);
                if (UtilValidate.isNotEmpty(productCategoryContentInfos)) {
                    GenericValue productCategoryContentInfo = EntityUtil.getFirst(productCategoryContentInfos);
                    productCategoryId = productCategoryContentInfo.getString("productCategoryId");
                }
            } catch (GenericEntityException e) {
                Debug.logWarning("Cannot look for product and product category", module);
            }
            
            // generate forward URL
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("/" + CONTROL_MOUNT_POINT);
            
            if (UtilValidate.isNotEmpty(productId)) {
                try {
                    List<EntityCondition> conds = FastList.newInstance();
                    conds.add(EntityCondition.makeCondition("productId", productId));
                    conds.add(EntityUtil.getFilterByDateExpr());
                    List<GenericValue> productCategoryMembers = delegator.findList("ProductCategoryMember", EntityCondition.makeCondition(conds), UtilMisc.toSet("productCategoryId"), UtilMisc.toList("-fromDate"), null, true);
                    if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                        GenericValue productCategoryMember = EntityUtil.getFirst(productCategoryMembers);
                        productCategoryId = productCategoryMember.getString("productCategoryId");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot find product category for product: " + productId, module);
                }
                urlBuilder.append("/" + PRODUCT_REQUEST);
                
            } else {
                urlBuilder.append("/" + CATEGORY_REQUEST);
            }

            // generate trail belong to a top category
            String topCategoryId = CategoryWorker.getCatalogTopCategory(httpRequest, null);
            List<GenericValue> trailCategories = CategoryWorker.getRelatedCategoriesRet(httpRequest, "trailCategories", topCategoryId, false, false, true);
            List<String> trailCategoryIds = EntityUtil.getFieldListFromEntityList(trailCategories, "productCategoryId", true);
            
            // look for productCategoryId from productId
            if (UtilValidate.isNotEmpty(productId)) {
                try {
                    List<EntityCondition> rolllupConds = FastList.newInstance();
                    rolllupConds.add(EntityCondition.makeCondition("productId", productId));
                    rolllupConds.add(EntityUtil.getFilterByDateExpr());
                    List<GenericValue> productCategoryMembers = delegator.findList("ProductCategoryMember", EntityCondition.makeCondition(rolllupConds), null, UtilMisc.toList("-fromDate"), null, true);
                    for (GenericValue productCategoryMember : productCategoryMembers) {
                        String trailCategoryId = productCategoryMember.getString("productCategoryId");
                        if (trailCategoryIds.contains(trailCategoryId)) {
                            productCategoryId = trailCategoryId;
                            break;
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot generate trail from product category", module);
                }
            }

            // generate trail elements from productCategoryId
            if (UtilValidate.isNotEmpty(productCategoryId)) {
                List<String> trailElements = FastList.newInstance();
                trailElements.add(productCategoryId);
                String parentProductCategoryId = productCategoryId;
                while (UtilValidate.isNotEmpty(parentProductCategoryId)) {
                    // find product category rollup
                    try {
                        List<EntityCondition> rolllupConds = FastList.newInstance();
                        rolllupConds.add(EntityCondition.makeCondition("productCategoryId", parentProductCategoryId));
                        rolllupConds.add(EntityUtil.getFilterByDateExpr());
                        List<GenericValue> productCategoryRollups = delegator.findList("ProductCategoryRollup", EntityCondition.makeCondition(rolllupConds), null, UtilMisc.toList("-fromDate"), null, true);
                        if (UtilValidate.isNotEmpty(productCategoryRollups)) {
                            // add only categories that belong to the top category to trail
                            for (GenericValue productCategoryRollup : productCategoryRollups) {
                                String trailCategoryId = productCategoryRollup.getString("parentProductCategoryId");
                                parentProductCategoryId = trailCategoryId;
                                if (trailCategoryIds.contains(trailCategoryId)) {
                                    trailElements.add(trailCategoryId);
                                    break;
                                }
                            }
                        } else {
                            parentProductCategoryId = null;
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Cannot generate trail from product category", module);
                    }
                }
                Collections.reverse(trailElements);
                
                List<String> trail = CategoryWorker.getTrail(httpRequest);
                if (trail == null) {
                    trail = FastList.newInstance();
                }

                // adjust trail
                String previousCategoryId = null;
                if (trail.size() > 0) {
                    previousCategoryId = trail.get(trail.size() - 1);
                }
                trail = CategoryWorker.adjustTrail(trail, productCategoryId, previousCategoryId);
                
                if (trailElements.size() == 1) {
                    CategoryWorker.setTrail(request, trailElements.get(0), null);
                } else if (trailElements.size() == 2) {
                    CategoryWorker.setTrail(request, trailElements.get(1), trailElements.get(0));
                } else if (trailElements.size() > 2) {
                    if (trail.contains(trailElements.get(0))) {
                        // first category is in the trail, so remove it everything after that and fill it in with the list from the pathInfo
                        int firstElementIndex = trail.indexOf(trailElements.get(0));
                        while (trail.size() > firstElementIndex) {
                            trail.remove(firstElementIndex);
                        }
                        trail.addAll(trailElements);
                    } else {
                        // first category is NOT in the trail, so clear out the trail and use the trailElements list
                        trail.clear();
                        trail.addAll(trailElements);
                    }
                    CategoryWorker.setTrail(request, trail);
                }

                if(UtilValidate.isNotEmpty(viewIndex)){
                    urlBuilder.append("/~VIEW_INDEX=" + viewIndex);
                    request.setAttribute("VIEW_INDEX", viewIndex);
                }
                if(UtilValidate.isNotEmpty(viewSize)){
                    urlBuilder.append("/~VIEW_SIZE=" + viewSize);
                    request.setAttribute("VIEW_SIZE", viewSize);
                }
                if(UtilValidate.isNotEmpty(viewSort)){
                    urlBuilder.append("/~VIEW_SORT=" + viewSort);
                    request.setAttribute("VIEW_SORT", viewSort);
                }
                if(UtilValidate.isNotEmpty(searchString)){
                    urlBuilder.append("/~SEARCH_STRING=" + searchString);
                    request.setAttribute("SEARCH_STRING", searchString);
                }

                request.setAttribute("productCategoryId", productCategoryId);
                
                if (productId != null) {
                    request.setAttribute("product_id", productId);
                    request.setAttribute("productId", productId);
                }
            }
            
            if (UtilValidate.isNotEmpty(productId) || UtilValidate.isNotEmpty(productCategoryId)) {
                Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", module);
                RequestDispatcher dispatch = request.getRequestDispatcher(urlBuilder.toString());
                dispatch.forward(request, response);
                return;
            }

            // check path alias
            GenericValue pathAlias = null;
            try {
                pathAlias = delegator.findByPrimaryKeyCache("WebSitePathAlias", UtilMisc.toMap("webSiteId", webSiteId, "pathAlias", pathInfo));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (pathAlias != null) {
                String alias = pathAlias.getString("aliasTo");
                String contentId = pathAlias.getString("contentId");
                if (contentId == null && UtilValidate.isNotEmpty(alias)) {
                    if (!alias.startsWith("/")) {
                       alias = "/" + alias;
                    }

                    RequestDispatcher rd = request.getRequestDispatcher(alias);
                    try {
                        rd.forward(request, response);
                        return;
                    } catch (ServletException e) {
                        Debug.logWarning(e, module);
                    } catch (IOException e) {
                        Debug.logWarning(e, module);
                    }
                }
            } else {
                // send 404 error if a URI is alias TO
                try {
                    List<GenericValue> aliasTos = delegator.findByAndCache("WebSitePathAlias", UtilMisc.toMap("webSiteId", webSiteId, "aliasTo", httpRequest.getRequestURI()));
                    if (UtilValidate.isNotEmpty(aliasTos)) {
                        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
                        return;
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }
        
        // we're done checking; continue on
        chain.doFilter(request, response);
    }
    
    public static String makeCategoryUrl(HttpServletRequest request, String previousCategoryId, String productCategoryId, String productId, String viewSize, String viewIndex, String viewSort, String searchString) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        try {
            GenericValue productCategory = delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId), true);
            CategoryContentWrapper wrapper = new CategoryContentWrapper(productCategory, request);
            StringWrapper alternativeUrl = wrapper.get("ALTERNATIVE_URL");
            
            if (UtilValidate.isNotEmpty(alternativeUrl) && UtilValidate.isNotEmpty(alternativeUrl.toString())) {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(request.getSession().getServletContext().getContextPath());
                if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
                    urlBuilder.append("/");
                }
                // append alternative URL
                urlBuilder.append(alternativeUrl);
                // append view index
                if (UtilValidate.isNotEmpty(viewIndex)) {
                    if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                        urlBuilder.append("?");
                    }
                    urlBuilder.append("viewIndex=" + viewIndex + "&");
                }
                // append view size
                if (UtilValidate.isNotEmpty(viewSize)) {
                    if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                        urlBuilder.append("?");
                    }
                    urlBuilder.append("viewSize=" + viewSize + "&");
                }
                // append view sort
                if (UtilValidate.isNotEmpty(viewSort)) {
                    if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                        urlBuilder.append("?");
                    }
                    urlBuilder.append("viewSort=" + viewSort + "&");
                }
                // append search string
                if (UtilValidate.isNotEmpty(searchString)) {
                    if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                        urlBuilder.append("?");
                    }
                    urlBuilder.append("searchString=" + searchString + "&");
                }
                return  urlBuilder.toString();
            } else {
                return CatalogUrlServlet.makeCatalogUrl(request, productId, productCategoryId, previousCategoryId);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Cannot create category's URL for: " + productCategoryId, module);
            return redirectUrl;
        }
    }
    
    public static String makeProductUrl(HttpServletRequest request, String previousCategoryId, String productCategoryId, String productId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        try {
            GenericValue product = delegator.findOne("Product", UtilMisc.toMap("productId", productId), true);
            ProductContentWrapper wrapper = new ProductContentWrapper(product, request);
            StringWrapper alternativeUrl = wrapper.get("ALTERNATIVE_URL");
            if (UtilValidate.isNotEmpty(alternativeUrl) && UtilValidate.isNotEmpty(alternativeUrl.toString())) {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(request.getSession().getServletContext().getContextPath());
                if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
                    urlBuilder.append("/");
                }
                // append alternative URL
                urlBuilder.append(alternativeUrl);
                return  urlBuilder.toString();
            } else {
                return CatalogUrlServlet.makeCatalogUrl(request, productId, productCategoryId, previousCategoryId);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Cannot create product's URL for: " + productId, module);
            return redirectUrl;
        }
    }
}
