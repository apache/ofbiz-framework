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

import org.ofbiz.widget.renderer.html.HtmlFormWrapper;

productionRunId = parameters.productionRunId ?: parameters.workEffortId;

taskInfos = [];
tasks = from("WorkEffort").where("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK").orderBy("workEffortId").queryList();
tasks.each { task ->
    records = from("InventoryItemDetail").where("workEffortId", task.workEffortId).queryList();
    HtmlFormWrapper taskForm = new HtmlFormWrapper("component://manufacturing/widget/manufacturing/ProductionRunForms.xml", "ProductionRunTaskActualComponents", request, response);
    taskForm.putInContext("records", records);
    taskInfos.add([task : task, taskForm : taskForm]);
}
context.taskInfos = taskInfos;
