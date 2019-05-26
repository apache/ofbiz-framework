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

import java.util.*
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil

facilityId = context.get("facilityId")
searchParameterString = "action=Y&facilityId=" + facilityId

EntityCondition whereConditions = EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId)
inventoryItems = select("productId").from("InventoryItem").where("facilityId", facilityId).orderBy("productId").queryList()
inventoryItemProducts = EntityUtil.getFieldListFromEntityList(inventoryItems, "productId", true)

inventoryAverageCosts = []
inventoryItemProducts.each { productId ->
    productFacility = from("ProductFacility").where("productId", productId, "facilityId", facilityId).queryOne()
    if (productFacility) {
        result = runService('calculateProductAverageCost', UtilMisc.toMap("productId": productId, "facilityId": facilityId, "userLogin": userLogin))
        totalQuantityOnHand = result.get("totalQuantityOnHand")

        totalInventoryCost = result.get("totalInventoryCost")
        productAverageCost = result.get("productAverageCost")
        currencyUomId = result.get("currencyUomId")
        if (!totalQuantityOnHand.equals(BigDecimal.ZERO)) {
            inventoryAverageCosts.add(UtilMisc.toMap("productId", productId, "totalQuantityOnHand", totalQuantityOnHand,
                    "productAverageCost", productAverageCost, "totalInventoryCost", totalInventoryCost, "currencyUomId", currencyUomId))
        }
    }
}

context.searchParameterString = searchParameterString
context.inventoryAverageCosts = inventoryAverageCosts
