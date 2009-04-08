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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.store.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import javolution.util.FastList;

cart = session.getAttribute("shoppingCart");
party = userLogin.getRelatedOne("Party");
partyId = party.partyId;
productStoreId = ProductStoreWorker.getProductStoreId(request);

productStoreId = cart.getProductStoreId();
if (productStoreId) {
    productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
    payToPartyId = productStore.payToPartyId;
}

shippingContactMechId = request.getParameter("shipping_contact_mech_id");
for (shipGroupIndex = 0; shipGroupIndex < cart.getShipGroupSize(); shipGroupIndex++) {
    supplierPartyId = cart.getSupplierPartyId(shipGroupIndex);
    context[shipGroupIndex + "_supplierPartyId"] = supplierPartyId;
}
exprs = FastList.newInstance();
exprs.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, payToPartyId));
exprs.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
agreements = delegator.findList("Agreement", EntityCondition.makeCondition(exprs, EntityOperator.AND), null, null, null, true);
agreements = EntityUtil.filterByDate(agreements);
context.agreements = agreements;

context.shoppingCart = cart;
context.userLogin = userLogin;
context.productStoreId = productStoreId;
context.shippingContactMechList = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
