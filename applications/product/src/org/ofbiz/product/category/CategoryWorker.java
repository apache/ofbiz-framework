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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.product.ProductWorker;

/**
 * CategoryWorker - Worker class to reduce code in JSPs.
 */
public class CategoryWorker {

    public static final String module = CategoryWorker.class.getName();

    public static String getCatalogTopCategory(ServletRequest request, String defaultTopCategory) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, Object> requestParameters = UtilHttp.getParameterMap(httpRequest);
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

    public static void getCategoriesWithNoParent(ServletRequest request, String attributeName) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Collection<GenericValue> results = FastList.newInstance();

        try {
            Collection<GenericValue> allCategories = delegator.findList("ProductCategory", null, null, null, null, false);

            for (GenericValue curCat: allCategories) {
                Collection<GenericValue> parentCats = curCat.getRelatedCache("CurrentProductCategoryRollup");

                if (parentCats.isEmpty())
                    results.add(curCat);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        request.setAttribute(attributeName, results);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, boolean limitView) {
        Map<String, Object> requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String requestId = null;

        requestId = UtilFormatOut.checkNull((String)requestParameters.get("catalog_id"), (String)requestParameters.get("CATALOG_ID"),
                (String)requestParameters.get("category_id"), (String)requestParameters.get("CATEGORY_ID"));

        if (requestId.equals(""))
            return;
        if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.getRelatedCategories] RequestID: " + requestId, module);
        getRelatedCategories(request, attributeName, requestId, limitView);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, String parentId, boolean limitView) {
        getRelatedCategories(request, attributeName, parentId, limitView, false);
    }

    public static void getRelatedCategories(ServletRequest request, String attributeName, String parentId, boolean limitView, boolean excludeEmpty) {
        List<GenericValue> categories = getRelatedCategoriesRet(request, attributeName, parentId, limitView, excludeEmpty);

        if (!categories.isEmpty())
            request.setAttribute(attributeName, categories);
    }

    public static List<GenericValue> getRelatedCategoriesRet(ServletRequest request, String attributeName, String parentId, boolean limitView) {
        return getRelatedCategoriesRet(request, attributeName, parentId, limitView, false);
    }

    public static List<GenericValue> getRelatedCategoriesRet(ServletRequest request, String attributeName, String parentId, boolean limitView, boolean excludeEmpty) {
        return getRelatedCategoriesRet(request, attributeName, parentId, limitView, excludeEmpty, false);
    }

    public static List<GenericValue> getRelatedCategoriesRet(ServletRequest request, String attributeName, String parentId, boolean limitView, boolean excludeEmpty, boolean recursive) {
        List<GenericValue> categories = FastList.newInstance();

        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.getRelatedCategories] ParentID: " + parentId, module);

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        List<GenericValue> rollups = null;

        try {
            rollups = delegator.findByAndCache("ProductCategoryRollup",
                        UtilMisc.toMap("parentProductCategoryId", parentId),
                        UtilMisc.toList("sequenceNum"));
            if (limitView) {
                rollups = EntityUtil.filterByDate(rollups, true);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }
        if (rollups != null) {
            // Debug.log("Rollup size: " + rollups.size(), module);
            for (GenericValue parent: rollups) {
                // Debug.log("Adding child of: " + parent.getString("parentProductCategoryId"), module);
                GenericValue cv = null;

                try {
                    cv = parent.getRelatedOneCache("CurrentProductCategory");
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
                if (cv != null) {
                    if (excludeEmpty) {
                        if (!isCategoryEmpty(cv)) {
                            //Debug.log("Child : " + cv.getString("productCategoryId") + " is not empty.", module);
                            categories.add(cv);
                            if (recursive) {
                                categories.addAll(getRelatedCategoriesRet(request, attributeName, cv.getString("productCategoryId"), limitView, excludeEmpty, recursive));
                            }
                        }
                    } else {
                        categories.add(cv);
                        if (recursive) {
                            categories.addAll(getRelatedCategoriesRet(request, attributeName, cv.getString("productCategoryId"), limitView, excludeEmpty, recursive));
                        }
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
        Delegator delegator = category.getDelegator();
        long count = 0;
        try {
            count = delegator.findCountByCondition("ProductCategoryMember", buildCountCondition("productCategoryId", category.getString("productCategoryId")), null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return count;
    }

    public static long categoryRollupCount(GenericValue category) {
        if (category == null) return 0;
        Delegator delegator = category.getDelegator();
        long count = 0;
        try {
            count = delegator.findCountByCondition("ProductCategoryRollup", buildCountCondition("parentProductCategoryId", category.getString("productCategoryId")), null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return count;
    }

    private static EntityCondition buildCountCondition(String fieldName, String fieldValue) {
        List<EntityCondition> orCondList = FastList.newInstance();
        orCondList.add(EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp()));
        orCondList.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
        EntityCondition orCond = EntityCondition.makeCondition(orCondList, EntityOperator.OR);

        List<EntityCondition> andCondList = FastList.newInstance();
        andCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp()));
        andCondList.add(EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, fieldValue));
        andCondList.add(orCond);
        EntityCondition andCond = EntityCondition.makeCondition(andCondList, EntityOperator.AND);

        return andCond;
    }

    public static void setTrail(ServletRequest request, String currentCategory) {
        Map<String, Object> requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String previousCategory = (String) requestParameters.get("pcategory");
        setTrail(request, currentCategory, previousCategory);
    }

    public static void setTrail(ServletRequest request, String currentCategory, String previousCategory) {
        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.setTrail] Start: previousCategory=" + previousCategory + " currentCategory=" + currentCategory, module);

        // if there is no current category, just return and do nothing to that the last settings will stay
        if (UtilValidate.isEmpty(currentCategory)) {
            return;
        }

        // always get the last crumb list
        List<String> crumb = getTrail(request);
        crumb = adjustTrail(crumb, currentCategory, previousCategory);
        setTrail(request, crumb);
    }

    public static List<String> adjustTrail(List<String> origTrail, String currentCategoryId, String previousCategoryId) {
        List<String> trail = FastList.newInstance();
        if (origTrail != null) {
            trail.addAll(origTrail);
        }

        // if no previous category was specified, check to see if currentCategory is in the list
        if (UtilValidate.isEmpty(previousCategoryId)) {
            if (trail.contains(currentCategoryId)) {
                // if cur category is in crumb, remove everything after it and return
                int cindex = trail.lastIndexOf(currentCategoryId);

                if (cindex < (trail.size() - 1)) {
                    for (int i = trail.size() - 1; i > cindex; i--) {
                        String deadCat = trail.remove(i);
                        //if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Removed after current category index: " + i + " catname: " + deadCat, module);
                    }
                }
                return trail;
            } else {
                // current category is not in the list, and no previous category was specified, go back to the beginning
                trail.clear();
                trail.add("TOP");
                if (UtilValidate.isNotEmpty(previousCategoryId)) {
                    trail.add(previousCategoryId);
                }
                //if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Starting new list, added TOP and previousCategory: " + previousCategoryId, module);
            }
        }

        if (!trail.contains(previousCategoryId)) {
            // previous category was NOT in the list, ERROR, start over
            //if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] previousCategory (" + previousCategoryId + ") was not in the crumb list, position is lost, starting over with TOP", module);
            trail.clear();
            trail.add("TOP");
            if (UtilValidate.isNotEmpty(previousCategoryId)) {
                trail.add(previousCategoryId);
            }
        } else {
            // remove all categories after the previous category, preparing for adding the current category
            int index = trail.indexOf(previousCategoryId);
            if (index < (trail.size() - 1)) {
                for (int i = trail.size() - 1; i > index; i--) {
                    String deadCat = trail.remove(i);
                    //if (Debug.infoOn()) Debug.logInfo("[CategoryWorker.setTrail] Removed after current category index: " + i + " catname: " + deadCat, module);
                }
            }
        }

        // add the current category to the end of the list
        trail.add(currentCategoryId);
        if (Debug.verboseOn()) Debug.logVerbose("[CategoryWorker.setTrail] Continuing list: Added currentCategory: " + currentCategoryId, module);

        return trail;
    }

    public static List<String> getTrail(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        List<String> crumb = UtilGenerics.checkList(session.getAttribute("_BREAD_CRUMB_TRAIL_"));
        return crumb;
    }

    public static List<String> setTrail(ServletRequest request, List<String> crumb) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        session.setAttribute("_BREAD_CRUMB_TRAIL_", crumb);
        return crumb;
    }

    public static boolean checkTrailItem(ServletRequest request, String category) {
        List<String> crumb = getTrail(request);

        if (crumb != null && crumb.contains(category)) {
            return true;
        } else {
            return false;
        }
    }

    public static String lastTrailItem(ServletRequest request) {
        List<String> crumb = getTrail(request);

        if (UtilValidate.isNotEmpty(crumb)) {
            return crumb.get(crumb.size() - 1);
        } else {
            return null;
        }
    }

    public static boolean isProductInCategory(Delegator delegator, String productId, String productCategoryId) throws GenericEntityException {
        if (productCategoryId == null) return false;
        if (UtilValidate.isEmpty(productId)) return false;

        List<GenericValue> productCategoryMembers = EntityUtil.filterByDate(delegator.findByAndCache("ProductCategoryMember",
                UtilMisc.toMap("productCategoryId", productCategoryId, "productId", productId)), true);
        if (UtilValidate.isEmpty(productCategoryMembers)) {
            //before giving up see if this is a variant product, and if so look up the virtual product and check it...
            GenericValue product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            List<GenericValue> productAssocs = ProductWorker.getVariantVirtualAssocs(product);
            //this does take into account that a product could be a variant of multiple products, but this shouldn't ever really happen...
            if (productAssocs != null) {
                for (GenericValue productAssoc: productAssocs) {
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

    public static List<GenericValue> filterProductsInCategory(Delegator delegator, List<GenericValue> valueObjects, String productCategoryId) throws GenericEntityException {
        return filterProductsInCategory(delegator, valueObjects, productCategoryId, "productId");
    }

    public static List<GenericValue> filterProductsInCategory(Delegator delegator, List<GenericValue> valueObjects, String productCategoryId, String productIdFieldName) throws GenericEntityException {
        if (valueObjects == null) return null;
        if (productCategoryId == null) return FastList.newInstance();

        EntityCondition productCategoryIdCondition = EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, productCategoryId);
        Iterator<GenericValue> it = valueObjects.iterator();
        Set<String> allowedProductIds = FastSet.newInstance();
        Map<String, Set<String>> variants = FastMap.newInstance();
        Timestamp now = UtilDateTime.nowTimestamp();
        while (it.hasNext()) {
            Set<String> lookupProductIds = FastSet.newInstance();
            while (lookupProductIds.size() < 100 && it.hasNext()) {
                GenericValue value = it.next();
                lookupProductIds.add(value.getString("productId"));
            }
            EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("productId", EntityOperator.IN, lookupProductIds), EntityOperator.AND, productCategoryIdCondition);
            //Debug.logInfo("query(ProductAndCategoryMember)->" + condition, module);
            List<GenericValue> subProducts = delegator.findList("ProductAndCategoryMember", condition, null, null, null, true);
            for (GenericValue subProduct: subProducts) {
                String productId = subProduct.getString("productId");
                if (EntityUtil.isValueActive(subProduct, now)) {
                    allowedProductIds.add(productId);
                    variants.remove(productId);
                } else if ("Y".equals(subProduct.get("isVariant"))) {
                    variants.put(productId, null);
                }
            }
        }

        return filterVariantsInCategory(delegator, now, productCategoryIdCondition, valueObjects, allowedProductIds, variants, productIdFieldName);
    }

    private static List<GenericValue> filterVariantsInCategory(Delegator delegator, Timestamp now, EntityCondition productCategoryIdCondition, List<GenericValue> valueObjects, Set<String> allowedProductIds, Map<String, Set<String>> variants, String productIdFieldName) throws GenericEntityException {
        List<GenericValue> newList = FastList.newInstance();
        EntityCondition assocTypeIdCondition = EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT");
        Map<String, Set<String>> revVariantMap = FastMap.newInstance();
        Map<String, Set<String>> revParentMap = FastMap.newInstance();
        // there may have been multiple rows with a variant's productId,
        // with some of those rows not being active by date, with later
        // rows being active.  This would cause the earlier rows to get
        // added to variants, and the later ones to be added to
        // allowed products.  Since we only care about finding allowed
        // products, once something is allowed, we no longer need to be
        // concerned about it.
        variants.keySet().removeAll(allowedProductIds);
        while (!variants.isEmpty()) {
            Iterator<Map.Entry<String, Set<String>>> variantIt = variants.entrySet().iterator();
            // this maps from possible variant child id, to original id,
            // from the first loop above; this is so we can finally mark
            // the original product as allowed
            while (variantIt.hasNext()) {
                Map.Entry<String, Set<String>> entry = variantIt.next();
                Set<String> assocParents = entry.getValue();
                if (assocParents == null) {
                    // this only happens the first time thru the outer
                    // loop.
                    UtilMisc.addToSetInMap(entry.getKey(), revVariantMap, entry.getKey());
                    entry.setValue(FastSet.<String>newInstance());
                } else if (assocParents.isEmpty()) {
                    variantIt.remove();
                } else {
                    for (String assocParent: assocParents) {
                        UtilMisc.addToSetInMap(entry.getKey(), revVariantMap, assocParent);
                    }
                    assocParents.clear();
                }
            }
            Iterator<String> variantIdIt = revVariantMap.keySet().iterator();
            while (variantIdIt.hasNext()) {
                Set<String> lookupProductIds = FastSet.newInstance();
                while (variantIdIt.hasNext() && lookupProductIds.size() < 100) {
                    lookupProductIds.add(variantIdIt.next());
                }
                // FIXME: use correct productAssocTypeId
                EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("productIdTo", EntityOperator.IN, lookupProductIds), EntityOperator.AND, assocTypeIdCondition);
                Debug.logInfo("query(ProductAndAssoc)->" + condition, module);
                List<GenericValue> assocValues = delegator.findList("ProductAndAssoc", condition, null, null, null, true);
                for (GenericValue assocValue: assocValues) {
                    if (!EntityUtil.isValueActive(assocValue, now)) {
                        continue;
                    }
                    String productIdTo = assocValue.getString("productIdTo");
                    String parentProductId = assocValue.getString("productId");
                    for (String originalProductId: revVariantMap.get(productIdTo)) {
                        variants.get(originalProductId).add(parentProductId);
                        //allParentProductIds.add(parentProductId);
                        UtilMisc.addToSetInMap(originalProductId, revParentMap, parentProductId);
                    }
                }
            }
            revVariantMap.clear();
            // at this point, the values in variants contain all the
            // parent productIds.  Now do another membership test in
            // the requested productCategory.
            Iterator<String> parentIdIt = revParentMap.keySet().iterator();
            while (parentIdIt.hasNext()) {
                Set<String> lookupProductIds = FastSet.newInstance();
                while (parentIdIt.hasNext() && lookupProductIds.size() < 100) {
                    lookupProductIds.add(parentIdIt.next());
                }
                EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("productId", EntityOperator.IN, lookupProductIds), EntityOperator.AND, productCategoryIdCondition);
                Debug.logInfo("query(ProductAndCategoryMember)->" + condition, module);
                List<GenericValue> subProducts = delegator.findList("ProductAndCategoryMember", condition, null, null, null, true);
                for (GenericValue subProduct: subProducts) {
                    String productId = subProduct.getString("productId");
                    if (EntityUtil.isValueActive(subProduct, now)) {
                        // yay, found a membership
                        for (String originalProductId: revParentMap.get(productId)) {
                            allowedProductIds.add(originalProductId);
                            variants.remove(originalProductId);
                        }
                    } else if ("Y".equals(subProduct.get("isVariant"))) {
                        for (String originalProductId: revParentMap.get(productId)) {
                            UtilMisc.addToSetInMap(productId, variants, originalProductId);
                        }
                    }
                }
            }
            revParentMap.clear();
        }
        for (GenericValue curValue: valueObjects) {
            String productId = curValue.getString(productIdFieldName);
            if (allowedProductIds.contains(productId)) {
                newList.add(curValue);
            }
        }
        return newList;
    }

    public static List<GenericValue> filterProductsInCategory(Delegator delegator, EntityCondition lookupCondition, List<String> orderByFields, boolean activeOnly, List<EntityCondition> filterConditions, String productCategoryId) throws GenericEntityException {
        List<GenericValue> newList = FastList.newInstance();
        if (productCategoryId == null) return newList;

        EntityCondition productCategoryIdCondition = EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, productCategoryId);
        EntityCondition assocTypeIdCondition = EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT");
        Set<String> allowedProductIds = FastSet.newInstance();
        Map<String, Set<String>> variants = FastMap.newInstance();
        Map<String, Set<String>> revVariantMap = FastMap.newInstance();
        Map<String, Set<String>> revParentMap = FastMap.newInstance();
        Timestamp now = UtilDateTime.nowTimestamp();

        EntityCondition valueCondition = EntityCondition.makeCondition(lookupCondition, EntityOperator.AND, EntityCondition.makeCondition("secondaryProductCategoryId", EntityOperator.EQUALS, productCategoryId));
        //Debug.logInfo("findList(ProductAndCategoryMember, " + valueCondition + ", " + orderByFields + ")", module);
        List<GenericValue> valueObjects = delegator.findList("ProductAndCategoryMemberDouble", valueCondition, null, orderByFields, null, true);
        //Debug.logInfo("valueObjects.size()=" + valueObjects.size(), module);
        if (filterConditions.isEmpty()) {
            valueObjects = EntityUtil.filterByAnd(valueObjects, filterConditions);
        }
        if (activeOnly) {
            valueObjects = EntityUtil.filterByDate(valueObjects, now);
        }
        //Debug.logInfo("valueObjects.size()=" + valueObjects.size(), module);
        Iterator<GenericValue> it = valueObjects.iterator();
        while (it.hasNext()) {
            GenericValue row = it.next();
            String productId = row.getString("productId");
            if (EntityUtil.isValueActive(row, now, "secondaryFromDate", "secondaryThruDate")) {
                allowedProductIds.add(productId);
            } else if ("Y".equals(row.get("isVariant"))) {
                variants.put(productId, null);
            }
        }
        //Debug.logInfo("allowedProductIds.size()=" + allowedProductIds.size(), module);
        return filterVariantsInCategory(delegator, now, productCategoryIdCondition, valueObjects, allowedProductIds, variants, "productId");
    }

    public static void getCategoryContentWrappers(Map<String, CategoryContentWrapper> catContentWrappers, List<GenericValue> categoryList, HttpServletRequest request) throws GenericEntityException {
        if (catContentWrappers == null || categoryList == null) {
            return;
        }
        for (GenericValue cat: categoryList) {
            String productCategoryId = (String) cat.get("productCategoryId");

            if (catContentWrappers.containsKey(productCategoryId)) {
                // if this ID is already in the Map, skip it (avoids inefficiency, infinite recursion, etc.)
                continue;
            }

            CategoryContentWrapper catContentWrapper = new CategoryContentWrapper(cat, request);
            catContentWrappers.put(productCategoryId, catContentWrapper);
            List<GenericValue> subCat = getRelatedCategoriesRet(request, "subCatList", productCategoryId, true);
            if (subCat != null) {
                getCategoryContentWrappers(catContentWrappers, subCat, request);
            }
        }
    }
}
