/*
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


import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

projectId = parameters.projectId
entryExprs =
    EntityCondition.makeCondition([
        EntityCondition.makeCondition("projectId", EntityOperator.EQUALS, projectId),
        EntityCondition.makeCondition("invoiceId", EntityOperator.NOT_EQUAL, null),
        ], EntityOperator.AND)
orderBy = ["-fromDate"]
// check if latest invoice generated is still in process so allow re-generation to correct errors
entryIterator = from("ProjectPhaseTaskAndTimeEntryTimeSheet")
                    .where(EntityCondition.makeCondition([
                                EntityCondition.makeCondition("projectId", EntityOperator.EQUALS, projectId),
                                EntityCondition.makeCondition("invoiceId", EntityOperator.NOT_EQUAL, null),
                            ], EntityOperator.AND))
                    .orderBy("-fromDate")
                    .queryIterator()
while (entryItem = entryIterator.next()) {
    invoice = entryItem.getRelatedOne("Invoice", false)
    if (invoice.getString("statusId").equals("INVOICE_IN_PROCESS")) {
        context.partyIdFrom = invoice.partyIdFrom
        context.partyId = invoice.partyId
        context.invoiceId = invoice.invoiceId
        break
        }
    }
entryIterator.close()
//start of this month
context.thruDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp())
