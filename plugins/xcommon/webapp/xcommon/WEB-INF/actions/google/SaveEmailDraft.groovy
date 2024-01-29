import com.simbaquartz.xcommon.collections.FastList
import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.service.ServiceUtil

String sendTo = parameters.sendTo
String sendCc = parameters.sendCc
String sendBcc = parameters.sendBcc
String subject = parameters.subject
String emailBody = parameters.emailBody
String linkedTaskId = parameters.linkedTaskId
String linkedQuoteId = parameters.linkedQuoteId
String linkedOpportunityId = parameters.linkedOpportunityId
String attachmentSelected = parameters.attachmentSelected
String linkedQuoteAttachments = parameters.linkedQuoteAttachments
String linkedOpportunityAttachments = parameters.linkedOpportunityAttachments
String linkedTaskAttachments = parameters.linkedTaskAttachments
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

String emailBodyStr = ""
if (UtilValidate.isNotEmpty(emailBody)) {
    List textParts = StringUtil.split(emailBody, "\\R+")
    StringBuffer textDataStr = new StringBuffer()
    for (Object textPart : textParts) {
        textDataStr.append(textPart)
    }
    emailBodyStr = textDataStr.toString()
    emailBodyStr = emailBodyStr.replaceAll("'", "\\'")
}

boolean firstContentId = true
StringBuffer attachmentBuilder = new StringBuffer()
if(UtilValidate.isNotEmpty(attachmentContentIds)){
    attachmentContentIds.each {contentId ->
        if (firstContentId) {
            firstContentId = false
        } else {
            attachmentBuilder.append(',')
        }
        attachmentBuilder.append(contentId)
    }
}
Map saveEmailDraftCtx = [
        "userLogin"           : userLogin,
        "toString"            : sendTo,
        "ccString"            : sendCc,
        "bccString"           : sendBcc,
        "subject"             : subject,
        "content"             : emailBodyStr,
        "attachmentContentIds": attachmentBuilder.toString(),
        "linkedQuoteId"       : linkedQuoteId,
        "linkedTaskId"        : linkedTaskId,
        "linkedOpportunityId" : linkedOpportunityId
]

Map<String, Object> resp = dispatcher.runSync("fsdCreateEmailDraft", saveEmailDraftCtx)

if (ServiceUtil.isError(resp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(resp))
    return "error"
}

return "success"
