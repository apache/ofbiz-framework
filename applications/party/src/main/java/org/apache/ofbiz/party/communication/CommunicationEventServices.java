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

package org.apache.ofbiz.party.communication;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.email.NotificationServices;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.mail.MimeMessageWrapper;

public class CommunicationEventServices {

    private static final String MODULE = CommunicationEventServices.class.getName();
    private static final String RESOURCE = "PartyErrorUiLabels";

    public static Map<String, Object> sendCommEventAsEmail(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String communicationEventId = (String) context.get("communicationEventId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Object> errorMessages = new LinkedList<>(); // used to keep a list of all error messages returned from sending emails to contact list

        try {
            // find the communication event and make sure that it is actually an email
            GenericValue communicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne();
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_not_found_failure", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }
            String communicationEventType = communicationEvent.getString("communicationEventTypeId");
            if (communicationEventType == null || !("EMAIL_COMMUNICATION".equals(communicationEventType) || "AUTO_EMAIL_COMM".equals(communicationEventType))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_must_be_email_for_email", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }

            // make sure the from contact mech is an email if it is specified
            if ((communicationEvent.getRelatedOne("FromContactMech", false) == null)
                 || (!("EMAIL_ADDRESS".equals(communicationEvent.getRelatedOne("FromContactMech", false).getString("contactMechTypeId")))
                 || (communicationEvent.getRelatedOne("FromContactMech", false).getString("infoString") == null))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_from_contact_mech_must_be_email", locale);
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
            Map<String, Object> sendMailParams = new HashMap<>();
            sendMailParams.put("sendFrom", communicationEvent.getRelatedOne("FromContactMech", false).getString("infoString"));
            sendMailParams.put("subject", communicationEvent.getString("subject"));
            sendMailParams.put("contentType", communicationEvent.getString("contentMimeTypeId"));
            sendMailParams.put("userLogin", userLogin);

            Debug.logInfo("Sending communicationEvent: " + communicationEventId, MODULE);

            // check for attachments
            boolean isMultiPart = false;
            List<GenericValue> comEventContents = EntityQuery.use(delegator).from("CommEventContentAssoc").where("communicationEventId", communicationEventId).filterByDate().queryList();
            if (UtilValidate.isNotEmpty(comEventContents)) {
                isMultiPart = true;
                List<Map<String, ? extends Object>> bodyParts = new LinkedList<>();
                if (UtilValidate.isNotEmpty(communicationEvent.getString("content"))) {
                    bodyParts.add(UtilMisc.<String, Object>toMap("content", communicationEvent.getString("content"), "type", communicationEvent.getString("contentMimeTypeId")));
                }
                for (GenericValue comEventContent : comEventContents) {
                    GenericValue content = comEventContent.getRelatedOne("FromContent", false);
                    GenericValue dataResource = content.getRelatedOne("DataResource", false);
                    ByteBuffer dataContent = DataResourceWorker.getContentAsByteBuffer(delegator, dataResource.getString("dataResourceId"), null, null, locale, null);
                    bodyParts.add(UtilMisc.<String, Object>toMap("content", dataContent.array(), "type", dataResource.getString("mimeTypeId"), "filename", dataResource.getString("dataResourceName")));
                }
                sendMailParams.put("bodyParts", bodyParts);
            } else {
                sendMailParams.put("body", communicationEvent.getString("content"));
            }

            // if there is no contact list, then send look for a contactMechIdTo and partyId
            if ((UtilValidate.isEmpty(communicationEvent.getString("contactListId")))) {
                // send to address
                String sendTo = communicationEvent.getString("toString");

                if (UtilValidate.isEmpty(sendTo)) {
                    GenericValue toContactMech = communicationEvent.getRelatedOne("ToContactMech", false);
                    if (toContactMech != null && "EMAIL_ADDRESS".equals(toContactMech.getString("contactMechTypeId"))) {
                        sendTo = toContactMech.getString("infoString");
                    }
                }
                if (UtilValidate.isEmpty(sendTo)) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_to_contact_mech_must_be_email", locale);
                    return ServiceUtil.returnError(errMsg + " " + communicationEventId);
                }

                // add other parties from roles
                String sendCc = null;
                String sendBcc = null;
                List<GenericValue> commRoles = communicationEvent.getRelated("CommunicationEventRole", null, null, false);
                if (UtilValidate.isNotEmpty(commRoles)) {
                    for (GenericValue commRole : commRoles) { // 'from' and 'to' already defined on communication event
                        if (commRole.getString("partyId").equals(communicationEvent.getString("partyIdFrom")) || commRole.getString("partyId").equals(communicationEvent.getString("partyIdTo"))) {
                            continue;
                        }
                        GenericValue contactMech = commRole.getRelatedOne("ContactMech", false);
                        if (contactMech != null && UtilValidate.isNotEmpty(contactMech.getString("infoString"))) {
                            if ("ADDRESSEE".equals(commRole.getString("roleTypeId"))) {
                                sendTo += "," + contactMech.getString("infoString");
                            } else if ("CC".equals(commRole.getString("roleTypeId"))) {
                                if (sendCc != null) {
                                    sendCc += "," + contactMech.getString("infoString");
                                } else {
                                    sendCc = contactMech.getString("infoString");
                                }
                            } else if ("BCC".equals(commRole.getString("roleTypeId"))) {
                                if (sendBcc != null) {
                                    sendBcc += "," + contactMech.getString("infoString");
                                } else {
                                    sendBcc = contactMech.getString("infoString");
                                }
                            }
                        }
                    }
                }

                sendMailParams.put("communicationEventId", communicationEventId);
                sendMailParams.put("sendTo", sendTo);
                if (sendCc != null) {
                    sendMailParams.put("sendCc", sendCc);
                }
                if (sendBcc != null) {
                    sendMailParams.put("sendBcc", sendBcc);
                }
                sendMailParams.put("partyId", communicationEvent.getString("partyIdTo"));  // who it's going to

                // send it - using a new transaction
                Map<String, Object> tmpResult = null;
                if (isMultiPart) {
                    tmpResult = dispatcher.runSync("sendMailMultiPart", sendMailParams, 360, true);
                    if (ServiceUtil.isError(tmpResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                    }
                } else {
                    tmpResult = dispatcher.runSync("sendMail", sendMailParams, 360, true);
                    if (ServiceUtil.isError(tmpResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                    }
                }

                if (ServiceUtil.isError(tmpResult)) {
                    if (ServiceUtil.getErrorMessage(tmpResult).startsWith("[ADDRERR]")) {
                        // address error; mark the communication event as BOUNCED
                        communicationEvent.set("statusId", "COM_BOUNCED");
                        try {
                            communicationEvent.store();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
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
                        Debug.logError(e, MODULE);
                        return ServiceUtil.returnError(e.getMessage());
                    }

                    Map<String, Object> completeResult = dispatcher.runSync("setCommEventComplete", UtilMisc.<String, Object>toMap("communicationEventId", communicationEventId, "partyIdFrom", communicationEvent.getString("partyIdFrom"), "userLogin", userLogin));
                    if (ServiceUtil.isError(completeResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(completeResult));
                    }
                }

            } else {
                // Call the sendEmailToContactList service if there's a contactListId present
                Map<String, Object> sendEmailToContactListContext = new HashMap<>();
                sendEmailToContactListContext.put("contactListId", communicationEvent.getString("contactListId"));
                sendEmailToContactListContext.put("communicationEventId", communicationEventId);
                sendEmailToContactListContext.put("userLogin", userLogin);
                try {
                    dispatcher.runAsync("sendEmailToContactList", sendEmailToContactListContext);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.errorCallingSendEmailToContactListService", locale);
                    Debug.logError(e, errMsg, MODULE);
                    errorMessages.add(errMsg);
                    errorMessages.addAll(e.getMessageList());
                }
            }
        } catch (IOException | GeneralException eey) {
            return ServiceUtil.returnError(eey.getMessage());
        }

        // If there were errors, then the result of this service should be error with the full list of messages
        if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        }
        return result;
    }

    /**
     * Service to send all content associated to a FILE_TRANSFER_COMM CommunicationEvent,
     * with contactMechIdTo as a FtpAdress contactMech
     *
     * @param ctx
     * @param context
     * @return
     */
    public static Map<String, Object> sendCommEventAsFtp(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String communicationEventId = (String) context.get("communicationEventId");
        List<String> errorMessages = new ArrayList<>();
        try {
            GenericValue communicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne();
            if (communicationEvent == null) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_not_found_failure", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }

            if ("COM_COMPLETE".equals(communicationEvent.getString("statusId"))) return ServiceUtil.returnSuccess();

            String communicationEventType = communicationEvent.getString("communicationEventTypeId");
            if (communicationEventType == null || !"FILE_TRANSFER_COMM".equals(communicationEventType)) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_must_be_ftp_for_ftp", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }

            String contactMechId = communicationEvent.getString("contactMechIdTo");

            // Check contactMech type to FTP_ADDRESS
            GenericValue contactMech = EntityQuery.use(delegator).from("ContactMech").cache().where("contactMechId", contactMechId).queryOne();
            GenericValue ftpAddress = EntityQuery.use(delegator).from("FtpAddress").cache().where("contactMechId", contactMechId).queryOne();
            if (null == contactMech || null == ftpAddress || !"FTP_ADDRESS".equals(contactMech.getString("contactMechTypeId"))) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_to_contact_mech_must_be_ftp", locale);
                return ServiceUtil.returnError(errMsg + " " + communicationEventId);
            }

            // Get list of children communication events, to avoid same content multi-send
            List<GenericValue> childrenCommunicationEvent = EntityQuery.use(delegator).select("communicationEventId", "statusId")
                    .from("CommunicationEvent").where("parentCommEventId", communicationEventId).cache().queryList();
            List<String> childrenCommunicationEventIds = EntityUtil.getFieldListFromEntityList(childrenCommunicationEvent, "communicationEventId", true);
            // Retrieve all contents to send
            List<GenericValue> contents = EntityQuery.use(delegator).from("CommEventContentDataResource").where("communicationEventId", communicationEventId).cache().queryList();

            if (UtilValidate.isNotEmpty(contents)) {
                if (UtilValidate.isEmpty(communicationEvent.getTimestamp("datetimeStarted"))) {
                    //store the startDate into the communication
                    Map<String, Object> updateCommEventResult = dispatcher.runSync("updateCommunicationEvent",
                            UtilMisc.toMap("communicationEventId", communicationEventId, "datetimeStarted", UtilDateTime.nowTimestamp(), "userLogin", userLogin), 600, true);
                    if (ServiceUtil.isError(updateCommEventResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(updateCommEventResult));
                    }
                }

                for (GenericValue content : contents) {
                    Map<String, Object> ftpServiceMap = new HashMap<>();
                    //store the child Communication Event, to keep track of errorMessages in note field
                    String childCommunicationEventId = "";
                    ftpServiceMap.put("userLogin", userLogin);
                    ftpServiceMap.put("contentId", content.getString("contentId"));
                    ftpServiceMap.put("partyId", communicationEvent.getString("partyIdTo"));
                    ftpServiceMap.put("contactMechId", contactMechId);
                    // no need to create a child CommEvent if it is a single content transfer
                    if (contents.size() == 1) {
                        ftpServiceMap.put("communicationEventId", communicationEvent.get("communicationEventId"));
                    } else {
                        // check if currentContent is already sent by an existing children communicationEvent
                        EntityCondition sentCond = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("communicationEventId", EntityOperator.IN, childrenCommunicationEventIds),
                                EntityCondition.makeCondition("contentId", content.getString("contentId"))));
                        GenericValue alreadySent = EntityQuery.use(delegator).from("CommEventContentAssoc").where(sentCond).cache().queryFirst();

                        if (null != alreadySent) {
                            GenericValue childCommEvent = EntityUtil.getFirst(EntityUtil.filterByCondition(childrenCommunicationEvent,
                                    EntityCondition.makeCondition("communicationEventId", alreadySent.getString("communicationEventId"))));
                            // if completely sent, continue to next content
                            if ("COM_COMPLETE".equals(childCommEvent.getString("statusId"))) continue;
                            ftpServiceMap.put("communicationEventId", childCommEvent.getString("communicationEventId"));
                        }
                    }

                    Map<String, Object> resultTmp = dispatcher.runSync("sendContentToFtp", ftpServiceMap, 600, true);
                    if (ServiceUtil.isError(resultTmp)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(resultTmp));
                    }

                    // attach the parent communication event to the new event created when sending the content, and store error if needed
                    if (UtilValidate.isNotEmpty(resultTmp.get("communicationEventId"))) childCommunicationEventId = (String) resultTmp.get("communicationEventId");
                    if (UtilValidate.isNotEmpty(childCommunicationEventId) && !childCommunicationEventId.equals(communicationEventId)) {
                        GenericValue childCommunicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", childCommunicationEventId).queryOne();
                        childCommunicationEvent.set("parentCommEventId", communicationEventId);
                        if (ServiceUtil.isError(resultTmp)) {
                            childCommunicationEvent.set("statusId", "COM_BOUNCED");
                            childCommunicationEvent.set("note", ServiceUtil.getErrorMessage(resultTmp));
                        }
                        childCommunicationEvent.store();
                    }
                }
            } else {
                errorMessages.add(UtilProperties.getMessage(RESOURCE, "commeventservices.communication_event_not_without_content", locale));
            }

            if (errorMessages.size() > 0) {
                communicationEvent.set("statusId", "COM_BOUNCED");
                communicationEvent.set("note", errorMessages.toString());
                communicationEvent.store();
            } else {
                //Update content status
                for (GenericValue content : contents) {
                    Map<String, Object> updateContentResult = dispatcher.runSync("setContentStatus", UtilMisc.<String, Object>toMap("contentId", content.getString("contentId"), "statusId", "CTNT_PUBLISHED", "userLogin", userLogin));
                    if (ServiceUtil.isError(updateContentResult)) {
                        errorMessages.add(ServiceUtil.getErrorMessage(updateContentResult));
                    }
                }

                Map<String, Object> completeResult = dispatcher.runSync("setCommEventComplete", UtilMisc.<String, Object>toMap("communicationEventId", communicationEventId, "userLogin", userLogin));
                if (ServiceUtil.isError(completeResult)) {
                    errorMessages.add(ServiceUtil.getErrorMessage(completeResult));
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (errorMessages.size() > 0) {
            return ServiceUtil.returnFailure(errorMessages);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendEmailToContactList(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        List<Object> errorMessages = new LinkedList<>();
        String errorCallingUpdateContactListPartyService = UtilProperties.getMessage(RESOURCE, "commeventservices.errorCallingUpdateContactListPartyService", locale);
        String errorCallingSendMailService = UtilProperties.getMessage(RESOURCE, "commeventservices.errorCallingSendMailService", locale);
        String errorInSendEmailToContactListService = UtilProperties.getMessage(RESOURCE, "commeventservices.errorInSendEmailToContactListService", locale);
        String skippingInvalidEmailAddress = UtilProperties.getMessage(RESOURCE, "commeventservices.skippingInvalidEmailAddress", locale);

        String contactListId = (String) context.get("contactListId");
        String communicationEventId = (String) context.get("communicationEventId");

        // Any exceptions thrown in this block will cause the service to return error
        try {
            GenericValue communicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", communicationEventId).queryOne();
            GenericValue contactList = EntityQuery.use(delegator).from("ContactList").where("contactListId", contactListId).queryOne();

            Map<String, Object> sendMailParams = new HashMap<>();
            sendMailParams.put("sendFrom", communicationEvent.getRelatedOne("FromContactMech", false).getString("infoString"));
            sendMailParams.put("subject", communicationEvent.getString("subject"));
            sendMailParams.put("contentType", communicationEvent.getString("contentMimeTypeId"));
            sendMailParams.put("userLogin", userLogin);

            // Find a list of distinct email addresses from active, ACCEPTED parties in the contact list
            //      using a list iterator (because there can be a large number)
            List<EntityCondition> conditionList = UtilMisc.toList(
                        EntityCondition.makeCondition("contactListId", EntityOperator.EQUALS, contactList.get("contactListId")),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CLPT_ACCEPTED"),
                        EntityCondition.makeCondition("preferredContactMechId", EntityOperator.NOT_EQUAL, null),
                        EntityUtil.getFilterByDateExpr(), EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate"));

            EntityQuery eq = EntityQuery.use(delegator).select("partyId", "preferredContactMechId", "fromDate", "infoString")
                    .from("ContactListPartyAndContactMech")
                    .where(EntityCondition.makeCondition(conditionList, EntityOperator.AND))
                    .cursorScrollInsensitive()
                    .distinct();


            try (EntityListIterator eli = eq.queryIterator()) {
                // Send an email to each contact list member
                // loop through the list iterator
                for (GenericValue contactListPartyAndContactMech; (contactListPartyAndContactMech = eli.next()) != null;) {
                    Debug.logInfo("Contact info: " + contactListPartyAndContactMech, MODULE);
                    // Any exceptions thrown in this inner block will only relate to a single email of the list, so should
                    //  only be logged and not cause the service to return an error
                    try {

                        String emailAddress = contactListPartyAndContactMech.getString("infoString");
                        if (UtilValidate.isEmpty(emailAddress)) {
                            continue;
                        }
                        emailAddress = emailAddress.trim();

                        if (!UtilValidate.isEmail(emailAddress)) {

                            // If validation fails, just log and skip the email address
                            Debug.logError(skippingInvalidEmailAddress + ": " + emailAddress, MODULE);
                            errorMessages.add(skippingInvalidEmailAddress + ": " + emailAddress);
                            continue;
                        }

                        // Because we're retrieving infoString only above (so as not to pollute the distinctness), we
                        //      need to retrieve the partyId it's related to. Since this could be multiple parties, get
                        //      only the most recent valid one via ContactListPartyAndContactMech.
                        List<EntityCondition> clpConditionList = UtilMisc.makeListWritable(conditionList);
                        clpConditionList.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, emailAddress));

                        GenericValue lastContactListPartyACM = EntityQuery.use(delegator).from("ContactListPartyAndContactMech")
                                .where(EntityCondition.makeCondition(clpConditionList, EntityOperator.AND))
                                .orderBy("-fromDate")
                                .cache(true)
                                .queryFirst();
                        if (lastContactListPartyACM == null) {
                            continue;
                        }

                        String partyId = lastContactListPartyACM.getString("partyId");

                        sendMailParams.put("sendTo", emailAddress);
                        sendMailParams.put("partyId", partyId);

                        // Retrieve a record for this contactMechId from ContactListCommStatus
                        Map<String, String> contactListCommStatusRecordMap = UtilMisc.toMap("contactListId", contactListId, "communicationEventId", communicationEventId, "contactMechId", lastContactListPartyACM.getString("preferredContactMechId"));
                        GenericValue contactListCommStatusRecord = EntityQuery.use(delegator).from("ContactListCommStatus")
                                .where(contactListCommStatusRecordMap)
                                .queryOne();
                        if (contactListCommStatusRecord == null) {

                            // No attempt has been made previously to send to this address, so create a record to reflect
                            //  the beginning of the current attempt
                            Map<String, String> newContactListCommStatusRecordMap = UtilMisc.makeMapWritable(contactListCommStatusRecordMap);
                            newContactListCommStatusRecordMap.put("statusId", "COM_IN_PROGRESS");
                            newContactListCommStatusRecordMap.put("partyId", partyId);
                            contactListCommStatusRecord = delegator.create("ContactListCommStatus", newContactListCommStatusRecordMap);
                        } else if (contactListCommStatusRecord.get("statusId") != null && "COM_COMPLETE".equals(contactListCommStatusRecord.getString("statusId"))) {

                            // There was a successful earlier attempt, so skip this address
                            continue;
                        }

                        // Send e-mail
                        Debug.logInfo("Sending email to contact list [" + contactListId + "] party [" + partyId + "] : " + emailAddress, MODULE);
                        // Make the attempt to send the email to the address

                        Map<String, Object> tmpResult = null;

                        // Retrieve a contact list party status
                        GenericValue contactListPartyStatus = EntityQuery.use(delegator).from("ContactListPartyStatus")
                                .where("contactListId", contactListId, "partyId", contactListPartyAndContactMech.getString("partyId"), "fromDate", contactListPartyAndContactMech.getTimestamp("fromDate"), "statusId", "CLPT_ACCEPTED")
                                .queryFirst();
                        if (contactListPartyStatus != null) {
                            // prepare body parameters
                            Map<String, Object> bodyParameters = new HashMap<>();
                            bodyParameters.put("contactListId", contactListId);
                            bodyParameters.put("partyId", contactListPartyAndContactMech.getString("partyId"));
                            bodyParameters.put("preferredContactMechId", contactListPartyAndContactMech.getString("preferredContactMechId"));
                            bodyParameters.put("emailAddress", emailAddress);
                            bodyParameters.put("fromDate", contactListPartyAndContactMech.getTimestamp("fromDate"));
                            bodyParameters.put("optInVerifyCode", contactListPartyStatus.getString("optInVerifyCode"));
                            bodyParameters.put("content", communicationEvent.getString("content"));
                            NotificationServices.setBaseUrl(delegator, contactList.getString("verifyEmailWebSiteId"), bodyParameters);

                            GenericValue webSite = EntityQuery.use(delegator).from("WebSite").where("webSiteId", contactList.getString("verifyEmailWebSiteId")).queryOne();
                            if (webSite != null) {
                                GenericValue productStore = webSite.getRelatedOne("ProductStore", false);
                                if (productStore != null) {
                                    List<GenericValue> productStoreEmailSettings = productStore.getRelated("ProductStoreEmailSetting", UtilMisc.toMap("emailType", "CONT_EMAIL_TEMPLATE"), null, false);
                                    GenericValue productStoreEmailSetting = EntityUtil.getFirst(productStoreEmailSettings);
                                    if (productStoreEmailSetting != null) {
                                        // send e-mail using screen template
                                        sendMailParams.put("bodyScreenUri", productStoreEmailSetting.getString("bodyScreenLocation"));
                                        sendMailParams.put("bodyParameters", bodyParameters);
                                        sendMailParams.remove("body");
                                        tmpResult = dispatcher.runSync("sendMailFromScreen", sendMailParams, 360, true);
                                        if (ServiceUtil.isError(tmpResult)) {
                                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                                        }
                                    }
                                }
                            }
                        }

                        // If the e-mail does not be sent then send normal e-mail
                        if (UtilValidate.isEmpty(tmpResult)) {
                            sendMailParams.put("body", communicationEvent.getString("content"));
                            tmpResult = dispatcher.runSync("sendMail", sendMailParams, 360, true);
                            if (ServiceUtil.isError(tmpResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(tmpResult));
                            }
                        }

                        if (tmpResult == null || ServiceUtil.isError(tmpResult)) {
                            if (ServiceUtil.getErrorMessage(tmpResult).startsWith("[ADDRERR]")) {
                                // address error; mark the communication event as BOUNCED
                                contactListCommStatusRecord.set("statusId", "COM_BOUNCED");
                                try {
                                    contactListCommStatusRecord.store();
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, MODULE);
                                    errorMessages.add(e.getMessage());
                                }
                                // deactivate from the contact list
                                try {
                                    GenericValue contactListParty = contactListPartyAndContactMech.getRelatedOne("ContactListParty", false);
                                    if (contactListParty != null) {
                                        contactListParty.set("statusId", "CLPT_INVALID");
                                        contactListParty.store();
                                    }
                                } catch (GenericEntityException e) {
                                    Debug.logError(e, MODULE);
                                    errorMessages.add(e.getMessage());
                                }
                                continue;
                            }
                            // If the send attempt fails, just log and skip the email address
                            Debug.logError(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(tmpResult),
                                    MODULE);
                            errorMessages.add(errorCallingSendMailService + ": " + ServiceUtil.getErrorMessage(
                                    tmpResult));
                            continue;
                        }
                        // attach the parent communication event to the new event created when sending
                        // the mail
                        String thisCommEventId = (String) tmpResult.get("communicationEventId");
                        GenericValue thisCommEvent = EntityQuery.use(delegator).from("CommunicationEvent").where(
                                "communicationEventId", thisCommEventId).queryOne();
                        if (thisCommEvent != null) {
                            thisCommEvent.set("contactListId", contactListId);
                            thisCommEvent.set("parentCommEventId", communicationEventId);
                            thisCommEvent.store();
                        }
                        String messageId = (String) tmpResult.get("messageId");
                        contactListCommStatusRecord.set("messageId", messageId);

                        if ("Y".equals(contactList.get("singleUse"))) {

                            // Expire the ContactListParty if the list is single use and sendEmail finishes successfully
                            tmpResult = dispatcher.runSync("updateContactListParty", UtilMisc.toMap("contactListId", lastContactListPartyACM.get("contactListId"),
                                    "partyId", partyId, "fromDate", lastContactListPartyACM.get("fromDate"),
                                    "thruDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
                            if (ServiceUtil.isError(tmpResult)) {

                                // If the expiry fails, just log and skip the email address
                                Debug.logError(errorCallingUpdateContactListPartyService + ": " + ServiceUtil.getErrorMessage(tmpResult), MODULE);
                                errorMessages.add(errorCallingUpdateContactListPartyService + ": " + ServiceUtil.getErrorMessage(tmpResult));
                                continue;
                            }
                        }

                        // All is successful, so update the ContactListCommStatus record
                        contactListCommStatusRecord.set("statusId", "COM_COMPLETE");
                        delegator.store(contactListCommStatusRecord);

                        // Don't return a service error just because of failure for one address - just log the error and continue
                    } catch (GenericEntityException | GenericServiceException nonFatalGEE) {
                        Debug.logError(nonFatalGEE, errorInSendEmailToContactListService, MODULE);
                        errorMessages.add(errorInSendEmailToContactListService + ": " + nonFatalGEE.getMessage());
                    }
                }
            } catch (GenericEntityException fatalGEE) {
                return ServiceUtil.returnError(fatalGEE.getMessage());
            }

        } catch (GenericEntityException fatalGEE) {
            return ServiceUtil.returnError(fatalGEE.getMessage());
        }

        return errorMessages.size() == 0 ? ServiceUtil.returnSuccess() : ServiceUtil.returnError(errorMessages);
    }

    public static Map<String, Object> setCommEventComplete(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String communicationEventId = (String) context.get("communicationEventId");
        String partyIdFrom = (String) context.get("partyIdFrom");

        try {
            GenericValue communicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", communicationEventId).cache().queryOne();
            if (communicationEvent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage("PartyUiLabels", "PartyCommunicationEventNotFound",
                        UtilMisc.toMap("communicationEventId", communicationEventId), (Locale) context.get("locale")));
            }
            Timestamp endDate = communicationEvent.getTimestamp("datetimeEnded");
            if (endDate == null) {
                endDate = UtilDateTime.nowTimestamp();
            }
            Map<String, Object> result = dispatcher.runSync("updateCommunicationEvent", UtilMisc.<String, Object>toMap("communicationEventId", communicationEventId,
                    "partyIdFrom", partyIdFrom, "statusId", "COM_COMPLETE", "datetimeEnded", endDate, "userLogin", userLogin));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GeneralException esx) {
            return ServiceUtil.returnError(esx.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /*
     * Store an outgoing file transfer as a communication event;
     * runs as a pre-invoke ECA on sendContentToFtp service
     * - service should run as the 'system' user
     */
    public static Map<String, Object> createCommEventFromFtpTransfer(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");
        String contactMechId = (String) context.get("contactMechId");
        String partyId = (String) context.get("partyId");
        String communicationEventId;

        Timestamp now = UtilDateTime.nowTimestamp();

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put("communicationEventTypeId", "FILE_TRANSFER_COMM");
        commEventMap.put("contactMechTypeId", "FTP_ADDRESS");
        commEventMap.put("contactMechIdTo", contactMechId);
        commEventMap.put("statusId", "COM_PENDING");
        commEventMap.put("datetimeStarted", now);
        commEventMap.put("entryDate", now);
        commEventMap.put("userLogin", userLogin);
        if (UtilValidate.isNotEmpty(partyId)) {
            commEventMap.put("partyIdTo", partyId);
        }

        Map<String, Object> createResult;
        try {
            createResult = dispatcher.runSync("createCommunicationEvent", commEventMap);
            if (ServiceUtil.isError(createResult)) {
                return createResult;
            }
            communicationEventId = (String) createResult.get("communicationEventId");

            //add content to newly created commEvent
            Map<String, Object> createCommEventContentMap = new HashMap<>();
            createCommEventContentMap.put("userLogin", userLogin);
            createCommEventContentMap.put("contentId", contentId);
            createCommEventContentMap.put("communicationEventId", communicationEventId);
            createResult = dispatcher.runSync("createCommEventContentAssoc", createCommEventContentMap);
            if (ServiceUtil.isError(createResult)) {
                return createResult;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("communicationEventId", communicationEventId);
        return result;
    }

    /*
     * Store an outgoing email as a communication event;
     * runs as a pre-invoke ECA on sendMail and sendMultipartMail services
     * - service should run as the 'system' user
     */
    public static Map<String, Object> createCommEventFromEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String subject = (String) context.get("subject");
        String sendFrom = (String) context.get("sendFrom");
        String sendTo = (String) context.get("sendTo");
        String partyId = (String) context.get("partyId");
        String contentType = (String) context.get("contentType");
        String statusId = (String) context.get("statusId");
        String orderId = (String) context.get("orderId");
        String returnId = (String) context.get("returnId");
        if (statusId == null) {
            statusId = "COM_PENDING";
        }

        // get the from contact mech info
        String contactMechIdFrom = null;
        String partyIdFrom = null;
        GenericValue fromCm;
        try {
            fromCm = EntityQuery.use(delegator).from("PartyAndContactMech").where("infoString", sendFrom).orderBy("-fromDate").filterByDate().queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (fromCm != null) {
            contactMechIdFrom = fromCm.getString("contactMechId");
            partyIdFrom = fromCm.getString("partyId");
        }

        // get the to contact mech info
        String contactMechIdTo = null;
        GenericValue toCm;
        try {
            toCm = EntityQuery.use(delegator).from("PartyAndContactMech").where("infoString", sendTo, "partyId", partyId).orderBy("-fromDate").filterByDate().queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (toCm != null) {
            contactMechIdTo = toCm.getString("contactMechId");
        }

        Timestamp now = UtilDateTime.nowTimestamp();

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
        commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
        commEventMap.put("contactMechIdFrom", contactMechIdFrom);
        commEventMap.put("contactMechIdTo", contactMechIdTo);
        commEventMap.put("statusId", statusId);

        commEventMap.put("partyIdFrom", partyIdFrom);
        commEventMap.put("partyIdTo", partyId);
        commEventMap.put("datetimeStarted", now);
        commEventMap.put("entryDate", now);

        commEventMap.put("subject", subject);
        commEventMap.put("userLogin", userLogin);
        commEventMap.put("contentMimeTypeId", contentType);
        if (UtilValidate.isNotEmpty(orderId)) {
            commEventMap.put("orderId", orderId);
        }
        if (UtilValidate.isNotEmpty(returnId)) {
            commEventMap.put("returnId", returnId);
        }

        Map<String, Object> createResult;
        try {
            createResult = dispatcher.runSync("createCommunicationEvent", commEventMap);
            if (ServiceUtil.isError(createResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(createResult)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
        }
        String communicationEventId = (String) createResult.get("communicationEventId");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("communicationEventId", communicationEventId);
        return result;
    }

    /*
     * Update the communication event with information from the email;
     * runs as a post-commit ECA on sendMail and sendMultiPartMail services
     * - service should run as the 'system' user
     */
    public static Map<String, Object> updateCommEventAfterEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String communicationEventId = (String) context.get("communicationEventId");
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");

        Map<String, Object> commEventMap = new HashMap<>();
        commEventMap.put("communicationEventId", communicationEventId);
        commEventMap.put("subject", wrapper.getSubject());
        commEventMap.put("statusId", "COM_COMPLETE");
        commEventMap.put("datetimeEnded", UtilDateTime.nowTimestamp());
        commEventMap.put("entryDate", wrapper.getSentDate());
        commEventMap.put("messageId", wrapper.getMessageId());
        commEventMap.put("userLogin", userLogin);
        commEventMap.put("content", wrapper.getMessageBody());

        // populate the address (to/from/cc/bcc) data
        populateAddressesFromMessage(wrapper, commEventMap);

        // save the communication event
        try {
            Map<String, Object> result = dispatcher.runSync("updateCommunicationEvent", commEventMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        // attachments
        try {
            createAttachmentContent(dispatcher, dctx.getDelegator(), wrapper, communicationEventId, userLogin);
        } catch (GenericServiceException | GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
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
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> storeIncomingEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String partyIdTo = null;
        String partyIdFrom = null;
        String communicationEventId = null;
        String contactMechIdFrom = null;
        String contactMechIdTo = null;

        Map<String, Object> result = null;
        try {
            Address[] addressesFrom = wrapper.getFrom();
            Address[] addressesTo = wrapper.getTo();
            Address[] addressesCC = wrapper.getCc();
            Address[] addressesBCC = wrapper.getBcc();
            String messageId = wrapper.getMessageId().replaceAll("[<>]", "");

            String aboutThisEmail = "message [" + messageId + "] from ["
                    + ((addressesFrom == null || addressesFrom[0] == null) ? "not found" : addressesFrom[0].toString()) + "] to ["
                    + ((addressesTo == null || addressesTo[0] == null) ? "not found" : addressesTo[0].toString()) + "]";

            if (Debug.verboseOn()) {
                Debug.logVerbose("Processing Incoming Email " + aboutThisEmail, MODULE);
            }

            // ignore the message when the spam status = yes
            String spamHeaderName = EntityUtilProperties.getPropertyValue("general", "mail.spam.name", "N", delegator);
            String configHeaderValue = EntityUtilProperties.getPropertyValue("general", "mail.spam.value", delegator);
            //          only execute when config file has been set && header variable found
            if (!"N".equals(spamHeaderName) && wrapper.getHeader(spamHeaderName) != null && wrapper.getHeader(spamHeaderName).length > 0) {
                String msgHeaderValue = wrapper.getHeader(spamHeaderName)[0];
                if (msgHeaderValue != null && msgHeaderValue.startsWith(configHeaderValue)) {
                    Debug.logInfo("Incoming Email message ignored, was detected by external spam checker", MODULE);
                    return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                            "PartyCommEventMessageIgnoredDetectedByExternalSpamChecker", locale));
                }
            }

            // if no 'from' addresses specified ignore the message
            if (addressesFrom == null) {
                Debug.logInfo("Incoming Email message ignored, had not 'from' email address", MODULE);
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                        "PartyCommEventMessageIgnoredNoFromAddressSpecified", locale));
            }

            // make sure this isn't a duplicate
            List<GenericValue> commEvents;
            try {
                commEvents = EntityQuery.use(delegator).from("CommunicationEvent").where("messageId", messageId).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (!commEvents.isEmpty()) {
                Debug.logInfo("Ignoring Duplicate Email: " + aboutThisEmail, MODULE);
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                        "PartyCommEventMessageIgnoredDuplicateMessageId", locale));
            }

            // get the related partId's
            List<Map<String, Object>> toParties = buildListOfPartyInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);
            List<Map<String, Object>> ccParties = buildListOfPartyInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List<Map<String, Object>> bccParties = buildListOfPartyInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            //Get the first address from the list - this is the partyIdTo field of the CommunicationEvent
            if (!toParties.isEmpty()) {
                Map<String, Object> firstAddressTo = toParties.get(0);
                partyIdTo = (String) firstAddressTo.get("partyId");
                contactMechIdTo = (String) firstAddressTo.get("contactMechId");
            }

            String deliveredTo = wrapper.getFirstHeader("Delivered-To");
            if (deliveredTo != null) {
                // check if started with the domain name if yes remove including the dash.
                String dn = deliveredTo.substring(deliveredTo.indexOf('@') + 1, deliveredTo.length());
                if (deliveredTo.startsWith(dn)) {
                    deliveredTo = deliveredTo.substring(dn.length() + 1, deliveredTo.length());
                }
            }

            // if partyIdTo not found try to find the "to" address using the delivered-to header
            if ((partyIdTo == null) && (deliveredTo != null)) {
                result = dispatcher.runSync("findPartyFromEmailAddress", UtilMisc.<String, Object>toMap("address", deliveredTo, "userLogin", userLogin));
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                partyIdTo = (String) result.get("partyId");
                contactMechIdTo = (String) result.get("contactMechId");
            }
            if (userLogin.get("partyId") == null && partyIdTo != null) {
                int ch = 0;
                for (ch = partyIdTo.length(); ch > 0 && Character.isDigit(partyIdTo.charAt(ch - 1)); ch--) { }
                userLogin.put("partyId", partyIdTo.substring(0, ch)); //allow services to be called to have prefix
            }

            // get the 'from' partyId
            result = getParyInfoFromEmailAddress(addressesFrom, userLogin, dispatcher);
            partyIdFrom = (String) result.get("partyId");
            contactMechIdFrom = (String) result.get("contactMechId");

            Map<String, Object> commEventMap = new HashMap<>();
            commEventMap.put("communicationEventTypeId", "AUTO_EMAIL_COMM");
            commEventMap.put("contactMechTypeId", "EMAIL_ADDRESS");
            commEventMap.put("messageId", messageId);

            String subject = wrapper.getSubject();
            commEventMap.put("subject", subject);

            // Set sent and received dates
            commEventMap.put("entryDate", nowTimestamp);
            commEventMap.put("datetimeStarted", UtilDateTime.toTimestamp(wrapper.getSentDate()));
            commEventMap.put("datetimeEnded", UtilDateTime.toTimestamp(wrapper.getReceivedDate()));

            // default role types (_NA_)
            commEventMap.put("roleTypeIdFrom", "_NA_");
            commEventMap.put("roleTypeIdTo", "_NA_");

            // get the content(type) part
            String messageBodyContentType = wrapper.getMessageBodyContentType();
            if (messageBodyContentType.indexOf(';') > -1) {
                messageBodyContentType = messageBodyContentType.substring(0, messageBodyContentType.indexOf(';'));
            }

            // select the plain text bodypart
            String messageBody = null;
            if (wrapper.getMainPartCount() > 1) {
                for (int ind = 0; ind < wrapper.getMainPartCount(); ind++) {
                    BodyPart p = wrapper.getPart(ind + "");
                    if (p.getContentType().toLowerCase(Locale.getDefault()).indexOf("text/plain") > -1) {
                        messageBody = (String) p.getContent();
                        break;
                    }
                }
            }

            if (messageBody == null) {
                messageBody = wrapper.getMessageBody();
            }

            commEventMap.put("content", messageBody);
            commEventMap.put("contentMimeTypeId", messageBodyContentType.toLowerCase(Locale.getDefault()));

            // check for for a reply to communication event (using in-reply-to the parent messageID)
            String[] inReplyTo = wrapper.getHeader("In-Reply-To");
            if (inReplyTo != null && inReplyTo[0] != null) {
                GenericValue parentCommEvent = null;
                try {
                    parentCommEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("messageId", inReplyTo[0].replaceAll("[<>]", "")).queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (parentCommEvent != null) {
                    String parentCommEventId = parentCommEvent.getString("communicationEventId");
                    String orgCommEventId = parentCommEvent.getString("origCommEventId");
                    if (orgCommEventId == null) {
                        orgCommEventId = parentCommEventId;
                    }
                    commEventMap.put("parentCommEventId", parentCommEventId);
                    commEventMap.put("origCommEventId", orgCommEventId);
                }
            }

            // populate the address (to/from/cc/bcc) data
            populateAddressesFromMessage(wrapper, commEventMap);

            // store from/to parties, but when not found make a note of the email to/from address in the workEffort Note Section.
            String commNote = "";
            if (partyIdFrom != null) {
                commEventMap.put("partyIdFrom", partyIdFrom);
                commEventMap.put("contactMechIdFrom", contactMechIdFrom);
            } else {
                commNote += "Sent from: " +  ((InternetAddress) addressesFrom[0]).getAddress() + "; ";
                commNote += "Sent Name from: " + ((InternetAddress) addressesFrom[0]).getPersonal() + "; ";
            }

            if (partyIdTo != null) {
                commEventMap.put("partyIdTo", partyIdTo);
                commEventMap.put("contactMechIdTo", contactMechIdTo);
            } else {
                commNote += "Sent to: " + ((InternetAddress) addressesTo[0]).getAddress()  + "; ";
                if (deliveredTo != null) {
                    commNote += "Delivered-To: " + deliveredTo + "; ";
                }
            }

            commNote += "Sent to: " + ((InternetAddress) addressesTo[0]).getAddress()  + "; ";
            commNote += "Delivered-To: " + deliveredTo + "; ";

            if (partyIdTo != null && partyIdFrom != null) {
                commEventMap.put("statusId", "COM_ENTERED");
            } else {
                commEventMap.put("statusId", "COM_UNKNOWN_PARTY");
            }
            if (commNote.length() > 255) {
                commNote = commNote.substring(0,255);
            }

            if (!("".equals(commNote))) {
                commEventMap.put("note", commNote);
            }

            commEventMap.put("userLogin", userLogin);

            // Populate the CommunicationEvent.headerString field with the email headers
            StringBuilder headerString = new StringBuilder();
            Enumeration<?> headerLines = wrapper.getMessage().getAllHeaderLines();
            while (headerLines.hasMoreElements()) {
                headerString.append(System.getProperty("line.separator"));
                headerString.append(headerLines.nextElement());
            }
            String header = headerString.toString();
            commEventMap.put("headerString", header.replaceAll("[<>]", ""));

            result = dispatcher.runSync("createCommunicationEvent", commEventMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            communicationEventId = (String) result.get("communicationEventId");
            Debug.logInfo("Persisting New Email: " + aboutThisEmail + " into CommunicationEventId: " + communicationEventId, MODULE);

            // handle the attachments
            createAttachmentContent(dispatcher, delegator, wrapper, communicationEventId, userLogin);

            // For all addresses create a CommunicationEventRoles
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, toParties, "ADDRESSEE");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, ccParties, "CC");
            createCommEventRoles(userLogin, delegator, dispatcher, communicationEventId, bccParties, "BCC");

            // get the related work effort info
            List<Map<String, Object>> toWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesTo, userLogin, dispatcher);
            List<Map<String, Object>> ccWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesCC, userLogin, dispatcher);
            List<Map<String, Object>> bccWorkEffortInfos = buildListOfWorkEffortInfoFromEmailAddresses(addressesBCC, userLogin, dispatcher);

            // For all WorkEffort addresses create a CommunicationEventWorkEffs
            createCommunicationEventWorkEffs(userLogin, dispatcher, toWorkEffortInfos, communicationEventId);
            createCommunicationEventWorkEffs(userLogin, dispatcher, ccWorkEffortInfos, communicationEventId);
            createCommunicationEventWorkEffs(userLogin, dispatcher, bccWorkEffortInfos, communicationEventId);

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("communicationEventId", communicationEventId);
            results.put("statusId", commEventMap.get("statusId"));
            return results;
        } catch (MessagingException | GenericServiceException | GenericEntityException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static void populateAddressesFromMessage(MimeMessageWrapper wrapper, Map<String, Object> commEventMap) {
        // Retrieve all the addresses from the email
        Address[] addressesFrom = wrapper.getFrom();
        Address[] addressesTo = wrapper.getTo();
        Address[] addressesCC = wrapper.getCc();
        Address[] addressesBCC = wrapper.getBcc();

        Set<String> emailAddressesFrom = new TreeSet<>();
        Set<String> emailAddressesTo = new TreeSet<>();
        Set<String> emailAddressesCC = new TreeSet<>();
        Set<String> emailAddressesBCC = new TreeSet<>();
        for (Address element : addressesFrom) {
            emailAddressesFrom.add(((InternetAddress) element).getAddress());
        }
        for (Address element : addressesTo) {
            emailAddressesTo.add(((InternetAddress) element).getAddress());
        }
        if (addressesCC != null) {
            for (Address element : addressesCC) {
                emailAddressesCC.add(((InternetAddress) element).getAddress());
            }
        }
        if (addressesBCC != null) {
            for (Address element : addressesBCC) {
                emailAddressesBCC.add(((InternetAddress) element).getAddress());
            }
        }
        String fromString = StringUtil.join(emailAddressesFrom, ",");
        String toString = StringUtil.join(emailAddressesTo, ",");
        String ccString = StringUtil.join(emailAddressesCC, ",");
        String bccString = StringUtil.join(emailAddressesBCC, ",");

        if (UtilValidate.isNotEmpty(fromString)) {
            commEventMap.put("fromString", fromString);
        }
        if (UtilValidate.isNotEmpty(toString)) {
            commEventMap.put("toString", toString);
        }
        if (UtilValidate.isNotEmpty(ccString)) {
            commEventMap.put("ccString", ccString);
        }
        if (UtilValidate.isNotEmpty(bccString)) {
            commEventMap.put("bccString", bccString);
        }
    }

    private static List<String> getCommEventAttachmentNames(final Delegator delegator, final String communicationEventId) throws GenericEntityException {
        List<GenericValue> commEventContentAssocList = EntityQuery.use(delegator)
                .from("CommEventContentDataResource")
                .where(EntityCondition.makeCondition("communicationEventId", communicationEventId))
                .filterByDate()
                .queryList();

        List<String> attachmentNames = new ArrayList<>();
        for (GenericValue commEventContentAssoc : commEventContentAssocList) {
            String dataResourceName = commEventContentAssoc.getString("drDataResourceName");
            attachmentNames.add(dataResourceName);
        }

        return attachmentNames;
    }

    private static void createAttachmentContent(LocalDispatcher dispatcher, Delegator delegator, MimeMessageWrapper wrapper, String communicationEventId, GenericValue userLogin) throws GenericServiceException, GenericEntityException {
        // handle the attachments
        String subject = wrapper.getSubject();
        List<String> attachmentIndexes = wrapper.getAttachmentIndexes();
        List<String> currentAttachmentNames = getCommEventAttachmentNames(delegator, communicationEventId);

        if (attachmentIndexes.size() > 0) {
            Debug.logInfo("=== message has attachments [" + attachmentIndexes.size() + "] =====", MODULE);
            for (String attachmentIdx : attachmentIndexes) {
                String attFileName = wrapper.getPartFilename(attachmentIdx);
                if (currentAttachmentNames.contains(attFileName)) {
                    Debug.logWarning(String.format("CommunicationEvent [%s] already has attachment named '%s'", communicationEventId, attFileName), MODULE);
                    continue;
                }

                Map<String, Object> attachmentMap = new HashMap<>();
                attachmentMap.put("communicationEventId", communicationEventId);
                attachmentMap.put("contentTypeId", "DOCUMENT");
                attachmentMap.put("mimeTypeId", "text/html");
                attachmentMap.put("userLogin", userLogin);
                if (subject != null && subject.length() > 80) {
                    subject = subject.substring(0, 80); // make sure not too big for database field. (20 characters for filename)
                }

                String attContentType = wrapper.getPartContentType(attachmentIdx);
                if (attContentType != null && attContentType.indexOf(';') > -1) {
                    attContentType = attContentType.toLowerCase(Locale.getDefault()).substring(0, attContentType.indexOf(';'));
                }

                if (UtilValidate.isNotEmpty(attFileName)) {
                    attachmentMap.put("contentName", attFileName);
                    attachmentMap.put("description", subject + "-" + attachmentIdx);
                } else {
                    attachmentMap.put("contentName", subject + "-" + attachmentIdx);
                }

                attachmentMap.put("drMimeTypeId", attContentType);
                if (attContentType != null && attContentType.startsWith("text")) {
                    String text = wrapper.getPartText(attachmentIdx);
                    attachmentMap.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                    attachmentMap.put("textData", text);
                } else {
                    ByteBuffer data = wrapper.getPartByteBuffer(attachmentIdx);
                    if (Debug.infoOn()) {
                        Debug.logInfo("Binary attachment size: " + data.limit(), MODULE);
                    }
                    attachmentMap.put("drDataResourceName", attFileName);
                    attachmentMap.put("imageData", data);
                    attachmentMap.put("drDataResourceTypeId", "IMAGE_OBJECT"); // TODO: why always use IMAGE
                    attachmentMap.put("_imageData_contentType", attContentType);
                }

                // save the content
                Map<String, Object> result = dispatcher.runSync("createCommContentDataResource", attachmentMap);
                if (ServiceUtil.isError(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMessage, MODULE);
                    throw new GenericServiceException(errorMessage);
                }
            }
        }
    }

    private static void createCommEventRoles(GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher, String communicationEventId, List<Map<String, Object>> parties, String roleTypeId) {
        // It's not clear what the "role" of this communication event should be, so we'll just put _NA_
        // check and see if this role was already created and ignore if true
        try {
            for (Map<String, Object> result : parties) {
                String partyId = (String) result.get("partyId");
                GenericValue commEventRole = EntityQuery.use(delegator).from("CommunicationEventRole")
                        .where("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId)
                        .queryOne();
                if (commEventRole == null) {
                    Map<String, Object> input = UtilMisc.toMap("communicationEventId", communicationEventId,
                            "partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin,
                            "contactMechId", (String) result.get("contactMechId"),
                            "statusId", "COM_ROLE_CREATED");
                    Map<String, Object> resultMap = dispatcher.runSync("createCommunicationEventRole", input);
                    if (ServiceUtil.isError(resultMap)) {
                        String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                        Debug.logError(errorMessage, MODULE);
                    }
                }
            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    private static void createCommunicationEventWorkEffs(GenericValue userLogin, LocalDispatcher dispatcher, List<Map<String, Object>> workEffortInfos, String communicationEventId) {
        // create relationship between communication event and work efforts
        try {
            for (Map<String, Object> result : workEffortInfos) {
                String workEffortId = (String) result.get("workEffortId");
                Map<String, Object> resultMap = dispatcher.runSync("createCommunicationEventWorkEff", UtilMisc.toMap("workEffortId", workEffortId, "communicationEventId", communicationEventId, "userLogin", userLogin));
                if (ServiceUtil.isError(resultMap)) {
                    String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                    Debug.logError(errorMessage, MODULE);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
    }

    /*
     * Helper method to retrieve the party information from the first email address of the Address[] specified.
     */
    private static Map<String, Object> getParyInfoFromEmailAddress(Address[] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> map = null;
        Map<String, Object> result = null;

        if (addresses == null) {
            return null;
        }

        if (addresses.length > 0) {
            Address addr = addresses[0];
            if (addr instanceof InternetAddress) {
                emailAddress = (InternetAddress) addr;
            }
        }

        if (emailAddress != null) {
            map = new HashMap<>();
            map.put("address", emailAddress.getAddress());
            map.put("userLogin", userLogin);
            result = dispatcher.runSync("findPartyFromEmailAddress", map);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                Debug.logError(errorMessage, MODULE);
                throw new GenericServiceException(errorMessage);
            }
        }

        return result;
    }

    /*
     * Calls findPartyFromEmailAddress service and returns a List of the results for the array of addresses
     */
    private static List<Map<String, Object>> buildListOfPartyInfoFromEmailAddresses(Address[] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> result = null;
        List<Map<String, Object>> tempResults = new LinkedList<>();

        if (addresses != null) {
            for (Address addr: addresses) {
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress) addr;

                    result = dispatcher.runSync("findPartyFromEmailAddress",
                            UtilMisc.toMap("address", emailAddress.getAddress(), "userLogin", userLogin));
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GenericServiceException(errorMessage);
                    }
                    if (result.get("partyId") != null) {
                        tempResults.add(result);
                    }
                }
            }
        }
        return tempResults;
    }

    /*
     * Gets WorkEffort info from e-mail address and returns a List of the results for the array of addresses
     */
    private static List<Map<String, Object>> buildListOfWorkEffortInfoFromEmailAddresses(Address[] addresses, GenericValue userLogin, LocalDispatcher dispatcher) throws GenericServiceException {
        InternetAddress emailAddress = null;
        Map<String, Object> result = null;
        Delegator delegator = dispatcher.getDelegator();
        List<Map<String, Object>> tempResults = new LinkedList<>();
        String caseInsensitiveEmail = EntityUtilProperties.getPropertyValue("general", "mail.address.caseInsensitive", "N", delegator);

        if (addresses != null) {
            for (Address addr: addresses) {
                if (addr instanceof InternetAddress) {
                    emailAddress = (InternetAddress) addr;
                    Map<String, String> inputFields = new HashMap<>();
                    inputFields.put("infoString", emailAddress.getAddress());
                    inputFields.put("infoString_ic", caseInsensitiveEmail);
                    result = dispatcher.runSync("performFind", UtilMisc.<String, Object>toMap("entityName",
                            "WorkEffortContactMechView", "inputFields", inputFields, "userLogin", userLogin));
                    if (ServiceUtil.isError(result)) {
                        String errorMessage = ServiceUtil.getErrorMessage(result);
                        Debug.logError(errorMessage, MODULE);
                        throw new GenericServiceException(errorMessage);
                    }
                    try (EntityListIterator listIt = (EntityListIterator) result.get("listIt")) {
                        List<GenericValue> list = listIt.getCompleteList();
                        List<GenericValue> filteredList = EntityUtil.filterByDate(list);
                        tempResults.addAll(filteredList);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }
        return tempResults;
    }

    /*
     * Service to process incoming email and look for a bounce message. If the email is indeed a bounce message
     * the CommunicationEvent will be updated with the proper COM_BOUNCED status.
     */
    public static Map<String, Object> processBouncedMessage(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logInfo("Running process bounced message check...", MODULE);
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        int parts = wrapper.getMainPartCount();

        if (parts >= 3) { // it must have all three parts in order to process correctly
            // get the second part (delivery report)
            String contentType = wrapper.getPartContentType("1"); // index 1 should be the second part
            if (contentType != null && "message/delivery-status".equalsIgnoreCase(contentType)) {
                Debug.logInfo("Delivery status report part found; processing...", MODULE);

                // get the content of the part
                String part2Text = wrapper.getPartRawText("1");
                if (part2Text == null) {
                    part2Text = "";
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Part 2 Text :\n\n" + part2Text, MODULE);
                }

                // find the "Action" element and obtain its value (looking for "failed")
                Pattern p2 = Pattern.compile("^Action: (.*)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher m2 = p2.matcher(part2Text);
                String action = null;
                if (m2.find()) {
                    action = m2.group(1);
                }

                if (action != null && "failed".equalsIgnoreCase(action)) {
                    // message bounced -- get the original message
                    String part3Text = wrapper.getPartRawText("2"); // index 2 should be the third part
                    if (part3Text == null) {
                        part3Text = "";
                    }
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Part 3 Text :\n\n" + part3Text, MODULE);
                    }

                    // find the "Message-Id" element and obtain its value (looking for "failed")
                    Pattern p3 = Pattern.compile("^Message-Id: (.*)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                    Matcher m3 = p3.matcher(part3Text);
                    String messageId = null;
                    if (m3.find()) {
                        Debug.logInfo("Found message-id : " + m3.group(), MODULE);
                        messageId = m3.group(1);
                    }

                    // find the matching communication event
                    if (messageId != null) {
                        List<GenericValue> values;
                        try {
                            values = EntityQuery.use(delegator).from("CommunicationEvent").where("messageId", messageId).queryList();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (UtilValidate.isNotEmpty(values)) {
                            // there should be only one; unique key
                            GenericValue value = values.get(0);

                            // update the communication event status
                            Map<String, Object> updateCtx = new HashMap<>();
                            updateCtx.put("communicationEventId", value.getString("communicationEventId"));
                            updateCtx.put("statusId", "COM_BOUNCED");
                            updateCtx.put("userLogin", context.get("userLogin"));
                            Map<String, Object> result;
                            try {
                                result = dispatcher.runSync("updateCommunicationEvent", updateCtx);
                                if (ServiceUtil.isError(result)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(result);
                                    Debug.logError(errorMessage, MODULE);
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        } else {
                            if (Debug.infoOn()) {
                                Debug.logInfo("Unable to find CommunicationEvent with the matching messageId : " + messageId, MODULE);
                            }

                            // no communication events found for that message ID; possible this is a NEWSLETTER
                            try {
                                values = EntityQuery.use(delegator).from("ContactListCommStatus").where("messageId", messageId).queryList();
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                                return ServiceUtil.returnError(e.getMessage());
                            }
                            if (UtilValidate.isNotEmpty(values)) {
                                // there should be only one; unique key
                                GenericValue value = values.get(0);

                                Map<String, Object> updateCtx = new HashMap<>();
                                updateCtx.put("communicationEventId", value.getString("communicationEventId"));
                                updateCtx.put("contactListId", value.getString("contactListId"));
                                updateCtx.put("contactMechId", value.getString("contactMechId"));
                                updateCtx.put("partyId", value.getString("partyId"));
                                updateCtx.put("statusId", "COM_BOUNCED");
                                updateCtx.put("userLogin", context.get("userLogin"));
                                Map<String, Object> result;
                                try {
                                    result = dispatcher.runSync("updateContactListCommStatus", updateCtx);
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, MODULE);
                                    return ServiceUtil.returnError(e.getMessage());
                                }
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                }
                            } else {
                                if (Debug.infoOn()) {
                                    Debug.logInfo("Unable to find ContactListCommStatus with the matching messageId : "  + messageId, MODULE);
                                }
                            }
                        }
                    } else {
                        Debug.logWarning("No message ID attached to part", MODULE);
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> logIncomingMessage(DispatchContext dctx, Map<String, ? extends Object> context) {
        MimeMessageWrapper wrapper = (MimeMessageWrapper) context.get("messageWrapper");
        Debug.logInfo("Message recevied         : " + wrapper.getSubject(), MODULE);
        Debug.logInfo("-- Content Type          : " + wrapper.getContentType(), MODULE);
        Debug.logInfo("-- Number of parts       : " + wrapper.getMainPartCount(), MODULE);
        Debug.logInfo("-- Number of attachments : " + wrapper.getAttachmentIndexes().size(), MODULE);
        Debug.logInfo("-- Message ID            : " + wrapper.getMessageId(), MODULE);

        Debug.logInfo("### MESSAGE ###\n\n" + wrapper.getMessageBody(), MODULE);

        List<String> attachmentIndexes = wrapper.getAttachmentIndexes();
        if (attachmentIndexes.size() > 0) {
            Debug.logInfo("### ATTACHMENTS ###", MODULE);
            for (String idx : attachmentIndexes) {
                Debug.logInfo("### -- Filename          : " + wrapper.getPartFilename(idx), MODULE);
                Debug.logInfo("### -- Content Type      : " + wrapper.getPartContentType(idx), MODULE);
            }
        }

        return ServiceUtil.returnSuccess();

    }

    /*
     * Event which marks a communication event as read, and returns a 1px image to the browser/mail client
     * Is updated because the read status is now stored in the communicationEventRole
     * This services is updated but could not be tested. assumed is "read" for partyIdTo on the commevent
     */
    public static String markCommunicationAsRead(HttpServletRequest request, HttpServletResponse response) {
        String communicationEventId = null;

        // pull the communication event from path info, so we can hide the process from the user
        String pathInfo = request.getPathInfo();
        String[] pathParsed = pathInfo.split("/", 3);
        if (pathParsed.length > 2) {
            pathInfo = pathParsed[2];
        } else {
            pathInfo = null;
        }
        if (pathInfo != null && pathInfo.indexOf('/') > -1) {
            pathParsed = pathInfo.split("/");
            communicationEventId = pathParsed[0];
        }

        // update the communication event
        if (communicationEventId != null) {
            Debug.logInfo("Marking communicationEventId [" + communicationEventId + "] from path info : " + request.getPathInfo() + " as read.", MODULE);
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            GenericValue communicationEvent = null;
            try {
                communicationEvent = EntityQuery.use(delegator).from("CommunicationEvent").where("communicationEventId", communicationEventId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            try {
                dispatcher.runAsync("setCommEventRoleToRead", UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", communicationEvent.getString("partyIdTo")));
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
        }

        // return the 1px image (spacer.gif)
        URL imageUrl;
        try {
            imageUrl = FlexibleLocation.resolveLocation("component://common-theme/webapp/images/spacer.gif");
            try (InputStream imageStream = imageUrl.openStream()) {
                UtilHttp.streamContentToBrowser(response, imageStream, 43, "image/gif", null);
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
        }

        // return null to not return any view
        return null;
    }
}
