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

import org.apache.ofbiz.entity.condition.EntityExpr
import org.apache.ofbiz.entity.condition.EntityFunction
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.condition.EntityFieldValue
import org.apache.ofbiz.entity.condition.EntityConditionList
import org.apache.ofbiz.entity.condition.EntityCondition

productId = request.getParameter("productId") ?: ""

conditionList = []
orConditionList = []
mainConditionList = []

if (productId) {
    //make sure the look up is case insensitive
    conditionList.add(EntityCondition.makeCondition(EntityFunction.UPPER(EntityFieldValue.makeFieldValue("productId")),
            EntityOperator.LIKE, productId.toUpperCase() + "%"))
}

// do not include configurable products
conditionList.add(EntityCondition.makeCondition("productTypeId", EntityOperator.NOT_EQUAL, "AGGREGATED"))
conditionList.add(EntityCondition.makeCondition("productTypeId", EntityOperator.NOT_EQUAL, "AGGREGATED_SERVICE"))

// no virtual products: note that isVirtual could be null,
// we consider those products to be non-virtual and hence addable to the order in bulk
orConditionList.add(EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, "N"))
orConditionList.add(EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, null))

orConditions = EntityCondition.makeCondition(orConditionList, EntityOperator.OR)
conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND)

mainConditionList.add(orConditions)
mainConditionList.add(conditions)

context.productList = select("productId", "brandName", "internalName").from("Product").where(mainConditionList).orderBy("productId").queryList()
