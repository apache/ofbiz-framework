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
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityUtil

/**
 * Get the product's routing and routing tasks
 * **/

def getProductRouting() {
    Map result = success()
    def routing
    List tasks = []
    filterDate = parameters.applicableDate ?: UtilDateTime.nowTimestamp()
    // If a workEffortId has been passed, use it to look up the desired routing
    exprBldr = new EntityConditionBuilder()

    def condition = exprBldr.AND() {
        EQUALS(productId: parameters.productId)
        EQUALS(workEffortGoodStdTypeId: "ROU_PROD_TEMPLATE")
        if (parameters.workEffortId) {
            EQUALS(workEffortId: parameters.workEffortId)
        }
    }
    routings = from("WorkEffortGoodStandard").where(condition).filterByDate(filterDate).queryList()
    routingGS = EntityUtil.getFirst(routings)
    // If the routing is not associated with our product and it's a variant, then check to see if it's virtual product has the routing
    if (!routingGS) {
        virtualProductAssocList = from("ProductAssoc").where("productIdTo", parameters.productId, "productAssocTypeId", "PRODUCT_VARIANT").filterByDate().queryList()
        virtualProductAssoc = EntityUtil.getFirst(virtualProductAssocList)
        if (virtualProductAssoc) {
            routings = from("WorkEffortGoodStandard").where("productId", virtualProductAssoc.productId, "workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE", "workEffortId", parameters.workEffortId).filterByDate(filterDate).queryList()
            outingGS = EntityUtil.getFirst(routings)
        }
    }

    if (routingGS) {
        routing = from("WorkEffort").where("workEffortId", routingGS.workEffortId).queryOne()
        // The default routing is used when no explicit routing is associated to the product and the ignoreDefaultRouting is not equals to Y
    } else {
        if (!parameters.ignoreDefaultRouting || parameters.ignoreDefaultRouting == "N") {
            routing = from("WorkEffort").where("workEffortId", "DEFAULT_ROUTING").queryOne()
        }
    }

    if (routing) {
        tasks = from("WorkEffortAssoc").where("workEffortAssocTypeId", "ROUTING_COMPONENT", "workEffortIdFrom", routing.workEffortId).filterByDate().queryList()
    }

    result.tasks = tasks
    result.routing = routing
    return result
}

/**
 * Get the routing task assocs of a given routing
 * **/

def getRoutingTaskAssocs() {
    Map result = success()
    List routingTaskAssocs = []
    routingTaskAssocs = from("WorkEffortAssoc").where("workEffortIdFrom", parameters.workEffortId, "workEffortAssocTypeId", "ROUTING_COMPONENT").orderBy("sequenceNum").filterByDate().queryList()
    result.routingTaskAssocs = routingTaskAssocs
    return result
}