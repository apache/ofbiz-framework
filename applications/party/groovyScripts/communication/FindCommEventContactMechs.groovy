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


/**
 * This script finds and places in the context of the form current (un-expired) contact mechs for the logged in user and the
 * party for whom the communication event is intended.  It currently just does searches for email but should be
 * expanded to work off other communication event types.
 */

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.util.EntityUtil;

partyIdFrom = context.partyIdFrom;
partyIdTo = context.partyIdTo;

if (parameters.communicationEventTypeId) {
   if ("EMAIL_COMMUNICATION".equals(parameters.communicationEventTypeId)) {
      userEmailAddresses = from("PartyContactWithPurpose").where("contactMechTypeId", "EMAIL_ADDRESS" , "partyId", partyIdFrom).filterByDate(UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate").queryList();
      context.userEmailAddresses = userEmailAddresses;

      targetEmailAddresses = from("PartyContactWithPurpose").where("contactMechTypeId", "EMAIL_ADDRESS", "partyId", partyIdTo).filterByDate(UtilDateTime.nowTimestamp(), "contactFromDate", "contactThruDate").queryList();
      context.targetEmailAddresses = targetEmailAddresses;
   }
}
