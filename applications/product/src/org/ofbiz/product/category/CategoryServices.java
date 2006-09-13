/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.product.category;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * CategoryServices - Category Services
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CategoryServices {
    
    public static final String module = CategoryServices.class.getName();

    public static Map getCategoryMembers(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String categoryId = (String) context.get("categoryId");
        GenericValue productCategory = null;
        List members = null;

        try {
            productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId));
            members = EntityUtil.filterByDate(productCategory.getRelatedCache("ProductCategoryMember", null, UtilMisc.toList("sequenceNum")), true);
            if (Debug.verboseOn()) Debug.logVerbose("Category: " + productCategory + " Member Size: " + members.size() + " Members: " + members, module);
        } catch (GenericEntityException e) {
            String errMsg = "Problem reading product categories: " + e.getMessage();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("category", productCategory);
        result.put("categoryMembers", members);
        return result;
    }

    public static Map getNextPreviousCategoryMembers(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String categoryId = (String) context.get("categoryId");
        String productId = (String) context.get("productId");
        Integer index = (Integer) context.get("index");

        if (index == null && productId == null) {
            return ServiceUtil.returnError("Both Index and ProductID cannot be null.");
        }

        Map values = getCategoryMembers(dctx, context);

        if (values.containsKey(ModelService.ERROR_MESSAGE)) {
            return values;
        }
        if (!values.containsKey("categoryMembers") || values.get("categoryMembers") == null) {
            return ServiceUtil.returnError("Problem reading category data.");
        }

        Collection memberCol = (Collection) values.get("categoryMembers");
        if (memberCol == null || memberCol.size() == 0) {
            // this is not going to be an error condition because we don't want it to be so critical, ie rolling back the transaction and such
            return ServiceUtil.returnSuccess("Product not found in the current category.");
        }

        List memberList = new ArrayList(memberCol);

        if (productId != null && index == null) {
            Iterator i = memberList.iterator();

            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();

                if (v.getString("productId").equals(productId))
                    index = new Integer(memberList.indexOf(v));
            }
        }

        if (index == null) {
            // this is not going to be an error condition because we don't want it to be so critical, ie rolling back the transaction and such
            return ServiceUtil.returnSuccess("Product not found in the current category.");
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("category", values.get("category"));

        String previous = null;
        String next = null;

        if (index.intValue() - 1 >= 0 && index.intValue() - 1 < memberList.size()) {
            previous = ((GenericValue) memberList.get(index.intValue() - 1)).getString("productId");
            result.put("previousProductId", previous);
        } else {
            previous = ((GenericValue) memberList.get(memberList.size() - 1)).getString("productId");
            result.put("previousProductId", previous);
        }

        if (index.intValue() + 1 < memberList.size()) {
            next = ((GenericValue) memberList.get(index.intValue() + 1)).getString("productId");
            result.put("nextProductId", next);
        } else {
            next = ((GenericValue) memberList.get(0)).getString("productId");
            result.put("nextProductId", next);
        }
        return result;
    }

    public static Map getProductCategoryAndLimitedMembers(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productCategoryId = (String) context.get("productCategoryId");
        boolean limitView = ((Boolean) context.get("limitView")).booleanValue();
        int defaultViewSize = ((Integer) context.get("defaultViewSize")).intValue();

        String prodCatalogId = (String) context.get("prodCatalogId");

        boolean useCacheForMembers = (context.get("useCacheForMembers") != null ? ((Boolean) context.get("useCacheForMembers")).booleanValue() : true);
        boolean activeOnly = (context.get("activeOnly") != null ? ((Boolean) context.get("activeOnly")).booleanValue() : true);
        // checkViewAllow defaults to false, must be set to true and pass the prodCatalogId to enable
        boolean checkViewAllow = (context.get("checkViewAllow") != null ? ((Boolean) context.get("checkViewAllow")).booleanValue() : false);
        
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
        }
        
        List productCategoryMembers = null;
        if (productCategory != null) {
            try {
                if (useCacheForMembers) {
                    productCategoryMembers = productCategory.getRelatedCache("ProductCategoryMember", null, UtilMisc.toList("sequenceNum"));
                    if (activeOnly) {
                        productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    }
                    listSize = productCategoryMembers.size();
                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }

                    if (limitView) {
                        productCategoryMembers = productCategoryMembers.subList(lowIndex-1, highIndex);
                    } else {
                        lowIndex = 1;
                        highIndex = listSize;
                    }
                } else {
                    List mainCondList = UtilMisc.toList(new EntityExpr("productCategoryId", EntityOperator.EQUALS, productCategory.getString("productCategoryId")));
                    if (activeOnly) {
                        mainCondList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));
                        mainCondList.add(new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN, nowTimestamp)));
                    }
                    EntityCondition mainCond = new EntityConditionList(mainCondList, EntityOperator.AND);
                
                    // set distinct on so we only get one row per order
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
                    // using list iterator
                    EntityListIterator pli = delegator.findListIteratorByCondition("ProductCategoryMember", mainCond, null, null, UtilMisc.toList("sequenceNum", "productId"), findOpts);
                
                    // get the partial list for this page
                    if (limitView) {
                        productCategoryMembers = pli.getPartialList(lowIndex, viewSize);
                        // attempt to get the full size
                        pli.last();
                        listSize = pli.currentIndex();
                    } else {
                        productCategoryMembers = pli.getCompleteList();
                        listSize = productCategoryMembers.size();
                        lowIndex = 1;
                        highIndex = listSize;
                    }
                    if (productCategoryMembers == null) {
                        productCategoryMembers = FastList.newInstance();
                    }
                
                    if (highIndex > listSize) {
                        highIndex = listSize;
                    }
                
                    // close the list iterator
                    pli.close();
                }
                
                // first check to see if there is a view allow category and if this product is in it...
                if (checkViewAllow && prodCatalogId != null && productCategoryMembers != null && productCategoryMembers.size() > 0) {
                    String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, prodCatalogId);
                    if (viewProductCategoryId != null) {
                        productCategoryMembers = CategoryWorker.filterProductsInCategory(delegator, productCategoryMembers, viewProductCategoryId);
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        Map result = new HashMap();
        result.put("viewIndex", new Integer(viewIndex));
        result.put("viewSize", new Integer(viewSize));
        result.put("lowIndex", new Integer(lowIndex));
        result.put("highIndex", new Integer(highIndex));
        result.put("listSize", new Integer(listSize));
        if (productCategory != null) result.put("productCategory", productCategory);
        if (productCategoryMembers != null) result.put("productCategoryMembers", productCategoryMembers);
        return result;
    }
}
