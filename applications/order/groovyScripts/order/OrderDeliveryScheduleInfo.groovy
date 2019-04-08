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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*

orderId = request.getParameter("orderId")
orderTypeId = null
orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
if (orderHeader) {
    orderTypeId = orderHeader.orderTypeId
}

//Determine whether a schedule has already been defined for this PO
schedule = from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId", "_NA_").queryOne()

// Determine whether the current user can VIEW the order
checkResult = runService('checkSupplierRelatedOrderPermission', [orderId : orderId, userLogin : session.getAttribute("userLogin"), checkAction : "VIEW"])
hasSupplierRelatedPermissionStr = checkResult.hasSupplierRelatedPermission

// Determine what the reuslt is, no result is FALSE
hasSupplierRelatedPermission = "true".equals(hasSupplierRelatedPermissionStr)

context.orderId = orderId
context.orderItemSeqId = "_NA_"
context.orderTypeId = orderTypeId
context.orderHeader = orderHeader
context.schedule = schedule
context.hasPermission = hasSupplierRelatedPermission