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

import org.ofbiz.base.util.*;

partyId = parameters.partyId ? parameters.partyId : userLogin.partyId ;

if (partyId) {
    // get the system user
    system = from("UserLogin").where("userLoginId", "system").queryOne();

    monthsToInclude = 12;

    Map result = runService('getOrderedSummaryInformation', ["partyId": partyId, "roleTypeId": "PLACING_CUSTOMER", "orderTypeId": "SALES_ORDER",
            "statusId": "ORDER_COMPLETED", "monthsToInclude": monthsToInclude, "userLogin": system]);

    context.monthsToInclude = monthsToInclude;
    context.totalSubRemainingAmount = result.totalSubRemainingAmount;
    context.totalOrders = result.totalOrders;
}
