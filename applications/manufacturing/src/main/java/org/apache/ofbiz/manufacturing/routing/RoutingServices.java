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
package org.apache.ofbiz.manufacturing.routing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.manufacturing.jobshopmgt.ProductionRun;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Routing related services
 *
 */
public class RoutingServices {

    public static final String module = RoutingServices.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    /**
     * Computes the estimated time needed to perform the task.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getEstimatedTaskTime(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        // The mandatory IN parameters
        String taskId = (String) context.get("taskId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        // The optional IN parameters
        String productId = (String) context.get("productId");
        String routingId = (String) context.get("routingId");

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }

        GenericValue task = null;
        try {
            task = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", taskId).queryOne();
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingErrorFindingTask", UtilMisc.toMap("taskId", taskId), locale));
        }
        // FIXME: the ProductionRun.getEstimatedTaskTime(...) method will be removed and
        // its logic will be implemented inside this method.
        long estimatedTaskTime = ProductionRun.getEstimatedTaskTime(task, quantity, productId, routingId, dispatcher);
        result.put("estimatedTaskTime", Long.valueOf(estimatedTaskTime));
        if (task != null && task.get("estimatedSetupMillis") != null) {
            result.put("setupTime", task.getBigDecimal("estimatedSetupMillis"));
        }
        if (task != null && task.get("estimatedMilliSeconds") != null) {
            result.put("taskUnitTime", task.getBigDecimal("estimatedMilliSeconds"));
        }
        return result;
    }
}
