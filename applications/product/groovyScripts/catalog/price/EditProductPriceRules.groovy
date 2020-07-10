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

import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.base.util.UtilMisc

context.inputParamEnums = from("Enumeration").where("enumTypeId", "PROD_PRICE_IN_PARAM").orderBy("sequenceId").cache(true).queryList()
context.condOperEnums = from("Enumeration").where("enumTypeId", "PROD_PRICE_COND").orderBy("sequenceId").cache(true).queryList()
context.productPriceActionTypes = from("ProductPriceActionType").orderBy("description").cache(true).queryList()

String priceRuleId = request.getParameter("productPriceRuleId")

if (!priceRuleId) {
    priceRuleId = parameters.get("productPriceRuleId")
}

if (priceRuleId) {
    productPriceRules = []
    productPriceRules.add(from("ProductPriceRule").where("productPriceRuleId", priceRuleId).queryOne())
    productPriceConds = productPriceRules[0].getRelated("ProductPriceCond", null, ["productPriceCondSeqId"], true)
    productPriceActions = productPriceRules[0].getRelated("ProductPriceAction", null, ["productPriceActionSeqId"], true)
    
    productPriceCondAdd = []
    productPriceCondAdd.add(makeValue("ProductPriceCond"))
    productPriceCondAdd[0].productPriceRuleId = priceRuleId
    productPriceCondAdd[0].inputParamEnumId = context.inputParamEnums[0].enumId
    productPriceCondAdd[0].operatorEnumId = context.condOperEnums[0].enumId
    
    productPriceActionAdd = []
    productPriceActionAdd.add(makeValue("ProductPriceAction"))
    productPriceActionAdd[0].productPriceRuleId = priceRuleId
    productPriceActionAdd[0].productPriceActionTypeId = context.productPriceActionTypes[0].productPriceActionTypeId
    productPriceActionAdd[0].amount = BigDecimal.ZERO
    
    context.productPriceRules = productPriceRules
    context.productPriceConds = productPriceConds
    context.productPriceActions = productPriceActions
    context.productPriceCondAdd = productPriceCondAdd
    context.productPriceActionAdd = productPriceActionAdd
    
} else {
    context.productPriceRules = null
    context.productPriceConds = null
    context.productPriceActions = null
    context.productPriceCondsAdd = null
    context.productPriceActionsAdd = null
}
