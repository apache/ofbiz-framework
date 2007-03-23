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

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * InventoryEventPlannedServices - InventoryEventPlanned related Services
 *
 */
public class InventoryEventPlannedServices {
    
    public static final String module = InventoryEventPlannedServices.class.getName();
   
    /**
     *
     *  Create an InventoryEventPlanned.
     *  Make an update if a record exist with same key,  (adding the eventQuantity to the exiting record)
     *
     * @param ctx
     * @param context: a map containing the parameters used to create an InventoryEventPlanned (see the servcie definition)
     * @return result: a map with service status
     */
    public static Map createInventoryEventPlanned(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        Map parameters = UtilMisc.toMap("productId", context.get("productId"),
                                        "eventDate", context.get("eventDate"),
                                        "inventoryEventPlanTypeId", context.get("inventoryEventPlanTypeId"));
        Double quantity = (Double)context.get("eventQuantity");
        GenericValue inventoryEventPlanned = null;
        try {
            createOrUpdateInventoryEventPlanned(parameters, quantity, (String)context.get("facilityId"), (String)context.get("eventName"), false, delegator);
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : delegator.findByPrimaryKey(\"InventoryEventPlanned\", parameters =)"+parameters, module);
            return ServiceUtil.returnError("Problem, on database access, for more detail look at the log");
        }
        return ServiceUtil.returnSuccess();
    }

    public static void createOrUpdateInventoryEventPlanned(Map inventoryEventPlannedKeyMap, Double newQuantity, String facilityId, String eventName, boolean isLate, GenericDelegator delegator) throws GenericEntityException {
        GenericValue inventoryEventPlanned = null;
        inventoryEventPlanned = delegator.findByPrimaryKey("InventoryEventPlanned", inventoryEventPlannedKeyMap);
        if (inventoryEventPlanned == null) {
            inventoryEventPlanned = delegator.makeValue("InventoryEventPlanned", inventoryEventPlannedKeyMap);
            inventoryEventPlanned.put("eventQuantity", newQuantity);
            inventoryEventPlanned.put("eventName", eventName);
            inventoryEventPlanned.put("facilityId", facilityId);
            inventoryEventPlanned.put("isLate", (isLate? "Y": "N"));
            inventoryEventPlanned.create();
        } else {
            double qties = newQuantity.doubleValue() + ((Double)inventoryEventPlanned.get("eventQuantity")).doubleValue();
            inventoryEventPlanned.put("eventQuantity", new Double(qties));
            if (!UtilValidate.isEmpty(eventName)) {
                String existingEventName = inventoryEventPlanned.getString("eventName");
                inventoryEventPlanned.put("eventName", (UtilValidate.isEmpty(existingEventName)? eventName: existingEventName + ", " + eventName));
            }
            if (isLate) {
                inventoryEventPlanned.put("isLate", "Y");
            }
            inventoryEventPlanned.store();
        }
    }
}
