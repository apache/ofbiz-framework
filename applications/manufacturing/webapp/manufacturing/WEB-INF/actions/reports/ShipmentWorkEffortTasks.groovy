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

import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.order.order.*;
import org.ofbiz.content.report.*;

delegator = request.getAttribute("delegator");
dispatcher = request.getAttribute("dispatcher");
userLogin = request.getSession().getAttribute("userLogin");

shipmentId = request.getParameter("shipmentId");
shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));

context.put("shipmentIdPar", shipment.getString("shipmentId"));
context.put("date", new Date());
Double fixedAssetTime = new Double(0);

if (shipment != null) {
    shipmentPlans = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId));
    shipmentPlansIt = shipmentPlans.iterator();
    records = new ArrayList();

    while(shipmentPlansIt.hasNext()) {
        shipmentPlan = shipmentPlansIt.next();
        productionRuns = delegator.findByAnd("WorkOrderItemFulfillment", UtilMisc.toMap("orderId", shipmentPlan.getString("orderId"), "orderItemSeqId", shipmentPlan.getString("orderItemSeqId")), UtilMisc.toList("workEffortId")); // TODO: add shipmentId
        if (productionRuns != null && productionRuns.size() > 0) {
            productionRunsIt = productionRuns.iterator();
            while (productionRunsIt.hasNext()) {
                productionRun = productionRunsIt.next();
                productionRunProduct = null;
                productionRunProducts = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", productionRun.getString("workEffortId"), "workEffortGoodStdTypeId", "PRUN_PROD_DELIV", "statusId", "WEGS_CREATED"));
                if (productionRunProducts != null && productionRunProducts.size() > 0) {
                    //productionRunProduct = ((GenericValue)productionRunProducts.get(0)).getString("productId");
                    productionRunProduct = ((GenericValue)productionRunProducts.get(0)).getRelatedOne("Product");
                }
                tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRun.getString("workEffortId"), "workEffortTypeId", "PROD_ORDER_TASK"));
                tasksIt = tasks.iterator();
                while (tasksIt.hasNext()) {
                    task = tasksIt.next();
                    record = new HashMap();
                    record.put("productId", productionRunProduct.getString("productId"));
                    record.put("productName", productionRunProduct.getString("internalName"));
                    record.put("fixedAssetId", task.getString("fixedAssetId"));
                    record.put("priority", task.getLong("priority"));
                    record.put("workEffortId", productionRun.getString("workEffortId"));
                    record.put("taskId", task.getString("workEffortId"));
                    record.put("taskName", task.getString("workEffortName"));
                    record.put("taskDescription", task.getString("description"));
                    record.put("taskEstimatedTime", task.getDouble("estimatedMilliSeconds"));
                    record.put("taskEstimatedSetup", task.getDouble("estimatedSetupMillis"));
                    records.add(record);
                    fixedAssetTime = fixedAssetTime + task.getDouble("estimatedMilliSeconds");
                }
            }
        }
    }
    context.put("fixedAssetTime", fixedAssetTime);
    context.put("records", records);
    
    // check permission
    hasPermission = false;
    if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
        hasPermission = true;
    } 
    context.put("hasPermission", hasPermission);
}

return "success";
