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
package org.ofbiz.order.requirement;

import java.util.*;
import java.sql.Timestamp;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Requirement Services
 */

public class RequirementServices {

    public static final String module = RequirementServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static final Map getRequirementsForSupplier(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        EntityCondition requirementConditions = (EntityCondition) context.get("requirementConditions");
        String partyId = (String) context.get("partyId");
        String unassignedRequirements = (String) context.get("unassignedRequirements");
        String currencyUomId = (String) context.get("currencyUomId");
        try {
            List orderBy = UtilMisc.toList("partyId", "requirementId");
            List conditions = UtilMisc.toList(
                    new EntityExpr("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                    new EntityExpr("statusId", EntityOperator.EQUALS, "REQ_APPROVED"),
                    EntityUtil.getFilterByDateExpr()
                    );
            if (requirementConditions != null) conditions.add(requirementConditions);

            // we're either getting the requirements for a given supplier, unassigned requirements, or requirements for all suppliers
            if (UtilValidate.isNotEmpty(partyId)) {
                conditions.add( new EntityExpr("partyId", EntityOperator.EQUALS, partyId) );
                conditions.add( new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER") );
            } else if (UtilValidate.isNotEmpty(unassignedRequirements)) {
                conditions.add( new EntityExpr("partyId", EntityOperator.EQUALS, null) );
            } else {
                conditions.add( new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER") );
            }
            List requirementAndRoles = delegator.findByAnd("RequirementAndRole", conditions, orderBy);

            // maps to cache the associated suppliers and products data, so we don't do redundant DB and service requests
            Map suppliers = FastMap.newInstance();
            Map gids = FastMap.newInstance();
            Map inventories = FastMap.newInstance();
            Map productsSold = FastMap.newInstance();

            // to count quantity, running total, and distinct products in list
            double quantity = 0.0;
            double amountTotal = 0.0;
            Set products = new HashSet();

            // time period to count products ordered from, six months ago and the 1st of that month
            Timestamp timePeriodStart = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, -6);

            // join in fields with extra data about the suppliers and products
            List requirements = FastList.newInstance();
            for (Iterator iter = requirementAndRoles.iterator(); iter.hasNext(); ) {
                Map union = FastMap.newInstance();
                GenericValue requirement = (GenericValue) iter.next();
                String productId = requirement.getString("productId");
                partyId = requirement.getString("partyId");
                String facilityId = requirement.getString("facilityId");
                double requiredQuantity = requirement.getDouble("quantity").doubleValue();

                // get an available supplier product, preferably the one with the smallest minimum quantity to order, followed by price
                String supplierKey =  partyId + "^" + productId;
                GenericValue supplierProduct = (GenericValue) suppliers.get(supplierKey);
                if (supplierProduct == null) {
                    conditions = UtilMisc.toList(
                            // TODO: it is possible to restrict to quantity > minimumOrderQuantity, but then the entire requirement must be skipped
                            new EntityExpr("partyId", EntityOperator.EQUALS, partyId),
                            new EntityExpr("productId", EntityOperator.EQUALS, productId),
                            EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate")
                            );
                    supplierProduct = EntityUtil.getFirst( delegator.findByAnd("SupplierProduct", conditions, UtilMisc.toList("minimumOrderQuantity", "lastPrice")) );
                    suppliers.put(supplierKey, supplierProduct);
                }

                // add our supplier product and cost of this line to the data
                if (supplierProduct != null) {
                    union.putAll(supplierProduct.getAllFields());
                    double lastPrice = supplierProduct.getDouble("lastPrice").doubleValue();
                    amountTotal += lastPrice * requiredQuantity;
                }

                // for good identification, get the UPCA type (UPC code)
                GenericValue gid = (GenericValue) gids.get(productId);
                if (gid == null) {
                    gid = delegator.findByPrimaryKey("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", requirement.get("productId")));
                    gids.put(productId, gid);
                }
                if (gid != null) union.put("idValue", gid.get("idValue"));

                // the ATP and QOH quantities
                if (UtilValidate.isNotEmpty(facilityId)) {
                    String inventoryKey = facilityId + "^" + productId;
                    Map inventory = (Map) inventories.get(inventoryKey);
                    if (inventory == null) {
                        inventory = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
                        if (ServiceUtil.isError(inventory)) {
                            return inventory;
                        }
                        inventories.put(inventoryKey, inventory);
                    }
                    if (inventory != null) {
                        union.put("qoh", inventory.get("quantityOnHandTotal"));
                        union.put("atp", inventory.get("availableToPromiseTotal"));
                    }
                }

                // how many of the products were sold (note this is for a fixed time period across all product stores)
                Double sold = (Double) productsSold.get(productId);
                if (sold == null) {
                    EntityCondition prodConditions = new EntityConditionList( UtilMisc.toList(
                                new EntityExpr("productId", EntityOperator.EQUALS, productId),
                                new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                                new EntityExpr("orderStatusId", EntityOperator.NOT_IN, UtilMisc.toList("ORDER_REJECTED", "ORDER_CANCELLED")),
                                new EntityExpr("orderItemStatusId", EntityOperator.NOT_IN, UtilMisc.toList("ITEM_REJECTED", "ITEM_CANCELLED")),
                                new EntityExpr("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, timePeriodStart)
                                ), EntityOperator.AND);
                    GenericValue count = EntityUtil.getFirst( delegator.findByCondition("OrderItemQuantityReportGroupByProduct", prodConditions, UtilMisc.toList("quantityOrdered"), null) );
                    if (count != null) {
                        sold = count.getDouble("quantityOrdered");
                        if (sold != null) productsSold.put(productId, sold);
                    }
                }
                if (sold != null) {
                    union.put("qtySold", sold);
                }

                // keep a running total of distinct products and quantity to order
                if (requirement.getDouble("quantity") == null) requirement.put("quantity", new Double("1")); // default quantity = 1
                quantity += requiredQuantity;
                products.add(productId);

                // add all the requirement fields last, to overwrite any conflicting fields
                union.putAll(requirement.getAllFields());
                requirements.add(union);
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("requirementsForSupplier", requirements);
            results.put("distinctProductCount", new Integer(products.size()));
            results.put("quantityTotal", new Double(quantity));
            results.put("amountTotal", new Double(amountTotal));
            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderServiceExceptionSeeLogs", locale));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderEntityExceptionSeeLogs", locale));
        }
    }
}
