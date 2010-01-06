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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
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

    public static Map<String, Object> getCategoryMembers(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String categoryId = (String) context.get("categoryId");
        GenericValue productCategory = null;
        List<GenericValue> members = null;

        try {
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId));
            members = EntityUtil.filterByDate(productCategory.getRelatedCache("ProductCategoryMember", null, UtilMisc.toList("sequenceNum")), true);
            if (Debug.verboseOn()) Debug.logVerbose("Category: " + productCategory + " Member Size: " + members.size() + " Members: " + members, module);
        } catch (GenericEntityException e) {
            String errMsg = "Problem reading product categories: " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
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

        if (index == null && productId == null) {
            return ServiceUtil.returnError("Both Index and ProductID cannot be null.");
        }

        List<String> orderByFields = UtilGenerics.checkList(context.get("orderByFields"));
        if (orderByFields == null) orderByFields = FastList.newInstance();
        String entityName = getCategoryFindEntityName(delegator, orderByFields);

        GenericValue productCategory;
        List<GenericValue> productCategoryMembers;
        try {
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId));
            productCategoryMembers = delegator.findByAndCache(entityName, UtilMisc.toMap("productCategoryId", categoryId), orderByFields);
        } catch (GenericEntityException e) {
            String errMsg = "Error finding previous/next product info: " + e.toString();
            Debug.logInfo(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        if (activeOnly) {
            productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
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
            return ServiceUtil.returnSuccess("Product not found in the current category.");
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

    private static String getCategoryFindEntityName(Delegator delegator, List<String> orderByFields) {
        // allow orderByFields to contain fields from the Product entity, if there are such fields
        String entityName = "ProductCategoryMember";
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

        List<String> orderByFields = UtilGenerics.checkList(context.get("orderByFields"));
        if (orderByFields == null) orderByFields = FastList.newInstance();
        String entityName = getCategoryFindEntityName(delegator, orderByFields);

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
                    EntityCondition mainCond = EntityCondition.makeCondition(mainCondList, EntityOperator.AND);

                    // set distinct on
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
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
}
