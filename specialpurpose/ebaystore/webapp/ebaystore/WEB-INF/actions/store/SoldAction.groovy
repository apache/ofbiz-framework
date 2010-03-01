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
import javolution.util.FastList;
import javolution.util.FastMap;

actionList = FastList.newInstance();
hasAction = false;
//Unpaid Item Dispute
if (unpaidItemStatus == null && paidTime == null && checkoutStatus != "CheckoutComplete") {
    inMap = FastMap.newInstance();
    inMap.put("action","openUnpaid");
    inMap.put("actionName","Open Unpaid");
    actionList.add(inMap);
    hasAction = true;
}
//Second Chance Offer
inMap = FastMap.newInstance();
inMap.put("action","makeSecondChanceOffer");
inMap.put("actionName","Make Second Chance Offer");
actionList.add(inMap);
hasAction = true;

context.actionList = actionList;
context.hasAction = hasAction;