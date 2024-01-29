package google


import org.apache.ofbiz.service.ServiceUtil

String userId = parameters.userId
String partyId = parameters.partyId
String messageId = parameters.messageId

Map syncGmailConversationCtx = [
        userId   : userId,
        partyId  : partyId,
        messageId : messageId,
        userLogin: userLogin
]

Map resyncGmailMsgCtxResp = dispatcher.runSync("reSyncEmailMessage", syncGmailConversationCtx)

if (!ServiceUtil.isSuccess(resyncGmailMsgCtxResp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(resyncGmailMsgCtxResp))
    return "error"
}


request.setAttribute("_EVENT_MESSAGE_", "Message has been re-syncd successfully.")
return "success"