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

import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;

productId = request.getParameter("productId") ?: "";

ShoppingCart shoppingCart = ShoppingCartEvents.getCartObject(request);

conditionList = [];

// make sure the look up is case insensitive
conditionList.add(EntityCondition.makeCondition(EntityFunction.UPPER(EntityFieldValue.makeFieldValue("productId")),
                                 EntityOperator.LIKE, productId.toUpperCase() + "%"));

conditionList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, shoppingCart.getOrderPartyId()));
conditionList.add(EntityCondition.makeCondition("currencyUomId", EntityOperator.EQUALS, shoppingCart.getCurrency()));
conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

selectedFields = ["productId", "supplierProductId", "supplierProductName", "lastPrice", "minimumOrderQuantity"] as Set;
selectedFields.add("availableFromDate");
selectedFields.add("availableThruDate");
productList = delegator.findList("SupplierProduct", conditions, selectedFields, ["productId"], null, false);

context.productList = EntityUtil.filterByDate(productList, nowTimestamp, "availableFromDate", "availableThruDate", true);
