import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
module = "SyncGmailConversationThread.groovy"

String userId = parameters.userId
String partyId = parameters.partyId
String threadId = parameters.threadId
String taskId = parameters.taskId
String opportunityId = parameters.opportunityId
String projectItemId = parameters.projectItemId

Map syncGmailConversationCtx = [
        userId   : userId,
        partyId  : partyId,
        threadId : threadId,
        userLogin: userLogin
]

Debug.logInfo("Beginning to Sync Emails with Input: " +  syncGmailConversationCtx, module);

Map syncGmailConversationCtxResp = dispatcher.runSync("syncEmailMessageConversationThread", syncGmailConversationCtx)
Debug.logInfo("Emails Sync Complete: " +  syncGmailConversationCtxResp, module);

if (!ServiceUtil.isSuccess(syncGmailConversationCtxResp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(syncGmailConversationCtxResp))
    Debug.logError("Error while syncing emails: " + syncGmailConversationCtxResp, module)
    return "error"
}

if (UtilValidate.isNotEmpty(taskId)) {
    Debug.logInfo("Linking Emails with task: " + taskId, module);
    List<GenericValue> commEvents = from("CommunicationEvent").where("mailboxMessageId", threadId).queryList()

    if (UtilValidate.isNotEmpty(commEvents)) {
        Debug.logInfo("Comm Events found for thread : " + threadId + " : " + commEvents.size(), module)
        for (GenericValue commEvent : commEvents) {
            // check if comm event is already associated with task or not
            GenericValue taskCommAssoc = from("TaskCommunicationEventAssoc").where("taskId", taskId, "communicationEventId", commEvent.communicationEventId).queryFirst()
            Debug.logInfo("Existing TaskCommEvnetAssoc: " + taskCommAssoc, module)

            if (UtilValidate.isEmpty(taskCommAssoc)) {
                //associate with task
                Map createTaskAssocWithCommEventCtx = [
                        userLogin           : userLogin,
                        taskId              : taskId,
                        createdByPartyId    : partyId,
                        communicationEventId: commEvent.communicationEventId
                ]
                Debug.logInfo("Creating TaskCommEventAssoc with: " + createTaskAssocWithCommEventCtx, module)
                Map createTaskAssocWithCommEventCtxResponse = dispatcher.runSync("createTaskCommunicationEventAssoc", createTaskAssocWithCommEventCtx)
                if (!ServiceUtil.isSuccess(createTaskAssocWithCommEventCtxResponse)) {
                    Debug.logError("Error creating TaskCommEventAssoc: " + createTaskAssocWithCommEventCtxResponse, module)
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createTaskAssocWithCommEventCtxResponse))
                    return "error"
                }
            }
        }
    } else {
        // This could happen when the first msg of the thread (i.e threadId == msgId) is missing
        Debug.logInfo("No msg found with message id: " + threadId + ", trying to get msgs under this thread instead", module)
        commEvents = from("CommunicationEvent").where("mailboxThreadId", threadId).queryList()

        if (UtilValidate.isNotEmpty(commEvents)) {
            Debug.logInfo("Comm Events found under thread : " + threadId + " : " + commEvents.size(), module)
            for (GenericValue commEvent : commEvents) {
                // check if comm event is already associated with task or not
                GenericValue taskCommAssoc = from("TaskCommunicationEventAssoc").where("taskId", taskId, "communicationEventId", commEvent.communicationEventId).queryFirst()
                Debug.logInfo("Existing TaskCommEvnetAssoc: " + taskCommAssoc, module)

                if (UtilValidate.isEmpty(taskCommAssoc)) {
                    //associate with task
                    Map createTaskAssocWithCommEventCtx = [
                            userLogin           : userLogin,
                            taskId              : taskId,
                            createdByPartyId    : partyId,
                            communicationEventId: commEvent.communicationEventId
                    ]
                    Debug.logInfo("Creating TaskCommEventAssoc with: " + createTaskAssocWithCommEventCtx, module)
                    Map createTaskAssocWithCommEventCtxResponse = dispatcher.runSync("createTaskCommunicationEventAssoc", createTaskAssocWithCommEventCtx)
                    if (!ServiceUtil.isSuccess(createTaskAssocWithCommEventCtxResponse)) {
                        Debug.logError("Error creating TaskCommEventAssoc: " + createTaskAssocWithCommEventCtxResponse, module)
                        request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createTaskAssocWithCommEventCtxResponse))
                        return "error"
                    }
                }
            } // End of iterating commEvents
        }
    }
}

//link message to opportunity
if (UtilValidate.isNotEmpty(opportunityId)) {
    List<GenericValue> commEvents = from("CommunicationEvent").where("mailboxMessageId", threadId).queryList()
    if (UtilValidate.isNotEmpty(commEvents)) {
        for (GenericValue commEvent : commEvents) {
            // check if comm event is already associated with opportunity or not
            GenericValue taskCommAssoc = from("OpportunityCommunicationAssoc").where("opportunityId", opportunityId, "communicationEventId", commEvent.communicationEventId).queryFirst()
            if (UtilValidate.isEmpty(taskCommAssoc)) {
                //associate with opportunity
                Map createOpportunityCommunicationAssocCtx = [
                        userLogin           : userLogin,
                        opportunityId       : opportunityId,
                        communicationEventId: commEvent.communicationEventId
                ]
                Map createOpportunityCommunicationAssocResp = dispatcher.runSync("createOpportunityCommunicationAssoc", createOpportunityCommunicationAssocCtx)
                if (!ServiceUtil.isSuccess(createOpportunityCommunicationAssocResp)) {
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createOpportunityCommunicationAssocResp))
                    return "error"
                }
            }
        }
    }
}

//link message to issue
if (UtilValidate.isNotEmpty(projectItemId)) {
    List<GenericValue> commEvents = from("CommunicationEvent").where("mailboxMessageId", threadId).queryList()
    if (UtilValidate.isNotEmpty(commEvents)) {
        for (GenericValue commEvent : commEvents) {
            // check if comm event is already associated with opportunity or not
            GenericValue issueCommAssoc = from("ProjectItemCommunicationEventAssoc").where("projectItemId", projectItemId, "communicationEventId", commEvent.communicationEventId).queryFirst()
            if (UtilValidate.isEmpty(issueCommAssoc)) {
                //associate with issue
                Map createCommEventProjectItemAssocCtx = [
                        userLogin           : userLogin,
                        projectItemId       : projectItemId,
                        communicationEventId: commEvent.communicationEventId
                ]
                Map createCommEventProjectItemAssocCtxResponse = dispatcher.runSync("createCommEventProjectItemAssoc", createCommEventProjectItemAssocCtx)
                if (!ServiceUtil.isSuccess(createCommEventProjectItemAssocCtxResponse)) {
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(createCommEventProjectItemAssocCtxResponse))
                    return "error"
                }
            }
        }
    }
}

request.setAttribute("_EVENT_MESSAGE_", "Message has been linked successfully.")
return "success"