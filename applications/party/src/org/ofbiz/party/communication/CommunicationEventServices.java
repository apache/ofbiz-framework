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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.mail.MimeMessageWrapper;

public class CommunicationEventServices {

    public static final String module = CommunicationEventServices.class.getName();
    public static final String resource = "PartyErrorUiLabels";

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

                // send it - using a new transaction
                Map<String, Object> tmpResult = dispatcher.runSync("sendMail", sendMailParams, 360, true);
                if (ServiceUtil.isError(tmpResult)) {
                    if (ServiceUtil.getErrorMessage(tmpResult).startsWith("[ADDRERR]")) {
                        // address error; mark the communication event as BOUNCED
                        communicationEvent.set("statusId", "COM_BOUNCED");
                        try {
                            communicationEvent.store();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    } else {
                        // setup or communication error
                        errorMessages.add(ServiceUtil.getErrorMessage(tmpResult));
                    }
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
                } catch ( GenericServiceException e ) {
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
            for (GenericValue contactListPartyAndContactMech : sendToEmails) {
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

                    // Make the attempt to send the email to the address
                    Map<String, Object> tmpResult = dispatcher.runSync("sendMail", sendMailParams, 360, true);
                    if (tmpResult == null || ServiceUtil.isError(tmpResult)) {
                        if (ServiceUtil.getErrorMessage(tmpResult).startsWith("[ADDRERR]")) {
                            // address error; mark the communication event as BOUNCED
                            communicationEvent.set("statusId", "COM_BOUNCED");
                            contactListCommStatusRecord.set("statusId", "COM_BOUNCED");
                            try {
                                communicationEvent.store();
                                contactListCommStatusRecord.store();
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                errorMessages.add(e.getMessage());
                            }
                            // deactivate from the contact list
                            try {
                                GenericValue contactListParty = contactListPartyAndContactMech.getRelatedOne("ContactListParty");
                                if (contactListParty != null) {
                                    contactListParty.set("statusId", "CLPT_INVALID");
                                    contactListParty.store();
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, module);
                                errorMessages.add(e.getMessage());
                            }
                            continue;
                        } else {
                            // If the send attempt fails, just log and skip the email address
                            Debug.logError(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(tmpResult), module);
                            errorMessages.add(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(tmpResult));
                            continue;
                        }
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

    public static Map<String, Object> setCommEventComplete(DispatchContext dctx, Map<String, ? extends Object> context) {
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

    /**
     * Store email as communication event
     *@param dctx The DispatchContext that this service is operating in
     *@param serviceContext Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
     public static Map<String, Object> storeEmailAsCommunication(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) serviceContext.get("userLogin");

        String subject = (String) serviceContext.get("subject");
        String body = (String) serviceContext.get("body");
        String partyId = (String) serviceContext.get("partyId");
        String communicationEventId = (String) serviceContext.get("communicationEventId");
        String contentType = (String) serviceContext.get("contentType");
        String emailType = (String) serviceContext.get("emailType");

        // only create a new communication event if the email is not already associated with one
        if (communicationEventId == null) {
            String partyIdFrom = (String) userLogin.get("partyId");
            Map<String, Object> commEventMap = FastMap.newInstance();
            commEventMap.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
            commEventMap.put("statusId", "COM_COMPLETE");
            commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
            commEventMap.put("partyIdFrom", partyIdFrom);
            commEventMap.put("partyIdTo", partyId);
            commEventMap.put("datetimeStarted", UtilDateTime.nowTimestamp());
            commEventMap.put("datetimeEnded", UtilDateTime.nowTimestamp());
            commEventMap.put("subject", subject);
            commEventMap.put("content", body);
            commEventMap.put("userLogin", userLogin);
            commEventMap.put("contentMimeTypeId", contentType);
            String runService = "createCommunicationEvent";
            if ("PARTY_REGIS_CONFIRM".equals(emailType)) {
                runService = "createCommunicationEventWithoutPermission"; // used to create a new Customer, Prospect or Employee
            }
            try {
                dispatcher.runSync(runService, commEventMap);
            } catch (Exception e) {
                Debug.logError(e, "Cannot store email as communication event", module);
                return ServiceUtil.returnError("Cannot store email as communication event; see logs");
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * This service is the main one for processing incoming emails.
     *
     * Its only argument is a wrapper for the JavaMail MimeMessage object.
     * From this object, all the fields, headers and content of the message can be accessed.
     *
     * The first thing this service does is try to discover the partyId of the message sender
     * by doing a reverse find on the email address. It uses the findPartyFromEmailAddress service to do this.
     *
     * It then creates a CommunicationEvent entity by calling the createCommunicationEvent service using the appropriate fields from the email and the
     * discovered partyId, if it exists, as the partyIdFrom. Note that it sets the communicationEventTypeId
     * field to AUTO_EMAIL_COMM. This is useful for tracking email generated communications.
     *
     * The service tries to find appropriate content for inclusion in the CommunicationEvent.content field.
     * If the contentType of the content starts with "text", the getContent() call returns a string and it is used.
     * If the contentType starts with "multipart", then the "parts" of the content are iterated thru and the first
     * one of mime type, "text/..." is used.
     *
     * If the contentType has a value of "multipart" then the parts of the content (except the one used in the main
     * CommunicationEvent.content field) are cycled thru and attached to the CommunicationEvent entity using the
     * createCommContentDataResource service. This happens in the EmailWorker.addAttachmentsToCommEvent method.
     *
     * However multiparts can contain multiparts. A recursive function has been added.
     *
     * -Al Byers - Hans Bakker
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> storeIncomingEmail(DispatchContext dctx, Map<String, ? extends Object> context) {

        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        MimeMessage message = wrapper.getMessage();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyIdTo = null;
        String partyIdFrom = null;
        String contentType = null;
        String communicationEventId = null;
        String contactMechIdFrom = null;
        String contactMechIdTo = null;

        Map result = null;
        try {
            String contentTypeRaw = message.getContentType();
            int idx = contentTypeRaw.indexOf(";");
            if (idx == -1) idx = contentTypeRaw.length();
            contentType = contentTypeRaw.substring(0, idx);
            if (contentType == null || contentType.equals("")) contentType = "text/html";
            contentType = contentType.toLowerCase();
            Address[] addressesFrom = message.getFrom();
            Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
            Address[] addressesCC = message.getRecipients(MimeMessage.RecipientType.CC);
            Address[] addressesBCC = message.getRecipients(MimeMessage.RecipientType.BCC);
            String messageId = message.getMessageID();

            String aboutThisEmail = "message [" + messageId + "] from [" +
                (addressesFrom[0] == null? "not found" : addressesFrom[0].toString()) + "] to [" +
                (addressesTo[0] == null? "not found" : addressesTo[0].toString()) + "]";
            if (Debug.verboseOn()) Debug.logVerbose("Processing Incoming Email " + aboutThisEmail, module);

            // ignore the message when the spam status = yes
            String spamHeaderName = UtilProperties.getPropertyValue("general.properties", "mail.spam.name", "N");
            String configHeaderValue = UtilProperties.getPropertyValue("general.properties", "mail.spam.value");
            //          only execute when config file has been set && header variable found
            if (!spamHeaderName.equals("N") && message.getHeader(spamHeaderName) != null && message.getHeader(spamHeaderName).length > 0) {
                String msgHeaderValue = message.getHeader(spamHeaderName)[0];
                if (msgHeaderValue != null && msgHeaderValue.startsWith(configHeaderValue)) {
                    Debug.logInfo("Incoming Email message ignored, was detected by external spam checker", module);
                    return ServiceUtil.returnSuccess(" Message Ignored: detected by external spam checker");
                }
            }

            // if no 'from' addresses specified ignore the message
            if (addressesFrom == null) {
                Debug.logInfo("Incoming Email message ignored, had not 'from' email address", module);
                return ServiceUtil.returnSuccess(" Message Ignored: no 'From' address specified");
            }

            // make sure this isn't a duplicate
            List commEvents;
            try {
                commEvents = delegator.findByAnd("CommunicationEvent", UtilMisc.toMap("messageId", messageId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (!commEvents.isEmpty()) {
                Debug.logInfo("Ignoring Duplicate Email: " + aboutThisEmail, module);
                return ServiceUtil.returnSuccess(" Message Ignored: deplicate messageId");
            } else {
                Debug.logInfo("Persisting New Email: " + aboutThisEmail, module);
            }


            // get the related partId's
            List toParties = buildListOfPartyInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);
            List ccParties = buildListOfPartyInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List bccParties = buildListOfPartyInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            //Get the first address from the list - this is the partyIdTo field of the CommunicationEvent
            if (!toParties.isEmpty()) {
                Iterator itr = toParties.iterator();
                Map firstAddressTo = (Map) itr.next();
                partyIdTo = (String)firstAddressTo.get("partyId");
                contactMechIdTo = (String)firstAddressTo.get("contactMechId");
            }

            String deliveredTo = null;
            if (message.getHeader("Delivered-To") != null) {
                deliveredTo = message.getHeader("Delivered-To")[0];
                // check if started with the domain name if yes remove including the dash.
                String dn = deliveredTo.substring(deliveredTo.indexOf("@")+1, deliveredTo.length());
                if (deliveredTo.startsWith(dn)) {
                    deliveredTo = deliveredTo.substring(dn.length()+1, deliveredTo.length());
                }
            }

            // if partyIdTo not found try to find the "to" address using the delivered-to header
            if ((partyIdTo == null) && (deliveredTo != null)) {
                result = dispatcher.runSync("findPartyFromEmailAddress", UtilMisc.<String, Object>toMap("address", deliveredTo, "userLogin", userLogin));
                partyIdTo = (String)result.get("partyId");
                contactMechIdTo = (String)result.get("contactMechId");
            }
            if (userLogin.get("partyId") == null && partyIdTo != null) {
                int ch = 0;
                for (ch=partyIdTo.length(); ch > 0 && Character.isDigit(partyIdTo.charAt(ch-1)); ch--) {
                    ;
                }
                userLogin.put("partyId", partyIdTo.substring(0,ch)); //allow services to be called to have prefix
            }

            // get the 'from' partyId
            result = getParyInfoFromEmailAddress(addressesFrom, userLogin, dispatcher);
            partyIdFrom = (String)result.get("partyId");
            contactMechIdFrom = (String)result.get("contactMechId");

            Map commEventMap = FastMap.newInstance();
            commEventMap.put("communicationEventTypeId", "AUTO_EMAIL_COMM");
            commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
            commEventMap.put("messageId", messageId);

            String subject = message.getSubject();
            commEventMap.put("subject", subject);

            // Set sent and received dates
            commEventMap.put("entryDate", nowTimestamp);
            commEventMap.put("datetimeStarted", UtilDateTime.toTimestamp(message.getSentDate()));
            commEventMap.put("datetimeEnded", UtilDateTime.toTimestamp(message.getReceivedDate()));

            // default role types (_NA_)
            commEventMap.put("roleTypeIdFrom", "_NA_");
            commEventMap.put("roleTypeIdTo", "_NA_");

            // get the content(type) part
            Object messageContent = message.getContent();
            if (contentType.startsWith("text")) {
                commEventMap.put("content", messageContent);
                commEventMap.put("contentMimeTypeId", contentType);
            } else if (messageContent instanceof Multipart) {
                contentIndex = "";
                commEventMap = addMessageBody(commEventMap, (Multipart) messageContent);
            }

            // check for for a reply to communication event (using in-reply-to the parent messageID)
            String[] inReplyTo = message.getHeader("In-Reply-To");
            if (inReplyTo != null && inReplyTo[0] != null) {
                GenericValue parentCommEvent = null;
                try {
                    List events = delegator.findByAnd("CommunicationEvent", UtilMisc.toMap("messageId", inReplyTo[0]));
                    parentCommEvent = EntityUtil.getFirst(events);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (parentCommEvent != null) {
                    String parentCommEventId = parentCommEvent.getString("communicationEventId");
                    String orgCommEventId = parentCommEvent.getString("origCommEventId");
                    if (orgCommEventId == null) orgCommEventId = parentCommEventId;
                    commEventMap.put("parentCommEventId", parentCommEventId);
                    commEventMap.put("origCommEventId", orgCommEventId);
                }
            }

            // Retrieve all the addresses from the email
            Set emailAddressesFrom = new TreeSet();
            Set emailAddressesTo = new TreeSet();
            Set emailAddressesCC = new TreeSet();
            Set emailAddressesBCC = new TreeSet();
            for (int x = 0 ; x < addressesFrom.length ; x++) {
                emailAddressesFrom.add(((InternetAddress) addressesFrom[x]).getAddress());
            }
            for (int x = 0 ; x < addressesTo.length ; x++) {
                emailAddressesTo.add(((InternetAddress) addressesTo[x]).getAddress());
            }
            if (addressesCC != null) {
                for (int x = 0 ; x < addressesCC.length ; x++) {
                    emailAddressesCC.add(((InternetAddress) addressesCC[x]).getAddress());
                }
            }
            if (addressesBCC != null) {
                for (int x = 0 ; x < addressesBCC.length ; x++) {
                    emailAddressesBCC.add(((InternetAddress) addressesBCC[x]).getAddress());
                }
            }
            String fromString = StringUtil.join(UtilMisc.toList(emailAddressesFrom), ",");
            String toString = StringUtil.join(UtilMisc.toList(emailAddressesTo), ",");
            String ccString = StringUtil.join(UtilMisc.toList(emailAddressesCC), ",");
            String bccString = StringUtil.join(UtilMisc.toList(emailAddressesBCC), ",");

            if (UtilValidate.isNotEmpty(fromString)) commEventMap.put("fromString", fromString);
            if (UtilValidate.isNotEmpty(toString)) commEventMap.put("toString", toString);
            if (UtilValidate.isNotEmpty(ccString)) commEventMap.put("ccString", ccString);
            if (UtilValidate.isNotEmpty(bccString)) commEventMap.put("bccString", bccString);

            // store from/to parties, but when not found make a note of the email to/from address in the workEffort Note Section.
            String commNote = "";
            if (partyIdFrom != null) {
                commEventMap.put("partyIdFrom", partyIdFrom);
                commEventMap.put("contactMechIdFrom", contactMechIdFrom);
            } else {
                commNote += "Sent from: " +  ((InternetAddress)addressesFrom[0]).getAddress() + "; ";
                commNote += "Sent Name from: " + ((InternetAddress)addressesFrom[0]).getPersonal() + "; ";
            }

            if (partyIdTo != null) {
                commEventMap.put("partyIdTo", partyIdTo);
                commEventMap.put("contactMechIdTo", contactMechIdTo);
            } else {
                commNote += "Sent to: " + ((InternetAddress)addressesTo[0]).getAddress()  + "; ";
                if (deliveredTo != null) {
                    commNote += "Delivered-To: " + deliveredTo + "; ";
                }
            }

            commNote += "Sent to: " + ((InternetAddress)addressesTo[0]).getAddress()  + "; ";
            commNote += "Delivered-To: " + deliveredTo + "; ";

            if (partyIdTo != null && partyIdFrom != null) {
                commEventMap.put("statusId", "COM_ENTERED");
            } else {
                commEventMap.put("statusId", "COM_UNKNOWN_PARTY");
            }
            if (commNote.length() > 255) commNote = commNote.substring(0,255);

            if (!("".equals(commNote))) {
                commEventMap.put("note", commNote);
            }

            commEventMap.put("userLogin", userLogin);

            // Populate the CommunicationEvent.headerString field with the email headers
            String headerString = "";
            Enumeration headerLines = message.getAllHeaderLines();
            while (headerLines.hasMoreElements()) {
                headerString += System.getProperty("line.separator");
                headerString += headerLines.nextElement();
            }
            commEventMap.put("headerString", headerString);

            result = dispatcher.runSync("createCommunicationEvent", commEventMap);
            communicationEventId = (String)result.get("communicationEventId");

            if (messageContent instanceof Multipart) {
                Debug.logInfo("===message has attachments=====", module);
                int attachmentCount = addAttachmentsToCommEvent((Multipart) messageContent, subject, communicationEventId, dispatcher, userLogin);
                if (Debug.infoOn()) Debug.logInfo(attachmentCount + " attachments added to CommunicationEvent:" + communicationEventId,module);
            }

            // For all addresses create a CommunicationEventRoles
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, toParties, "ADDRESSEE");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, ccParties, "CC");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, bccParties, "BCC");

            Map results = ServiceUtil.returnSuccess();
            results.put("communicationEventId", communicationEventId);
            results.put("statusId", commEventMap.get("statusId"));
            return results;
        } catch (MessagingException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static void createCommEventRoles(GenericValue userLogin, GenericDelegator delegator, LocalDispatcher dispatcher, String communicationEventId, List parties, String roleTypeId) {
        // It's not clear what the "role" of this communication event should be, so we'll just put _NA_
        // check and see if this role was already created and ignore if true
        try {
            Iterator it = parties.iterator();
            while (it.hasNext()) {
                Map result = (Map) it.next();
                String partyId = (String) result.get("partyId");
                GenericValue commEventRole = delegator.findByPrimaryKey("CommunicationEventRole",
                        UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId));
                if (commEventRole == null) {
                    Map input = UtilMisc.toMap("communicationEventId", communicationEventId,
                            "partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin,
                            "contactMechId", (String) result.get("contactMechId"),
                            "statusId", "COM_ROLE_CREATED");
                    dispatcher.runSync("createCommunicationEventRole", input);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        } catch (Exception e) {
            Debug.logError(e, module);
        }
    }

    /*
     * Helper method to retrieve the party information from the first email address of the Address[] specified.
     */
    private static Map getParyInfoFromEmailAddress(Address [] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map map = null;
        Map result = null;

        if (addresses == null) {
            return null;
        }

        if (addresses.length > 0) {
            Address addr = addresses[0];
            if (addr instanceof InternetAddress) {
                emailAddress = (InternetAddress)addr;
            }
        }

        if (!UtilValidate.isEmpty(emailAddress)) {
            map = FastMap.newInstance();
            map.put("address", emailAddress.getAddress());
            map.put("userLogin", userLogin);
            result = dispatcher.runSync("findPartyFromEmailAddress", map);
        }

        return result;
    }

    /*
     * Calls findPartyFromEmailAddress service and returns a List of the results for the array of addresses
     */
    private static List buildListOfPartyInfoFromEmailAddresses(Address [] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Address addr = null;
        Map map = null;
        Map result = null;
        List tempResults = FastList.newInstance();

        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                addr = addresses[i];
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress)addr;

                    if (!UtilValidate.isEmpty(emailAddress)) {
                        result = dispatcher.runSync("findPartyFromEmailAddress",
                                UtilMisc.<String, Object>toMap("address", emailAddress.getAddress(), "userLogin", userLogin));
                        if (result.get("partyId") != null) {
                            tempResults.add(result);
                        }
                    }
                }
            }
        }
        return tempResults;
    }

    public static String contentIndex = "";
    private static Map addMessageBody( Map commEventMap, Multipart multipart) throws MessagingException, IOException {
        try {
            int multipartCount = multipart.getCount();
            for (int i=0; i < multipartCount  && i < 10; i++) {
                Part part = multipart.getBodyPart(i);
                String thisContentTypeRaw = part.getContentType();
                String content = null;
                int idx2 = thisContentTypeRaw.indexOf(";");
                if (idx2 == -1) {
                    idx2 = thisContentTypeRaw.length();
                }
                String thisContentType = thisContentTypeRaw.substring(0, idx2);
                if (thisContentType == null || thisContentType.equals("")) thisContentType = "text/html";
                thisContentType = thisContentType.toLowerCase();
                String disposition = part.getDisposition();

                if (thisContentType.startsWith("multipart") || thisContentType.startsWith("Multipart")) {
                    contentIndex = contentIndex.concat("." + i);
                    return addMessageBody(commEventMap, (Multipart) part.getContent());
                }
                // See this case where the disposition of the inline text is null
                else if ((disposition == null) && (i == 0) && thisContentType.startsWith("text")) {
                    content = (String)part.getContent();
                    if (UtilValidate.isNotEmpty(content)) {
                        contentIndex = contentIndex.concat("." + i);
                        commEventMap.put("content", content);
                        commEventMap.put("contentMimeTypeId", thisContentType);
                        return commEventMap;
                    }
                } else if ((disposition != null)
                        && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))
                        && thisContentType.startsWith("text")) {
                    contentIndex = contentIndex.concat("." + i);
                    commEventMap.put("content", part.getContent());
                    commEventMap.put("contentMimeTypeId", thisContentType);
                    return commEventMap;
                }
            }
            return commEventMap;
        } catch (MessagingException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    public String getForwardedField(MimeMessage message) {
        String fieldValue = null;
        return fieldValue;
    }

    public static int addAttachmentsToCommEvent(Multipart messageContent, String subject, String communicationEventId, LocalDispatcher dispatcher, GenericValue userLogin)
        throws MessagingException, IOException, GenericServiceException {
        Map commEventMap = FastMap.newInstance();
        commEventMap.put("communicationEventId", communicationEventId);
        commEventMap.put("contentTypeId", "DOCUMENT");
        commEventMap.put("mimeTypeId", "text/html");
        commEventMap.put("userLogin", userLogin);
        if (subject != null && subject.length() > 80) {
            subject = subject.substring(0,80); // make sure not too big for database field. (20 characters for filename)
        }
        currentIndex = "";
        attachmentCount = 0;
        return addMultipartAttachementToComm(messageContent, commEventMap, subject, dispatcher, userLogin);
    }
    private static String currentIndex = "";
    private static int attachmentCount = 0;
    private static int addMultipartAttachementToComm(Multipart multipart, Map commEventMap, String subject, LocalDispatcher dispatcher, GenericValue userLogin)
    throws MessagingException, IOException, GenericServiceException {
        try {
            int multipartCount = multipart.getCount();
            // Debug.logInfo(currentIndex + "====number of attachments: " + multipartCount, module);
            for (int i=0; i < multipartCount; i++) {
                // Debug.logInfo(currentIndex + "====processing attachment: " + i, module);
                Part part = multipart.getBodyPart(i);
                String thisContentTypeRaw = part.getContentType();
                // Debug.logInfo("====thisContentTypeRaw: " + thisContentTypeRaw, module);
                int idx2 = thisContentTypeRaw.indexOf(";");
                if (idx2 == -1) idx2 = thisContentTypeRaw.length();
                String thisContentType = thisContentTypeRaw.substring(0, idx2);
                String disposition = part.getDisposition();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (part instanceof Multipart) {
                    currentIndex = currentIndex.concat("." + i);
                    // Debug.logInfo("=====attachment contain attachment, index:" + currentIndex, module);
                    return addMultipartAttachementToComm((Multipart) part.getContent(), commEventMap, subject, dispatcher, userLogin);
                }
                // Debug.logInfo("=====attachment not contains attachment, index:" + currentIndex, module);
                // Debug.logInfo("=====check for currentIndex(" + currentIndex  + ") against master contentIndex(" + EmailServices.contentIndex + ")", module);
                if (currentIndex.concat("." + i).equals(CommunicationEventServices.contentIndex)) continue;

                // The first test should not pass, because if it exists, it should be the bodyContentIndex part
                // Debug.logInfo("====check for disposition: " + disposition + " contentType: '" + thisContentType + "' variable i:" + i, module);
                if ((disposition == null && thisContentType.startsWith("text"))
                        || ((disposition != null)
                                && (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))
                                ) )
                {
                    String attFileName = part.getFileName();
                    Debug.logInfo("===processing attachment: " + attFileName, module);
                    if (!UtilValidate.isEmpty(attFileName)) {
                           commEventMap.put("contentName", attFileName);
                           commEventMap.put("description", subject + "-" + attachmentCount);
                    } else {
                        commEventMap.put("contentName", subject + "-" + attachmentCount);
                    }
                    commEventMap.put("drMimeTypeId", thisContentType);
                    if (thisContentType.startsWith("text")) {
                        String content = (String)part.getContent();
                        commEventMap.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                        commEventMap.put("textData", content);
                    } else {
                        InputStream is = part.getInputStream();
                        int c;
                        while ((c = is.read()) > -1) {
                            baos.write(c);
                        }
                        ByteBuffer imageData = ByteBuffer.wrap(baos.toByteArray());
                        int len = imageData.limit();
                        if (Debug.infoOn()) Debug.logInfo("imageData length: " + len, module);
                        commEventMap.put("drDataResourceName", part.getFileName());
                        commEventMap.put("imageData", imageData);
                        commEventMap.put("drDataResourceTypeId", "IMAGE_OBJECT");
                        commEventMap.put("_imageData_contentType", thisContentType);
                    }
                    dispatcher.runSync("createCommContentDataResource", commEventMap);
                    attachmentCount++;
                }
            }
        } catch (MessagingException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return attachmentCount;
    }
}
