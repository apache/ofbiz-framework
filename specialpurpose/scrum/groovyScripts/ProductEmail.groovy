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

import java.util.*
import java.lang.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.util.*

productId = parameters.productId
loginPartyId = userLogin.partyId
communicationEventId = parameters.communicationEventId
now = UtilDateTime.nowTimestamp()
try{
    if (UtilValidate.isNotEmpty(loginPartyId)) {
        if (UtilValidate.isNotEmpty(productId)) {
        context.product = from("Product").where("productId", productId).queryOne()
        }
        communicationEvent = from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne()
        communicationEvent.communicationEventTypeId = "EMAIL_COMMUNICATION"
        communicationEvent.contactMechTypeId = "EMAIL_ADDRESS"
        communicationEvent.datetimeStarted = now
        checkOwner = from("ProductRole").where("productId", productId,"partyId", loginPartyId,"roleTypeId", "PRODUCT_OWNER").queryList()
        if (checkOwner) {
            /* for product owner to our company */
            
            // for owner
            productRole = from("ProductRole").where("productId", productId,"roleTypeId", "PRODUCT_OWNER").queryList()
            context.productOwnerId = productRole[0].partyId
            parentCom = from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne()
            if (parentCom) {
                context.partyIdFrom = productRole[0].partyId
            } else {
                context.partyIdFrom = parentCom.partyIdTo
            }
            resultsIdFrom = runService('getPartyEmail', ["partyId" : productRole[0].partyId, "userLogin" : userLogin])
            if (resultsIdFrom.contactMechId != null) {
                context.contactMechIdFrom = resultsIdFrom.contactMechId
                communicationEvent.contactMechIdFrom = resultsIdFrom.contactMechId
            }
            // for team
            defaultPartyIdTo = organizationPartyId
            resultsIdTo = runService('getPartyEmail', ["partyId" : defaultPartyIdTo,"contactMechPurposeTypeId" :"SUPPORT_EMAIL", "userLogin" : userLogin])
            if (resultsIdTo.contactMechId != null) {
                context.contactMechIdTo = resultsIdTo.contactMechId
                communicationEvent.contactMechIdTo = resultsIdTo.contactMechId
            }
            context.partyIdTo = defaultPartyIdTo
            communicationEvent.store()
            context.communicationEvent = communicationEvent
        } else {
            /* from company to owner */
            
            // for team
            defaultPartyIdFrom = organizationPartyId
            context.partyIdFrom = defaultPartyIdFrom
            resultsIdFrom = runService('getPartyEmail', ["partyId" : defaultPartyIdFrom,"contactMechPurposeTypeId" :"SUPPORT_EMAIL", "userLogin" : userLogin])
            if (resultsIdFrom.contactMechId != null) {
                context.contactMechIdFrom = resultsIdFrom.contactMechId
                communicationEvent.contactMechIdFrom = resultsIdFrom.contactMechId
            }
            // for owner
            productRole = from("ProductRole").where("productId", productId,"roleTypeId", "PRODUCT_OWNER").queryList()
            context.productOwnerId = productRole[0].partyId
            parentCom = from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne()
            if(parentCom){
                context.partyIdTo = productRole[0].partyId
            } else {
                 context.partyIdTo = parentCom.partyIdFrom
            }
           resultsIdTo = runService('getPartyEmail', ["partyId" : productRole[0].partyId, "userLogin" : userLogin])
           if (resultsIdTo.contactMechId != null) {
              context.contactMechIdTo = resultsIdTo.contactMechId
              communicationEvent.contactMechIdTo = resultsIdTo.contactMechId
           }
           communicationEvent.store()
           context.communicationEvent = communicationEvent
       }
    }
} catch (exeption) {
    Debug.logInfo("catch exeption ================" + exeption,"")
} catch (GenericEntityException e) {
    Debug.logInfo("catch GenericEntityException ================" + e.getMessage(),"")
}
