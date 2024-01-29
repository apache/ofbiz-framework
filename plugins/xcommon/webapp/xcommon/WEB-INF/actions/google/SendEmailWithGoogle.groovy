import com.simbaquartz.util.FsdUtilValidate
import com.simbaquartz.xcommon.collections.FastList
import org.apache.commons.validator.routines.EmailValidator
import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.codehaus.jackson.map.ObjectMapper
import com.fidelissd.fsdParty.party.FsdPartyHelper
import com.simbaquartz.util.FsdUtilFormat

import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import java.sql.Timestamp

mapper = new ObjectMapper()
String serverRootUrl = parameters._SERVER_ROOT_URL_

String partyId = parameters.partyId
String sendTo = parameters.sendTo
String sendFrom = parameters.sendFrom
String sendCc = parameters.sendCc
String sendBcc = parameters.sendBcc
String subject = parameters.subject
String emailBody = parameters.emailBody
String threadId = parameters.threadId
String linkedTaskId = parameters.linkedTaskId
String linkedQuoteId = parameters.linkedQuoteId
String linkedOpportunityId = parameters.linkedOpportunityId
String attachmentSelected = parameters.attachmentSelected
String linkedQuoteAttachments = parameters.linkedQuoteAttachments
String linkedOpportunityAttachments = parameters.linkedOpportunityAttachments
String linkedTaskAttachments = parameters.linkedTaskAttachments
String emailDraftId = parameters.emailDraftId
String linkedIssueAttachments = parameters.linkedIssueAttachments
String linkedIssueId = parameters.linkedIssueId

List attachmentContentIds = []
if (attachmentSelected == "Y") {
    attachmentContentIds = session.getAttribute("EMAIL_ATTACHMENTS_LIST")
} else {
    session.removeAttribute('EMAIL_ATTACHMENTS_LIST')
}
if (UtilValidate.isEmpty(attachmentContentIds)) {
    attachmentContentIds = FastList.newInstance()
}

// If just one attachment - make it as list
if (!(attachmentContentIds instanceof List)) {
    String tempContentId = attachmentContentIds
    attachmentContentIds = FastList.newInstance()
    attachmentContentIds.add(tempContentId)
}

//if quote attachments exists
if (UtilValidate.isNotEmpty(linkedQuoteAttachments)) {
    List<String> attachments = StringUtil.split(linkedQuoteAttachments, ",")
    for (String attachment : attachments) {
        attachmentContentIds.add(attachment)
    }
}

//if opportunity attachments exists
if (UtilValidate.isNotEmpty(linkedOpportunityAttachments)) {
    List<String> attachments = StringUtil.split(linkedOpportunityAttachments, ",")
    for (String attachment : attachments) {
        attachmentContentIds.add(attachment)
    }
}

//if task attachments exists
if (UtilValidate.isNotEmpty(linkedTaskAttachments)) {
    List<String> attachments = StringUtil.split(linkedTaskAttachments, ",")
    for (String attachment : attachments) {
        attachmentContentIds.add(attachment)
    }
}

//if issue attachments exists
if (UtilValidate.isNotEmpty(linkedIssueAttachments)) {
    List<String> attachments = StringUtil.split(linkedIssueAttachments, ",")
    for (String attachment : attachments) {
        attachmentContentIds.add(attachment)
    }
}

if (UtilValidate.isEmpty(sendTo) && UtilValidate.isEmpty(sendCc) && UtilValidate.isEmpty(sendBcc)) {
    request.setAttribute("_ERROR_MESSAGE_", "Please select at least one of the To/Cc/Bcc Recipients.")
    return "error"
}
if (UtilValidate.isEmpty(subject)) {
    request.setAttribute("_ERROR_MESSAGE_", "Please enter a valid subject for your email.")
    return "error"
}

//perform email validation
//TO Field validations
if(UtilValidate.isNotEmpty(sendTo)){
    List<String> emailAddresses = StringUtil.split(sendTo, ",")

    List<String> invalidEmailAddresses = []
    emailAddresses.each { email ->
        hasInvalidEmailAddress = !FsdUtilValidate.isValidEmail(email)

        if(!FsdUtilValidate.isValidEmail(email)){
            invalidEmailAddresses.add(email)
        }
    }
    if(invalidEmailAddresses.size() > 0){
        String errorMessage = "<strong>" + invalidEmailAddresses.get(0) + "</strong> in To section is not a valid email address. <p>" + UtilValidate.isEmailMsg + "</p>"
        request.setAttribute("_ERROR_MESSAGE_", errorMessage)
        return "error"
    }
}
//Cc Field validations
if(UtilValidate.isNotEmpty(sendCc)){
    List<String> emailAddresses = StringUtil.split(sendCc, ",")

    List<String> invalidEmailAddresses = []
    emailAddresses.each { email ->
        hasInvalidEmailAddress = !FsdUtilValidate.isValidEmail(email)

        if(!FsdUtilValidate.isValidEmail(email)){
            invalidEmailAddresses.add(email)
        }
    }
    if(invalidEmailAddresses.size() > 0){
        String errorMessage = "<strong>" + invalidEmailAddresses.get(0) + "</strong> in Cc section is not a valid email address. <p><small>" + UtilValidate.isEmailMsg + "</small></p>"
        request.setAttribute("_ERROR_MESSAGE_", errorMessage)
        return "error"
    }
}

//Bcc Field validations
if(UtilValidate.isNotEmpty(sendBcc)){
    List<String> emailAddresses = StringUtil.split(sendBcc, ",")

    List<String> invalidEmailAddresses = []
    emailAddresses.each { email ->
        hasInvalidEmailAddress = !FsdUtilValidate.isValidEmail(email)

        if(!FsdUtilValidate.isValidEmail(email)){
            invalidEmailAddresses.add(email)
        }
    }
    if(invalidEmailAddresses.size() > 0){
        String errorMessage = "<strong>" + invalidEmailAddress.get(0) + "</strong> in Bcc section is not a valid email address. <p>" + UtilValidate.isEmailMsg + "</p>"
        request.setAttribute("_ERROR_MESSAGE_", errorMessage)
        return "error"
    }
}

String emailBodyStr = ""
if (UtilValidate.isNotEmpty(emailBody)) {
    List textParts = StringUtil.split(emailBody, "")
    StringBuffer textDataStr = new StringBuffer()
    for (Object textPart : textParts) {
        textDataStr.append(textPart)
    }
    emailBodyStr = textDataStr.toString()
    emailBodyStr = emailBodyStr.replaceAll("'", "\\'")
}

Map sendEmailWithGmailCtx = [
        "userLogin"           : userLogin,
        "sendFrom"            : sendFrom,
        "partyId"             : partyId,
        "sendTo"              : sendTo,
        "sendCc"              : sendCc,
        "sendBcc"             : sendBcc,
        "subject"             : subject,
        "emailBody"           : emailBodyStr,
        "attachmentContentIds": attachmentContentIds,
        "serverRootUrl"       : serverRootUrl,
        "threadId"            : threadId
]
Map<String, Object> resp = dispatcher.runSync("sendEmailWithGmail", sendEmailWithGmailCtx)

if (ServiceUtil.isError(resp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(resp))
    return "error"
}
String communicationEventId = resp.communicationEventId

int notesCount = 0

// Note: Any change to the code below may need change to ECA "makeAssociationsAfterSendEmailWithGmail"
// called after sendEmailWithGmail service
if(UtilValidate.isNotEmpty(communicationEventId)) {
    if (UtilValidate.isNotEmpty(linkedTaskId)) {
        Map createTaskAssocWithCommEventCtx = [
                userLogin           : userLogin,
                taskId              : linkedTaskId,
                createdByPartyId    : userLogin.partyId,
                communicationEventId: communicationEventId
        ]
        Map createTaskAssocWithCommEventCtxResponse = dispatcher.runSync("createTaskCommunicationEventAssoc", createTaskAssocWithCommEventCtx)
        if (!ServiceUtil.isSuccess(createTaskAssocWithCommEventCtxResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createTaskAssocWithCommEventCtxResponse))
            return "error"
        }
    }

    if (UtilValidate.isNotEmpty(linkedQuoteId)) {

        //Uncommenting the below code as communication event was not being associated with quote.
        Map createQuoteAssocWithCommEventCtx = [
                userLogin           : userLogin,
                quoteId             : linkedQuoteId,
                communicationEventId: communicationEventId
        ]
        Map createQuoteAssocWithCommEventCtxResponse = dispatcher.runSync("createQuoteCommunicationEventAssoc", createQuoteAssocWithCommEventCtx)
        if (!ServiceUtil.isSuccess(createQuoteAssocWithCommEventCtxResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createQuoteAssocWithCommEventCtxResponse))
            return "error"
        }

        //add a quote note as well
        String noteInfo = "[Email Log] Quote Email sent to " + sendTo
        if (UtilValidate.isNotEmpty(sendCc)) {
            noteInfo = noteInfo + ", cc: " + sendCc
        }
        Map fsdCreateQuoteNoteCtx = [
                quoteId   : linkedQuoteId,
                noteInfo  : noteInfo,
                noteTypeId: "GENERAL_NOTE",
                isInternal: "Y",
                userLogin : userLogin
        ];
        Map fsdCreateQuoteNoteCtxResponse = dispatcher.runSync("fsdCreateQuoteNote", fsdCreateQuoteNoteCtx)
        if (!ServiceUtil.isSuccess(fsdCreateQuoteNoteCtxResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(fsdCreateQuoteNoteCtxResponse))
            return "error"
        }

        GenericValue commEventNoteAssoc = delegator.makeValue("CommunicationEventNoteAssoc")
        commEventNoteAssoc.set("noteId", fsdCreateQuoteNoteCtxResponse.noteId)
        commEventNoteAssoc.set("communicationEventId", communicationEventId)
        commEventNoteAssoc.create()
    }

    if (UtilValidate.isNotEmpty(linkedOpportunityId)) {

        Map createOpportunityCommunicationAssocCtx = [
                userLogin           : userLogin,
                opportunityId       : linkedOpportunityId,
                communicationEventId: communicationEventId
        ]
        Map createOpportunityCommunicationAssocResp = dispatcher.runSync("createOpportunityCommunicationAssoc", createOpportunityCommunicationAssocCtx)
        if (!ServiceUtil.isSuccess(createOpportunityCommunicationAssocResp)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createOpportunityCommunicationAssocResp))
            return "error"
        }

        //add a opportunity note as well
        String noteInfo = "[Email Log] Opportunity Email sent to " + sendTo
        if (UtilValidate.isNotEmpty(sendCc)) {
            noteInfo = noteInfo + ", cc: " + sendCc
        }

        Map createOpportunityNoteCtx = [
                userLogin         : userLogin,
                salesOpportunityId: linkedOpportunityId,
                noteTypeId        : "OPP_NOTES",
                noteInfo          : noteInfo,
                isInternal        : "Y"
        ]

        Map createOpportunityNoteServiceResponse = dispatcher.runSync("createSalesOpportunityNote", createOpportunityNoteCtx)

        if (!ServiceUtil.isSuccess(createOpportunityNoteServiceResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createOpportunityNoteServiceResponse))
            return "error"
        }

        GenericValue commEventNoteAssoc = delegator.makeValue("CommunicationEventNoteAssoc")
        commEventNoteAssoc.set("noteId", createOpportunityNoteServiceResponse.noteId)
        commEventNoteAssoc.set("communicationEventId", communicationEventId)
        commEventNoteAssoc.create()
    }
}

if (UtilValidate.isNotEmpty(emailDraftId)) {
    //delete draft from draft list
    Map fsdDeleteEmailDraftCtx = [
            userLogin: userLogin,
            draftId  : emailDraftId
    ]
    Map fsdDeleteEmailDraftCtxResponse = dispatcher.runSync("fsdDeleteEmailDraft", fsdDeleteEmailDraftCtx)
    if (!ServiceUtil.isSuccess(fsdDeleteEmailDraftCtxResponse)) {
        request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(fsdDeleteEmailDraftCtxResponse))
        return "error"
    }
}

//create event when email sent
String eventName = subject
String emailWriter = FsdPartyHelper.getPartyName(delegator, userLogin.partyId)
String eventTypeId = "EMAIL"
String eventRecurrence = "doesNotRepeat"
String isPublic = "N"
String createdEventId = ""
Timestamp newEventTimeZoneId = null
String description = emailWriter + " has sent an email to " + sendTo
if (UtilValidate.isNotEmpty(sendCc)) {
    description = eventName + ", cc: " + sendCc
}
description = description + "on " + FsdUtilFormat.formatDateLong(UtilDateTime.nowTimestamp())


Timestamp estimatedStartDateTs = UtilDateTime.nowTimestamp()
estimatedStartDateWithEndTs = UtilDateTime.addDaysToTimestamp(estimatedStartDateTs, 0.02)
Map createEventMap = [
        userLogin              : userLogin,
        eventName              : eventName,
        eventTypeId            : eventTypeId,
        description            : description,
        eventRecurrenceOption  : eventRecurrence,
        isPublic               : isPublic,
        communicationEventId   : communicationEventId,
        allDayCheck            : "N",
        timezoneId             : newEventTimeZoneId,
        estimatedStartDate     : estimatedStartDateTs,
        estimatedCompletionDate: estimatedStartDateWithEndTs
]
createEventResp = dispatcher.runSync("createEvent", createEventMap)

if (ServiceUtil.isFailure(createEventResp)) {
    return "error"
}
createdEventId = createEventResp.eventId

//link event id with task, quote, opportunity if exists
if (UtilValidate.isNotEmpty(createdEventId)) {
    if (UtilValidate.isNotEmpty(linkedQuoteId)) {
        Map createQuoteAssociationCtx = [
                userLogin: userLogin,
                eventId  : createdEventId,
                quoteId  : linkedQuoteId
        ]
        Map createQuoteAssociationResponse = dispatcher.runSync("createQuoteEventAssoc", createQuoteAssociationCtx)
        if (!ServiceUtil.isSuccess(createQuoteAssociationResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createQuoteAssociationResponse))
            return "error"
        }
    }

    if (UtilValidate.isNotEmpty(linkedOpportunityId)) {
        Map createEventOppAssociationCtx = [
                userLogin    : userLogin,
                opportunityId: linkedOpportunityId,
                eventId      : createdEventId
        ]

        Map creatEventOppAssociationResponse = dispatcher.runSync("createOpportunityEventAssoc", createEventOppAssociationCtx)
        if (!ServiceUtil.isSuccess(creatEventOppAssociationResponse)) {
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(creatEventOppAssociationResponse))
            return "error"
        }
    }

    if (UtilValidate.isNotEmpty(linkedTaskId)) {
        Map createEventAssociationCtx = [
                userLogin       : userLogin,
                idFrom          : linkedTaskId,
                idTo            : createdEventId,
                eventAssocTypeId: "EVENT_ASSO_EML_TASK"
        ]

        Map createEventAssociationResponse = dispatcher.runSync("createEventAssociation", createEventAssociationCtx);
        if (ServiceUtil.isFailure(createEventAssociationResponse)) {
            return "error"
        }
    }
}

if (UtilValidate.isNotEmpty(linkedIssueId)) {
    Map createCommEventProjectItemAssocCtx = [
            userLogin           : userLogin,
            projectItemId       : linkedIssueId,
            communicationEventId: communicationEventId
    ]
    Map createCommEventProjectItemAssocCtxResponse = dispatcher.runSync("createCommEventProjectItemAssoc", createCommEventProjectItemAssocCtx)
    if (!ServiceUtil.isSuccess(createCommEventProjectItemAssocCtxResponse)) {
        request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createCommEventProjectItemAssocCtxResponse))
        return "error"
    }
}


dojoResponseMap = [
        notesCount: notesCount
]

mapper.writeValue(response.getWriter(), dojoResponseMap)
