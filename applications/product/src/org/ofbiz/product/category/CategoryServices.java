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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.json.JSONObject;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

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
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId));
            members = EntityUtil.filterByDate(productCategory.getRelatedCache("ProductCategoryMember", null, UtilMisc.toList("sequenceNum")), true);
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
        if (orderByFields == null) orderByFields = FastList.newInstance();
        String entityName = getCategoryFindEntityName(delegator, orderByFields, introductionDateLimit, releaseDateLimit);

        GenericValue productCategory;
        List<GenericValue> productCategoryMembers;
        try {
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId));
            productCategoryMembers = delegator.findByAndCache(entityName, UtilMisc.toMap("productCategoryId", categoryId), orderByFields);
        } catch (GenericEntityException e) {
            Debug.logInfo(e, "Error finding previous/next product info: " + e.toString(), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "categoryservices.error_find_next_products", UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (activeOnly) {
            productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
        }
        List<EntityCondition> filterConditions = FastList.newInstance();
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
        String productCategoryId = (String) context.get("productCategoryId");
        boolean limitView = ((Boolean) context.get("limitView")).booleanValue();
        int defaultViewSize = ((Integer) context.get("defaultViewSize")).intValue();
        Timestamp introductionDateLimit = (Timestamp) context.get("introductionDateLimit");
        Timestamp releaseDateLimit = (Timestamp) context.get("releaseDateLimit");

        List<String> orderByFields = UtilGenerics.checkList(context.get("orderByFields"));
        if (orderByFields == null) orderByFields = FastList.newInstance();
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

        int viewIndex = 1;
        try {
            viewIndex = Integer.valueOf((String) context.get("viewIndexString")).intValue();
        } catch (Exception e) {
            viewIndex = 1;
        }

        int viewSize = defaultViewSize;
        try {
            viewSize = Integer.valueOf((String) context.get("viewSizeString")).intValue();
        } catch (Exception e) {
            viewSize = defaultViewSize;
        }

        GenericValue productCategory = null;
        try {
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            productCategory = null;
        }

        int listSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        if (limitView) {
            // get the indexes for the partial list
            lowIndex = (((viewIndex - 1) * viewSize) + 1);
            highIndex = viewIndex * viewSize;
        } else {
            lowIndex = 0;
            highIndex = 0;
        }

        List<GenericValue> productCategoryMembers = null;
        if (productCategory != null) {
            try {
                if (useCacheForMembers) {
                    productCategoryMembers = delegator.findByAndCache(entityName, UtilMisc.toMap("productCategoryId", productCategoryId), orderByFields);
                    if (activeOnly) {
                        productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    }
                    List<EntityCondition> filterConditions = FastList.newInstance();
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

                    // filter out the view allow before getting the sublist
                    if (UtilValidate.isNotEmpty(viewProductCategoryId)) {
                        productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                        listSize = productCategoryMembers.size();
                    }

                    // set the index and size
                    listSize = productCategoryMembers.size();
                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }

                    // get only between low and high indexes
                    if (limitView) {
                        if (UtilValidate.isNotEmpty(productCategoryMembers)) {
                            productCategoryMembers = productCategoryMembers.subList(lowIndex-1, highIndex);
                        }
                    } else {
                        lowIndex = 1;
                        highIndex = listSize;
                    }
                } else {
                    List<EntityCondition> mainCondList = FastList.newInstance();
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

                    // set distinct on
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, false);
                    findOpts.setMaxRows(highIndex);
                    // using list iterator
                    EntityListIterator pli = delegator.find(entityName, mainCond, null, null, orderByFields, findOpts);

                    // get the partial list for this page
                    if (limitView) {
                        if (viewProductCategoryId != null) {
                            // do manual checking to filter view allow
                            productCategoryMembers = FastList.newInstance();
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
                            // fiter out the view allow
                            productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                        }

                        listSize = productCategoryMembers.size();
                        lowIndex = 1;
                        highIndex = listSize;
                    }

                    // null safety
                    if (productCategoryMembers == null) {
                        productCategoryMembers = FastList.newInstance();
                    }

                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }

                    // close the list iterator
                    pli.close();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        Map<String, Object> result = FastMap.newInstance();
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
    public static void getChildCategoryTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productCategoryId = request.getParameter("productCategoryId");
        String isCatalog = request.getParameter("isCatalog");
        String isCategoryType = request.getParameter("isCategoryType");
        String entityName = null;
        String primaryKeyName = null;
        
        if (isCatalog.equals("true")) {
            entityName = "ProdCatalog";
            primaryKeyName = "prodCatalogId";
        } else {
            entityName = "ProductCategory";
            primaryKeyName = "productCategoryId";
        }
        
        List categoryList = FastList.newInstance();
        List<GenericValue> childOfCats;
        List<String> sortList = org.ofbiz.base.util.UtilMisc.toList("sequenceNum", "title");
        
        try {
            GenericValue category = delegator.findByPrimaryKey(entityName ,UtilMisc.toMap(primaryKeyName, productCategoryId));
            if (UtilValidate.isNotEmpty(category)) {
                if (isCatalog.equals("true") && isCategoryType.equals("false")) {
                    CategoryWorker.getRelatedCategories(request, "ChildCatalogList", CatalogWorker.getCatalogTopCategoryId(request, productCategoryId), true);
                    childOfCats = EntityUtil.filterByDate((List<GenericValue>) request.getAttribute("ChildCatalogList"));
                    
                } else if(isCatalog.equals("false") && isCategoryType.equals("false")){
                    childOfCats = EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryRollupAndChild", UtilMisc.toMap(
                            "parentProductCategoryId", productCategoryId )));
                } else {
                    childOfCats = EntityUtil.filterByDate(delegator.findByAnd("ProdCatalogCategory", UtilMisc.toMap("prodCatalogId", productCategoryId)));
                }
                if (UtilValidate.isNotEmpty(childOfCats)) {
                	
                    for (GenericValue childOfCat : childOfCats ) {
                        
                        Object catId = null;
                        String catNameField = null;
                        
                        catId = childOfCat.get("productCategoryId");
                        catNameField = "CATEGORY_NAME";
                        
                        Map josonMap = FastMap.newInstance();
                        List<GenericValue> childList = null;
                        
                        // Get the child list of chosen category
                        childList = EntityUtil.filterByDate(delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap(
                                    "parentProductCategoryId", catId)));
                        
                        // Get the chosen category information for the categoryContentWrapper
                        GenericValue cate = delegator.findByPrimaryKey("ProductCategory" ,UtilMisc.toMap("productCategoryId",catId));
                        
                        // If chosen category's child exists, then put the arrow before category icon
                        if (UtilValidate.isNotEmpty(childList)) {
                            josonMap.put("state", "closed");
                        }
                        Map dataMap = FastMap.newInstance();
                        Map dataAttrMap = FastMap.newInstance();
                        CategoryContentWrapper categoryContentWrapper = new CategoryContentWrapper(cate, request);
                        
                        String title = null;
                        if (UtilValidate.isNotEmpty(categoryContentWrapper.get(catNameField))) {
                            title = categoryContentWrapper.get(catNameField)+" "+"["+catId+"]";
                            dataMap.put("title", title);
                        } else {
                            title = catId.toString();
                            dataMap.put("title", catId);
                        }
                        dataAttrMap.put("onClick","window.location.href='EditCategory?productCategoryId="+catId+"'; return false;");
                        
                        dataMap.put("attr", dataAttrMap);
                        josonMap.put("data", dataMap);
                        Map attrMap = FastMap.newInstance();
                        attrMap.put("id", catId);
                        attrMap.put("isCatalog", false);
                        attrMap.put("rel", "CATEGORY");
                        josonMap.put("attr",attrMap);
                        josonMap.put("sequenceNum",childOfCat.get("sequenceNum"));
                        josonMap.put("title",title);
                        
                        categoryList.add(josonMap);
                    }
                    List<Map<Object, Object>> sortedCategoryList = UtilMisc.sortMaps(categoryList, sortList);
                    toJsonObjectList(sortedCategoryList,response);
                }
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void toJsonObjectList(List attrList, HttpServletResponse response){
        String jsonStr = "[";
        for (Object attrMap : attrList) {
            JSONObject json = JSONObject.fromObject(attrMap);
            jsonStr = jsonStr + json.toString() + ',';
        }
        jsonStr = jsonStr + "{ } ]";
        if (UtilValidate.isEmpty(jsonStr)) {
            Debug.logError("JSON Object was empty; fatal error!",module);
        }
        // set the X-JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        try {
            response.setContentLength(jsonStr.getBytes("UTF8").length);
        } catch (UnsupportedEncodingException e) {
            Debug.logError("Problems with Json encoding",module);
        }
        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            Debug.logError("Unable to get response writer",module);
        }
    }
}
