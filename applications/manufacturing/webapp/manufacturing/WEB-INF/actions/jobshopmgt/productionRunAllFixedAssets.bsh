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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun;

import javolution.util.FastList;

productionRunId = request.getParameter("productionRunId");
if (UtilValidate.isEmpty(productionRunId)) {
    productionRunId = request.getParameter("workEffortId");
}
if (!UtilValidate.isEmpty(productionRunId)) {
    ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
    if (productionRun.exist()){
        List productionRunFixedAssetsData = FastList.newInstance();
        List productionRunRoutingTasks = productionRun.getProductionRunRoutingTasks();
        for (Iterator tasksIt = productionRunRoutingTasks.iterator(); tasksIt.hasNext();) {
            GenericValue prodRunTask = (GenericValue)tasksIt.next();
            List taskFixedAssets = prodRunTask.getRelated("WorkEffortFixedAssetAssign");
            productionRunFixedAssetsData.addAll(taskFixedAssets);
        }
	    context.put("productionRunFixedAssetsData", productionRunFixedAssetsData);
    }
}
