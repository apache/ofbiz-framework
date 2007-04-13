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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.product.ProductWorker;

/**
 * CategoryWorker - Worker class to reduce code in JSPs.
 */
public class CategoryWorker {

    public static final String module = CategoryWorker.class.getName();

    public static String getCatalogTopCategory(PageContext pageContext, String defaultTopCategory) {
        return getCatalogTopCategory(pageContext.getRequest(), defaultTopCategory);
    }

    public static String getCatalogTopCategory(ServletRequest request, String defaultTopCategory) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map requestParameters = UtilHttp.getParameterMap(httpRequest);
        String topCatName = null;
        boolean fromSession = false;

        // first see if a new category was specified as a parameter
        topCatName = (String) requestParameters.get("CATALOG_TOP_CATEGORY");
        // if no parameter, try from session
        if (topCatName == null) {
            topCatName = (String) httpRequest.getSession().getAttribute("CATALOG_TOP_CATEGORY");
            if (topCatName != null)
                fromSession = true;
        }
        // if nothing else, just use a default top category name
        if (topCatName == null)
            topCatName = defaultTopCategory;
        if (topCatName == null)
            topCatName = "CATALOG1";

        if (!fromSession) {
            if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.getCatalogTopCategory] Setting new top category: " + topCatName, module);
            httpRequest.getSession().setAttribute("CATALOG_TOP_CATEGORY", topCatName);
        }
        return topCatName;
    }

    public static void getCategoriesWithNoParent(PageContext pageContext, String attributeName) {
        getCategoriesWithNoParent(pageContext.getRequest(), attributeName);
    }

    public static void getCategoriesWithNoParent(ServletRequest request, String attributeName) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Collection results = new LinkedList();

        try {
            Collection allCategories = delegator.findAll("ProductCategory");

            if (allCategories == null)
                return;
            Iterator aciter = allCategories.iterator();

            while (aciter.hasNext()) {
                GenericValue curCat = (GenericValue) aciter.next();
                Collection parentCats = curCat.getRelatedCache("CurrentProductCategoryRollup");

                if (parentCats == null || parentCats.size() <= 0)
                    results.add(curCat);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        request.setAttribute(attributeName, results);
    }

    public static void getRelatedCategories(PageContext pageContext, String attributeName, boolean limitView) {
            getRelatedCategories(pageContext.getRequest(), attributeName, limitView);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, boolean limitView) {
        Map requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String requestId = null;

        requestId = UtilFormatOut.checkNull((String)requestParameters.get("catalog_id"), (String)requestParameters.get("CATALOG_ID"),
                (String)requestParameters.get("category_id"), (String)requestParameters.get("CATEGORY_ID"));

        if (requestId.equals(""))
            return;
        if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.getRelatedCategories] RequestID: " + requestId, module);
        getRelatedCategories(request, attributeName, requestId, limitView);
    }

    public static void getRelatedCategories(PageContext pageContext, String attributeName, String parentId, boolean limitView) {
        getRelatedCategories(pageContext.getRequest(), attributeName, parentId, limitView);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, String parentId, boolean limitView) {
        getRelatedCategories(request, attributeName, parentId, limitView, false);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, String parentId, boolean limitView, boolean excludeEmpty) {
        List categories = getRelatedCategoriesRet(request, attributeName, parentId, limitView, excludeEmpty);

        if (categories.size() > 0)
            request.setAttribute(attributeName, categories);
    }

    public static List getRelatedCategoriesRet(PageContext pageContext, String attributeName, String parentId, boolean limitView) {
        return getRelatedCategoriesRet(pageContext.getRequest(), attributeName, parentId, limitView);
    }

    public static List getRelatedCategoriesRet(ServletRequest request, String attributeName, String parentId, boolean limitView) {
        return getRelatedCategoriesRet(request, attributeName, parentId, limitView, false);
    }

    public static List getRelatedCategoriesRet(ServletRequest request, String attributeName, String parentId, boolean limitView, boolean excludeEmpty) {
        List categories = FastList.newInstance();

        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.getRelatedCategories] ParentID: " + parentId, module);

        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        List rollups = null;

        try {
            rollups = delegator.findByAndCache("ProductCategoryRollup",
                        UtilMisc.toMap("parentProductCategoryId", parentId),
                        UtilMisc.toList("sequenceNum"));
            if (limitView) {
                rollups = EntityUtil.filterByDate(rollups, true);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            rollups = null;
        }
        if (rollups != null && rollups.size() > 0) {
            // Debug.log("Rollup size: " + rollups.size(), module);
            Iterator ri = rollups.iterator();

            while (ri.hasNext()) {
                GenericValue parent = (GenericValue) ri.next();
                // Debug.log("Adding child of: " + parent.getString("parentProductCategoryId"), module);
                GenericValue cv = null;

                try {
                    cv = parent.getRelatedOneCache("CurrentProductCategory");
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                    cv = null;
                }
                if (cv != null) {
                    if (excludeEmpty) {
                        if (!isCategoryEmpty(cv)) {
                            //Debug.log("Child : " + cv.getString("productCategoryId") + " is not empty.", module);
                            categories.add(cv);
                        }
                    } else {
                        categories.add(cv);
                    }
                }
            }
        }
        return categories;
    }

    public static boolean isCategoryEmpty(GenericValue category) {
        boolean empty = true;
        long members = categoryMemberCount(category);
        //Debug.log("Category : " + category.get("productCategoryId") + " has " + members  + " members", module);
        if (members > 0) {
            empty = false;
        }

        if (empty) {
            long rollups = categoryRollupCount(category);
            //Debug.log("Category : " + category.get("productCategoryId") + " has " + rollups  + " rollups", module);
            if (rollups > 0) {
                empty = false;
            }
        }

        return empty;
    }

    public static long categoryMemberCount(GenericValue category) {
        if (category == null) return 0;
        GenericDelegator delegator = category.getDelegator();
        long count = 0;
        try {
            count = delegator.findCountByCondition("ProductCategoryMember", buildCountCondition("productCategoryId", category.getString("productCategoryId")), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return count;
    }

    public static long categoryRollupCount(GenericValue category) {
        if (category == null) return 0;
        GenericDelegator delegator = category.getDelegator();
        long count = 0;
        try {
            count = delegator.findCountByCondition("ProductCategoryRollup", buildCountCondition("parentProductCategoryId", category.getString("productCategoryId")), null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return count;
    }

    private static EntityCondition buildCountCondition(String fieldName, String fieldValue) {
        List orCondList = FastList.newInstance();
        orCondList.add(new EntityExpr("thruDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp()));
        orCondList.add(new EntityExpr("thruDate", EntityOperator.EQUALS, null));
        EntityCondition orCond = new EntityConditionList(orCondList, EntityOperator.OR);

        List andCondList = FastList.newInstance();
        andCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp()));
        andCondList.add(new EntityExpr(fieldName, EntityOperator.EQUALS, fieldValue));
        andCondList.add(orCond);
        EntityCondition andCond = new EntityConditionList(andCondList, EntityOperator.AND);

        return andCond;
    }

    public static void setTrail(PageContext pageContext, String currentCategory) {
        setTrail(pageContext.getRequest(), currentCategory);
    }

    public static void setTrail(ServletRequest request, String currentCategory) {
        Map requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String previousCategory = (String) requestParameters.get("pcategory");

        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.setTrail] Start: previousCategory=" + previousCategory +
                " currentCategory=" + currentCategory, module);

        // if there is no current category, just return and do nothing to that the last settings will stay
        if (currentCategory == null || currentCategory.length() <= 0)
            return;

        // always get the last crumb list
        List crumb = getTrail(request);

        if (crumb == null) {
            crumb = FastList.newInstance();
        }

        // if no previous category was specified, check to see if currentCategory is in the list
        if (previousCategory == null || previousCategory.length() <= 0) {
            if (crumb.contains(currentCategory)) {
                // if cur category is in crumb, remove everything after it and return
                int cindex = crumb.lastIndexOf(currentCategory);

                if (cindex < (crumb.size() - 1)) {
                    for (int i = crumb.size() - 1; i > cindex; i--) {
                        String deadCat = (String) crumb.remove(i);

                        if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Removed after current category index: " + i +
                                " catname: " + deadCat, module);
                    }
                }
                return;
            } else {
                // current category is not in the list, and no previous category was specified, go back to the beginning
                crumb.clear();
                crumb.add("TOP");
                if (UtilValidate.isNotEmpty(previousCategory)) {
                    crumb.add(previousCategory);
                }
                if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Starting new list, added TOP and previousCategory: " + previousCategory, module);
            }
        }

        if (!crumb.contains(previousCategory)) {
            // previous category was NOT in the list, ERROR, start over
            if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] ERROR: previousCategory (" + previousCategory +
                    ") was not in the crumb list, position is lost, starting over with TOP", module);
            crumb.clear();
            crumb.add("TOP");
            if (UtilValidate.isNotEmpty(previousCategory)) {
                crumb.add(previousCategory);
            }
        } else {
            // remove all categories after the previous category, preparing for adding the current category
            int index = crumb.indexOf(previousCategory);

            if (index < (crumb.size() - 1)) {
                for (int i = crumb.size() - 1; i > index; i--) {
                    String deadCat = (String) crumb.remove(i);

                    if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Removed after previous category index: " + i +
                            " catname: " + deadCat, module);
                }
            }
        }

        // add the current category to the end of the list
        crumb.add(currentCategory);
        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.setTrail] Continuing list: Added currentCategory: " + currentCategory, module);
        setTrail(request, crumb);
    }

    public static List getTrail(PageContext pageContext) {
        return getTrail(pageContext.getRequest());
    }

    public static List getTrail(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        List crumb = (List) session.getAttribute("_BREAD_CRUMB_TRAIL_");
        return crumb;
    }

    public static List setTrail(PageContext pageContext, List crumb) {
        return setTrail(pageContext.getRequest(), crumb);
    }

    public static List setTrail(ServletRequest request, List crumb) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        session.setAttribute("_BREAD_CRUMB_TRAIL_", crumb);
        return crumb;
    }

    public static boolean checkTrailItem(PageContext pageContext, String category) {
        return checkTrailItem(pageContext.getRequest(), category);
    }

    public static boolean checkTrailItem(ServletRequest request, String category) {
        List crumb = getTrail(request);

        if (crumb != null && crumb.contains(category))
            return true;
        else
            return false;
    }

    public static String lastTrailItem(PageContext pageContext) {
        return lastTrailItem(pageContext.getRequest());
    }

    public static String lastTrailItem(ServletRequest request) {
        List crumb = getTrail(request);

        if (crumb != null && crumb.size() > 0) {
            return (String) crumb.get(crumb.size() - 1);
        } else {
            return null;
        }
    }

    public static boolean isProductInCategory(GenericDelegator delegator, String productId, String productCategoryId) throws GenericEntityException {
        if (productCategoryId == null) return false;
        if (productId == null || productId.length() == 0) return false;

        List productCategoryMembers = EntityUtil.filterByDate(delegator.findByAndCache("ProductCategoryMember",
                UtilMisc.toMap("productCategoryId", productCategoryId, "productId", productId)), true);
        if (productCategoryMembers == null || productCategoryMembers.size() == 0) {
            //before giving up see if this is a variant product, and if so look up the virtual product and check it...
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            List productAssocs = ProductWorker.getVariantVirtualAssocs(product);
            //this does take into account that a product could be a variant of multiple products, but this shouldn't ever really happen...
            if (productAssocs != null && productAssocs.size() > 0) {
                Iterator pasIter = productAssocs.iterator();
                while (pasIter.hasNext()) {
                    GenericValue productAssoc = (GenericValue) pasIter.next();
                    if (isProductInCategory(delegator, productAssoc.getString("productId"), productCategoryId)) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public static List filterProductsInCategory(GenericDelegator delegator, List valueObjects, String productCategoryId) throws GenericEntityException {
        return filterProductsInCategory(delegator, valueObjects, productCategoryId, "productId");
    }

    public static List filterProductsInCategory(GenericDelegator delegator, List valueObjects, String productCategoryId, String productIdFieldName) throws GenericEntityException {
        List newList = FastList.newInstance();

        if (productCategoryId == null) return newList;
        if (valueObjects == null) return null;
        
        Iterator valIter = valueObjects.iterator();
        while (valIter.hasNext()) {
            GenericValue curValue = (GenericValue) valIter.next();
            String productId = curValue.getString(productIdFieldName);
            if (isProductInCategory(delegator, productId, productCategoryId)) {
                newList.add(curValue);
            }
        }
        return newList;
    }
    
    public static void getCategoryContentWrappers(Map catContentWrappers, List categoryList, HttpServletRequest request) throws GenericEntityException {
        if (catContentWrappers == null || categoryList == null) {
            return;
        }
        Iterator catIterator = categoryList.iterator();
        while(catIterator.hasNext()) {
            GenericValue cat = (GenericValue) catIterator.next();
            String productCategoryId = (String) cat.get("productCategoryId");
            
            if (catContentWrappers.containsKey(productCategoryId)) {
                // if this ID is already in the Map, skip it (avoids inefficiency, infinite recursion, etc.)
                continue;
            }
            
            CategoryContentWrapper catContentWrapper = new CategoryContentWrapper(cat, request);
            catContentWrappers.put(productCategoryId, catContentWrapper);
            List subCat = FastList.newInstance();
            subCat = getRelatedCategoriesRet(request, "subCatList", productCategoryId, true);
            if(subCat != null) {                
                getCategoryContentWrappers(catContentWrappers, subCat, request );
            }    
        }
    }
}
