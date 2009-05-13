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
import org.ofbiz.party.contact.ContactMechWorker;

parentCommEventId = parameters.parentCommEventId;

if (parentCommEventId) {
    parentEvent = delegator.findByPrimaryKey("CommunicationEvent", ["communicationEventId" : parentCommEventId]);
    if (parentEvent) {
        orgEventId = parentEvent.origCommEventId ? parentEvent.origCommEventId : parentCommEventId;
        parameters.communicationEventTypeId = parentEvent.communicationEventTypeId;
        parameters.parentCommEventId = parentCommEventId;
        parameters.origCommEventId = orgEventId;

        parameters.contactMechIdTo = parentEvent.contactMechIdFrom;
        if (!parameters.contactMechIdTo) {
            String partyId = parentEvent.partyIdFrom;
            contactMechList = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, false);
            for (int i=0;i<contactMechList.size();i++) {
                GenericValue contactMech = (GenericValue) contactMechList.get(i).get("contactMech");
                String contactMechTypeId = (String) contactMech.getString("contactMechTypeId");
                if ("EMAIL_ADDRESS" == contactMechTypeId) {
                    parameters.contactMechIdTo = (String) contactMech.getString("contactMechId");
                }

            }
        }
        parameters.contactMechIdFrom = parentEvent.contactMechIdTo;

        parameters.partyIdFrom = parentEvent.partyIdTo;
        parameters.partyIdTo = parentEvent.partyIdFrom;
        parameters.statusId = "COM_IN_PROGRESS";

        parameters.subject = "RE: " + parentEvent.subject;
        parameters.content = "\n\n\n--------------- In reply to:\n\n" + parentEvent.content;
    }
}
