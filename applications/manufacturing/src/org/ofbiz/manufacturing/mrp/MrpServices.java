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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.bom.BOMNode;
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
    
    
    
    /**
     * Initialize the InventoryEventPlanned table.
     * <li>PreConditions : none</li>
     * <li>Result : The table InventoryEventPlannedForMRP is initialized</li>
     * <li>INPUT : Parameter to get from the context :</li><ul>
     * <li>Boolean reInitialize<br/>
     * if true : we must reinitialize the table, else we synchronize the table (not for the moment)</li></ul>
     *
     * <li>OUTPUT : Result to put in the map :</li><ul>
     * <li>none</li></ul>
     *
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    
    public static Map initInventoryEventPlanned(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        
        Integer defaultYearsOffset = (Integer)context.get("defaultYearsOffset");
        
        //Erases the old table for the moment and initializes it with the new orders,
        //Does not modify the old one now.
        Debug.logInfo("initInventoryEventPlanned called", module);
        
        List listResult = null;
        try{
            listResult = delegator.findAll("InventoryEventPlanned");
            //int numOfRecordsRemoved = delegator.removeByCondition("InventoryEventPlanned", null);
        } catch(GenericEntityException e) {
            Debug.logError(e,"Error : delegator.findAll(\"InventoryEventPlanned\")", module);
            return ServiceUtil.returnError("Problem, we can not find all the items of InventoryEventPlanned, for more detail look at the log");
        }
        if(listResult != null){
            try{
                delegator.removeAll(listResult);
            } catch(GenericEntityException e) {
                Debug.logError(e,"Error : delegator.removeAll(listResult), listResult ="+listResult, module);
                return ServiceUtil.returnError("Problem, we can not remove the InventoryEventPlanned items, for more detail look at the log");
            }
        }

        // Proposed requirements are deleted
        listResult = null;
        List listResultRoles = new ArrayList();
        try{
            listResult = delegator.findByAnd("Requirement", UtilMisc.toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "statusId", "REQ_PROPOSED"));
        } catch(GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of InventoryEventPlanned, for more detail look at the log");
        }
        if (listResult != null){
            try{
                Iterator listResultIt = listResult.iterator();
                while (listResultIt.hasNext()){
                    GenericValue tmpRequirement = (GenericValue)listResultIt.next();
                    listResultRoles.addAll(tmpRequirement.getRelated("RequirementRole"));
                    //int numOfRecordsRemoved = delegator.removeRelated("RequirementRole", tmpRequirement);
                }
                delegator.removeAll(listResultRoles);
                delegator.removeAll(listResult);
            } catch(GenericEntityException e) {
                return ServiceUtil.returnError("Problem, we can not remove the InventoryEventPlanned items, for more detail look at the log");
            }
        }
        listResult = null;
        try{
            listResult = delegator.findByAnd("Requirement", UtilMisc.toMap("requirementTypeId", "INTERNAL_REQUIREMENT", "statusId", "REQ_PROPOSED"));
        } catch(GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of InventoryEventPlanned, for more detail look at the log");
        }
        if(listResult != null){
            try{
                delegator.removeAll(listResult);
            } catch(GenericEntityException e) {
                return ServiceUtil.returnError("Problem, we can not remove the InventoryEventPlanned items, for more detail look at the log");
            }
        }

        GenericValue genericResult = null;
        Map parameters = null;
        List resultList = null;
        Iterator iteratorResult = null;
        // ----------------------------------------
        // Loads all the approved sales order items and purchase order items
        // ----------------------------------------
        // This is the default required date for orders without dates spesified:
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
        try {
            resultList = delegator.findByAnd("OrderHeaderItemAndShipGroup", parameters, UtilMisc.toList("orderId"));
        } catch(GenericEntityException e) {
            Debug.logError(e, "Error : delegator.findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while (iteratorResult.hasNext()) {
            genericResult = (GenericValue) iteratorResult.next();
            String productId =  genericResult.getString("productId");
            Double shipGroupQuantity = genericResult.getDouble("quantity");
            Double cancelledQuantity = genericResult.getDouble("cancelQuantity");
            if (UtilValidate.isNotEmpty(cancelledQuantity)) {
                shipGroupQuantity = new Double(shipGroupQuantity.doubleValue() - cancelledQuantity.doubleValue());
            }
            Double eventQuantityTmp = new Double(-1.0 * shipGroupQuantity.doubleValue());
            if (eventQuantityTmp.doubleValue() == 0) {
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
            parameters = UtilMisc.toMap("productId", productId, "eventDate", requiredByDate, "inventoryEventPlanTypeId", "SALES_ORDER_SHIP");
            try {
                InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, eventQuantityTmp, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the InventoryEventPlanned entity (SALES_ORDER_SHIP)");
            }
        }
        // ----------------------------------------
        // Loads all the approved product requirements (po requirements)
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("requirementTypeId", "PRODUCT_REQUIREMENT", "statusId", "REQ_APPROVED");
        try{
            resultList = delegator.findByAnd("Requirement", parameters);
        } catch(GenericEntityException e) {
            return ServiceUtil.returnError("Problem, we can not find all the items of InventoryEventPlanned, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while(iteratorResult.hasNext()){
            genericResult = (GenericValue) iteratorResult.next();
            String productId =  genericResult.getString("productId");
            Double eventQuantityTmp = genericResult.getDouble("quantity");
            if (productId == null || eventQuantityTmp == null) {
                continue;
            }
            Timestamp estimatedShipDate = genericResult.getTimestamp("requiredByDate");
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }
            
            parameters = UtilMisc.toMap("productId", productId, "eventDate", estimatedShipDate, "inventoryEventPlanTypeId", "PROD_REQ_RECP");
            try {
                InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, eventQuantityTmp, null, genericResult.getString("requirementId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the InventoryEventPlanned entity (PROD_REQ_RECP)");
            }
        }
        
        // ----------------------------------------
        // Loads all the approved purchase order items
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        String orderId = null;
        GenericValue orderDeliverySchedule = null;
        parameters = UtilMisc.toMap("orderTypeId", "PURCHASE_ORDER", "itemStatusId", "ITEM_APPROVED");
        try {
            resultList = delegator.findByAnd("OrderHeaderAndItems", parameters, UtilMisc.toList("orderId"));
        } catch(GenericEntityException e) {
            Debug.logError(e, "Error : delegator.findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while(iteratorResult.hasNext()){
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
            Double eventQuantityTmp = new Double(genericResult.getDouble("quantity").doubleValue());
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
                estimatedShipDate = genericResult.getTimestamp("estimatedDeliveryDate");
            }
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }
            
            parameters = UtilMisc.toMap("productId", productId, "eventDate", estimatedShipDate, "inventoryEventPlanTypeId", "PUR_ORDER_RECP");
            try {
                InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, eventQuantityTmp, null, genericResult.getString("orderId") + "-" + genericResult.getString("orderItemSeqId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the InventoryEventPlanned entity (PUR_ORDER_RECP)");
            }
        }

        // ----------------------------------------
        // PRODUCTION Run: components
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED", "statusId", "WEGS_CREATED");
        try {
            resultList = delegator.findByAnd("WorkEffortAndGoods", parameters);
        } catch(GenericEntityException e) {
            Debug.logError(e, "Error : delegator.findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while(iteratorResult.hasNext()){
            genericResult = (GenericValue) iteratorResult.next();
            String productId =  genericResult.getString("productId");
            Double eventQuantityTmp = new Double(-1.0 * genericResult.getDouble("estimatedQuantity").doubleValue());
            Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedStartDate");
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }
            
            parameters = UtilMisc.toMap("productId", productId, "eventDate", estimatedShipDate, "inventoryEventPlanTypeId", "MANUF_ORDER_REQ");
            try {
                String eventName = (UtilValidate.isEmpty(genericResult.getString("workEffortParentId"))? genericResult.getString("workEffortId"): genericResult.getString("workEffortParentId") + "-" + genericResult.getString("workEffortId"));
                InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, eventQuantityTmp, null, eventName, false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the InventoryEventPlanned entity (MRP_REQUIREMENT)");
            }
        }
        
        // ----------------------------------------
        // PRODUCTION Run: product produced
        // ----------------------------------------
        resultList = null;
        iteratorResult = null;
        parameters = UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV", "statusId", "WEGS_CREATED", "workEffortTypeId", "PROD_ORDER_HEADER");
        try {
            resultList = delegator.findByAnd("WorkEffortAndGoods", parameters);
        } catch(GenericEntityException e) {
            Debug.logError(e, "Error : delegator.findByAnd(\"OrderItem\", parameters\")", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, we can not find the order items, for more detail look at the log");
        }
        iteratorResult = resultList.iterator();
        while(iteratorResult.hasNext()){
            genericResult = (GenericValue) iteratorResult.next();
            if ("PRUN_CLOSED".equals(genericResult.getString("currentStatusId"))) {
                continue;
            }
            Double qtyToProduce = genericResult.getDouble("quantityToProduce");
            if (qtyToProduce == null) {
                qtyToProduce = new Double(0);
            }
            Double qtyProduced = genericResult.getDouble("quantityProduced");
            if (qtyProduced == null) {
                qtyProduced = new Double(0);
            }
            if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                continue;
            }
            double qtyDiff = qtyToProduce.doubleValue() - qtyProduced.doubleValue();
            String productId =  genericResult.getString("productId");
            Double eventQuantityTmp = new Double(qtyDiff);
            Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedCompletionDate");
            if (estimatedShipDate == null) {
                estimatedShipDate = now;
            }
            
            parameters = UtilMisc.toMap("productId", productId, "eventDate", estimatedShipDate, "inventoryEventPlanTypeId", "MANUF_ORDER_RECP");
            try {
                InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, eventQuantityTmp, null, genericResult.getString("workEffortId"), false, delegator);
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Problem initializing the InventoryEventPlanned entity (MANUF_ORDER_RECP)");
            }
        }

        
        Map result = new HashMap();
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from initInventoryEventPlanned", module);
        return result;
    }
    /**
     * Create a List  with all the event of InventotyEventPlanned for one billOfMaterialLevel, sorted by productId and eventDate.
     *
     * <li>INPUT : Parameter to get from the context : </li><ul>
     * <li>Integer billOfMaterialLevel : 0 for root for more detail see BomHelper.getMaxDepth</li></ul>
     *
     * <li>OUTPUT : Result to put in the map :</li><ul>
     * <li>List listInventoryEventForMRP : all the event of InventotyEventPlanned for one billOfMaterialLevel, sorted by productId and eventDate<br/>
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map listProductForMrp(DispatchContext ctx, Map context) {
        Debug.logInfo("listProductForMrp called", module);
        // read parameters from context
        GenericDelegator delegator = ctx.getDelegator();
        Long billOfMaterialLevel = (Long) context.get("billOfMaterialLevel");
        
        // Find all products in MrpInventoryEventPlanned, ordered by bom and eventDate
        List listResult = null;
        // If billOfMaterialLevel == 0 the search must be done with (billOfMaterialLevel == 0 || billOfMaterialLevel == null)
        EntityCondition parameters = null;
        if (billOfMaterialLevel.intValue() == 0) {
            parameters = new EntityExpr(new EntityExpr("billOfMaterialLevel", EntityOperator.EQUALS, null),
                                        EntityOperator.OR,
                                        new EntityExpr("billOfMaterialLevel", EntityOperator.EQUALS, billOfMaterialLevel));
        } else {
            parameters = new EntityExpr("billOfMaterialLevel", EntityOperator.EQUALS, billOfMaterialLevel);
        }

        List orderBy = UtilMisc.toList("productId", "eventDate");
        try{
            //listResult = delegator.findByAnd("MrpInventoryEventPlanned", parameters, orderBy);
            listResult = delegator.findByCondition("MrpInventoryEventPlanned", parameters, null, orderBy);
        } catch(GenericEntityException e) {
            Debug.logError(e, "Error : delegator.findByCondition(\"MrpInventoryEventPlanned\", parameters, null, orderBy)", module);
            Debug.logError(e, "Error : parameters = "+parameters,module);
            Debug.logError(e, "Error : orderBy = "+orderBy,module);
            return ServiceUtil.returnError("Problem, we can not find the products, for more detail look at the log");
        }
        Map result = new HashMap();
        result.put("listInventoryEventForMrp",listResult);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        Debug.logInfo("return from listProductForMrp "+billOfMaterialLevel, module);
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
    public static double findProductMrpQoh(GenericValue product, String facilityId, LocalDispatcher dispatcher, GenericDelegator delegator) {
        List orderBy = UtilMisc.toList("facilityId", "-receivedDate", "-inventoryItemId");
        Map resultMap = null;
        try{
            if (facilityId == null) {
                resultMap = dispatcher.runSync("getProductInventoryAvailable", UtilMisc.toMap("productId", product.getString("productId")));
            } else {
                resultMap = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", product.getString("productId"), "facilityId", facilityId));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling getProductInventoryAvailableByFacility service", module);
            logMrpError(product.getString("productId"), "Unable to count inventory", delegator);
            return 0;
        }
        return ((Double)resultMap.get("quantityOnHandTotal")).doubleValue();
    }

    public static void logMrpError(String productId, String errorMessage, GenericDelegator delegator) {
        logMrpError(productId, UtilDateTime.nowTimestamp(), errorMessage, delegator);
    }
    public static void logMrpError(String productId, Timestamp eventDate, String errorMessage, GenericDelegator delegator) {
        try {
            if (UtilValidate.isNotEmpty(productId) && UtilValidate.isNotEmpty(errorMessage)) {
                GenericValue inventoryEventError = delegator.makeValue("InventoryEventPlanned", UtilMisc.toMap("productId", productId, 
                                                                                                               "eventDate", eventDate,
                                                                                                               "inventoryEventPlanTypeId", "ERROR",
                                                                                                               "eventName", errorMessage));
                delegator.createOrStore(inventoryEventError);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error calling logMrpError for productId [" + productId + "] and errorMessage [" + errorMessage + "]", module);
        }
    }

    /**
     * Process the bill of material (bom) of the product  to insert components in the InventoryEventPlanned table.
     *   Before inserting in the entity, test if there is the record already existing to add quantity rather to create a new one.
     *
     * @param product
     * @param eventQuantity the product quantity needed
     *  @param startDate the startDate of the productionRun which will used to produce the product
     *  @param routingTaskStartDate Map with all the routingTask as keys and startDate of each of them
     * @return None
     */
    
    public static void processBomComponent(GenericValue product, double eventQuantity, Timestamp startDate, Map routingTaskStartDate, List listComponent) {
        // TODO : change the return type to boolean to be able to test if all is ok or if it have had a exception
        GenericDelegator delegator = product.getDelegator();

        if (listComponent != null && listComponent.size() >0) {
            Iterator listComponentIter = listComponent.iterator();
            while (listComponentIter.hasNext()) {
                BOMNode node = (BOMNode) listComponentIter.next();
                GenericValue productComponent = node.getProductAssoc();
                // read the startDate for the component
                String routingTask = node.getProductAssoc().getString("routingWorkEffortId");
                Timestamp eventDate = (routingTask == null || !routingTaskStartDate.containsKey(routingTask)) ? startDate : (Timestamp) routingTaskStartDate.get(routingTask);
                // if the components is valid at the event Date create the Mrp requirement in the InventoryEventPlanned entity
                if (EntityUtil.isValueActive(productComponent, eventDate)) {
                    //Map parameters = UtilMisc.toMap("productId", productComponent.getString("productIdTo"));
                    Map parameters = UtilMisc.toMap("productId", node.getProduct().getString("productId"));
                    parameters.put("eventDate", eventDate);
                    parameters.put("inventoryEventPlanTypeId", "MRP_REQUIREMENT");
                    double componentEventQuantity = node.getQuantity();
                    try {
                        InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(parameters, new Double(-1.0 * componentEventQuantity), null, product.get("productId") + ": " + eventDate, false, delegator);
                    } catch (GenericEntityException e) {
                        Debug.logError("Error : delegator.findByPrimaryKey(\"InventoryEventPlanned\", parameters) ="+parameters+"--"+e.getMessage(), module);
                        logMrpError(node.getProduct().getString("productId"), "Unable to create event (processBomComponent)", delegator);
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

        GenericDelegator delegator = ctx.getDelegator();
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
        double stockTmp = 0;
        String oldProductId = null;
        String productId = null;
        GenericValue product = null;
        GenericValue productFacility = null;
        double eventQuantity = 0;
        Timestamp eventDate = null;
        boolean isNegative = false;
        double quantityNeeded = 0;
        double reorderQuantity = 0;
        double minimumStock = 0;
        int daysToShip = 0;
        List components = null;
        boolean isBuilt = false;
        GenericValue routing = null;
        
        Map result = null;
        Map parameters = null;
        List listInventoryEventForMRP = null;
        ListIterator iteratorListInventoryEventForMRP = null;
        GenericValue inventoryEventForMRP = null;
        
        // Initialisation of the InventoryEventPlanned table, This table will contain the products we want to buy or build.
        parameters = UtilMisc.toMap("reInitialize", Boolean.TRUE, "defaultYearsOffset", defaultYearsOffset, "userLogin", userLogin);
        try {
            result = dispatcher.runSync("initInventoryEventPlanned", parameters);
        } catch (GenericServiceException e) {
            Debug.logError("Error : initInventoryEventPlanned", module);
            Debug.logError("Error : parameters = "+parameters,module);
            return ServiceUtil.returnError("Problem, can not initialise the table InventoryEventPlanned, for more detail look at the log");
        }
        long bomLevel = 0;
        do {
            //get the products from the InventoryEventPlanned table for the current billOfMaterialLevel (ie. BOM)
            parameters = UtilMisc.toMap("billOfMaterialLevel", new Long(bomLevel), "userLogin", userLogin);
            try {
                result = dispatcher.runSync("listProductForMrp", parameters);
            } catch (GenericServiceException e) {
                Debug.logError("Error : listProductForMrp, parameters ="+parameters, module);
                return ServiceUtil.returnError("Problem, can not list the products for the MRP, for more detail look at the log");
            }
            listInventoryEventForMRP = (List) result.get("listInventoryEventForMrp");
            
            if (listInventoryEventForMRP != null && listInventoryEventForMRP.size()>0) {
                bomLevelWithNoEvent = 0;
                iteratorListInventoryEventForMRP = listInventoryEventForMRP.listIterator();
                
                oldProductId = "";
                while (iteratorListInventoryEventForMRP.hasNext()) {
                    inventoryEventForMRP = (GenericValue) iteratorListInventoryEventForMRP.next();
                    productId = inventoryEventForMRP.getString("productId");
                    eventQuantity = inventoryEventForMRP.getDouble("eventQuantity").doubleValue();

                    if (!productId.equals(oldProductId)) {
                        double positiveEventQuantity = (eventQuantity > 0? eventQuantity: -1 * eventQuantity);
                        // It's a new product, so it's necessary to  read the MrpQoh
                        try {
                            product = inventoryEventForMRP.getRelatedOneCache("Product");
                            productFacility = EntityUtil.getFirst(product.getRelatedByAndCache("ProductFacility", UtilMisc.toMap("facilityId", facilityId)));
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem, can not find the product for a event, for more detail look at the log");
                        }
                        stockTmp = findProductMrpQoh(product, facilityId, dispatcher, delegator);
                        try {
                            InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(UtilMisc.toMap("productId", product.getString("productId"), "inventoryEventPlanTypeId", "INITIAL_QOH", "eventDate", now),
                                                                                              new Double(stockTmp), facilityId, null, false,
                                                                                              delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem running createOrUpdateInventoryEventPlanned");
                        }
			// days to ship is only relevant for sales order to plan for preparatory days to ship.  Otherwise MRP will push event dates for manufacturing parts
                        // as well and cause problems
                        daysToShip = 0;
                        if (productFacility != null) {
                            reorderQuantity = (productFacility.getDouble("reorderQuantity") != null? productFacility.getDouble("reorderQuantity").doubleValue(): -1);
                            minimumStock = (productFacility.getDouble("minimumStock") != null? productFacility.getDouble("minimumStock").doubleValue(): 0);
                            if ("SALES_ORDER_SHIP".equals(inventoryEventForMRP.getString("inventoryEventPlanTypeId"))) {
                                daysToShip = (productFacility.getLong("daysToShip") != null? productFacility.getLong("daysToShip").intValue(): 0);
                            }
                        } else {
                            minimumStock = 0;
                            reorderQuantity = -1;
                        }
                        // -----------------------------------------------------
                        // The components are also loaded thru the configurator
                        Map serviceResponse = null;
                        try {
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.toMap("productId", product.getString("productId"), "quantity", new Double(positiveEventQuantity), "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
                        } catch (Exception e) {
                            return ServiceUtil.returnError("An error occurred exploding the product [" + product.getString("productId") + "]");
                        }
                        components = (List)serviceResponse.get("components");
                        if (components != null && components.size() > 0) {
                            BOMNode node = ((BOMNode)components.get(0)).getParentNode();
                            isBuilt = node.isManufactured();
                        } else {
                            isBuilt = false;
                        }
                        // #####################################################

                        oldProductId = productId;
                    }
                    
                    stockTmp = stockTmp + eventQuantity;
                    if(stockTmp < minimumStock){
                        double qtyToStock = minimumStock - stockTmp;
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
                            serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.toMap("productId", product.getString("productId"), "quantity", new Double(proposedOrder.getQuantity()), "excludeWIPs", Boolean.FALSE, "userLogin", userLogin));
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
                        if (components != null && components.size() > 0) {
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
                            processBomComponent(product, proposedOrder.getQuantity(), proposedOrder.getRequirementStartDate(), routingTaskStartDate, components);
                        }
                        // create the  ProposedOrder (only if the product is warehouse managed), and the InventoryEventPlanned associated
                        String requirementId = null;
                        if (productFacility != null) {
                            requirementId = proposedOrder.create(ctx, userLogin);
                        }
                        if (UtilValidate.isEmpty(productFacility) && !isBuilt) {
                            logMrpError(productId, now, "No ProductFacility record for [" + facilityId + "]; no requirement created.", delegator);
                        }
                        String eventName = null;
                        if (UtilValidate.isNotEmpty(requirementId)) {
                            eventName = "*" + requirementId + " (" + proposedOrder.getRequirementStartDate() + ")*";
                        }
                        Map eventMap = UtilMisc.toMap("productId", product.getString("productId"),
                                                      "eventDate", eventDate,
                                                      "inventoryEventPlanTypeId", (isBuilt? "PROP_MANUF_O_RECP" : "PROP_PUR_O_RECP"));
                        try {
                            InventoryEventPlannedServices.createOrUpdateInventoryEventPlanned(eventMap, new Double(proposedOrder.getQuantity()), null, eventName, (proposedOrder.getRequirementStartDate().compareTo(now) < 0), delegator);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError("Problem running createOrUpdateInventoryEventPlanned");
                        }
                        //
                        stockTmp = stockTmp + proposedOrder.getQuantity();
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
