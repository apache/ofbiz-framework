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
package org.ofbiz.manufacturing.jobshopmgt;

import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;


/**
 * Helper for Production Run maintenance
 *
 */
public class ProductionRunHelper {

    public static final String module = ProductionRunHelper.class.getName();
    public static final String resource = "ManufacturingUiLabels";


    /**
     * Get a Production Run.
     *  <li> check if routing - product link exist
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productionRunId
     * @return Map with the result of the service, the output parameters are
     * <li> the productionRun
     * <li> the productionRunProduct
     */
    public static Map<String, Object> getProductionRun(Delegator delegator, String productionRunId) {
        Map<String, Object> result = FastMap.newInstance();
        
        try {
            if (productionRunId != null) {
                GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
                if (productionRun != null) {
                    List<GenericValue> productionRunProducts = productionRun.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV"),null);
                    GenericValue productionRunProduct = EntityUtil.getFirst(productionRunProducts);
                    GenericValue productProduced = productionRunProduct.getRelatedOneCache("Product");
                    List<GenericValue> productionRunComponents = productionRun.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"),null);
                    List<GenericValue> productionRunRoutingTasks = productionRun.getRelated("FromWorkEffortAssoc",UtilMisc.toMap("workEffortTypeId","PROD_ORDER_TASK"),null);
                    result.put("productionRunProduct", productionRunProduct);
                    result.put("productProduced", productProduced);
                    result.put("productionRunComponents", productionRunComponents);
                    result.put("productionRunRoutingTasks", productionRunRoutingTasks);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }
        return result;
    }

    public static boolean hasTask(Delegator delegator, String taskName, String workEffortId) throws GenericEntityException {
        List<GenericValue> tasks = delegator.findByAnd("WorkEffort", 
                UtilMisc.toMap("workEffortParentId", workEffortId,
                        "workEffortTypeId", "PROD_ORDER_TASK",
                        "workEffortName", taskName));
        return (UtilValidate.isNotEmpty(tasks));
    }

    public static void getLinkedProductionRuns(Delegator delegator, LocalDispatcher dispatcher, String productionRunId, List<ProductionRun> productionRuns)  throws GenericEntityException {
        productionRuns.add(new ProductionRun(productionRunId, delegator, dispatcher));
        List<GenericValue> linkedWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", 
                UtilMisc.toMap("workEffortIdTo", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")));
        for (int i = 0; i < linkedWorkEfforts.size(); i++) {
            GenericValue link = linkedWorkEfforts.get(i);
            getLinkedProductionRuns(delegator, dispatcher, link.getString("workEffortIdFrom"), productionRuns);
        }
    }

    public static String getRootProductionRun(Delegator delegator, String productionRunId)  throws GenericEntityException {
        List<GenericValue> linkedWorkEfforts = delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY"));
        GenericValue linkedWorkEffort = EntityUtil.getFirst(linkedWorkEfforts);
        if (linkedWorkEffort != null) {
            productionRunId = getRootProductionRun(delegator, linkedWorkEffort.getString("workEffortIdTo"));
        }
        return productionRunId;
    }

}
