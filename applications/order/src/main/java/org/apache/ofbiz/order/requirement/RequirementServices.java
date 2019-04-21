/*
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
 */
package org.apache.ofbiz.order.requirement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Requirement Services
 */

public class RequirementServices {

    public static final String module = RequirementServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map<String, Object> getRequirementsForSupplier(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        EntityCondition requirementConditions = (EntityCondition) context.get("requirementConditions");
        String partyId = (String) context.get("partyId");
        String unassignedRequirements = (String) context.get("unassignedRequirements");
        List<String> statusIds = UtilGenerics.checkList(context.get("statusIds"));
        //TODO currencyUomId still not used
        try {
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                    EntityUtil.getFilterByDateExpr()
                   );
            if (UtilValidate.isNotEmpty(statusIds)) {
                conditions.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, statusIds));
            } else {
                conditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "REQ_APPROVED"));
            }
            if (requirementConditions != null) conditions.add(requirementConditions);

            // we're either getting the requirements for a given supplier, unassigned requirements, or requirements for all suppliers
            if (UtilValidate.isNotEmpty(partyId)) {
                conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
                conditions.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"));
            } else if (UtilValidate.isNotEmpty(unassignedRequirements)) {
                conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, null));
            } else {
                conditions.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SUPPLIER"));
            }

            List<GenericValue> requirementAndRoles = EntityQuery.use(delegator).from("RequirementAndRole")
                    .where(conditions)
                    .orderBy("partyId", "requirementId")
                    .queryList();

            // maps to cache the associated suppliers and products data, so we don't do redundant DB and service requests
            Map<String, GenericValue> suppliers = new HashMap<>();
            Map<String, GenericValue> gids = new HashMap<>();
            Map<String, Map<String, Object>> inventories = new HashMap<>();
            Map<String, BigDecimal> productsSold = new HashMap<>();

            // to count quantity, running total, and distinct products in list
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal amountTotal = BigDecimal.ZERO;
            Set<String> products = new HashSet<>();

            // time period to count products ordered from, six months ago and the 1st of that month
            Timestamp timePeriodStart = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, -6);

            // join in fields with extra data about the suppliers and products
            List<Map<String, Object>> requirements = new LinkedList<>();
            for (GenericValue requirement : requirementAndRoles) {
                Map<String, Object> union = new HashMap<>();
                String productId = requirement.getString("productId");
                partyId = requirement.getString("partyId");
                String facilityId = requirement.getString("facilityId");
                BigDecimal requiredQuantity = requirement.getBigDecimal("quantity");

                // get an available supplier product, preferably the one with the smallest minimum quantity to order, followed by price
                String supplierKey =  partyId + "^" + productId;
                GenericValue supplierProduct = suppliers.get(supplierKey);
                if (supplierProduct == null) {
                    // TODO: it is possible to restrict to quantity > minimumOrderQuantity, but then the entire requirement must be skipped
                    supplierProduct = EntityQuery.use(delegator).from("SupplierProduct")
                            .where("partyId", partyId, "productId", productId)
                            .orderBy("minimumOrderQuantity", "lastPrice")
                            .filterByDate("availableFromDate", "availableThruDate")
                            .queryFirst();
                    suppliers.put(supplierKey, supplierProduct);
                }

                // add our supplier product and cost of this line to the data
                if (supplierProduct != null) {
                    union.putAll(supplierProduct.getAllFields());
                    BigDecimal lastPrice = supplierProduct.getBigDecimal("lastPrice");
                    amountTotal = amountTotal.add(lastPrice.multiply(requiredQuantity));
                }

                // for good identification, get the UPCA type (UPC code)
                GenericValue gid = gids.get(productId);
                if (gid == null) {
                    gid = EntityQuery.use(delegator).from("GoodIdentification").where("goodIdentificationTypeId", "UPCA", "productId", requirement.get("productId")).queryOne();
                    gids.put(productId, gid);
                }
                if (gid != null) union.put("idValue", gid.get("idValue"));

                // the ATP and QOH quantities
                if (UtilValidate.isNotEmpty(facilityId)) {
                    String inventoryKey = facilityId + "^" + productId;
                    Map<String, Object> inventory = inventories.get(inventoryKey);
                    if (inventory == null) {
                        inventory = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
                        if (ServiceUtil.isError(inventory)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(inventory));
                        }
                        inventories.put(inventoryKey, inventory);
                    }
                    if (inventory != null) {
                        union.put("qoh", inventory.get("quantityOnHandTotal"));
                        union.put("atp", inventory.get("availableToPromiseTotal"));
                    }
                }

                // how many of the products were sold (note this is for a fixed time period across all product stores)
                BigDecimal sold = productsSold.get(productId);
                if (sold == null) {
                    EntityCondition prodConditions = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                                EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                                EntityCondition.makeCondition("orderStatusId", EntityOperator.NOT_IN, UtilMisc.toList("ORDER_REJECTED", "ORDER_CANCELLED")),
                                EntityCondition.makeCondition("orderItemStatusId", EntityOperator.NOT_IN, UtilMisc.toList("ITEM_REJECTED", "ITEM_CANCELLED")),
                                EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, timePeriodStart)
                               ), EntityOperator.AND);
                    GenericValue count = EntityQuery.use(delegator).select("quantityOrdered").from("OrderItemQuantityReportGroupByProduct").where(prodConditions).queryFirst();
                    if (count != null) {
                        sold = count.getBigDecimal("quantityOrdered");
                        if (sold != null) productsSold.put(productId, sold);
                    }
                }
                if (sold != null) {
                    union.put("qtySold", sold);
                }

                // keep a running total of distinct products and quantity to order
                if (requirement.getBigDecimal("quantity") == null) requirement.put("quantity", BigDecimal.ONE); // default quantity = 1
                quantity = quantity.add(requiredQuantity);
                products.add(productId);

                // add all the requirement fields last, to overwrite any conflicting fields
                union.putAll(requirement.getAllFields());
                requirements.add(union);
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("requirementsForSupplier", requirements);
            results.put("distinctProductCount", products.size());
            results.put("quantityTotal", quantity);
            results.put("amountTotal", amountTotal);
            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderServiceExceptionSeeLogs", locale));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderEntityExceptionSeeLogs", locale));
        }
    }

    // note that this service is designed to work only when a sales order status changes from CREATED -> APPROVED because HOLD -> APPROVED is too complex
    public static Map<String, Object> createAutoRequirementsForOrder(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String orderId = (String) context.get("orderId");
        try {
            GenericValue order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            GenericValue productStore = order.getRelatedOne("ProductStore", true);
            if (productStore == null) {
                Debug.logInfo("ProductStore for order ID " + orderId + " not found, requirements not created", module);
                return ServiceUtil.returnSuccess();
            }
            List<GenericValue> orderItemAndShipGroups = EntityQuery.use(delegator).select("orderId", "shipGroupSeqId", "orderItemSeqId").from("OrderItemAndShipGroupAssoc").where("orderId", orderId).distinct().queryList();
            for (GenericValue orderItemAndShipGroup : orderItemAndShipGroups) {
                GenericValue item = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderItemAndShipGroup.getString("orderId"), "orderItemSeqId", orderItemAndShipGroup.getString("orderItemSeqId")).queryOne();
                GenericValue product = item.getRelatedOne("Product", false);
                if (product == null) continue;
                if ((!"PRODRQM_AUTO".equals(product.get("requirementMethodEnumId")) &&
                        !"PRODRQM_AUTO".equals(productStore.get("requirementMethodEnumId"))) ||
                        (product.get("requirementMethodEnumId") == null &&
                           !"PRODRQM_AUTO".equals(productStore.get("requirementMethodEnumId")))) continue;
                BigDecimal quantity = item.getBigDecimal("quantity");
                BigDecimal cancelQuantity = item.getBigDecimal("cancelQuantity");
                BigDecimal required = quantity.subtract(cancelQuantity == null ? BigDecimal.ZERO : cancelQuantity);
                if (required.compareTo(BigDecimal.ZERO) <= 0) continue;
                GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId", orderItemAndShipGroup.getString("shipGroupSeqId")).cache().queryOne();
                Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "facilityId", orderItemShipGroup.getString("facilityId"), "productId", product.get("productId"), "quantity", required, "requirementTypeId", "PRODUCT_REQUIREMENT");
                Map<String, Object> results = dispatcher.runSync("createRequirement", input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                String requirementId = (String) results.get("requirementId");

                input = UtilMisc.toMap("userLogin", userLogin, "orderId", order.get("orderId"), "orderItemSeqId", item.get("orderItemSeqId"), "requirementId", requirementId, "quantity", required);
                results = dispatcher.runSync("createOrderRequirementCommitment", input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    // note that this service is designed to work only when a sales order status changes from CREATED -> APPROVED because HOLD -> APPROVED is too complex
    public static Map<String, Object> createATPRequirementsForOrder(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        /*
         * The strategy in this service is to begin making requirements when the product falls below the
         * ProductFacility.minimumStock.  Because the minimumStock is an upper bound, the quantity to be required
         * is either that required to bring the ATP back up to the minimumStock level or the amount ordered,
         * whichever is less.
         *
         * If there is a way to support reorderQuantity without losing the order item -> requirement association data,
         * then this service should be updated.
         *
         * The result is that this service generates many small requirements when stock levels are low for a product,
         * which is perfectly fine since the system is capable of creating POs in bulk from aggregate requirements.
         * The only concern would be a UI to manage numerous requirements with ease, preferrably by aggregating
         * on productId.
         */
        String orderId = (String) context.get("orderId");
        try {
            GenericValue order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            GenericValue productStore = order.getRelatedOne("ProductStore", true);
            if (productStore == null) {
                Debug.logInfo("ProductStore for order ID " + orderId + " not found, ATP requirements not created", module);
                return ServiceUtil.returnSuccess();
            }
            String facilityId = productStore.getString("inventoryFacilityId");
            List<GenericValue> orderItems = order.getRelated("OrderItem", null, null, false);
            for (GenericValue item : orderItems) {
                GenericValue product = item.getRelatedOne("Product", false);
                if (product == null) continue;

                if (!("PRODRQM_ATP".equals(product.get("requirementMethodEnumId")) ||
                        ("PRODRQM_ATP".equals(productStore.get("requirementMethodEnumId")) && product.get("requirementMethodEnumId") == null))) continue;

                BigDecimal quantity = item.getBigDecimal("quantity");
                BigDecimal cancelQuantity = item.getBigDecimal("cancelQuantity");
                BigDecimal ordered = quantity.subtract(cancelQuantity == null ? BigDecimal.ZERO : cancelQuantity);
                if (ordered.compareTo(BigDecimal.ZERO) <= 0) continue;

                // get the minimum stock for this facility (if not configured assume a minimum of zero, ie create requirements when it goes into backorder)
                GenericValue productFacility = EntityQuery.use(delegator).from("ProductFacility").where("facilityId", facilityId, "productId", product.get("productId")).queryOne();
                BigDecimal minimumStock = BigDecimal.ZERO;
                if (productFacility != null && productFacility.get("minimumStock") != null) {
                    minimumStock = productFacility.getBigDecimal("minimumStock");
                }

                // get the facility ATP for product, which should be updated for this item's reservation
                Map<String, Object> results = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("userLogin", userLogin, "productId", product.get("productId"), "facilityId", facilityId));
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                BigDecimal atp = ((BigDecimal) results.get("availableToPromiseTotal")); // safe since this is a required OUT param

                // count all current requirements for this product
                BigDecimal pendingRequirements = BigDecimal.ZERO;
                EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.get("productId")),
                        EntityCondition.makeCondition("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "REQ_ORDERED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "REQ_REJECTED")),
                        EntityOperator.AND);
                List<GenericValue> requirements = EntityQuery.use(delegator).from("Requirement").where(ecl).queryList();
                for (GenericValue requirement : requirements) {
                    pendingRequirements = pendingRequirements.add(requirement.get("quantity") == null ? BigDecimal.ZERO : requirement.getBigDecimal("quantity"));
                }

                // the minimum stock is an upper bound, therefore we either require up to the minimum stock or the input required quantity, whichever is less
                BigDecimal shortfall = minimumStock.subtract(atp).subtract(pendingRequirements);
                BigDecimal required = ordered.compareTo(shortfall) < 0 ? ordered : shortfall;
                if (required.compareTo(BigDecimal.ZERO) <= 0) continue;

                Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "facilityId", facilityId, "productId", product.get("productId"), "quantity", required, "requirementTypeId", "PRODUCT_REQUIREMENT");
                results = dispatcher.runSync("createRequirement", input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
                String requirementId = (String) results.get("requirementId");

                input = UtilMisc.toMap("userLogin", userLogin, "orderId", order.get("orderId"), "orderItemSeqId", item.get("orderItemSeqId"), "requirementId", requirementId, "quantity", required);
                results = dispatcher.runSync("createOrderRequirementCommitment", input);
                if (ServiceUtil.isError(results)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateRequirementsToOrdered (DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String orderId = (String) context.get("orderId");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        try {
            for(GenericValue orderItem: orh.getOrderItems()){
                GenericValue orderRequirementCommitment = EntityQuery.use(delegator).from("OrderRequirementCommitment")
                        .where(UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId")))
                        .queryFirst();
                if (orderRequirementCommitment != null) {
                    String requirementId = orderRequirementCommitment.getString("requirementId");
                    /* Change status of requirement to ordered */
                    Map<String, Object> inputMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "requirementId", requirementId, "statusId", "REQ_ORDERED", "quantity", orderItem.getBigDecimal("quantity"));
                    // TODO: check service result for an error return
                    Map<String, Object> results = dispatcher.runSync("updateRequirement", inputMap);
                    if (ServiceUtil.isError(results)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(results));
                    }
                }
            }
        } catch(GenericEntityException e){
            Debug.logError(e, module);
        } catch(GenericServiceException e){
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }
}

