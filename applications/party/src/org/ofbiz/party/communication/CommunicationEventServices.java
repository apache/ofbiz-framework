/*
* 
*  Copyright (c) 2005 The Open For Business Project - www.ofbiz.org
*
*  Permission is hereby granted, free of charge, to any person obtaining a
*  copy of this software and associated documentation files (the "Software"),
*  to deal in the Software without restriction, including without limitation
*  the rights to use, copy, modify, merge, publish, distribute, sublicense,
*  and/or sell copies of the Software, and to permit persons to whom the
*  Software is furnished to do so, subject to the following conditions:
*
*  The above copyright notice and this permission notice shall be included
*  in all copies or substantial portions of the Software.
*
*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
*  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
*  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
*  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
*  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
*  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
*  @author Si Chen (sichen@opensourcestrategies.com)
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
                GenericValue ContactList = communicationEvent.getRelatedOne("ContactList");
                List sendToParties = ContactList.getRelated("ContactListParty");
                
                for (Iterator it = sendToParties.iterator(); it.hasNext(); ) {
                    GenericValue nextSendToParty = (GenericValue) it.next();
                    if ((nextSendToParty != null) && (nextSendToParty.getRelatedOne("PreferredContactMech") != null)) {
                        sendMailParams.put("sendTo", nextSendToParty.getRelatedOne("PreferredContactMech").getString("infoString"));
                        sendMailParams.put("partyId", nextSendToParty.getString("partyId"));
                    } else {
                        Debug.logWarning("Cannot find a preferred contact mech for [" + nextSendToParty + "]", module);
                    }
                    
                    // no communicationEventId here - we want to create a communication event for each member of the contact list
            
                    // could be run async as well, but that may spawn a lot of processes if there's a large list and cause problems
                    Map tmpResult = dispatcher.runSync("sendMail", sendMailParams);
                    if (ServiceUtil.isError(tmpResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                    }
                }
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
