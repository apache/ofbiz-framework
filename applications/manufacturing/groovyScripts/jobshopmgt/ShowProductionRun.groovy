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

// The only required parameter is "productionRunId".

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate

delegator = request.getAttribute("delegator")

productionRunId = request.getParameter("productionRunId")
if (!productionRunId) {
    productionRunId = request.getParameter("workEffortId")
}
if (productionRunId) {

    GenericValue productionRun = from("WorkEffort").where("workEffortId", productionRunId).queryOne();
    if (productionRun) {
        // If this is a task, get the parent production run
        if (productionRun.getString("workEffortTypeId") != null && "PROD_ORDER_TASK".equals(productionRun.getString("workEffortTypeId"))) {
            productionRun = from("WorkEffort").where("workEffortId", productionRun.getString("workEffortParentId")).queryOne();
        }
    }

    if (!productionRun) {
        return "error"
    }
    if ("PRUN_CREATED".equals(productionRun.getString("currentStatusId")) ||
            "PRUN_SCHEDULED".equals(productionRun.getString("currentStatusId")) ||
            "PRUN_CANCELLED".equals(productionRun.getString("currentStatusId"))) {
        return "docs_not_printed"
    } else {
        return "docs_printed"
    }
}

return "error"
