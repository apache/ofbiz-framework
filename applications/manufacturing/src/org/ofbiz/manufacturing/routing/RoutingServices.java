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
package org.ofbiz.manufacturing.routing;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;

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
    public static Map getEstimatedTaskTime(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        
        // The mandatory IN parameters
        String taskId = (String) context.get("taskId");
        Double quantity = (Double) context.get("quantity");
        // The optional IN parameters
        String productId = (String) context.get("productId");
        String routingId = (String) context.get("routingId");
        
        if (quantity == null) {
            quantity = new Double(1);
        }

        GenericValue task = null;
        try {
            task = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", taskId));
        } catch(GenericEntityException gee) {
            ServiceUtil.returnError("Error finding routing task with id: " + taskId);
        }
        // FIXME: the ProductionRun.getEstimatedTaskTime(...) method will be removed and
        // its logic will be implemented inside this method.
        long estimatedTaskTime = ProductionRun.getEstimatedTaskTime(task, quantity, productId, routingId, dispatcher);
        result.put("estimatedTaskTime", new Long(estimatedTaskTime));
        if (task != null && task.get("estimatedSetupMillis") != null) {
            result.put("setupTime", task.getDouble("estimatedSetupMillis"));
        }
        if (task != null && task.get("estimatedMilliSeconds") != null) {
            result.put("taskUnitTime", task.getDouble("estimatedMilliSeconds"));
        }
        return result;
    }
}
