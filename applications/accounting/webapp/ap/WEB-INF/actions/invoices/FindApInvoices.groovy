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

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

invoiceMap = dispatcher.runSync("performFind", [inputFields : parameters,
                                                entityName : "Invoice",
                                                orderBy : parameters.sortField]);

invoiceTypes = EntityUtil.getFieldListFromEntityList(delegator.findList("InvoiceType", EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "PURCHASE_INVOICE"), null, null, null, false), "invoiceTypeId", false);
invoices = [];
invoiceList = [];

if (invoiceMap.listIt != null)
    invoiceList = invoiceMap.listIt.getCompleteList();

if (invoiceList) {
    invoiceList.each { invoice ->
        if (invoiceTypes.contains(invoice.invoiceTypeId))
        	invoices.add(invoice);
    }
}

context.invoices = invoices;