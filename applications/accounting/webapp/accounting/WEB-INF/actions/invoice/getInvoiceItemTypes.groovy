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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import javolution.util.FastList;

invoice = context.invoice;
if (!invoice) return;
glAccountOrganizationAndClassList = null;
if ("SALES_INVOICE".equals(invoice.invoiceTypeId)) {
    List parentTypes = FastList.newInstance();
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "INVOICE_ADJ"));
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "INVOICE_ITM_ADJ"));
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "INV_PROD_ITEM"));
    parentTypesCond = EntityCondition.makeCondition(parentTypes, EntityOperator.OR);
    invoiceItemTypes = delegator.findList("InvoiceItemType", parentTypesCond, null, ["parentTypeId", "invoiceItemTypeId"], null, false);
    glAccountOrganizationAndClassList = delegator.findByAnd("GlAccountOrganizationAndClass", [organizationPartyId : invoice.partyIdFrom]);
} else if ("PURCHASE_INVOICE".equals(invoice.invoiceTypeId)) {
    List parentTypes = FastList.newInstance();
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "PINVOICE_ADJ"));
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "PINVOICE_ITM_ADJ"));
    parentTypes.add(EntityCondition.makeCondition("parentTypeId", EntityOperator.EQUALS, "PINV_PROD_ITEM"));
    parentTypesCond = EntityCondition.makeCondition(parentTypes, EntityOperator.OR);
    invoiceItemTypes = delegator.findList("InvoiceItemType", parentTypesCond, null, ["parentTypeId", "invoiceItemTypeId"], null, false);
    glAccountOrganizationAndClassList = delegator.findByAnd("GlAccountOrganizationAndClass", [organizationPartyId : invoice.partyId]);
} else {
    map = delegator.findByAndCache("InvoiceItemTypeMap", [invoiceTypeId : invoice.invoiceTypeId]);
    invoiceItemTypes = EntityUtil.getRelated("InvoiceItemType", map);
}
context.invoiceItemTypes = invoiceItemTypes;

context.glAccountOrganizationAndClassList = glAccountOrganizationAndClassList;
