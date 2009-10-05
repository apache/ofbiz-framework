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

package org.ofbiz.manufacturing.mrp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.ibm.icu.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFieldMap;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.bom.BOMNode;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for running MRP
 *
 */
public class MrpServices {

    public static final String module = MrpServices.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    public static Map initMrpEvents(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();

        String facilityId = (String)context.get("facilityId");
        String manufacturingFacilityId = (String)context.get("manufacturingFacilityId");
        Integer defaultYearsOffset = (Integer)context.get("defaultYearsOffset");
        String mrpId = (String)context.get("mrpId");

        //Erases the old table for the moment and initializes it with the new orders,
        //Does not modify the old one now.

        List listResult = null;
        try {
            listResult = delegator.findList("MrpEvent", null, null, null, null, false);
            //int numOfRecordsRemoved = delegator.removeByCondition("MrpEvent", null);
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : findList(\"MrpEvent\", null, null, null, null, false)", module);
            return ServiceUtil.returnError("Problem, we can not find all the items of MrpEvent, for more detail look at the log");
        }
        if (listResult != null) {
            try {
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                Debug.logError(e,"Error : removeAll(listResult), listResult ="+listResult, module);
                return ServiceUtil.returnError("Problem, we can not remove the MrpEvent items, for more detail look at the log");
            }
        }

        // Proposed requirements are deleted
        listResult = null;
        List listResultRoles = new ArrayList();
        try {
            listResult = delegator.findByAnd("Requirement", UtilMisc.toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "statusId", "REQ_PROPOSED"));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of MrpEvent, for more detail look at the log");
        }
        if (listResult != null) {
            try {
                Iterator listResultIt = listResult.iterator();
                while (listResultIt.hasNext()) {
                    GenericValue tmpRequirement = (GenericValue)listResultIt.next();
                    listResultRoles.addAll(tmpRequirement.getRelated("RequirementRole"));
                    //int numOfRecordsRemoved = delegator.removeRelated("RequirementRole", tmpRequirement);
                }
                delegator.removeAll(listResultRoles);
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem, we can not remove the MrpEvent items, for more detail look at the log");
            }
        }
        listResult = null;
        try {
            listResult = delegator.findByAnd("Requirement", UtilMisc.toMap("requirementTypeId", "INTERNAL_REQUIREMENT", "statusId", "REQ_PROPOSED"));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of MrpEvent, for more detail look at the log");
        }
        if (listResult != null) {
            try {
                delegator.removeAll(listResult);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem, we can not remove the MrpEvent items, for more detail look at the log");
            }
        }

        GenericValue genericResult = null;
        Map parameters = null;
        List resultList = null;
        Iterator iteratorResult = null;
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
        iteratorResult = null;
        parameters = UtilMisc.toMap("orderTypeId", "SALES_ORDER", "oiStatusId", "ITEM_APPROVED");
        parameters.put("facilityId", facilityId);
        try {
            resultList = delegator.findByAnd("OrderHeaderItemAndShipGroup", parameters, UtilMisc.toList("orderId"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error : findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
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
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (SALES_ORDER_SHIP)");
            }
        }
        // ----------------------------------------
        // Loads all the approved product requirements (po requirements)
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "statusId", "REQ_APPROVED", "facilityId", facilityId);
        try {
            resultList = delegator.findByAnd("Requirement", parameters);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of MrpEvent, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
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
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (PROD_REQ_RECP)");
            }
        }

        // ----------------------------------------
        // Loads all the approved purchase order items
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        String orderId = null;
        GenericValue orderDeliverySchedule = null;
        try {
            List facilityContactMechs = EntityUtil.filterByDate(delegator.findByAnd("FacilityContactMech", UtilMisc.toMap("facilityId", facilityId)));
            List facilityContactMechIds = EntityUtil.getFieldListFromEntityList(facilityContactMechs, "contactMechId", true);
            List searchConditions = UtilMisc.toList(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"),
                                                    EntityCondition.makeCondition("oiStatusId", EntityOperator.EQUALS, "ITEM_APPROVED"),
                                                    EntityCondition.makeCondition("contactMechId", EntityOperator.IN, facilityContactMechIds));
            Set fieldsToSelect = UtilMisc.toSet("orderId", "orderItemSeqId", "productId", "quantity", "cancelQuantity", "oiEstimatedDeliveryDate");
            resultList = delegator.findList("OrderHeaderItemAndShipGroup", EntityCondition.makeCondition(searchConditions, EntityOperator.AND), fieldsToSelect, UtilMisc.toList("orderDate"), null, false);

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            String newOrderId =  genericResult.getString("orderId");
            if (!newOrderId.equals(orderId)) {
                orderDeliverySchedule = null;
                orderId = newOrderId;
                try {
                    orderDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
                } catch (GenericEntityException e) {
                }
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

            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            BigDecimal shippedQuantity = null;
            try {
                shippedQuantity = orh.getItemShippedQuantity(genericResult.getRelatedOne("OrderItem"));
            } catch (GenericEntityException e) {
            }
            if (UtilValidate.isNotEmpty(shippedQuantity)) {
                shipGroupQuantity = shipGroupQuantity.subtract(shippedQuantity);
            }

            GenericValue orderItemDeliverySchedule = null;
            try {
                orderItemDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", genericResult.getString("orderItemSeqId")));
            } catch (GenericEntityException e) {
            }
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
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, shipGroupQuantity, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (PUR_ORDER_RECP)");
            }
        }

        // ----------------------------------------
        // PRODUCTION Run: components
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED", "statusId", "WEGS_CREATED", "facilityId", facilityId);
        try {
            resultList = delegator.findByAnd("WorkEffortAndGoods", parameters);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error : findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            String productId =  genericResult.getString("productId");
            BigDecimal eventQuantityTmp = genericResult.getBigDecimal("estimatedQuantity").negate();
            Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedStartDate");
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }

            parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", estimatedShipDate, "mrpEventTypeId", "MANUF_ORDER_REQ");
            try {
                String eventName = (UtilValidate.isEmpty(genericResult.getString("workEffortParentId"))? genericResult.getString("workEffortId"): genericResult.getString("workEffortParentId") + "-" + genericResult.getString("workEffortId"));
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, eventName, false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (MRP_REQUIREMENT)");
            }
        }

        // ----------------------------------------
        // PRODUCTION Run: product produced
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV", "statusId", "WEGS_CREATED", "workEffortTypeId", "PROD_ORDER_HEADER", "facilityId", facilityId);
        try {
            resultList = delegator.findByAnd("WorkEffortAndGoods", parameters);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error : findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            if ("PRUN_CLOSED".equals(genericResult.getString("currentStatusId"))) {
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
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString("workEffortId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (MANUF_ORDER_RECP)");
            }
        }

        // ----------------------------------------
        // Products without upcoming events but that are already under minimum quantity in warehouse
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("facilityId", facilityId);
        try {
            resultList = delegator.findByAnd("ProductFacility", parameters);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to retrieve ProductFacility records.", module);
            return ServiceUtil.returnError("Unable to retrieve ProductFacility records.");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            String productId = genericResult.getString("productId");
            BigDecimal minimumStock = genericResult.getBigDecimal("minimumStock");
            if (minimumStock == null) {
                minimumStock = BigDecimal.ZERO;
            }
            try {
                EntityFieldMap ecl = EntityCondition.makeCondition(UtilMisc.toMap("mrpId", mrpId, "productId", productId), EntityOperator.AND);
                long numOfEvents = delegator.findCountByCondition("MrpEvent", ecl, null, null);
                if (numOfEvents > 0) {
                    continue;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to count MrpEvent records.", module);
                return ServiceUtil.returnError("Unable to count MrpEvent records.");
            }
            BigDecimal qoh = findProductMrpQoh(mrpId, productId, facilityId, dispatcher, delegator);
            if (qoh.compareTo(minimumStock) >= 0) {
                continue;
            }
            parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", now, "mrpEventTypeId", "REQUIRED_MRP");
            try {
                InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, BigDecimal.ZERO, null, null, false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the MrpEvent entity (REQUIRED_MRP)");
            }
        }

        // ----------------------------------------
        // SALES FORECASTS
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        GenericValue facility = null;
        try {
            facility = delegator.findOne("Facility", UtilMisc.toMap("facilityId", facilityId), false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find Facility, for more detail look at the log");
        }
        String partyId =  (String)facility.get("ownerPartyId");
        try {
            resultList = delegator.findByAnd("SalesForecast", UtilMisc.toMap("organizationPartyId", partyId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find SalesForecasts, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            String customTimePeriodId =  genericResult.getString("customTimePeriodId");
            GenericValue customTimePeriod = null;
            try {
                customTimePeriod = delegator.findOne("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId), false);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem, we can not find CustomTimePeriod, for more detail look at the log");
            }
            if (customTimePeriod != null) {
                if (customTimePeriod.getDate("thruDate") != null && customTimePeriod.getDate("thruDate").before(UtilDateTime.nowDate())) {
                    continue;
                } else {
                    List salesForecastDetails = null;
                    Iterator sfdIter = null;
                    try {
                        salesForecastDetails = delegator.findByAnd("SalesForecastDetail", UtilMisc.toMap("salesForecastId", genericResult.getString("salesForecastId")));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError("Problem, we can not find SalesForecastDetails, for more detail look at the log");
                    }
                    sfdIter = salesForecastDetails.iterator();
                    while (sfdIter.hasNext()) {
                        genericResult = (GenericValue) sfdIter.next();
                        String productId =  genericResult.getString("productId");
                        BigDecimal eventQuantityTmp = genericResult.getBigDecimal("quantity");
                        if (productId == null || eventQuantityTmp == null) {
                            continue;
                        }
                        eventQuantityTmp = eventQuantityTmp.negate();
                        parameters = UtilMisc.toMap("mrpId", mrpId, "productId", productId, "eventDate", customTimePeriod.getDate("fromDate"), "mrpEventTypeId", "SALES_FORECAST");
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, eventQuantityTmp, null, genericResult.getString("salesForecastDetailId"), false, delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem initializing the MrpEvent entity (SalesForecastDetail)");
                        }
                    }
                }
            }
        }
        Map result = new HashMap();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from initMrpEvent", module);
        return result;
    }

    /**
     * Find the quantity on hand of products for MRP.
     * <li>PreConditions : none</li>
     * <li>Result : We get the quantity of product available in the stocks.</li>
     *
     * @param product the product for which the Quantity Available is required
     * @return the sum of all the totalAvailableToPromise of the inventoryItem related to the product, if the related facility is Mrp available (not yet implemented!!)
     */
    public static BigDecimal findProductMrpQoh(String mrpId, GenericValue product, String facilityId, LocalDispatcher dispatcher, Delegator delegator) {
        return findProductMrpQoh(mrpId, product.getString("productId"), facilityId, dispatcher, delegator);
    }
    public static BigDecimal findProductMrpQoh(String mrpId, String productId, String facilityId, LocalDispatcher dispatcher, Delegator delegator) {
        Map resultMap = null;
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
     *   Before inserting in the entity, test if there is the record already existing to add quantity rather to create a new one.
     *
     * @param product
     * @param eventQuantity the product quantity needed
     *  @param startDate the startDate of the productionRun which will used to produce the product
     *  @param routingTaskStartDate Map with all the routingTask as keys and startDate of each of them
     * @return None
     */

    public static void processBomComponent(String mrpId, GenericValue product, BigDecimal eventQuantity, Timestamp startDate, Map routingTaskStartDate, List listComponent) {
        // TODO : change the return type to boolean to be able to test if all is ok or if it have had a exception
        Delegator delegator = product.getDelegator();

        if (UtilValidate.isNotEmpty(listComponent)) {
            Iterator listComponentIter = listComponent.iterator();
            while (listComponentIter.hasNext()) {
                BOMNode node = (BOMNode) listComponentIter.next();
                GenericValue productComponent = node.getProductAssoc();
                // read the startDate for the component
                String routingTask = node.getProductAssoc().getString("routingWorkEffortId");
                Timestamp eventDate = (routingTask == null || !routingTaskStartDate.containsKey(routingTask)) ? startDate : (Timestamp) routingTaskStartDate.get(routingTask);
                // if the components is valid at the event Date create the Mrp requirement in the M entity
                if (EntityUtil.isValueActive(productComponent, eventDate)) {
                    //Map parameters = UtilMisc.toMap("productId", productComponent.getString("productIdTo"));
                    Map parameters = UtilMisc.toMap("productId", node.getProduct().getString("productId"));
                    parameters.put("mrpId", mrpId);
                    parameters.put("eventDate", eventDate);
                    parameters.put("mrpEventTypeId", "MRP_REQUIREMENT");
                    BigDecimal componentEventQuantity = node.getQuantity();
                    try {
                        InventoryEventPlannedServices.createOrUpdateMrpEvent(parameters, componentEventQuantity.negate(), null, product.get("productId") + ": " + eventDate, false, delegator);
                    } catch (GenericEntityException e) {
                        Debug.logError("Error : findByPrimaryKey(\"MrpEvent\", parameters) ="+parameters+"--"+e.getMessage(), module);
                        logMrpError(mrpId, node.getProduct().getString("productId"), "Unable to create event (processBomComponent)", delegator);
                    }
                }
            }
        }
    }

    /**
     * Launch the MRP.
     * <li>PreConditions : none</li>
     * <li>Result : The date when we must order or begin to build the products and subproducts we need are calclated</li>
     *
     * <li>INPUT : parameters to get from the context :</li><ul>
     * <li>String mrpName</li></ul>
     *
     * <li>OUTPUT : Result to put in the map :</li><ul>
     * <li>none</li></ul>
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId routingId, quantity, startDate.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map executeMrp(DispatchContext ctx, Map context) {
        Debug.logInfo("executeMrp called", module);

        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();

        String mrpName = (String)context.get("mrpName");
        Integer defaultYearsOffset = (Integer)context.get("defaultYearsOffset");
        String facilityGroupId = (String)context.get("facilityGroupId");
        String facilityId = (String)context.get("facilityId");
        String manufacturingFacilityId = null;
        if (UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(facilityGroupId)) {
            return ServiceUtil.returnError("facilityId and facilityGroupId cannot be both null");
        }
        if (UtilValidate.isEmpty(facilityId)) {
            try {
                GenericValue facilityGroup = delegator.findByPrimaryKey("FacilityGroup", UtilMisc.toMap("facilityGroupId", facilityGroupId));
                if (UtilValidate.isEmpty(facilityGroup)) {
                    return ServiceUtil.returnError("facilityGroupId [" + facilityGroupId + "] is not valid");
                }
                List facilities = facilityGroup.getRelated("FacilityGroupMember", UtilMisc.toList("sequenceNum"));
                if (UtilValidate.isEmpty(facilities)) {
                    return ServiceUtil.returnError("No facility associated to facilityGroupId [" + facilityGroupId + "]");
                }
                Iterator facilitiesIt = facilities.iterator();
                while (facilitiesIt.hasNext()) {
                    GenericValue facilityMember = (GenericValue)facilitiesIt.next();
                    GenericValue facility = facilityMember.getRelatedOne("Facility");
                    if ("WAREHOUSE".equals(facility.getString("facilityTypeId")) && UtilValidate.isEmpty(facilityId)) {
                        facilityId = facility.getString("facilityId");
                    }
                    if ("PLANT".equals(facility.getString("facilityTypeId")) && UtilValidate.isEmpty(manufacturingFacilityId)) {
                        manufacturingFacilityId = facility.getString("facilityId");
                    }
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem loading facility group information: " + e.getMessage());
            }
        } else {
            manufacturingFacilityId = facilityId;
        }

        if (UtilValidate.isEmpty(facilityId) || UtilValidate.isEmpty(manufacturingFacilityId)) {
            return ServiceUtil.returnError("facilityId and manufacturingFacilityId cannot be null");
        }

        int bomLevelWithNoEvent = 0;
        BigDecimal stockTmp = BigDecimal.ZERO;
        String oldProductId = null;
        String productId = null;
        GenericValue product = null;
        GenericValue productFacility = null;
        BigDecimal eventQuantity = BigDecimal.ZERO;
        Timestamp eventDate = null;
        boolean isNegative = false;
        BigDecimal quantityNeeded = BigDecimal.ZERO;
        BigDecimal reorderQuantity = BigDecimal.ZERO;
        BigDecimal minimumStock = BigDecimal.ZERO;
        int daysToShip = 0;
        List components = null;
        boolean isBuilt = false;
        GenericValue routing = null;

        String mrpId = delegator.getNextSeqId("MrpEvent");

        Map result = null;
        Map parameters = null;
        List listInventoryEventForMRP = null;
        ListIterator iteratorListInventoryEventForMRP = null;
        GenericValue inventoryEventForMRP = null;

        // Initialization of the MrpEvent table, This table will contain the products we want to buy or build.
        parameters = UtilMisc.toMap("mrpId", mrpId, "reInitialize", Boolean.TRUE, "defaultYearsOffset", defaultYearsOffset, "userLogin", userLogin);
        parameters.put("facilityId", facilityId);
        parameters.put("manufacturingFacilityId", manufacturingFacilityId);
        try {
            result = dispatcher.runSync("initMrpEvents", parameters);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError("Error running the initMrpEvents service: " + e.getMessage());
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
                listInventoryEventForMRP = delegator.findList("MrpEventView", filterByConditions, null, UtilMisc.toList("productId", "eventDate"), null, false);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("MRP Error retieving MRP event for the bom level: " + bomLevel + ". Error: " + e.getMessage());
            }

            if (UtilValidate.isNotEmpty(listInventoryEventForMRP)) {
                bomLevelWithNoEvent = 0;
                iteratorListInventoryEventForMRP = listInventoryEventForMRP.listIterator();

                oldProductId = "";
                while (iteratorListInventoryEventForMRP.hasNext()) {
                    inventoryEventForMRP = (GenericValue) iteratorListInventoryEventForMRP.next();
                    productId = inventoryEventForMRP.getString("productId");
                    eventQuantity = inventoryEventForMRP.getBigDecimal("quantity");

                    if (!productId.equals(oldProductId)) {
                        BigDecimal positiveEventQuantity = eventQuantity.compareTo(BigDecimal.ZERO) > 0 ? eventQuantity: eventQuantity.negate();
                        // It's a new product, so it's necessary to  read the MrpQoh
                        try {
                            product = inventoryEventForMRP.getRelatedOneCache("Product");
                            productFacility = EntityUtil.getFirst(product.getRelatedByAndCache("ProductFacility", UtilMisc.toMap("facilityId", facilityId)));
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem, can not find the product for a event, for more detail look at the log");
                        }
                        stockTmp = findProductMrpQoh(mrpId, product, facilityId, dispatcher, delegator);
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(UtilMisc.toMap("mrpId", mrpId, "productId", product.getString("productId"), "mrpEventTypeId", "INITIAL_QOH", "eventDate", now),
                                                                                              stockTmp, facilityId, null, false,
                                                                                              delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem running createOrUpdateMrpEvent");
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
                        Map serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", positiveEventQuantity, "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                        } catch (Exception e) {
                            return ServiceUtil.returnError("An error occurred exploding the product [" + product.getString("productId") + "]");
                        }
                        components = (List)serviceResponse.get("components");
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = ((BOMNode)components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        oldProductId = productId;
                    }

                    stockTmp = stockTmp.add(eventQuantity);
                    if (stockTmp.compareTo(minimumStock) < 0) {
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
                        Map serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.<String, Object>toMap("productId", product.getString("productId"), "quantity", proposedOrder.getQuantity(), "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                        } catch (Exception e) {
                            return ServiceUtil.returnError("An error occurred exploding the product [" + product.getString("productId") + "]");
                        }
                        components = (List)serviceResponse.get("components");
                        String routingId = (String)serviceResponse.get("workEffortId");
                        if (routingId != null) {
                            try {
                                routing = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingId));
                            } catch (GenericEntityException e) {
                                return ServiceUtil.returnError("Problem, can not find the product for a event, for more detail look at the log");
                            }
                        } else {
                            routing = null;
                        }
                        if (UtilValidate.isNotEmpty(components)) {
                            BOMNode node = ((BOMNode)components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        // calculate the ProposedOrder requirementStartDate and update the requirementStartDate object property.
                        Map routingTaskStartDate = proposedOrder.calculateStartDate(daysToShip, routing, delegator, dispatcher, userLogin);
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
                        Map eventMap = UtilMisc.toMap("productId", product.getString("productId"),
                                                      "mrpId", mrpId,
                                                      "eventDate", eventDate,
                                                      "mrpEventTypeId", (isBuilt? "PROP_MANUF_O_RECP" : "PROP_PUR_O_RECP"));
                        try {
                            InventoryEventPlannedServices.createOrUpdateMrpEvent(eventMap, proposedOrder.getQuantity(), null, eventName, (proposedOrder.getRequirementStartDate().compareTo(now) < 0), delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem running createOrUpdateMrpEvent");
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

        result =  new HashMap();
        List msgResult = new LinkedList();
        result.put("msgResult",msgResult);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from executeMrp", module);
        return result;
    }
}
