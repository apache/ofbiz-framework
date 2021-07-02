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

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery

/**
 * "Get the product's routing and routing tasks
 * @return
 */
def getProductRouting() {
    Map result = success()
    Timestamp filterDate = null
    GenericValue routingGS = null
    GenericValue routing = null
    List tasks = null
    Map lookupRouting = [productId: parameters.productId, workEffortGoodStdTypeId: "ROU_PROD_TEMPLATE"]
    // If applicableDate has been passed use the value with all filter-by-date calls
    if (parameters.applicableDate) {
        filterDate = parameters.applicableDate
    }
    else {
        filterDate = UtilDateTime.nowTimestamp()
    }

    // If a workEffortId has been passed, use it to look up the desired routing
    if (parameters.workEffortId) {

        lookupRouting.workEffortId = parameters.workEffortId
        routingGS = from("WorkEffortGoodStandard").where(lookupRouting).filterByDate(filterDate).queryFirst()

        // If the routing is not associated with our product and it's a variant, then check to see if it's virtual product has the routing
        if (!routingGS) {
            GenericValue virtualProductAssoc = from("ProductAssoc").where(productIdTo: parameters.productId, productAssocTypeId: "PRODUCT_VARIANT").filterByDate().queryFirst()
            if (virtualProductAssoc) {
                lookupRouting.productId = virtualProductAssoc.productId
                // Consider the validity against a date passed as (optional) parameter
                routingGS = from("WorkEffortGoodStandard").where(lookupRouting).filterByDate(filterDate).queryFirst()
            }
        }
    }
    // No workEffortId has been passed, so retrieve the first routing found for this product
    else {
        // Consider the validity against a date passed as (optional) parameter
        // TODO: we should consider the quantity to select the best routing
        routingGS = from("WorkEffortGoodStandard").where(lookupRouting).filterByDate(filterDate).queryFirst()
        // If there are no routings associated with our product and it's a variant, then check to see if it's virtual product has a routing
        if (!routingGS) {
            GenericValue virtualProductAssoc = from("ProductAssoc").where(productIdTo: parameters.productId, productAssocTypeId: "PRODUCT_VARIANT").filterByDate(filterDate).queryFirst()
            if (virtualProductAssoc) {
                lookupRouting.productId = virtualProductAssoc.productId
                lookupRouting.workEffortGoodStdTypeId = "ROU_PROD_TEMPLATE"
                // Consider the validity against a date passed as (optional) parameter
                // TODO: we should consider the quantity to select the best routing
                routingGS = from("WorkEffortGoodStandard").where(lookupRouting).filterByDate(filterDate).queryFirst()
            }
        }
    }
    if (routingGS) {
        lookupRouting.clear()
        lookupRouting.workEffortId = routingGS.workEffortId
        routing = from("WorkEffort").where(lookupRouting).queryOne()
    }
    else {
        // The default routing is used when no explicit routing is associated to the product and the ignoreDefaultRouting is not equals to Y
        if (!parameters.ignoreDefaultRouting || parameters.ignoreDefaultRouting.equals("N")) {
            lookupRouting.clear()
            lookupRouting.workEffortId = "DEFAULT_ROUTING"
            routing = from("WorkEffort").where(lookupRouting).queryOne()
        }
    }
    if (routing) {
        Map lookupTasks = [workEffortIdFrom: routing.workEffortId, workEffortAssocTypeId: "ROUTING_COMPONENT"]
        List tasksOrder = ["sequenceNum"]
        tasks = from("WorkEffortAssoc").where(lookupTasks).orderBy(tasksOrder).filterByDate().queryList()
    }
    result.routing = routing
    result.tasks = tasks
    return result
}

/**
 * Get the routing task assocs of a given routing
 * @return
 */
def getRoutingTaskAssocs() {
    Map result = success()
    Map lookupTasks = [workEffortIdFrom: parameters.workEffortId, workEffortAssocTypeId: "ROUTING_COMPONENT"]
    List tasksOrder = ["sequenceNum"]
    List routingTaskAssocs = from("WorkEffortAssoc").where(lookupTasks).orderBy(tasksOrder).filterByDate().queryList()
    result.routingTaskAssocs = routingTaskAssocs

    return result
}
