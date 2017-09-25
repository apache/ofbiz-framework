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

package org.apache.ofbiz.manufacturing.mrp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

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
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.manufacturing.bom.BOMNode;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * Services for running MRP
 *
 */
public class MrpServices {

    public static final String module = MrpServices.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    public static Map<String, Object> initMrpEvents(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String facilityId = (String)context.get("facilityId");
        Integer defaultYearsOffset = (Integer)context.get("defaultYearsOffset");
        String mrpId = (String)context.get("mrpId");

        //Erases the old table for the moment and initializes it with the new orders,
        //Does not modify the old one now.

        List<GenericValue> listResult = null;
        try {
            listResult = EntityQuery.use(delegator).from("MrpEvent").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : findList(\"MrpEvent\", null, null, null, null, false)", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        if (listResult != null) {
            try {
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                Debug.logError(e,"Error : removeAll(listResult), listResult ="+listResult, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventRemoveError", locale));
            }
        }

        // Proposed requirements are deleted
        listResult = null;
        List<GenericValue> listResultRoles = new LinkedList<GenericValue>();
        try {
            listResult = EntityQuery.use(delegator).from("Requirement")
                    .where("requirementTypeId", "PRODUCT_REQUIREMENT","facilityId", facilityId,
                            "statusId", "REQ_PROPOSED")
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        if (listResult != null) {
            try {
                for (GenericValue tmpRequirement : listResult) {
                    listResultRoles.addAll(tmpRequirement.getRelated("RequirementRole", null, null, false));
                }
                delegator.removeAll(listResultRoles);
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventRemoveError", locale));
            }
        }
        listResult = null;
        try {
            listResult = EntityQuery.use(delegator).from("Requirement")
                    .where("requirementTypeId", "INTERNAL_REQUIREMENT","facilityId", facilityId,
                            "statusId", "REQ_PROPOSED")
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        if (listResult != null) {
            try {
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventRemoveError", locale));
            }
        }

        Map<String, Object> parameters = null;
        List<GenericValue> resultList = null;
        // ----------------------------------------
        // Loads all the approved sales order items and purchase order items
        // ----------------------------------------
        // This is the default required date for orders without dates specified:
        // by convention it is a date far in the future of 100 years.
        Timestamp notAssignedDate = null;
        if (UtilValidate.isEmpty(defaultYearsOffset)) {
            notAssignedDate = now;
        } else {
            Calendar calendar = UtilDateTime.toCalendar(now);
            calendar.add(Calendar.YEAR, defaultYearsOffset.intValue());
            notAssignedDate = new Timestamp(calendar.getTimeInMillis());
        }
        resultList = null;
        try {
            resultList = EntityQuery.use(delegator).from("OrderHeaderItemAndShipGroup")
                    .where("orderTypeId", "SALES_ORDER",
                            "oiStatusId", "ITEM_APPROVED",
                            "facilityId", facilityId)
                    .orderBy("orderId").queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId =  genericResult.getString("productId");
            BigDecimal reservedQuantity = genericResult.getBigDecimal("reservedQuantity");
            BigDecimal shipGroupQuantity = genericResult.getBigDecimal("quantity");
            BigDecimal cancelledQuantity = genericResult.getBigDecimal("cancelQuantity");
            BigDecimal eventQuantityTmp = BigDecimal.ZERO;

            if (UtilValidate.isNotEmpty(reservedQuantity)) {
                eventQuantityTmp = reservedQuantity.negate();
            } else {
                if (UtilValidate.isNotEmpty(cancelledQuantity)) {
                    shipGroupQuantity = shipGroupQuantity.subtract(cancelledQuantity);
                }
                eventQuantityTmp = shipGroupQuantity.negate();
            }

            if (eventQuantityTmp.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // This is the order in which order dates are considered:
            //   OrderItemShipGroup.shipByDate
            //   OrderItemShipGroup.shipAfterDate
            //   OrderItem.shipBeforeDate
            //   OrderItem.shipAfterDate
            //   OrderItem.estimatedDeliveryDate
            Timestamp requiredByDate = genericResult.getTimestamp("shipByDate");
            if (UtilValidate.isEmpty(requiredByDate)) {
                requiredByDate = genericResult.getTimestamp("shipAfterDate");
                if (UtilValidate.isEmpty(requiredByDate)) {
                    requiredByDate = genericResult.getTimestamp("oiShipBeforeDate");
                    if (UtilValidate.isEmpty(requiredByDate)) {
                        requiredByDate = genericResult.getTimestamp("oiShipAfterDate");
                        if (UtilValidate.isEmpty(requiredByDate)) {
                            requiredByDate = genericResult.getTimestamp("oiEstimatedDeliveryDate");
                            if (requiredByDate == null) {
                                requiredByDate = notAssignedDate;
                            }
                        }
                    }
                }
            }
            parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", requiredByDate, "mrpEventTypeId", "SALES_ORDER_SHIP");
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "SALES_ORDER_SHIP"), locale));
            }
        }
        // ----------------------------------------
        // Loads all the approved product requirements (po requirements)
        // ----------------------------------------
        resultList = null;
        try {
            resultList = EntityQuery.use(delegator).from("Requirement")
                    .where("requirementTypeId", "PRODUCT_REQUIREMENT",
                            "statusId", "REQ_APPROVED",
                            "facilityId", facilityId)
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId =  genericResult.getString("productId");
            BigDecimal eventQuantityTmp = genericResult.getBigDecimal("quantity");
            if (productId == null || eventQuantityTmp == null) {
                continue;
            }
            Timestamp estimatedShipDate = genericResult.getTimestamp("requiredByDate");
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }

            parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", estimatedShipDate, "mrpEventTypeId", "PROD_REQ_RECP");
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString("requirementId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "PROD_REQ_RECP"), locale));
            }
        }

        // ----------------------------------------
        // Loads all the approved purchase order items
        // ----------------------------------------
        resultList = null;
        String orderId = null;
        GenericValue orderDeliverySchedule = null;
        try {
            List<GenericValue> facilityContactMechs = EntityQuery.use(delegator).from("FacilityContactMech")
                    .where("facilityId", facilityId)
                    .filterByDate().queryList();
            List<String> facilityContactMechIds = EntityUtil.getFieldListFromEntityList(facilityContactMechs, "contactMechId", true);

            resultList = EntityQuery.use(delegator)
                    .select("orderId", "orderItemSeqId", "productId", "quantity", "cancelQuantity", "oiEstimatedDeliveryDate")
                    .from("OrderHeaderItemAndShipGroup")
                    .where(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"),
                            EntityCondition.makeCondition("oiStatusId", EntityOperator.EQUALS, "ITEM_APPROVED"),
                            EntityCondition.makeCondition("contactMechId", EntityOperator.IN, facilityContactMechIds))
                    .orderBy("orderDate")
                    .queryList();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        for (GenericValue genericResult : resultList) {
            try {
                String newOrderId =  genericResult.getString("orderId");
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    orderDeliverySchedule = EntityQuery.use(delegator).from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId", "_NA_").queryOne();
                }
                String productId =  genericResult.getString("productId");
    
                BigDecimal shipGroupQuantity = genericResult.getBigDecimal("quantity");
                BigDecimal cancelledQuantity = genericResult.getBigDecimal("cancelQuantity");
                if (UtilValidate.isEmpty(shipGroupQuantity)) {
                    shipGroupQuantity = BigDecimal.ZERO;
                }
                if (UtilValidate.isNotEmpty(cancelledQuantity)) {
                    shipGroupQuantity = shipGroupQuantity.subtract(cancelledQuantity);
                }

                try {
                    List<GenericValue> shipmentReceipts = EntityQuery.use(delegator).select("quantityAccepted", "quantityRejected").from("ShipmentReceipt").where("orderId", genericResult.getString("orderId"), "orderItemSeqId", genericResult.getString("orderItemSeqId")).queryList();
                    for (GenericValue shipmentReceipt : shipmentReceipts) {
                        shipGroupQuantity = shipGroupQuantity.subtract(shipmentReceipt.getBigDecimal("quantityAccepted"));
                        shipGroupQuantity = shipGroupQuantity.subtract(shipmentReceipt.getBigDecimal("quantityRejected"));
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
    
                GenericValue orderItemDeliverySchedule = null;
                orderItemDeliverySchedule = EntityQuery.use(delegator).from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId", genericResult.getString("orderItemSeqId")).queryOne();
                Timestamp estimatedShipDate = null;
                if (orderItemDeliverySchedule != null && orderItemDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderItemDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else if (orderDeliverySchedule != null && orderDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else {
                    estimatedShipDate = genericResult.getTimestamp("oiEstimatedDeliveryDate");
                }
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }
    
                parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", estimatedShipDate, "mrpEventTypeId", "PUR_ORDER_RECP");
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, shipGroupQuantity, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "PUR_ORDER_RECP"), locale));
            }
        }

        // ----------------------------------------
        // PRODUCTION Run: components
        // ----------------------------------------
        resultList = null;
        try {
            resultList = EntityQuery.use(delegator).from("WorkEffortAndGoods")
                    .where("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED",
                            "statusId", "WEGS_CREATED",
                            "facilityId", facilityId)
                    .queryList();
            for (GenericValue genericResult : resultList) {
                if ("PRUN_CLOSED".equals(genericResult.getString("currentStatusId")) ||
                    "PRUN_COMPLETED".equals(genericResult.getString("currentStatusId")) ||
                    "PRUN_CANCELLED".equals(genericResult.getString("currentStatusId"))) {
                    continue;
                }
                String productId =  genericResult.getString("productId");
                // get the inventory already consumed
                BigDecimal consumedInventoryTotal = BigDecimal.ZERO;
                List<GenericValue> consumedInventoryItems = EntityQuery.use(delegator).from("WorkEffortAndInventoryAssign")
                        .where("workEffortId", genericResult.get("workEffortId"), "productId", productId)
                        .queryList();
                for (GenericValue consumedInventoryItem : consumedInventoryItems) {
                    consumedInventoryTotal = consumedInventoryTotal.add(consumedInventoryItem.getBigDecimal("quantity"));
                }
                BigDecimal eventQuantityTmp = consumedInventoryTotal.subtract(genericResult.getBigDecimal("estimatedQuantity"));
                Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedStartDate");
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", estimatedShipDate, "mrpEventTypeId", "MANUF_ORDER_REQ");
                String eventName = (UtilValidate.isEmpty(genericResult.getString("workEffortParentId"))? genericResult.getString("workEffortId"): genericResult.getString("workEffortParentId") + "-" + genericResult.getString("workEffortId"));
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, eventName, false, delegator);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "MANUF_ORDER_REQ"), locale) + " " + e.getMessage());
        }

        // ----------------------------------------
        // PRODUCTION Run: product produced
        // ----------------------------------------
        resultList = null;
        try {
            resultList = EntityQuery.use(delegator).from("WorkEffortAndGoods")
                    .where("workEffortGoodStdTypeId", "PRUN_PROD_DELIV",
                            "statusId", "WEGS_CREATED",
                            "workEffortTypeId", "PROD_ORDER_HEADER",
                            "facilityId", facilityId)
                    .queryList();
            for (GenericValue genericResult : resultList) {
                if ("PRUN_CLOSED".equals(genericResult.getString("currentStatusId")) ||
                    "PRUN_COMPLETED".equals(genericResult.getString("currentStatusId")) ||
                    "PRUN_CANCELLED".equals(genericResult.getString("currentStatusId"))) {
                    continue;
                }
                BigDecimal qtyToProduce = genericResult.getBigDecimal("quantityToProduce");
                if (qtyToProduce == null) {
                    qtyToProduce = BigDecimal.ZERO;
                }
                BigDecimal qtyProduced = genericResult.getBigDecimal("quantityProduced");
                if (qtyProduced == null) {
                    qtyProduced = BigDecimal.ZERO;
                }
                if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                    continue;
                }
                BigDecimal qtyDiff = qtyToProduce.subtract(qtyProduced);
                String productId =  genericResult.getString("productId");
                BigDecimal eventQuantityTmp = qtyDiff;
                Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedCompletionDate");
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }

                parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", estimatedShipDate, "mrpEventTypeId", "MANUF_ORDER_RECP");
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString("workEffortId"), false, delegator);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "MANUF_ORDER_RECP"), locale) + " " + e.getMessage());
        }

        // ----------------------------------------
        // Products without upcoming events but that are already under minimum quantity in warehouse
        // ----------------------------------------
        resultList = null;
        parameters = UtilMisc.<String, Object>toMap("facilityId", facilityId);
        try {
            resultList = EntityQuery.use(delegator).from("ProductFacility")
                    .where("facilityId", facilityId)
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to retrieve ProductFacility records.", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindProductFacility", locale));
        }
        for (GenericValue genericResult : resultList) {
            String productId = genericResult.getString("productId");
            BigDecimal minimumStock = genericResult.getBigDecimal("minimumStock");
            if (minimumStock == null) {
                minimumStock = BigDecimal.ZERO;
            }
            try {
                long numOfEvents = EntityQuery.use(delegator).from("MrpEvent")
                        .where("mrpId", mrpId, "productId", productId)
                        .queryCount();
                if (numOfEvents > 0) {
                    continue;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to count MrpEvent records.", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotCountRecords", locale));
            }
            BigDecimal qoh = findProductMrpQoh(mrpId, productId, facilityId, dispatcher, delegator);
            if (qoh.compareTo(minimumStock) >= 0) {
                continue;
            }
            parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", now, "mrpEventTypeId", "REQUIRED_MRP");
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, BigDecimal.ZERO, null, null, false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "REQUIRED_MRP"), locale));
            }
        }

        // ----------------------------------------
        // SALES FORECASTS
        // ----------------------------------------
        resultList = null;
        GenericValue facility = null;
        try {
            facility = EntityQuery.use(delegator).from("Facility").where("facilityId", facilityId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventFindError", locale));
        }
        String partyId =  (String)facility.get("ownerPartyId");
        try {
            resultList = EntityQuery.use(delegator).from("SalesForecast")
                    .where("organizationPartyId", partyId)
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindSalesForecasts", locale));
        }
        for (GenericValue genericResult : resultList) {
            String customTimePeriodId =  genericResult.getString("customTimePeriodId");
            GenericValue customTimePeriod = null;
            try {
                customTimePeriod = EntityQuery.use(delegator).from("CustomTimePeriod").where("customTimePeriodId", customTimePeriodId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindCustomTimePeriod", locale));
            }
            if (customTimePeriod != null) {
                if (UtilValidate.isNotEmpty(customTimePeriod.getTimestamp("thruDate")) && customTimePeriod.getTimestamp("thruDate").before(UtilDateTime.nowTimestamp())) {
                    continue;
                } else {
                    List<GenericValue> salesForecastDetails = null;
                    try {
                        salesForecastDetails = EntityQuery.use(delegator).from("SalesForecastDetail")
                                .where("salesForecastId", genericResult.get("salesForecastId"))
                                .queryList();
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindSalesForecastDetails", locale));
                    }
                    for (GenericValue sfd : salesForecastDetails) {
                        String productId =  sfd.getString("productId");
                        BigDecimal eventQuantityTmp = sfd.getBigDecimal("quantity");
                        if (productId == null || eventQuantityTmp == null) {
                            continue;
                        }
                        eventQuantityTmp = eventQuantityTmp.negate();
                        parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", customTimePeriod.getDate("fromDate"), "mrpEventTypeId", "SALES_FORECAST");
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, sfd.getString("salesForecastDetailId"), false, delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpEventProblemInitializing", UtilMisc.toMap("mrpEventTypeId", "SALES_FORECAST"), locale));
                        }
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from initMrpEvent", module);
        return result;
    }

    /**
     * Find the quantity on hand of products for MRP.
     * <ul>
     * <li>PreConditions : none</li>
     * <li>Result : We get the quantity of product available in the stocks.</li>
     * </ul>
     *
     * @param product the product for which the Quantity Available is required
     * @return the sum of all the totalAvailableToPromise of the inventoryItem related to the product, if the related facility is Mrp available (not yet implemented!!)
     */
    public static BigDecimal findProductMrpQoh(String mrpId, GenericValue product, String facilityId, LocalDispatcher dispatcher, Delegator delegator) {
        return findProductMrpQoh(mrpId, product.getString("productId"), facilityId, dispatcher, delegator);
    }
    public static BigDecimal findProductMrpQoh(String mrpId, String productId, String facilityId, LocalDispatcher dispatcher, Delegator delegator) {
        Map<String, Object> resultMap = null;
        try {
            if (facilityId == null) {
                resultMap = dispatcher.runSync("getProductInventoryAvailable", UtilMisc.toMap("productId", productId));
            } else {
                resultMap = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling getProductInventoryAvailableByFacility service", module);
            logMrpError(mrpId, productId, "Unable to count inventory", delegator);
            return BigDecimal.ZERO;
        }
        return ((BigDecimal)resultMap.get("quantityOnHandTotal"));
    }

    public static void logMrpError(String mrpId, String productId, String errorMessage, Delegator delegator) {
        logMrpError(mrpId, productId, UtilDateTime.nowTimestamp(), errorMessage, delegator);
    }
    public static void logMrpError(String mrpId, String productId, Timestamp eventDate, String errorMessage, Delegator delegator) {
        try {
            if (UtilValidate.isNotEmpty(productId) && UtilValidate.isNotEmpty(errorMessage)) {
                GenericValue inventoryEventError = delegator.makeValue("MrpEvent", UtilMisc.toMap("productId", productId,
                                                                                                  "mrpId", mrpId,
                                                                                                  "eventDate", eventDate,
                                                                                                  "mrpEventTypeId", "ERROR",
                                                                                                  "eventName", errorMessage));
                delegator.createOrStore(inventoryEventError);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error calling logMrpError for productId [" + productId + "] and errorMessage [" + errorMessage + "]", module);
        }
    }

    /**
     * Process the bill of material (bom) of the product  to insert components in the MrpEvent table.
     * Before inserting in the entity, test if there is the record already existing to add quantity rather to create a new one.
     * @param mrpId the mrp id
     * @param product GenericValue oject of the product
     * @param eventQuantity the product quantity needed
     * @param startDate the startDate of the productionRun which will used to produce the product
     * @param routingTaskStartDate Map with all the routingTask as keys and startDate of each of them
     * @param listComponent a List with all the components
     */

    public static void processBomComponent(String mrpId, GenericValue product, BigDecimal eventQuantity, Timestamp startDate, Map<String, Object> routingTaskStartDate, List<BOMNode> listComponent) {
        // TODO : change the return type to boolean to be able to test if all is ok or if it have had a exception
        Delegator delegator = product.getDelegator();

        if (UtilValidate.isNotEmpty(listComponent)) {
            for (BOMNode node : listComponent) {
                GenericValue productComponent = node.getProductAssoc();
                // read the startDate for the component
                String routingTask = node.getProductAssoc().getString("routingWorkEffortId");
                Timestamp eventDate = (routingTask == null || !routingTaskStartDate.containsKey(routingTask)) ? startDate : (Timestamp) routingTaskStartDate.get(routingTask);
                // if the components is valid at the event Date create the Mrp requirement in the M entity
                if (EntityUtil.isValueActive(productComponent, eventDate)) {
                    Map<String, Object> parameters = UtilMisc.<String, Object>toMap("productId", node.getProduct().getString("productId"));
                    parameters.put("mrpId", mrpId);
                    parameters.put("eventDate", eventDate);
                    parameters.put("mrpEventTypeId", "MRP_REQUIREMENT");
                    BigDecimal componentEventQuantity = node.getQuantity();
                    try {
                        InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, componentEventQuantity.negate(), null, product.get("productId") + ": " + eventDate, false, delegator);
                    } catch (GenericEntityException e) {
                        Debug.logError("Error : findOne(\"MrpEvent\", parameters) ="+parameters+"--"+e.getMessage(), module);
                        logMrpError(mrpId, node.getProduct().getString("productId"), "Unable to create event (processBomComponent)", delegator);
                    }
                }
            }
        }
    }

    /**
     * Launch the MRP.
     * <ul>
     * <li>PreConditions : none</li>
     * <li>Result : The date when we must order or begin to build the products and subproducts we need are calculated</li>
     * <li>INPUT : parameters to get from the context: <ul><li>String mrpName</li></ul></li>
     * <li>OUTPUT : Result to put in the map : <ul><li>none</li></ul></li>
     * </ul>
     *
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId routingId, quantity, startDate.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> executeMrp(DispatchContext ctx, Map<String, ? extends Object> context) {
        Debug.logInfo("executeMrp called", module);
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String mrpName = (String)context.get("mrpName");
        Integer defaultYearsOffset = (Integer)context.get("defaultYearsOffset");
        String facilityGroupId = (String)context.get("facilityGroupId");
        String facilityId = (String)context.get("facilityId");
        String manufacturingFacilityId = null;
        if (UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(facilityGroupId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpFacilityNotAvailable", locale));
        }
        if (UtilValidate.isEmpty(facilityId)) {
            try {
                GenericValue facilityGroup = EntityQuery.use(delegator).from("FacilityGroup").where("facilityGroupId", facilityGroupId).queryOne();
                if (UtilValidate.isEmpty(facilityGroup)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpFacilityGroupIsNotValid", UtilMisc.toMap("facilityGroupId", facilityGroupId), locale));
                }
                List<GenericValue> facilities = facilityGroup.getRelated("FacilityGroupMember", null, UtilMisc.toList("sequenceNum"), false);
                if (UtilValidate.isEmpty(facilities)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpFacilityGroupIsNotAssociatedToFacility", UtilMisc.toMap("facilityGroupId", facilityGroupId), locale));
                }
                for (GenericValue facilityMember : facilities) {
                    GenericValue facility = facilityMember.getRelatedOne("Facility", false);
                    if ("WAREHOUSE".equals(facility.getString("facilityTypeId")) && UtilValidate.isEmpty(facilityId)) {
                        facilityId = facility.getString("facilityId");
                    }
                    if ("PLANT".equals(facility.getString("facilityTypeId")) && UtilValidate.isEmpty(manufacturingFacilityId)) {
                        manufacturingFacilityId = facility.getString("facilityId");
                    }
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpFacilityGroupCannotBeLoad", UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        } else {
            manufacturingFacilityId = facilityId;
        }

        if (UtilValidate.isEmpty(facilityId) || UtilValidate.isEmpty(manufacturingFacilityId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpFacilityOrManufacturingFacilityNotAvailable", locale));
        }

        int bomLevelWithNoEvent = 0;
        BigDecimal stockTmp = BigDecimal.ZERO;
        String oldProductId = null;
        String productId = null;
        GenericValue product = null;
        GenericValue productFacility = null;
        BigDecimal eventQuantity = BigDecimal.ZERO;
        Timestamp eventDate = null;
        BigDecimal reorderQuantity = BigDecimal.ZERO;
        BigDecimal minimumStock = BigDecimal.ZERO;
        int daysToShip = 0;
        List<BOMNode> components = null;
        boolean isBuilt = false;
        GenericValue routing = null;

        String mrpId = delegator.getNextSeqId("MrpEvent");

        Map<String, Object> result = null;
        Map<String, Object> parameters = null;
        List<GenericValue> listInventoryEventForMRP = null;
        ListIterator<GenericValue> iteratorListInventoryEventForMRP = null;

        // Initialization of the MrpEvent table, This table will contain the products we want to buy or build.
        parameters = UtilMisc.<String, Object>toMap("mrpId", mrpId, "reInitialize", Boolean.TRUE, "defaultYearsOffset", defaultYearsOffset, "userLogin", userLogin);
        parameters.put("facilityId", facilityId);
        parameters.put("manufacturingFacilityId", manufacturingFacilityId);
        try {
            result = dispatcher.runSync("initMrpEvents", parameters);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorRunningInitMrpEvents", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        long bomLevel = 0;
        do {
            // Find all products in MrpEventView, ordered by bom and eventDate
            EntityCondition filterByConditions = null;
            if (bomLevel == 0) {
                filterByConditions = EntityCondition.makeCondition(EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, null),
                                            EntityOperator.OR,
                                            EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, Long.valueOf(bomLevel)));
            } else {
                filterByConditions = EntityCondition.makeCondition("billOfMaterialLevel", EntityOperator.EQUALS, Long.valueOf(bomLevel));
            }
            try {
                listInventoryEventForMRP = EntityQuery.use(delegator).from("MrpEventView")
                        .where(filterByConditions)
                        .orderBy("productId", "eventDate")
                        .queryList();
            } catch (GenericEntityException e) {
                Long bomLevelToString = new Long(bomLevel);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorForBomLevel", UtilMisc.toMap("bomLevel", bomLevelToString.toString(), "errorString", e.getMessage()), locale));
            }

            if (UtilValidate.isNotEmpty(listInventoryEventForMRP)) {
                bomLevelWithNoEvent = 0;

                oldProductId = "";
                int eventCount = 0;
                for (GenericValue inventoryEventForMRP : listInventoryEventForMRP) {
                    eventCount++;

                    productId = inventoryEventForMRP.getString("productId");
                    boolean isLastEvent = (eventCount == listInventoryEventForMRP.size() || !productId.equals(listInventoryEventForMRP.get(eventCount).getString("productId")));
                    eventQuantity = inventoryEventForMRP.getBigDecimal("quantity");

                    if (!productId.equals(oldProductId)) {
                        BigDecimal positiveEventQuantity = eventQuantity.compareTo(BigDecimal.ZERO) > 0 ? eventQuantity: eventQuantity.negate();
                        // It's a new product, so it's necessary to  read the MrpQoh
                        try {
                            product = inventoryEventForMRP.getRelatedOne("Product", true);
                            productFacility = EntityUtil.getFirst(product.getRelated("ProductFacility", UtilMisc.toMap("facilityId", facilityId), null, true));
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindProductForEvent", locale));
                        }
                        stockTmp = findProductMrpQoh(mrpId, product, facilityId, dispatcher, delegator);
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(UtilMisc.<String, Object>toMap("mrpId", mrpId,
                                    "productId", product.getString("productId"), 
                                    "mrpEventTypeId", "INITIAL_QOH", "eventDate", now), 
                                    stockTmp, facilityId, null, false, delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCreateOrUpdateEvent", UtilMisc.toMap("parameters", parameters), locale));
                        }
                        // days to ship is only relevant for sales order to plan for preparatory days to ship.  Otherwise MRP will push event dates for manufacturing parts
                        // as well and cause problems
                        daysToShip = 0;
                        if (productFacility != null) {
                            reorderQuantity = (productFacility.getBigDecimal("reorderQuantity") != null ? productFacility.getBigDecimal("reorderQuantity"): BigDecimal.ONE.negate());
                            minimumStock = (productFacility.getBigDecimal("minimumStock") != null ? productFacility.getBigDecimal("minimumStock"): BigDecimal.ZERO);
                            if ("SALES_ORDER_SHIP".equals(inventoryEventForMRP.getString("mrpEventTypeId"))) {
                                daysToShip = (productFacility.getLong("daysToShip") != null? productFacility.getLong("daysToShip").intValue(): 0);
                            }
                        } else {
                            minimumStock = BigDecimal.ZERO;
                            reorderQuantity = BigDecimal.ONE.negate();
                        }
                        // -----------------------------------------------------
                        // The components are also loaded thru the configurator
                        Map<String, Object> serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", positiveEventQuantity, "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorExplodingProduct", UtilMisc.toMap("productId", product.getString("productId")), locale));
                        } catch (Exception e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorExplodingProduct", UtilMisc.toMap("productId", product.getString("productId")), locale));
                        }
                        components = UtilGenerics.checkList(serviceResponse.get("components"));
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = (components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        oldProductId = productId;
                    }

                    stockTmp = stockTmp.add(eventQuantity);
                    if (stockTmp.compareTo(minimumStock) < 0 && (eventQuantity.compareTo(BigDecimal.ZERO) < 0 || isLastEvent)) { // No need to create a supply event/requirement if the current event is not a demand and there are other events to process
                        BigDecimal qtyToStock = minimumStock.subtract(stockTmp);
                        //need to buy or build the product as we have not enough stock
                        eventDate = inventoryEventForMRP.getTimestamp("eventDate");
                        // to be just before the requirement
                        eventDate.setTime(eventDate.getTime()-1);
                        ProposedOrder proposedOrder = new ProposedOrder(product, facilityId, manufacturingFacilityId, isBuilt, eventDate, qtyToStock);
                        proposedOrder.setMrpName(mrpName);
                        // calculate the ProposedOrder quantity and update the quantity object property.
                        proposedOrder.calculateQuantityToSupply(reorderQuantity, minimumStock, iteratorListInventoryEventForMRP);

                        // -----------------------------------------------------
                        // The components are also loaded thru the configurator
                        Map<String, Object> serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", proposedOrder.getQuantity(), "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorExplodingProduct", UtilMisc.toMap("productId", product.getString("productId")), locale));
                        } catch (Exception e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpErrorExplodingProduct", UtilMisc.toMap("productId", product.getString("productId")), locale));
                        }
                        components = UtilGenerics.checkList(serviceResponse.get("components"));
                        String routingId = (String)serviceResponse.get("workEffortId");
                        if (routingId != null) {
                            try {
                                routing = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", routingId).queryOne();
                            } catch (GenericEntityException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCannotFindProductForEvent", locale));
                            }
                        } else {
                            routing = null;
                        }
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = (components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        // calculate the ProposedOrder requirementStartDate and update the requirementStartDate object property.
                        Map<String, Object> routingTaskStartDate = proposedOrder.calculateStartDate(daysToShip, routing, delegator, dispatcher, userLogin);
                        if (isBuilt) {
                            // process the product components
                            processBomComponent(mrpId, product, proposedOrder.getQuantity(), proposedOrder.getRequirementStartDate(), routingTaskStartDate, components);
                        }
                        // create the  ProposedOrder (only if the product is warehouse managed), and the MrpEvent associated
                        String requirementId = null;
                        if (productFacility != null) {
                            requirementId = proposedOrder.create(ctx, userLogin);
                        }
                        if (UtilValidate.isEmpty(productFacility) && !isBuilt) {
                            logMrpError(mrpId, productId, now, "No ProductFacility record for [" + facilityId + "]; no requirement created.", delegator);
                        }
                        String eventName = null;
                        if (UtilValidate.isNotEmpty(requirementId)) {
                            eventName = "*" + requirementId + " (" + proposedOrder.getRequirementStartDate() + ")*";
                        }
                        Map<String, Object> eventMap = UtilMisc.<String, Object>toMap("productId", product.getString("productId"),
                                                      "mrpId", mrpId,
                                                      "eventDate", eventDate,
                                                      "mrpEventTypeId", (isBuilt? "PROP_MANUF_O_RECP" : "PROP_PUR_O_RECP"));
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(eventMap, proposedOrder.getQuantity(), null, eventName, (proposedOrder.getRequirementStartDate().compareTo(now) < 0), delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingMrpCreateOrUpdateEvent", UtilMisc.toMap("parameters", parameters), locale));
                        }
                        //
                        stockTmp = stockTmp.add(proposedOrder.getQuantity());
                    }
                }
            } else {
                bomLevelWithNoEvent += 1;
            }

            bomLevel += 1;
            // if there are 3 levels with no inventoryEvenPanned we stop
        } while (bomLevelWithNoEvent < 3);

        result = new HashMap<String, Object>();
        List<Object> msgResult = new LinkedList<Object>();
        result.put("msgResult", msgResult);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from executeMrp", module);
        return result;
    }
}
