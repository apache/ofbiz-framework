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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.widget.html.HtmlFormWrapper;


productionRunId = request.getParameter("productionRunId");
if (UtilValidate.isEmpty(productionRunId)) {
    productionRunId = request.getParameter("workEffortId");
}

List taskInfos = new ArrayList();
List tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", productionRunId, "workEffortTypeId", "PROD_ORDER_TASK"), UtilMisc.toList("workEffortId"));
Iterator tasksIt = tasks.iterator();
while (tasksIt.hasNext()) {
    GenericValue task = (GenericValue)tasksIt.next();
    List records = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", task.getString("workEffortId")));
    HtmlFormWrapper taskForm = new HtmlFormWrapper("component://manufacturing/webapp/manufacturing/jobshopmgt/ProductionRunForms.xml", "ProductionRunTaskComponents", request, response);
    taskForm.putInContext("records", records);
    taskInfos.add(UtilMisc.toMap("task", task, "taskForm", taskForm));
}
context.put("taskInfos", taskInfos);
