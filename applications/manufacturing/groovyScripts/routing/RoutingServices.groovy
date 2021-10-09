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

/**
 * "Get the product's routing and routing tasks
 * @return
 */
def getProductRouting() {
    Map result = success()

    // If applicableDate has been passed use the value with all filter-by-date calls
    Timestamp filterDate = parameters.applicableDate ?: UtilDateTime.nowTimestamp()

    GenericValue routingGS = null
    GenericValue routing = null
    List tasks = null
    Map lookupRouting = [productId: parameters.productId, workEffortGoodStdTypeId: "ROU_PROD_TEMPLATE"]

    // If a workEffortId has been passed, use it to look up the desired routing
    if (parameters.workEffortId) {
        lookupRouting.workEffortId = parameters.workEffortId
        routingGS = from("WorkEffortGoodStandard")
                .where(lookupRouting)
                .filterByDate(filterDate)
                .queryFirst()

        // If the routing is not associated with our product and it's a variant, then check to see if it's virtual product has the routing
        if (!routingGS) {
            GenericValue virtualProductAssoc = from("ProductAssoc")
                    .where(productIdTo: parameters.productId,
                            productAssocTypeId: "PRODUCT_VARIANT")
                    .filterByDate(filterDate)
                    .queryFirst()
            if (virtualProductAssoc) {
                lookupRouting.productId = virtualProductAssoc.productId
                // Consider the validity against a date passed as (optional) parameter
                routingGS = from("WorkEffortGoodStandard")
                        .where(lookupRouting)
                        .filterByDate(filterDate)
                        .queryFirst()
            }
        }
    }

    // No workEffortId has been passed, so retrieve the first routing found for this product
    else {
        // Consider the validity against a date passed as (optional) parameter
        // TODO: we should consider the quantity to select the best routing
        routingGS = from("WorkEffortGoodStandard")
                .where(lookupRouting)
                .filterByDate(filterDate)
                .queryFirst()

        // If there are no routings associated with our product and it's a variant, then check to see if it's virtual product has a routing
        if (!routingGS) {
            GenericValue virtualProductAssoc = from("ProductAssoc")
                    .where(productIdTo: parameters.productId,
                            productAssocTypeId: "PRODUCT_VARIANT")
                    .filterByDate(filterDate)
                    .queryFirst()
            if (virtualProductAssoc) {
                lookupRouting.productId = virtualProductAssoc.productId
                lookupRouting.workEffortGoodStdTypeId = "ROU_PROD_TEMPLATE"
                // Consider the validity against a date passed as (optional) parameter
                // TODO: we should consider the quantity to select the best routing
                routingGS = from("WorkEffortGoodStandard")
                        .where(lookupRouting)
                        .filterByDate(filterDate)
                        .queryFirst()
            }
        }
    }
    if (routingGS) {
        routing = from("WorkEffort").where(workEffortId: routingGS.workEffortId).queryOne()
    } else {
        // The default routing is used when no explicit routing is associated to the product and the ignoreDefaultRouting is not equals to Y
        if (!parameters.ignoreDefaultRouting || parameters.ignoreDefaultRouting == "N") {
            routing = from("WorkEffort").where(workEffortId: "DEFAULT_ROUTING").queryOne()
        }
    }
    if (routing) {
        tasks = from("WorkEffortAssoc")
                .where(workEffortIdFrom: routing.workEffortId,
                        workEffortAssocTypeId: "ROUTING_COMPONENT")
                .orderBy("sequenceNum")
                .filterByDate()
                .queryList()
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
    result.routingTaskAssocs = from("WorkEffortAssoc")
            .where(workEffortIdFrom: parameters.workEffortId,
                    workEffortAssocTypeId: "ROUTING_COMPONENT")
            .orderBy("sequenceNum")
            .filterByDate()
            .queryList()
    return result
}
