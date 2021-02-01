/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.marketing.marketing;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * MarketingServices contains static service methods for Marketing Campaigns and Contact Lists.
 * See the documentation in marketing/servicedef/services.xml and use the service reference in
 * webtools.  Comments in this file are implemntation notes and technical details.
 */
public class MarketingServices {

    private static final String MODULE = MarketingServices.class.getName();
    public static final String RESOURCE = "MarketingUiLabels";
    private static final String RES_ORDER = "OrderUiLabels";

    public static Map<String, Object> signUpForContactList(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        Timestamp fromDate = UtilDateTime.nowTimestamp();
        String contactListId = (String) context.get("contactListId");
        String email = (String) context.get("email");
        String partyId = (String) context.get("partyId");

        if (!UtilValidate.isEmail(email)) {
            String error = UtilProperties.getMessage(RESOURCE, "MarketingCampaignInvalidEmailInput", locale);
            return ServiceUtil.returnError(error);
        }

        try {
            // locate the contact list
            GenericValue contactList = EntityQuery.use(delegator).from("ContactList").where("contactListId", contactListId).queryOne();
            if (contactList == null) {
                String error = UtilProperties.getMessage(RESOURCE, "MarketingContactListNotFound", UtilMisc.<String, Object>toMap("contactListId",
                        contactListId), locale);
                return ServiceUtil.returnError(error);
            }

            // perform actions as the system user
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();

            // associate the email with anonymous user TODO: do we need a custom contact mech purpose type, say MARKETING_EMAIL?
            if (partyId == null) {
                // Check existing email
                GenericValue contact = EntityQuery.use(delegator).from("PartyContactDetailByPurpose")
                        .where("infoString", email,
                                "contactMechTypeId", "EMAIL_ADDRESS",
                                "contactMechPurposeTypeId", "PRIMARY_EMAIL")
                        .orderBy("-fromDate")
                        .filterByDate("fromDate", "thruDate", "purposeFromDate", "purposeThruDate")
                        .queryFirst();
                if (contact != null) {
                    partyId = contact.getString("partyId");
                } else {
                    partyId = "_NA_";
                }
            }
            Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "emailAddress", email, "partyId", partyId,
                    "fromDate", fromDate, "contactMechPurposeTypeId", "OTHER_EMAIL");
            Map<String, Object> serviceResults = dispatcher.runSync("createPartyEmailAddress", input);
            if (ServiceUtil.isError(serviceResults)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
            }
            String contactMechId = (String) serviceResults.get("contactMechId");
            // create a new association at this fromDate to the anonymous party with status accepted
            input = UtilMisc.toMap("userLogin", userLogin, "contactListId", contactList.get("contactListId"),
                    "partyId", partyId, "fromDate", fromDate, "statusId", "CLPT_PENDING", "preferredContactMechId", contactMechId, "baseLocation",
                    context.get("baseLocation"));
            serviceResults = dispatcher.runSync("createContactListParty", input);
            if (ServiceUtil.isError(serviceResults)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
            }
        } catch (GenericEntityException e) {
            String error = UtilProperties.getMessage(RES_ORDER, "checkhelper.problems_reading_database", locale);
            Debug.logInfo(e, error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(error);
        } catch (GenericServiceException e) {
            String error = UtilProperties.getMessage(RESOURCE, "MarketingServiceError", locale);
            Debug.logInfo(e, error + e.getMessage(), MODULE);
            return ServiceUtil.returnError(error);
        }
        return ServiceUtil.returnSuccess();
    }
}
