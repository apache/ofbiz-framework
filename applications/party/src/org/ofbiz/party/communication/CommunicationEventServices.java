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

package org.ofbiz.party.communication;

import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class CommunicationEventServices {
    
    public static final String module = CommunicationEventServices.class.getName();
    public static final String resource = "PartyUiLabels";
    
    public static Map<String, Object> sendCommEventAsEmail(DispatchContext ctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
                
        String communicationEventId = (String) context.get("communicationEventId");
        
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Object> errorMessages = FastList.newInstance(); // used to keep a list of all error messages returned from sending emails to contact list
        
        try {
            // find the communication event and make sure that it is actually an email
            GenericValue communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_not_found_failure", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }
            String communicationEventType = communicationEvent.getString("communicationEventTypeId");
            if (communicationEventType == null || !("EMAIL_COMMUNICATION".equals(communicationEventType) || "AUTO_EMAIL_COMM".equals(communicationEventType))) {
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

            // assign some default values because required by sendmail and better not make them defaults over there
            if (UtilValidate.isEmpty(communicationEvent.getString("subject"))) {
            	communicationEvent.put("subject", " ");
            }
            if (UtilValidate.isEmpty(communicationEvent.getString("content"))) {
            	communicationEvent.put("content", " ");
            }
            
            // prepare the email
            Map<String, Object> sendMailParams = FastMap.newInstance();
            sendMailParams.put("sendFrom", communicationEvent.getRelatedOne("FromContactMech").getString("infoString"));
            sendMailParams.put("subject", communicationEvent.getString("subject"));
            sendMailParams.put("body", communicationEvent.getString("content"));
            sendMailParams.put("contentType", communicationEvent.getString("contentMimeTypeId"));
            sendMailParams.put("userLogin", userLogin);
            
            // if there is no contact list, then send look for a contactMechIdTo and partyId
            if ((UtilValidate.isEmpty(communicationEvent.getString("contactListId")))) {
                // send to address
                String sendTo = communicationEvent.getString("toString");

                if (UtilValidate.isEmpty(sendTo)) {
                    GenericValue toContactMech = communicationEvent.getRelatedOne("ToContactMech");
                    if (toContactMech != null && "EMAIL_ADDRESS".equals(toContactMech.getString("contactMechTypeId"))) {
                        sendTo = toContactMech.getString("infoString");
                    }
                }
                if (UtilValidate.isEmpty(sendTo)) {
                    String errMsg = UtilProperties.getMessage(resource,"commeventservices.communication_event_to_contact_mech_must_be_email", locale);
                    return ServiceUtil.returnError(errMsg + " " + communicationEventId);
                }

                sendMailParams.put("communicationEventId", communicationEventId);
                sendMailParams.put("sendTo", sendTo);
                sendMailParams.put("partyId", communicationEvent.getString("partyIdTo"));  // who it's going to
                
                // send it
                Map<String, Object> tmpResult = dispatcher.runSync("sendMail", sendMailParams);
                if (ServiceUtil.isError(tmpResult)) {
                    errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                } else {
                    // set the message ID on this communication event
                    String messageId = (String) tmpResult.get("messageId");
                    communicationEvent.set("messageId", messageId);
                    try {
                        communicationEvent.store();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> completeResult = dispatcher.runSync("setCommEventComplete", UtilMisc.<String, Object>toMap("communicationEventId", communicationEventId, "userLogin", userLogin));                    
                    if (ServiceUtil.isError(completeResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(completeResult));
                    }
                }

            } else {
                // Call the sendEmailToContactList service if there's a contactListId present
                Map<String, Object> sendEmailToContactListContext = FastMap.newInstance();
                sendEmailToContactListContext.put("contactListId", communicationEvent.getString("contactListId"));
                sendEmailToContactListContext.put("communicationEventId", communicationEventId);
                sendEmailToContactListContext.put("userLogin", userLogin);
                try {
                    dispatcher.runAsync("sendEmailToContactList", sendEmailToContactListContext);
                } catch( GenericServiceException e ) {
                    String errMsg = UtilProperties.getMessage(resource, "commeventservices.errorCallingSendEmailToContactListService", locale);
                    Debug.logError(e, errMsg, module);
                    errorMessages.add(errMsg);
                    errorMessages.addAll(e.getMessageList());
                }
            }
        } catch (GenericEntityException eex) {
            ServiceUtil.returnError(eex.getMessage());
        } catch (GenericServiceException esx) {
            ServiceUtil.returnError(esx.getMessage());
        }
        
        // If there were errors, then the result of this service should be error with the full list of messages
        if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        }
        return result;
    }
    
    public static Map<String, Object> sendEmailToContactList(DispatchContext ctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<Object> errorMessages = FastList.newInstance();
        String errorCallingUpdateContactListPartyService = UtilProperties.getMessage(resource, "commeventservices.errorCallingUpdateContactListPartyService", locale);
        String errorCallingSendMailService = UtilProperties.getMessage(resource, "commeventservices.errorCallingSendMailService", locale);
        String errorInSendEmailToContactListService = UtilProperties.getMessage(resource, "commeventservices.errorInSendEmailToContactListService", locale);
        String skippingInvalidEmailAddress = UtilProperties.getMessage(resource, "commeventservices.skippingInvalidEmailAddress", locale);
        
        String contactListId = (String) context.get("contactListId");
        String communicationEventId = (String) context.get("communicationEventId");

        // Any exceptions thrown in this block will cause the service to return error
        try {
            
            GenericValue communicationEvent = delegator.findByPrimaryKey("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId));
            GenericValue contactList = delegator.findByPrimaryKey("ContactList", UtilMisc.toMap("contactListId", contactListId));

            Map<String, Object> sendMailParams = FastMap.newInstance();
            sendMailParams.put("sendFrom", communicationEvent.getRelatedOne("FromContactMech").getString("infoString"));
            sendMailParams.put("subject", communicationEvent.getString("subject"));
            sendMailParams.put("body", communicationEvent.getString("content"));
            sendMailParams.put("contentType", communicationEvent.getString("contentMimeTypeId"));
            sendMailParams.put("userLogin", userLogin);

            // Find a list of distinct email addresses from active, ACCEPTED parties in the contact list
            //      using a list iterator (because there can be a large number)
            List<EntityCondition> conditionList = UtilMisc.toList(
                        EntityCondition.makeCondition("contactListId", EntityOperator.EQUALS, contactList.get("contactListId")),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CLPT_ACCEPTED"),
                        EntityCondition.makeCondition("preferredContactMechId", EntityOperator.NOT_EQUAL, null),
                        EntityUtil.getFilterByDateExpr(), EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate")
                        );
            EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
            Set<String> fieldsToSelect = UtilMisc.toSet("infoString");

            List<GenericValue> sendToEmails = delegator.findList("ContactListPartyAndContactMech", conditions, fieldsToSelect, null,
                    new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true), false);
            
            // Send an email to each contact list member
            // TODO: Contact lists for emails really should be written as an EntityListIterator for very large lists! 
            List<String> orderBy = UtilMisc.toList("-fromDate");
            for (GenericValue contactListPartyAndContactMech: sendToEmails) {
                // Any exceptions thrown in this inner block will only relate to a single email of the list, so should
                //  only be logged and not cause the service to return an error
                try {
    
                    String emailAddress = contactListPartyAndContactMech.getString("infoString");
                    if (UtilValidate.isEmpty(emailAddress)) continue;
                    emailAddress = emailAddress.trim();
                    
                    if (! UtilValidate.isEmail(emailAddress)) {
                        
                        // If validation fails, just log and skip the email address
                        Debug.logError(skippingInvalidEmailAddress + ": " + emailAddress, module);
                        errorMessages.add(skippingInvalidEmailAddress + ": " + emailAddress);
                        continue;
                    }
    
                    // Because we're retrieving infoString only above (so as not to pollute the distinctness), we
                    //      need to retrieve the partyId it's related to. Since this could be multiple parties, get
                    //      only the most recent valid one via ContactListPartyAndContactMech.
                    List<EntityCondition> clpConditionList = UtilMisc.makeListWritable(conditionList);
                    clpConditionList.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, emailAddress));
                    EntityConditionList clpConditions = EntityCondition.makeCondition(clpConditionList, EntityOperator.AND);
    
                    List<GenericValue> emailCLPaCMs = delegator.findList("ContactListPartyAndContactMech", clpConditions, null, orderBy, null, true);
                    GenericValue lastContactListPartyACM = EntityUtil.getFirst(emailCLPaCMs);
                    if (lastContactListPartyACM == null) continue;
                    
                    String partyId = lastContactListPartyACM.getString("partyId");
                    
                    sendMailParams.put("sendTo", emailAddress);
                    sendMailParams.put("partyId", partyId);
                   
                    // if it is a NEWSLETTER then we do not want the outgoing emails stored, so put a communicationEventId in the sendMail context to prevent storeEmailAsCommunicationEvent from running
                    if ("NEWSLETTER".equals(contactList.getString("contactListTypeId"))) {
                        sendMailParams.put("communicationEventId", communicationEventId);
                    }
                    
                    // Retrieve a record for this contactMechId from ContactListCommStatus
                    Map<String, String> contactListCommStatusRecordMap = UtilMisc.toMap("contactListId", contactListId, "communicationEventId", communicationEventId, "contactMechId", lastContactListPartyACM.getString("preferredContactMechId")); 
                    GenericValue contactListCommStatusRecord = delegator.findByPrimaryKey("ContactListCommStatus", contactListCommStatusRecordMap);
                    if (contactListCommStatusRecord == null) {
                        
                        // No attempt has been made previously to send to this address, so create a record to reflect
                        //  the beginning of the current attempt
                        Map<String, String> newContactListCommStatusRecordMap = UtilMisc.makeMapWritable(contactListCommStatusRecordMap);
                        newContactListCommStatusRecordMap.put("statusId", "COM_IN_PROGRESS");
                        contactListCommStatusRecord = delegator.create("ContactListCommStatus", newContactListCommStatusRecordMap);
                    } else if (contactListCommStatusRecord.get("statusId") != null && contactListCommStatusRecord.getString("statusId").equals("COM_COMPLETE")) {
    
                        // There was a successful earlier attempt, so skip this address
                        continue;
                    }

                    Map<String, Object> tmpResult = null;
                    
                    // Make the attempt to send the email to the address
                    tmpResult = dispatcher.runSync("sendMail", sendMailParams);
                    if (tmpResult == null || ServiceUtil.isError(tmpResult)) {
    
                        // If the send attempt fails, just log and skip the email address
                        Debug.logError(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(tmpResult), module);
                        errorMessages.add(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(tmpResult));
                        continue;
                    }
                    
                    if ("Y".equals(contactList.get("singleUse"))) {
                        
                        // Expire the ContactListParty if the list is single use and sendEmail finishes successfully
                        tmpResult = dispatcher.runSync("updateContactListParty", UtilMisc.toMap("contactListId", lastContactListPartyACM.get("contactListId"),
                                                                                                "partyId", partyId, "fromDate", lastContactListPartyACM.get("fromDate"),
                                                                                                "thruDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
                        if (ServiceUtil.isError(tmpResult)) {
    
                            // If the expiry fails, just log and skip the email address
                            Debug.logError(errorCallingUpdateContactListPartyService + ": " + ServiceUtil.getErrorMessage(tmpResult), module);
                            errorMessages.add(errorCallingUpdateContactListPartyService + ": " + ServiceUtil.getErrorMessage(tmpResult));
                            continue;
                        }
                    }
    
                    // All is successful, so update the ContactListCommStatus record
                    contactListCommStatusRecord.set("statusId", "COM_COMPLETE");
                    delegator.store(contactListCommStatusRecord);
                    
                // Don't return a service error just because of failure for one address - just log the error and continue
                } catch (GenericEntityException nonFatalGEE) {
                    Debug.logError(nonFatalGEE, errorInSendEmailToContactListService, module);
                    errorMessages.add(errorInSendEmailToContactListService + ": " + nonFatalGEE.getMessage());
                } catch (GenericServiceException nonFatalGSE) {
                    Debug.logError(nonFatalGSE, errorInSendEmailToContactListService, module);
                    errorMessages.add(errorInSendEmailToContactListService + ": " + nonFatalGSE.getMessage());
                }
            }
            
        } catch (GenericEntityException fatalGEE) {
            ServiceUtil.returnError(fatalGEE.getMessage());
        }

        return errorMessages.size() == 0 ? ServiceUtil.returnSuccess() : ServiceUtil.returnError(errorMessages);
    }


    public static Map setCommEventComplete(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String communicationEventId = (String) context.get("communicationEventId");
        
        try {
            Map<String, Object> result = dispatcher.runSync("updateCommunicationEvent", UtilMisc.<String, Object>toMap("communicationEventId", communicationEventId,
                    "statusId", "COM_COMPLETE", "userLogin", userLogin));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException esx) {
            return ServiceUtil.returnError(esx.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }
}
