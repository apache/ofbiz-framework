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
 
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

invoiceId = parameters.invoiceId
userLoginId = userLogin.userLoginId
userLogin = from("UserLogin").where("userLoginId", userLoginId).queryOne();
userLoginNameView = from("PartyNameView").where("partyId", userLogin.get("partyId")).queryOne();
userLoginName = userLoginNameView.getString("firstName") + " " + userLoginNameView.getString("lastName")
dateFormatter = new java.text.SimpleDateFormat("dd MMMMM yyyy")

invoice = from("Invoice").where("invoiceId", invoiceId).queryOne();
if (invoice) {
    context.invoiceDescription = invoice.get("description")
    context.invoiceTypeDescription = invoice.getRelatedOne("InvoiceType", false).get("description")
    context.statusDescription = invoice.getRelatedOne("StatusItem", false).get("description")
    context.invoiceDate = invoice.get("invoiceDate")
    context.referenceNumber = invoice.get("referenceNumber")
}
partyId = null
if ("PURCHASE_INVOICE".equals(invoice.get("invoiceTypeId")) || "PURCHASE_INVOICE".equals(invoice.getRelatedOne("InvoiceType", false).get("parentTypeId"))) {
    partyId = invoice.get("partyIdFrom")
} else {
    partyId = invoice.get("partyId")
}
partyName = ""
partyNameView = from("PartyNameView").where("partyId", partyId).queryOne();
if (partyNameView.get("firstName")) {
    partyName += partyNameView.get("firstName") + " "
}
if (partyNameView.get("lastName")) {
    partyName += partyNameView.get("lastName") + " "
}
if (partyNameView.get("groupName")) {
    partyName += partyNameView.get("groupName") + " "
}

orderBy = UtilMisc.toList("acctgTransId", "acctgTransEntrySeqId")
conds = []
conds.add(EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId))
conds.add(EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, null))
invoiceAcctgTransAndEntries = delegator.findList("AcctgTransAndEntries", EntityCondition.makeCondition(conds), null, orderBy, null, false)
context.invoiceAcctgTransAndEntries = invoiceAcctgTransAndEntries
context.partyName = partyName
context.partyId = partyId