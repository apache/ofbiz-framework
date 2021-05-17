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
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class InventoryEventPlannedServices {

    private static final String MODULE = InventoryEventPlannedServices.class.getName();
    private static final String RESOURCE = "ManufacturingUiLabels";

    /**
     *  Create an MrpEvent.
     *  Make an update if a record exist with same key, (adding the event quantity to the exiting record)
     * @param ctx the dispatch context
     * @param context a map containing the parameters used to create an MrpEvent
     * @return result a map with service status
     */
    public static Map<String, Object> createMrpEvent(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> parameters = UtilMisc.<String, Object>toMap("mrpId", context.get("mrpId"),
                                        "productId", context.get("productId"),
                                        "eventDate", context.get("eventDate"),
                                        "mrpEventTypeId", context.get("mrpEventTypeId"));
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        try {
            createOrUpdateMrpEvent(parameters, quantity, (String) context.get("facilityId"), (String) context.get("eventName"), false, delegator);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error : findOne(\"MrpEvent\", parameters =)" + parameters, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ManufacturingMrpCreateOrUpdateEvent",
                    UtilMisc.toMap("parameters", parameters), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static void createOrUpdateMrpEvent(Map<String, Object> mrpEventKeyMap, BigDecimal newQuantity, String facilityId,
            String eventName, boolean isLate, Delegator delegator) throws GenericEntityException {
        GenericValue mrpEvent = null;
        mrpEvent = EntityQuery.use(delegator).from("MrpEvent").where(mrpEventKeyMap).queryOne();
        if (mrpEvent == null) {
            mrpEvent = delegator.makeValue("MrpEvent", mrpEventKeyMap);
            mrpEvent.put("quantity", newQuantity.doubleValue());
            mrpEvent.put("eventName", eventName);
            mrpEvent.put("facilityId", facilityId);
            mrpEvent.put("isLate", (isLate ? "Y" : "N"));
            mrpEvent.create();
        } else {
            BigDecimal qties = newQuantity.add(mrpEvent.getBigDecimal("quantity"));
            mrpEvent.put("quantity", qties.doubleValue());
            if (UtilValidate.isNotEmpty(eventName)) {
                String existingEventName = mrpEvent.getString("eventName");
                mrpEvent.put("eventName", (UtilValidate.isEmpty(existingEventName) ? eventName : existingEventName + ", " + eventName));
            }
            if (isLate) {
                mrpEvent.put("isLate", "Y");
            }
            mrpEvent.store();
        }
    }
}
