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

package org.apache.ofbiz.manufacturing.bom;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Helper class containing static method useful when dealing
 * with product's bills of materials.
 * These methods are also available as services (see {@link BOMServices}).
 */
public final class BOMHelper {

    private static final String MODULE = BOMHelper.class.getName();

    /**
     * Creates a new instance of BOMHelper
     */
    private BOMHelper() { }

    /**
     * Returns the product's low level code (llc) i.e. the maximum depth
     * in which the productId can be found in any of the
     * bills of materials of bomType type.
     * @param productId The product id
     * @param bomType   The bill of materials type (e.g. manufacturing, engineering,...)
     * @param delegator Validity date (if null, today is used).
     * @param inDate    The delegator
     * @return The low level code for the productId. (0 = root, 1 = first level, etc...)
     * @throws GenericEntityException If a db problem occurs.
     */
    /*
     * It's implemented as a recursive method that performs the following tasks:
     * 1.given a product id, it selects all the product's parents
     * 2.if no parents are found, the method returns zero (llc = 0, this is a root element)
     * 3.for every parent the method is called recursively, the llc returned is incremented by one and the max of these number is saved as maxDepth
     * 4.the maxDepth value is returned
     */
    public static int getMaxDepth(String productId, String bomType, Date inDate, Delegator delegator) throws GenericEntityException {
        // If the date is null, set it to today.
        if (inDate == null) inDate = new Date();
        int maxDepth = 0;
        List<GenericValue> productNodesList = EntityQuery.use(delegator).from("ProductAssoc")
                .where("productIdTo", productId,
                        "productAssocTypeId", bomType)
                .cache().filterByDate(inDate).queryList();
        int depth = 0;
        for (GenericValue oneNode : productNodesList) {
            depth = 0;
            depth = getMaxDepth(oneNode.getString("productId"), bomType, inDate, delegator);
            depth++;
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        return maxDepth;
    }

    /**
     * Returns the ProductAssoc generic value for a duplicate productIdKey
     * ancestor if present, null otherwise.
     * Useful to avoid loops when adding new assocs (components)
     * to a bill of materials.
     * @param productId    The product to which we want to add a new child.
     * @param productIdKey The new component we want to add to the existing bom.
     * @param bomType      The bill of materials type (e.g. manufacturing, engineering).
     * @param inDate       Validity date (if null, today is used).
     * @param delegator    The delegator used
     * @return the ProductAssoc generic value for a duplicate productIdKey
     * ancestor if present, null otherwise.
     * @throws GenericEntityException If a db problem occurs
     */
    public static GenericValue searchDuplicatedAncestor(String productId, String productIdKey, String bomType, Date inDate, Delegator delegator,
                                                        LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        return searchDuplicatedAncestor(productId, productIdKey, null, bomType, inDate, delegator, dispatcher, userLogin);
    }

    private static GenericValue searchDuplicatedAncestor(String productId, String productIdKey, List<String> productIdKeys,
                                                         String bomType, Date inDate, Delegator delegator, LocalDispatcher dispatcher,
                                                         GenericValue userLogin) throws GenericEntityException {
        // If the date is null, set it to today.
        if (inDate == null) inDate = new Date();
        if (productIdKeys == null) {
            BOMTree tree = new BOMTree(productIdKey, bomType, inDate, delegator, dispatcher, userLogin);
            productIdKeys = tree.getAllProductsId();
            productIdKeys.add(productIdKey);
        }
        List<GenericValue> productNodesList = EntityQuery.use(delegator).from("ProductAssoc")
                .where("productIdTo", productId,
                        "productAssocTypeId", bomType)
                .cache().filterByDate(inDate).queryList();
        GenericValue duplicatedNode = null;
        for (GenericValue oneNode : productNodesList) {
            for (String idKey : productIdKeys) {
                if (oneNode.getString("productId").equals(idKey)) {
                    return oneNode;
                }
            }
            duplicatedNode = searchDuplicatedAncestor(oneNode.getString("productId"), productIdKey, productIdKeys, bomType, inDate, delegator,
                    dispatcher, userLogin);
            if (duplicatedNode != null) {
                break;
            }
        }
        return duplicatedNode;
    }

    public static String createProductionRunsForShipment(javax.servlet.http.HttpServletRequest request,
                                                         javax.servlet.http.HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        String shipmentId = request.getParameter("shipmentId");

        try {
            List<GenericValue> shipmentPlans = EntityQuery.use(delegator).from("OrderShipment")
                    .where("shipmentId", shipmentId).queryList();
            for (GenericValue shipmentPlan : shipmentPlans) {
                GenericValue orderItem = shipmentPlan.getRelatedOne("OrderItem", false);

                List<GenericValue> productionRuns = EntityQuery.use(delegator).from("WorkOrderItemFulfillment")
                        .where("orderId", shipmentPlan.get("orderId"),
                                "orderItemSeqId", shipmentPlan.get("orderItemSeqId"),
                                "shipGroupSeqId", shipmentPlan.get("shipGroupSeqId"))
                        .cache().queryList();
                if (UtilValidate.isNotEmpty(productionRuns)) {
                    Debug.logError("Production Run for order item (" + orderItem.getString("orderId") + "/"
                            + orderItem.getString("orderItemSeqId") + ") not created.", MODULE);
                    continue;
                }
                Map<String, Object> result = dispatcher.runSync("createProductionRunsForOrder", UtilMisc.<String, Object>toMap("quantity",
                        shipmentPlan.getBigDecimal("quantity"), "orderId",
                        shipmentPlan.getString("orderId"), "orderItemSeqId", shipmentPlan.getString("orderItemSeqId"), "shipGroupSeqId",
                        shipmentPlan.getString("shipGroupSeqId"), "shipmentId",
                        shipmentId, "userLogin", userLogin));
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            Debug.logWarning(ge, MODULE);
        }

        return "success";
    }

}
