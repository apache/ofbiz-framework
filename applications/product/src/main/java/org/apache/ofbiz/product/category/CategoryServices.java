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
package org.apache.ofbiz.product.category;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * CategoryServices - Category Services
 */
public class CategoryServices {

    public static final String module = CategoryServices.class.getName();
    public static final String resourceError = "ProductErrorUiLabels";

    public static Map<String, Object> getCategoryMembers(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String categoryId = (String) context.get("categoryId");
        Locale locale = (Locale) context.get("locale");
        GenericValue productCategory = null;
        List<GenericValue> members = null;

        try {
            productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", categoryId).cache().queryOne();
            members = EntityUtil.filterByDate(productCategory.getRelated("ProductCategoryMember", null, UtilMisc.toList("sequenceNum"), true), true);
            if (Debug.verboseOn()) Debug.logVerbose("Category: " + productCategory + " Member Size: " + members.size() + " Members: " + members, module);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem reading product categories: " + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "categoryservices.problems_reading_category_entity", 
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("category", productCategory);
        result.put("categoryMembers", members);
        return result;
    }

    public static Map<String, Object> getPreviousNextProducts(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String categoryId = (String) context.get("categoryId");
        String productId = (String) context.get("productId");
        boolean activeOnly = (context.get("activeOnly") != null ? ((Boolean) context.get("activeOnly")).booleanValue() : true);
        Integer index = (Integer) context.get("index");
        Timestamp introductionDateLimit = (Timestamp) context.get("introductionDateLimit");
        Timestamp releaseDateLimit = (Timestamp) context.get("releaseDateLimit");
        Locale locale = (Locale) context.get("locale");
        
        if (index == null && productId == null) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "categoryservices.problems_getting_next_products", locale));
        }

        List<String> orderByFields = UtilGenerics.checkList(context.get("orderByFields"));
        if (orderByFields == null) orderByFields = new LinkedList<String>();
        String entityName = getCategoryFindEntityName(delegator, orderByFields, introductionDateLimit, releaseDateLimit);

        GenericValue productCategory;
        List<GenericValue> productCategoryMembers;
        try {
            productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", categoryId).cache().queryOne();
            productCategoryMembers = EntityQuery.use(delegator).from(entityName).where("productCategoryId", categoryId).orderBy(orderByFields).cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logInfo(e, "Error finding previous/next product info: " + e.toString(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "categoryservices.error_find_next_products", UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (activeOnly) {
            productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
        }
        List<EntityCondition> filterConditions = new LinkedList<EntityCondition>();
        if (introductionDateLimit != null) {
            EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("introductionDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("introductionDate", EntityOperator.LESS_THAN_EQUAL_TO, introductionDateLimit));
            filterConditions.add(condition);
        }
        if (releaseDateLimit != null) {
            EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("releaseDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("releaseDate", EntityOperator.LESS_THAN_EQUAL_TO, releaseDateLimit));
            filterConditions.add(condition);
        }
        if (!filterConditions.isEmpty()) {
            productCategoryMembers = EntityUtil.filterByCondition(productCategoryMembers, EntityCondition.makeCondition(filterConditions, EntityOperator.AND));
        }

        if (productId != null && index == null) {
            for (GenericValue v: productCategoryMembers) {
                if (v.getString("productId").equals(productId)) {
                    index = Integer.valueOf(productCategoryMembers.indexOf(v));
                }
            }
        }

        if (index == null) {
            // this is not going to be an error condition because we don't want it to be so critical, ie rolling back the transaction and such
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "categoryservices.product_not_found", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("category", productCategory);

        String previous = null;
        String next = null;

        if (index.intValue() - 1 >= 0 && index.intValue() - 1 < productCategoryMembers.size()) {
            previous = productCategoryMembers.get(index.intValue() - 1).getString("productId");
            result.put("previousProductId", previous);
        } else {
            previous = productCategoryMembers.get(productCategoryMembers.size() - 1).getString("productId");
            result.put("previousProductId", previous);
        }

        if (index.intValue() + 1 < productCategoryMembers.size()) {
            next = productCategoryMembers.get(index.intValue() + 1).getString("productId");
            result.put("nextProductId", next);
        } else {
            next = productCategoryMembers.get(0).getString("productId");
            result.put("nextProductId", next);
        }
        return result;
    }

    private static String getCategoryFindEntityName(Delegator delegator, List<String> orderByFields, Timestamp introductionDateLimit, Timestamp releaseDateLimit) {
        // allow orderByFields to contain fields from the Product entity, if there are such fields
        String entityName = introductionDateLimit == null && releaseDateLimit == null ? "ProductCategoryMember" : "ProductAndCategoryMember";
        if (orderByFields == null) {
            return entityName;
        }
        if (orderByFields.size() == 0) {
            orderByFields.add("sequenceNum");
            orderByFields.add("productId");
        }

        ModelEntity productModel = delegator.getModelEntity("Product");
        ModelEntity productCategoryMemberModel = delegator.getModelEntity("ProductCategoryMember");
        for (String orderByField: orderByFields) {
            // Get the real field name from the order by field removing ascending/descending order
            if (UtilValidate.isNotEmpty(orderByField)) {
                int startPos = 0, endPos = orderByField.length();

                if (orderByField.endsWith(" DESC")) {
                    endPos -= 5;
                } else if (orderByField.endsWith(" ASC")) {
                    endPos -= 4;
                } else if (orderByField.startsWith("-")) {
                    startPos++;
                } else if (orderByField.startsWith("+")) {
                    startPos++;
                }

                if (startPos != 0 || endPos != orderByField.length()) {
                    orderByField = orderByField.substring(startPos, endPos);
                }
            }

            if (!productCategoryMemberModel.isField(orderByField)) {
                if (productModel.isField(orderByField)) {
                    entityName = "ProductAndCategoryMember";
                    // that's what we wanted to find out, so we can quit now
                    break;
                } else {
                    // ahh!! bad field name, don't worry, it will blow up in the query
                }
            }
        }
        return entityName;
    }

    public static Map<String, Object> getProductCategoryAndLimitedMembers(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productCategoryId = (String) context.get("productCategoryId");
        boolean limitView = ((Boolean) context.get("limitView")).booleanValue();
        int defaultViewSize = ((Integer) context.get("defaultViewSize")).intValue();
        Timestamp introductionDateLimit = (Timestamp) context.get("introductionDateLimit");
        Timestamp releaseDateLimit = (Timestamp) context.get("releaseDateLimit");

        List<String> orderByFields = UtilGenerics.checkList(context.get("orderByFields"));
        if (orderByFields == null) orderByFields = new LinkedList<String>();
        String entityName = getCategoryFindEntityName(delegator, orderByFields, introductionDateLimit, releaseDateLimit);

        String prodCatalogId = (String) context.get("prodCatalogId");

        boolean useCacheForMembers = (context.get("useCacheForMembers") == null || ((Boolean) context.get("useCacheForMembers")).booleanValue());
        boolean activeOnly = (context.get("activeOnly") == null || ((Boolean) context.get("activeOnly")).booleanValue());

        // checkViewAllow defaults to false, must be set to true and pass the prodCatalogId to enable
        boolean checkViewAllow = (prodCatalogId != null && context.get("checkViewAllow") != null &&
                ((Boolean) context.get("checkViewAllow")).booleanValue());

        String viewProductCategoryId = null;
        if (checkViewAllow) {
            viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
        }

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        int viewIndex = 0;
        if (context.containsKey("viewIndexString")) {
            try {
                viewIndex = Integer.parseInt((String) context.get("viewIndexString"));
            } catch (Exception e) {
                viewIndex = 0;
            }
        }

        int viewSize = defaultViewSize;
        if (context.containsKey("viewSizeString")) {
            try {
                viewSize = Integer.parseInt((String) context.get("viewSizeString"));
            } catch (NumberFormatException e) {
                Debug.logWarning("Fail to parse viewSizeString "
                        + context.get("viewSizeString")
                        + " " + e.getMessage(), module);
            }
        }

        GenericValue productCategory = null;
        try {
            productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            productCategory = null;
        }

        int listSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        if (limitView) {
            // get the indexes for the partial list
            lowIndex = ((viewIndex * viewSize) + 1);
            highIndex = (viewIndex + 1) * viewSize;
        } else {
            lowIndex = 0;
            highIndex = 0;
        }
        
        boolean filterOutOfStock = false;
        try {
            String productStoreId = (String) context.get("productStoreId");
            if (UtilValidate.isNotEmpty(productStoreId)) {
                GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
                if (productStore != null && "N".equals(productStore.getString("showOutOfStockProducts"))) {
                    filterOutOfStock = true;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        List<GenericValue> productCategoryMembers = null;
        if (productCategory != null) {
            try {
                if (useCacheForMembers) {
                    productCategoryMembers = EntityQuery.use(delegator).from(entityName).where("productCategoryId", productCategoryId).orderBy(orderByFields).cache(true).queryList();
                    if (activeOnly) {
                        productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    }
                    List<EntityCondition> filterConditions = new LinkedList<EntityCondition>();
                    if (introductionDateLimit != null) {
                        EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("introductionDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("introductionDate", EntityOperator.LESS_THAN_EQUAL_TO, introductionDateLimit));
                        filterConditions.add(condition);
                    }
                    if (releaseDateLimit != null) {
                        EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("releaseDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("releaseDate", EntityOperator.LESS_THAN_EQUAL_TO, releaseDateLimit));
                        filterConditions.add(condition);
                    }
                    if (!filterConditions.isEmpty()) {
                        productCategoryMembers = EntityUtil.filterByCondition(productCategoryMembers, EntityCondition.makeCondition(filterConditions, EntityOperator.AND));
                    }
                    
                    // filter out of stock products
                    if (filterOutOfStock) {
                        try {
                            productCategoryMembers = ProductWorker.filterOutOfStockProducts(productCategoryMembers, dispatcher, delegator);
                        } catch (GeneralException e) {
                            Debug.logWarning("Problem filtering out of stock products :"+e.getMessage(), module);
                        }
                    }
                    // filter out the view allow before getting the sublist
                    if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
                        productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                    }

                    // set the index and size
                    listSize = productCategoryMembers.size();         
                    if (limitView) {
                        // limit high index to (filtered) listSize
                        if (highIndex > listSize) {
                            highIndex = listSize;
                        }
                        // if lowIndex > listSize, the input is wrong => reset to first page
                        if (lowIndex > listSize) {
                            viewIndex = 0;
                            lowIndex = 1;
                            highIndex = Math.min(viewSize, highIndex);
                        }
                        // get only between low and high indexes
                        if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                            productCategoryMembers = productCategoryMembers.subList(lowIndex-1, highIndex);
                        }
                    } else {
                        lowIndex = 1;
                        highIndex = listSize;
                    }
                } else {
                    List<EntityCondition> mainCondList = new LinkedList<EntityCondition>();
                    mainCondList.add(EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, productCategory.getString("productCategoryId")));
                    if (activeOnly) {
                        mainCondList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
                    }
                    if (introductionDateLimit != null) {
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("introductionDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("introductionDate", EntityOperator.LESS_THAN_EQUAL_TO, introductionDateLimit)));
                    }
                    if (releaseDateLimit != null) {
                        mainCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("releaseDate", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("releaseDate", EntityOperator.LESS_THAN_EQUAL_TO, releaseDateLimit)));
                    }
                    EntityCondition mainCond = EntityCondition.makeCondition(mainCondList, EntityOperator.AND);

                    // set distinct on using list iterator
                    EntityQuery eq = EntityQuery.use(delegator)
                            .from(entityName)
                            .where(mainCond)
                            .orderBy(orderByFields)
                            .cursorScrollInsensitive()
                            .maxRows(highIndex);
                    
                    try (EntityListIterator pli = eq.queryIterator()) {
                        // get the partial list for this page
                        if (limitView) {
                            if (viewProductCategoryId != null) {
                                // do manual checking to filter view allow
                                productCategoryMembers = new LinkedList<GenericValue>();
                                GenericValue nextValue;
                                int chunkSize = 0;
                                listSize = 0;
    
                                while ((nextValue = pli.next()) != null) {
                                    String productId = nextValue.getString("productId");
                                    if (CategoryWorker.isProductInCategory(delegator, productId, viewProductCategoryId)) {
                                        if (listSize + 1 >= lowIndex && chunkSize < viewSize) {
                                            productCategoryMembers.add(nextValue);
                                            chunkSize++;
                                        }
                                        listSize++;
                                    }
                                }
                            } else {
                                productCategoryMembers = pli.getPartialList(lowIndex, viewSize);
                                listSize = pli.getResultsSizeAfterPartialList();
                            }
                        } else {
                            productCategoryMembers = pli.getCompleteList();
                            if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
                                // filter out the view allow
                                productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                            }
    
                            listSize = productCategoryMembers.size();
                            lowIndex = 1;
                            highIndex = listSize;
                        }
                    }
                    // filter out of stock products
                    if (filterOutOfStock) {
                        try {
                            productCategoryMembers = ProductWorker.filterOutOfStockProducts(productCategoryMembers, dispatcher, delegator);
                            listSize = productCategoryMembers.size();
                        } catch (GeneralException e) {
                            Debug.logWarning("Problem filtering out of stock products :"+e.getMessage(), module);
                        }
                    }

                    // null safety
                    if (productCategoryMembers == null) {
                        productCategoryMembers = new LinkedList<GenericValue>();
                    }

                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("viewIndex", Integer.valueOf(viewIndex));
        result.put("viewSize", Integer.valueOf(viewSize));
        result.put("lowIndex", Integer.valueOf(lowIndex));
        result.put("highIndex", Integer.valueOf(highIndex));
        result.put("listSize", Integer.valueOf(listSize));
        if (productCategory != null) result.put("productCategory", productCategory);
        if (productCategoryMembers != null) result.put("productCategoryMembers", productCategoryMembers);
        return result;
    }

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    @SuppressWarnings("unchecked")
    public static String getChildCategoryTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productCategoryId = request.getParameter("productCategoryId");
        String isCatalog = request.getParameter("isCatalog");
        String isCategoryType = request.getParameter("isCategoryType");
        String onclickFunction = request.getParameter("onclickFunction");
        String additionParam = request.getParameter("additionParam");
        String hrefString = request.getParameter("hrefString");
        String hrefString2 = request.getParameter("hrefString2");
        String entityName = null;
        String primaryKeyName = null;

        if ("true".equals(isCatalog)) {
            entityName = "ProdCatalog";
            primaryKeyName = "prodCatalogId";
        } else {
            entityName = "ProductCategory";
            primaryKeyName = "productCategoryId";
        }

        List categoryList = new LinkedList();
        List<GenericValue> childOfCats;
        List<String> sortList = org.apache.ofbiz.base.util.UtilMisc.toList("sequenceNum", "title");

        try {
            GenericValue category = EntityQuery.use(delegator).from(entityName).where(primaryKeyName, productCategoryId).queryOne();
            if (category != null) {
                if ("true".equals(isCatalog) && "false".equals(isCategoryType)) {
                    CategoryWorker.getRelatedCategories(request, "ChildCatalogList", CatalogWorker.getCatalogTopCategoryId(request, productCategoryId), true);
                    childOfCats = EntityUtil.filterByDate((List<GenericValue>) request.getAttribute("ChildCatalogList"));
                    
                } else if("false".equals(isCatalog) && "false".equals(isCategoryType)){
                    childOfCats = EntityQuery.use(delegator).from("ProductCategoryRollupAndChild").where("parentProductCategoryId", productCategoryId).filterByDate().queryList();
                } else {
                    childOfCats = EntityQuery.use(delegator).from("ProdCatalogCategory").where("prodCatalogId", productCategoryId).filterByDate().queryList();
                }
                if (UtilValidate.isNotEmpty(childOfCats)) {
                        
                    for (GenericValue childOfCat : childOfCats ) {
                        
                        Object catId = null;
                        String catNameField = null;
                        
                        catId = childOfCat.get("productCategoryId");
                        catNameField = "CATEGORY_NAME";

                        Map josonMap = new HashMap();
                        List<GenericValue> childList = null;

                        // Get the child list of chosen category
                        childList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId", catId).filterByDate().queryList();

                        // Get the chosen category information for the categoryContentWrapper
                        GenericValue cate = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId",catId).queryOne();

                        // If chosen category's child exists, then put the arrow before category icon
                        if (UtilValidate.isNotEmpty(childList)) {
                            josonMap.put("state", "closed");
                        }
                        Map dataMap = new HashMap();
                        Map dataAttrMap = new HashMap();
                        CategoryContentWrapper categoryContentWrapper = new CategoryContentWrapper(cate, request);
                        String title = null;
                        if (UtilValidate.isNotEmpty(categoryContentWrapper.get(catNameField, "html"))) {
                            title = new StringBuffer(categoryContentWrapper.get(catNameField, "html").toString()).append(" [").append(catId).append("]").toString();
                            dataMap.put("title", title);
                        } else {
                            title = catId.toString();
                            dataMap.put("title", catId);
                        }
                        dataAttrMap.put("onClick", new StringBuffer(onclickFunction).append("('").append(catId).append(additionParam).append("')").toString());

                        String hrefStr = hrefString + catId;
                        if (UtilValidate.isNotEmpty(hrefString2)) {
                            hrefStr = hrefStr + hrefString2;
                        }
                        dataAttrMap.put("href", hrefStr);
                        dataMap.put("attr", dataAttrMap);
                        josonMap.put("data", dataMap);
                        Map attrMap = new HashMap();
                        attrMap.put("id", catId);
                        attrMap.put("isCatalog", false);
                        attrMap.put("rel", "CATEGORY");
                        josonMap.put("attr", attrMap);
                        josonMap.put("sequenceNum", childOfCat.get("sequenceNum"));
                        josonMap.put("title", title);
                        categoryList.add(josonMap);
                    }
                    List<Map<Object, Object>> sortedCategoryList = UtilMisc.sortMaps(categoryList, sortList);
                    request.setAttribute("treeData", sortedCategoryList);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return "error";
        }
        return "success";
    }
}
