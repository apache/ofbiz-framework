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
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;

partyRole = from("PartyRole").where("partyId", userLogin.partyId, "roleTypeId", "SUPPLIER").queryOne();
if (partyRole) {
    if ("SUPPLIER".equals(partyRole.roleTypeId)) {
        /** drop shipper or supplier **/
        porderRoleCollection = from("OrderRole").where("partyId", userLogin.partyId, "roleTypeId", "SUPPLIER_AGENT").queryList();
        porderHeaderList = EntityUtil.orderBy(EntityUtil.filterByAnd(EntityUtil.getRelated("OrderHeader", null, porderRoleCollection, false),
                [EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                 EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER")]),
                 ["orderDate DESC"]);
        context.porderHeaderList = porderHeaderList;
    }
}
orderRoleCollection = from("OrderRole").where("partyId", userLogin.partyId, "roleTypeId", "PLACING_CUSTOMER").queryList();
orderHeaderList = EntityUtil.orderBy(EntityUtil.filterByAnd(EntityUtil.getRelated("OrderHeader", null, orderRoleCollection, false),
        [EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED")]), ["orderDate DESC"]);
context.orderHeaderList = orderHeaderList;

downloadOrderRoleAndProductContentInfoList = from("OrderRoleAndProductContentInfo").where("partyId", userLogin.partyId, "roleTypeId", "PLACING_CUSTOMER", "productContentTypeId", "DIGITAL_DOWNLOAD", "statusId", "ITEM_COMPLETED").queryList();
context.downloadOrderRoleAndProductContentInfoList = downloadOrderRoleAndProductContentInfoList;
