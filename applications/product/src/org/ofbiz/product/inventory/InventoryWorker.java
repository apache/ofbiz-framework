/*
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

package org.ofbiz.product.inventory;

import java.util.List;
import java.util.Iterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

public class InventoryWorker {
    
    public final static String module = InventoryWorker.class.getName();

    /**
     * Finds all outstanding Purchase orders for a productId.  The orders and the items cannot be completed, cancelled, or rejected
     * @param productId
     * @param delegator
     * @return
     */
    public static List getOutstandingPurchaseOrders(String productId, GenericDelegator delegator) {
        try {
            List purchaseOrderConditions = UtilMisc.toList(new EntityExpr("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED"),
                    new EntityExpr("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                    new EntityExpr("orderStatusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_COMPLETED"),
                    new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                    new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"));
            purchaseOrderConditions.add(new EntityExpr("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"));
            purchaseOrderConditions.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
            List purchaseOrders = delegator.findByCondition("OrderHeaderAndItems", new EntityConditionList(purchaseOrderConditions, EntityOperator.AND), 
                    null, UtilMisc.toList("estimatedDeliveryDate DESC", "orderDate"));
            return purchaseOrders;
        } catch (GenericEntityException ex) {
            Debug.logError("Unable to find outstanding purchase orders for product [" + productId + "] due to " + ex.getMessage() + " - returning null", module);
            return null;
        }
    }
    
    /**
     * Finds the net outstanding ordered quantity for a productId, netting quantity on outstanding purchase orders against cancelQuantity
     * @param productId
     * @param delegator
     * @return
     */
    public static double getOutstandingPurchasedQuantity(String productId, GenericDelegator delegator) {
        double qty = 0.0;
        List purchaseOrders = getOutstandingPurchaseOrders(productId, delegator);
        if (UtilValidate.isEmpty(purchaseOrders)) {
            return qty;
        } else {
            for (Iterator pOi = purchaseOrders.iterator(); pOi.hasNext();) {
                GenericValue nextOrder = (GenericValue) pOi.next();
                if (nextOrder.get("quantity") != null) {
                    double itemQuantity = nextOrder.getDouble("quantity").doubleValue();
                    double cancelQuantity = 0.0;
                    if (nextOrder.get("cancelQuantity") != null) {
                        cancelQuantity = nextOrder.getDouble("cancelQuantity").doubleValue();
                    }
                    itemQuantity -= cancelQuantity;
                    if (itemQuantity >= 0.0) {
                        qty += itemQuantity;
                    }
                }
            }
        }

        return qty;
    }

}    