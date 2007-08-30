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

delegator = request.getAttribute("delegator");
userLogin = request.getAttribute("userLogin");
parentCommEventId = parameters.get("parentCommEventId");

if (parentCommEventId != null) {
    parentEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", parentCommEventId));
    if (parentEvent != null) {
        orgEventId = parentEvent.get("origCommEventId");
        if (orgEventId == null) orgEventId = parentCommEventId;

        parameters.put("communicationEventTypeId", parentEvent.get("communicationEventTypeId"));
        parameters.put("parentCommEventId", parentCommEventId);
        parameters.put("origCommEventId", orgEventId);

        parameters.put("contactMechIdTo", parentEvent.get("contactMechIdFrom"));
        parameters.put("contactMechIdFrom", parentEvent.get("contactMechIdTo"));

        parameters.put("partyIdFrom", userLogin.get("partyId"));
        parameters.put("partyIdTo", parentEvent.get("partyIdFrom"));        
        parameters.put("toString", parentEvent.get("fromString"));
        parameters.put("statusId", "COM_IN_PROGRESS");
        
        parameters.put("subject", "RE: " + parentEvent.get("subject"));
        parameters.put("content", "\n\n\n--------------- In reply to:\n\n" + parentEvent.get("content"));
    }
}