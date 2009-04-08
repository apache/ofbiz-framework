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

import java.util.*;
import java.lang.*;
import java.lang.String;
import org.ofbiz.base.util.*;


originalCommEventId = parameters.originalCommEventId;

if (originalCommEventId) {
    originalEvent = delegator.findByPrimaryKey("CommunicationEvent", ["communicationEventId" : originalCommEventId]);
    if (originalEvent) {
        orgEventId = originalEvent.origCommEventId ? originalEvent.origCommEventId : originalCommEventId;
        parameters.communicationEventTypeId = originalEvent.communicationEventTypeId;
        parameters.origCommEventId = orgEventId;
        parameters.originalCommEventId = originalCommEventId;
        parameters.contactMechIdFrom = originalEvent.contactMechIdTo;
        parameters.partyIdFrom = userLogin.partyId;
        parameters.statusId = "COM_IN_PROGRESS";

        parameters.subject = "FW: " + originalEvent.subject;
        headcontent = "\n\n\n ________________________" + "\n" +
                  " > From: " + originalEvent.partyIdFrom + "\n" +
                  " > To: " + originalEvent.partyIdTo + "\n" +
                  " > Subject: " + originalEvent.subject + "\n" +
                  " > Date: " + originalEvent.entryDate + "\n >";
        bodycontent = originalEvent.content.toString();
        if (bodycontent) {
            int ix = -1;
            body  = "";
            if (bodycontent.indexOf('\n') != -1) {
                while ((ix = bodycontent.indexOf('\n')) != -1) {
                    bodycontent = bodycontent.replace('\n',' > ');
                    if (bodycontent != body) {
                        body = body + bodycontent;
                    }
                }
            } else{
                body = bodycontent;
            }
        }
        parameters.content = headcontent + "\n > " + body;
    }
}