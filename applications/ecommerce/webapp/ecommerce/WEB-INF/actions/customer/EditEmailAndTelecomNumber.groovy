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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;

if (userLogin) {
    party = userLogin.getRelatedOne("Party");
    
    contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "PRIMARY_EMAIL", "EMAIL_ADDRESS", false));
    if (contactMech) {
        parameters.emailContactMechId = contactMech.contactMechId;
        parameters.emailAddress = contactMech.infoString;
    }
    
	contactMech = EntityUtil.getFirst(ContactHelper.getContactMech(party, "PRIMARY_PHONE", "TELECOM_NUMBER", false));
	if (contactMech) {
	    partyContactMech = EntityUtil.getFirst(delegator.findByAnd("PartyContactMech", [partyId : party.partyId, contactMechId : contactMech.contactMechId]));
	    if (partyContactMech) {
	        telecomNumber = partyContactMech.getRelatedOne("TelecomNumber");
	        parameters.phoneContactMechId = telecomNumber.contactMechId;
	        parameters.countryCode = telecomNumber.countryCode;
	        parameters.areaCode = telecomNumber.areaCode;
	        parameters.contactNumber = telecomNumber.contactNumber;
	        parameters.extension = partyContactMech.extension; 
	    }
	}
}