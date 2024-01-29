import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

String userId = parameters.userId
String partyId = parameters.partyId
String threadId = parameters.threadId
String taskId = parameters.taskId

Map syncGmailConversationCtx = [
        userId   : userId,
        partyId  : partyId,
        threadId : threadId,
        userLogin: userLogin
]

// 1. First sync the thread, so that we have up-to-date info
Map syncGmailConversationCtxResp = dispatcher.runSync("syncEmailMessageConversationThread", syncGmailConversationCtx)

if (!ServiceUtil.isSuccess(syncGmailConversationCtxResp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(syncGmailConversationCtxResp))
    return "error"
}

// 2. Create task using mail message details

// 3. Add Task Roles

// 4. Associate task-commevent: TaskCommunicationEventAssoc


/*if (UtilValidate.isNotEmpty(taskId)) {
    List<GenericValue> commEvents = from("CommunicationEvent").where("mailboxMessageId", threadId).queryList()
    if (UtilValidate.isNotEmpty(commEvents)) {
        for (GenericValue commEvent : commEvents) {
            // check if comm event is already associated with task or not
            GenericValue taskCommAssoc = from("TaskCommunicationEventAssoc").where("taskId", taskId, "communicationEventId", commEvent.communicationEventId).queryFirst()
            if (UtilValidate.isEmpty(taskCommAssoc)) {
                //associate with task
                Map createTaskAssocWithCommEventCtx = [
                        userLogin           : userLogin,
                        taskId              : taskId,
                        communicationEventId: commEvent.communicationEventId
                ]
                Map createTaskAssocWithCommEventCtxResponse = dispatcher.runSync("createTaskCommunicationEventAssoc", createTaskAssocWithCommEventCtx)
                if (!ServiceUtil.isSuccess(createTaskAssocWithCommEventCtxResponse)) {
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createTaskAssocWithCommEventCtxResponse))
                    return "error"
                }
            }
        }
    }
}*/

return "success"