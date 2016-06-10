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
import org.ofbiz.entity.util.EntityUtil;

partyId = null;

if (userLogin) {
    partyId = userLogin.partyId;
}

if (!partyId && parameters.partyId) {
    partyId = parameters.partyId;
}

if (partyId) {
    parameters.partyId = partyId;

    // NOTE: if there was an error, then don't look up and fill in all of this data, just use the values from the previous request (which will be in the parameters Map automagically)
    if (!request.getAttribute("_ERROR_MESSAGE_") && !request.getAttribute("_ERROR_MESSAGE_LIST_")) {
        person = from("Person").where("partyId", partyId).queryOne();
        if (person) {
            context.callSubmitForm = true;
            // should never be null for the anonymous checkout, but just in case
            parameters.personalTitle = person.personalTitle;
            parameters.firstName = person.firstName;
            parameters.middleName = person.middleName;
            parameters.lastName = person.lastName;
            parameters.suffix = person.suffix;

            //Parameters not in use, Do we really need these here or should be removed.
            parameters.residenceStatusEnumId = person.residenceStatusEnumId;
            parameters.maritalStatus = person.maritalStatus;
            parameters.employmentStatusEnumId = person.employmentStatusEnumId;
            parameters.occupation = person.occupation;
            parameters.yearsWithEmployer = person.yearsWithEmployer;
            parameters.monthsWithEmployer = person.monthsWithEmployer;
            parameters.existingCustomer = person.existingCustomer;

            // birthDate -> birthDateDay, birthDateMonth, birthDateYear
            birthDate = person.birthDate;
            if (birthDate) {
                // will be in the format "yyyy-mm-dd", like "2006-10-21"
                birthDateString = birthDate.toString();
                parameters.birthDateDay = birthDateString.substring(8);
                parameters.birthDateMonth = birthDateString.substring(5, 7);
                parameters.birthDateYear = birthDateString.substring(0, 4);
                // and finally, the whole thing, just in case we want it that way
                parameters.birthDate = birthDateString;
            }
        }

        // get the Email Address
        emailPartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_EMAIL").filterByDate().queryFirst();
        if (emailPartyContactDetail) {
            parameters.emailContactMechId = emailPartyContactDetail.contactMechId;
            parameters.emailAddress = emailPartyContactDetail.infoString;
            parameters.emailSol = emailPartyContactDetail.allowSolicitation;
        }

        // get the Phone Numbers
        homePhonePartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "PHONE_HOME").filterByDate().queryFirst();
        if (homePhonePartyContactDetail) {
            parameters.homePhoneContactMechId = homePhonePartyContactDetail.contactMechId;
            parameters.homeCountryCode = homePhonePartyContactDetail.countryCode;
            parameters.homeAreaCode = homePhonePartyContactDetail.areaCode;
            parameters.homeContactNumber = homePhonePartyContactDetail.contactNumber;
            parameters.homeExt = homePhonePartyContactDetail.extension;
            parameters.homeSol = homePhonePartyContactDetail.allowSolicitation;
        }

        workPhonePartyContactDetail = from("PartyContactDetailByPurpose").where("partyId", partyId, "contactMechPurposeTypeId", "PHONE_WORK").filterByDate().queryFirst();
        if (workPhonePartyContactDetail) {
            parameters.workPhoneContactMechId = workPhonePartyContactDetail.contactMechId;
            parameters.workCountryCode = workPhonePartyContactDetail.countryCode;
            parameters.workAreaCode = workPhonePartyContactDetail.areaCode;
            parameters.workContactNumber = workPhonePartyContactDetail.contactNumber;
            parameters.workExt = workPhonePartyContactDetail.extension;
            parameters.workSol = workPhonePartyContactDetail.allowSolicitation;
        }
    }
}
