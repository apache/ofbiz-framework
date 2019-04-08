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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.base.util.*

orderId = request.getParameter("orderId")
paymentMethodTypes = from("PaymentMethodType").where(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.NOT_EQUAL, "EXT_OFFLINE")).queryList()
context.paymentMethodTypes = paymentMethodTypes

workEffortId = request.getParameter("workEffortId")
partyId = request.getParameter("partyId")
roleTypeId = request.getParameter("roleTypeId")
fromDate = request.getParameter("fromDate")

donePage = request.getParameter("DONE_PAGE") ?: "orderview?orderId=" + orderId
if (workEffortId)
    donePage += "&workEffortId=" + workEffortId
if (partyId)
    donePage += "&partyId=" + partyId
if (roleTypeId)
    donePage += "&roleTypeId=" + roleTypeId
if (fromDate)
    donePage += "&fromDate=" + fromDate
context.donePage = donePage


