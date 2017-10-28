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
import org.apache.ofbiz.common.email.NotificationServices

orderId = request.getParameter("orderId") ?: parameters.get("orderId")
context.orderId = orderId

partyId = request.getParameter("partyId")
sendTo = request.getParameter("sendTo")

context.partyId = partyId
context.sendTo = sendTo

donePage = request.getParameter("DONE_PAGE") ?: "orderview"
context.donePage = donePage

// Provide the correct order confirmation ProductStoreEmailSetting, if one exists
orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
if (orderHeader.productStoreId) {
    productStoreEmailSetting = from("ProductStoreEmailSetting").where("productStoreId", orderHeader.productStoreId, "emailType", emailType).queryOne()
    if (productStoreEmailSetting) {
        context.productStoreEmailSetting = productStoreEmailSetting
    }
}

// set the baseUrl parameter, required by some email bodies
NotificationServices.setBaseUrl(delegator, context.webSiteId, context)
