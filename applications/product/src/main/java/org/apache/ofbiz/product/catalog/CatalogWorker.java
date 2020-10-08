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
package org.apache.ofbiz.product.catalog;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * CatalogWorker - Worker class for catalog related functionality
 */
public final class CatalogWorker {

    private static final String MODULE = CatalogWorker.class.getName();

    private CatalogWorker() { }


    /**
     * @deprecated - Use WebSiteWorker.getWebSiteId(ServletRequest) instead
     */
    @Deprecated
    public static String getWebSiteId(ServletRequest request) {
        return WebSiteWorker.getWebSiteId(request);
    }

    /**
     * @deprecated - Use WebSiteWorker.getWebSite(ServletRequest) instead
     */
    @Deprecated
    public static GenericValue getWebSite(ServletRequest request) {
        return WebSiteWorker.getWebSite(request);
    }

    public static List<String> getAllCatalogIds(ServletRequest request) {
        List<String> catalogIds = new LinkedList<>();
        List<GenericValue> catalogs = null;
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        try {
            catalogs = EntityQuery.use(delegator).from("ProdCatalog").orderBy("catalogName").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up all catalogs", MODULE);
        }
        if (catalogs != null) {
            for (GenericValue c: catalogs) {
                catalogIds.add(c.getString("prodCatalogId"));
            }
        }
        return catalogIds;
    }

    public static List<GenericValue> getStoreCatalogs(ServletRequest request) {
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        return getStoreCatalogs(delegator, productStoreId);
    }

    public static List<GenericValue> getStoreCatalogs(Delegator delegator, String productStoreId) {
        try {
            return EntityQuery.use(delegator).from("ProductStoreCatalog").where("productStoreId", productStoreId)
                    .orderBy("sequenceNum", "prodCatalogId").cache(true).filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up store catalogs for store with id " + productStoreId, MODULE);
        }
        return null;
    }

    public static List<GenericValue> getPartyCatalogs(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin == null) userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        if (userLogin == null) return null;
        String partyId = userLogin.getString("partyId");
        if (partyId == null) return null;
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        return getPartyCatalogs(delegator, partyId);
    }

    public static List<GenericValue> getPartyCatalogs(Delegator delegator, String partyId) {
        if (delegator == null || partyId == null) {
            return null;
        }

        try {
            return EntityQuery.use(delegator).from("ProdCatalogRole").where("partyId", partyId, "roleTypeId", "CUSTOMER")
                    .orderBy("sequenceNum", "prodCatalogId").cache(true).filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up ProdCatalog Roles for party with id " + partyId, MODULE);
        }
        return null;
    }

    public static List<GenericValue> getProdCatalogCategories(ServletRequest request, String prodCatalogId, String prodCatalogCategoryTypeId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        return getProdCatalogCategories(delegator, prodCatalogId, prodCatalogCategoryTypeId);
    }

    public static List<GenericValue> getProdCatalogCategories(Delegator delegator, String prodCatalogId, String prodCatalogCategoryTypeId) {
        try {
            List<GenericValue> prodCatalogCategories = EntityQuery.use(delegator).from("ProdCatalogCategory")
                    .where("prodCatalogId", prodCatalogId)
                    .orderBy("sequenceNum", "productCategoryId")
                    .cache(true)
                    .filterByDate()
                    .queryList();

            if (UtilValidate.isNotEmpty(prodCatalogCategoryTypeId) && prodCatalogCategories != null) {
                prodCatalogCategories = EntityUtil.filterByAnd(prodCatalogCategories,
                            UtilMisc.toMap("prodCatalogCategoryTypeId", prodCatalogCategoryTypeId));
            }
            return prodCatalogCategories;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up ProdCatalogCategories for prodCatalog with id " + prodCatalogId, MODULE);
        }
        return null;
    }

    /**
     * Retrieves the current prodCatalogId.  First it will attempt to find it from a special
     * request parameter or session attribute named CURRENT_CATALOG_ID.  Failing that, it will
     * get the first catalog from the database as specified in getCatalogIdsAvailable().
     * If this behavior is undesired, give the user a selectable list of catalogs.
     */
    public static String getCurrentCatalogId(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        Map<String, Object> requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String prodCatalogId = null;
        boolean fromSession = false;

        // first see if a new catalog was specified as a parameter
        prodCatalogId = (String) requestParameters.get("CURRENT_CATALOG_ID");
        // if no parameter, try from session
        if (prodCatalogId == null) {
            prodCatalogId = (String) session.getAttribute("CURRENT_CATALOG_ID");
            if (prodCatalogId != null) fromSession = true;
        }
        // get it from the database
        if (prodCatalogId == null) {
            List<String> catalogIds = getCatalogIdsAvailable(request);
            if (UtilValidate.isNotEmpty(catalogIds)) prodCatalogId = catalogIds.get(0);
        }

        if (!fromSession) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("[CatalogWorker.getCurrentCatalogId] Setting new catalog name: " + prodCatalogId, MODULE);
            }
            session.setAttribute("CURRENT_CATALOG_ID", prodCatalogId);
            CategoryWorker.setTrail(request, new LinkedList<>());
        }
        return prodCatalogId;
    }

    public static List<String> getCatalogIdsAvailable(ServletRequest request) {
        List<GenericValue> partyCatalogs = getPartyCatalogs(request);
        List<GenericValue> storeCatalogs = getStoreCatalogs(request);
        return getCatalogIdsAvailable(partyCatalogs, storeCatalogs);
    }

    public static List<String> getCatalogIdsAvailable(Delegator delegator, String productStoreId, String partyId) {
        List<GenericValue> storeCatalogs = getStoreCatalogs(delegator, productStoreId);
        List<GenericValue> partyCatalogs = getPartyCatalogs(delegator, partyId);
        return getCatalogIdsAvailable(partyCatalogs, storeCatalogs);
    }

    public static List<String> getCatalogIdsAvailable(List<GenericValue> partyCatalogs, List<GenericValue> storeCatalogs) {
        List<String> categoryIds = new LinkedList<>();
        List<GenericValue> allCatalogLinks = new LinkedList<>();
        if (partyCatalogs != null) allCatalogLinks.addAll(partyCatalogs);
        if (storeCatalogs != null) allCatalogLinks.addAll(storeCatalogs);

        if (!allCatalogLinks.isEmpty()) {
            for (GenericValue catalogLink: allCatalogLinks) {
                categoryIds.add(catalogLink.getString("prodCatalogId"));
            }
        }
        return categoryIds;
    }

    public static String getCatalogName(ServletRequest request) {
        return getCatalogName(request, getCurrentCatalogId(request));
    }

    public static String getCatalogName(ServletRequest request, String prodCatalogId) {
        if (UtilValidate.isEmpty(prodCatalogId)) return null;
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        try {
            GenericValue prodCatalog = EntityQuery.use(delegator).from("ProdCatalog").where("prodCatalogId", prodCatalogId).cache().queryOne();

            if (prodCatalog != null) {
                return prodCatalog.getString("catalogName");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up name for prodCatalog with id " + prodCatalogId, MODULE);
        }

        return null;
    }

    public static String getContentPathPrefix(ServletRequest request) {
        GenericValue prodCatalog = getProdCatalog(request, getCurrentCatalogId(request));

        if (prodCatalog == null) return "";
        String contentPathPrefix = prodCatalog.getString("contentPathPrefix");

        return StringUtil.cleanUpPathPrefix(contentPathPrefix);
    }

    public static String getTemplatePathPrefix(ServletRequest request) {
        GenericValue prodCatalog = getProdCatalog(request, getCurrentCatalogId(request));

        if (prodCatalog == null) return "";
        String templatePathPrefix = prodCatalog.getString("templatePathPrefix");

        return StringUtil.cleanUpPathPrefix(templatePathPrefix);
    }

    public static GenericValue getProdCatalog(ServletRequest request) {
        return getProdCatalog(request, getCurrentCatalogId(request));
    }

    public static GenericValue getProdCatalog(ServletRequest request, String prodCatalogId) {
        if (UtilValidate.isEmpty(prodCatalogId)) return null;
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        try {
            return EntityQuery.use(delegator).from("ProdCatalog").where("prodCatalogId", prodCatalogId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up name for prodCatalog with id " + prodCatalogId, MODULE);
            return null;
        }
    }

    public static String getProdCatalogCategoryId(Delegator delegator, String prodCatalogId, String prodCatalogCategoryTypeId) {
        if (UtilValidate.isNotEmpty(prodCatalogId) && UtilValidate.isNotEmpty(prodCatalogCategoryTypeId)) {
            GenericValue prodCatalogCategory = EntityUtil.getFirst(getProdCatalogCategories(delegator, prodCatalogId, prodCatalogCategoryTypeId));

            if (prodCatalogCategory != null) {
                return prodCatalogCategory.getString("productCategoryId");
            }
        }
        return null;
    }

    public static String getCatalogTopCategoryId(ServletRequest request) {
        return getCatalogTopCategoryId(request, getCurrentCatalogId(request));
    }

    public static String getCatalogTopCategoryId(ServletRequest request, String prodCatalogId) {
        return getCatalogTopCategoryId((Delegator) request.getAttribute("delegator"), prodCatalogId);
    }

    public static String getCatalogTopCategoryId(Delegator delegator, String prodCatalogId) {
        return getProdCatalogCategoryId(delegator, prodCatalogId, "PCCT_BROWSE_ROOT");
    }

    public static String getCatalogSearchCategoryId(ServletRequest request) {
        return getCatalogSearchCategoryId(request, getCurrentCatalogId(request));
    }

    public static String getCatalogSearchCategoryId(ServletRequest request, String prodCatalogId) {
        return getCatalogSearchCategoryId((Delegator) request.getAttribute("delegator"), prodCatalogId);
    }
    public static String getCatalogSearchCategoryId(Delegator delegator, String prodCatalogId) {
        return getProdCatalogCategoryId(delegator, prodCatalogId, "PCCT_SEARCH");
    }

    public static String getCatalogViewAllowCategoryId(Delegator delegator, String prodCatalogId) {
        return getProdCatalogCategoryId(delegator, prodCatalogId, "PCCT_VIEW_ALLW");
    }

    public static String getCatalogPurchaseAllowCategoryId(Delegator delegator, String prodCatalogId) {
        return getProdCatalogCategoryId(delegator, prodCatalogId, "PCCT_PURCH_ALLW");
    }

    public static String getCatalogPromotionsCategoryId(ServletRequest request) {
        return getCatalogPromotionsCategoryId(request, getCurrentCatalogId(request));
    }

    public static String getCatalogPromotionsCategoryId(ServletRequest request, String prodCatalogId) {
        return getCatalogPromotionsCategoryId((Delegator) request.getAttribute("delegator"), prodCatalogId);
    }
    public static String getCatalogPromotionsCategoryId(Delegator delegator, String prodCatalogId) {
        return getProdCatalogCategoryId(delegator, prodCatalogId, "PCCT_PROMOTIONS");
    }

    public static boolean getCatalogQuickaddUse(ServletRequest request) {
        return getCatalogQuickaddUse(request, getCurrentCatalogId(request));
    }

    public static boolean getCatalogQuickaddUse(ServletRequest request, String prodCatalogId) {
        if (UtilValidate.isEmpty(prodCatalogId)) return false;
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        try {
            GenericValue prodCatalog = EntityQuery.use(delegator).from("ProdCatalog").where("prodCatalogId", prodCatalogId).cache().queryOne();

            if (prodCatalog != null) {
                return "Y".equals(prodCatalog.getString("useQuickAdd"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up name for prodCatalog with id " + prodCatalogId, MODULE);
        }
        return false;
    }

    public static String getCatalogQuickaddCategoryPrimary(ServletRequest request) {
        return getCatalogQuickaddCategoryPrimary(request, getCurrentCatalogId(request));
    }

    public static String getCatalogQuickaddCategoryPrimary(ServletRequest request, String prodCatalogId) {
        return getProdCatalogCategoryId((Delegator) request.getAttribute("delegator"), prodCatalogId, "PCCT_QUICK_ADD");
    }

    public static Collection<String> getCatalogQuickaddCategories(ServletRequest request) {
        return getCatalogQuickaddCategories(request, getCurrentCatalogId(request));
    }

    public static Collection<String> getCatalogQuickaddCategories(ServletRequest request, String prodCatalogId) {
        if (UtilValidate.isEmpty(prodCatalogId)) return null;

        return EntityUtil.getFieldListFromEntityList(
                getProdCatalogCategories(request, prodCatalogId, "PCCT_QUICK_ADD"),
                "productCategoryId", true);
    }

    public static String getCatalogTopEbayCategoryId(ServletRequest request, String prodCatalogId) {
        return getProdCatalogCategoryId((Delegator) request.getAttribute("delegator"), prodCatalogId, "PCCT_EBAY_ROOT");
    }
}
