/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.party.communication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class CommunicationEventServices {
    
    public static final String module = CommunicationEventServices.class.getName();
    public static final String resource = "PartyUiLabels";
    
    public static Map sendCommEventAsEmail(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
                
        String communicationEventId = (String) context.get("communicationEventId");
        
        Map result = ServiceUtil.returnSuccess();
        List errorMessages = new LinkedList();                   // used to keep a list of all error messages returned from sending emails to contact list
        
        try {
            // find the communication event and make sure that it is actually an email
            GenericValue communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_not_found_failure", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }
            if ((communicationEvent.getString("communicationEventTypeId") == null) ||
                !(communicationEvent.getString("communicationEventTypeId").equals("EMAIL_COMMUNICATION"))) {
                String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_must_be_email_for_email", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }
            // make sure the from contact mech is an email if it is specified
            if ((communicationEvent.getRelatedOne("FromContactMech") == null) ||
                 (!(communicationEvent.getRelatedOne("FromContactMech").getString("contactMechTypeId").equals("EMAIL_ADDRESS")) ||
                 (communicationEvent.getRelatedOne("FromContactMech").getString("infoString") == null))) {
                String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_from_contact_mech_must_be_email", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }
                        
            // prepare the email
            Map sendMailParams = new HashMap();
            sendMailParams.put("sendFrom", communicationEvent.getRelatedOne("FromContactMech").getString("infoString"));
            sendMailParams.put("subject", communicationEvent.getString("subject"));
            sendMailParams.put("body", communicationEvent.getString("content"));
            sendMailParams.put("contentType", communicationEvent.getString("contentMimeTypeId"));
            sendMailParams.put("userLogin", userLogin);
            
            // if there is no contact list, then send look for a contactMechIdTo and partyId
            if ((communicationEvent.getString("contactListId") == null) ||
                (communicationEvent.getString("contactListId").equals(""))) {
                
                // in this case, first make sure that the to contact mech actually is an email
                if ((communicationEvent.getRelatedOne("ToContactMech") == null) || 
                        (!(communicationEvent.getRelatedOne("ToContactMech").getString("contactMechTypeId").equals("EMAIL_ADDRESS")) ||
                        (communicationEvent.getRelatedOne("ToContactMech").getString("infoString") == null))) {
                       String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_to_contact_mech_must_be_email", locale);
                       return ServiceUtil.returnError(errMsg + " " + communicationEventId);
                   }
                
                sendMailParams.put("communicationEventId", communicationEventId);
                sendMailParams.put("sendTo", communicationEvent.getRelatedOne("ToContactMech").getString("infoString"));
                sendMailParams.put("partyId", communicationEvent.getString("partyIdTo"));  // who it's going to
                
                // send it
                Map tmpResult = dispatcher.runSync("sendMail", sendMailParams);
                if (ServiceUtil.isError(tmpResult)) {
                    errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                } 
            } else {
                // there's actually a contact list here, so we want to be sending to the entire contact list
                GenericValue contactList = communicationEvent.getRelatedOne("ContactList");

                // find active, ACCEPTED parties in the contact list using a list iterator (because there can be a large number)
                EntityConditionList conditions = new EntityConditionList( UtilMisc.toList(
                            new EntityExpr("contactListId", EntityOperator.EQUALS, contactList.get("contactListId")),
                            new EntityExpr("statusId", EntityOperator.EQUALS, "CLPT_ACCEPTED"),
                            new EntityExpr("preferredContactMechId", EntityOperator.NOT_EQUAL, null),
                            EntityUtil.getFilterByDateExpr()
                            ), EntityOperator.AND);
                List fieldsToSelect = UtilMisc.toList("partyId", "preferredContactMechId");
                EntityListIterator sendToPartiesIt = delegator.findListIteratorByCondition("ContactListParty", conditions,  null, fieldsToSelect, null,
                        new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));
                
                // send an email to each contact list member
                GenericValue nextSendToParty = null;
                while ((nextSendToParty = (GenericValue) sendToPartiesIt.next()) != null) {
                    GenericValue email = nextSendToParty.getRelatedOne("PreferredContactMech");
                    if (email == null) continue;

                    sendMailParams.put("sendTo", email.getString("infoString"));
                    sendMailParams.put("partyId", nextSendToParty.getString("partyId"));
                    
                    // no communicationEventId here - we want to create a communication event for each member of the contact list
            
                    // could be run async as well, but that may spawn a lot of processes if there's a large list and cause problems
                    Map tmpResult = dispatcher.runSync("sendMail", sendMailParams);
                    if (ServiceUtil.isError(tmpResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                    }
                }
                sendToPartiesIt.close();
            }
        } catch (GenericEntityException eex) {
            ServiceUtil.returnError(eex.getMessage());
        } catch (GenericServiceException esx) {
            ServiceUtil.returnError(esx.getMessage());
        }
        
        // if there were errors, then the result of this service should be error with the full list of messages
        if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        }
        return result;
    }
    
    public static Map setCommEventComplete(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String communicationEventId = (String) context.get("communicationEventId");
        
        // assume it's a success unless updateCommunicationEvent gives us an error
        Map result = ServiceUtil.returnSuccess();
        try {
            Map tmpResult = dispatcher.runSync("updateCommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId,
                    "statusId", "COM_COMPLETE", "userLogin", userLogin));
            if (ServiceUtil.isError(result)) {
                result = ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException esx) {
            return ServiceUtil.returnError(esx.getMessage());
        }

        return result;
    }
}
