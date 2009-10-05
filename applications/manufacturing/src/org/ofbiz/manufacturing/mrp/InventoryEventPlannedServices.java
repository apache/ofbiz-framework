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
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;


public class InventoryEventPlannedServices {

    public static final String module = InventoryEventPlannedServices.class.getName();

    /**
     *
     *  Create an MrpEvent.
     *  Make an update if a record exist with same key,  (adding the event quantity to the exiting record)
     *
     * @param ctx
     * @param context: a map containing the parameters used to create an MrpEvent
     * @return result: a map with service status
     */
    public static Map createMrpEvent(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        Map parameters = UtilMisc.toMap("mrpId", context.get("mrpId"),
                                        "productId", context.get("productId"),
                                        "eventDate", context.get("eventDate"),
                                        "mrpEventTypeId", context.get("mrpEventTypeId"));
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        GenericValue mrpEvent = null;
        try {
            createOrUpdateMrpEvent(parameters, quantity, (String)context.get("facilityId"), (String)context.get("eventName"), false, delegator);
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : findByPrimaryKey(\"MrpEvent\", parameters =)"+parameters, module);
            return ServiceUtil.returnError("Problem, on database access, for more detail look at the log");
        }
        return ServiceUtil.returnSuccess();
    }

    public static void createOrUpdateMrpEvent(Map mrpEventKeyMap, BigDecimal newQuantity, String facilityId, String eventName, boolean isLate, Delegator delegator) throws GenericEntityException {
        GenericValue mrpEvent = null;
        mrpEvent = delegator.findByPrimaryKey("MrpEvent", mrpEventKeyMap);
        if (mrpEvent == null) {
            mrpEvent = delegator.makeValue("MrpEvent", mrpEventKeyMap);
            mrpEvent.put("quantity", newQuantity.doubleValue());
            mrpEvent.put("eventName", eventName);
            mrpEvent.put("facilityId", facilityId);
            mrpEvent.put("isLate", (isLate? "Y": "N"));
            mrpEvent.create();
        } else {
            BigDecimal qties = newQuantity.add((BigDecimal)mrpEvent.getBigDecimal("quantity"));
            mrpEvent.put("quantity", qties.doubleValue());
            if (!UtilValidate.isEmpty(eventName)) {
                String existingEventName = mrpEvent.getString("eventName");
                mrpEvent.put("eventName", (UtilValidate.isEmpty(existingEventName)? eventName: existingEventName + ", " + eventName));
            }
            if (isLate) {
                mrpEvent.put("isLate", "Y");
            }
            mrpEvent.store();
        }
    }
}
