package com.fidelissd.zcp.xcommon.services.activity;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.google.common.collect.Iterables;
import com.ibm.common.activitystreams.ASObject;
import com.ibm.common.activitystreams.Activity;
import com.ibm.common.activitystreams.IO;
import com.ibm.common.activitystreams.LinkValue;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static com.ibm.common.activitystreams.IO.makeDefaultPrettyPrint;
import static com.ibm.common.activitystreams.Makers.activity;
import static com.ibm.common.activitystreams.Makers.object;

/**
 * Currently, Activity Services are implemented using Activity Streams 2.0.
 * However, only the 2 main services: registerActivity and readActivity directly
 * refer to Activity Streams. Other services use these 2 services.
 */
public class ActivityServices {

    public static final String module = ActivityServices.class.getName();

    // The IO object handles all of the reading and writing of the object
    private static final IO io = makeDefaultPrettyPrint();

    /**
     * Registers the service using Activity Streams 2.0. Variious parameters about the
     * activity are captures as an Activity Streams activity, serialized and saved in
     * the database. An activityId is returned by the service.
     *
     * @param dctx
     * @param context
     * @return serviceResult
     */
    public static Map<String, Object> registerActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();

        String verb = (String) context.get("verb");
        String taskId = (String) context.get("taskId");
        String custRequestId = (String) context.get("custRequestId");
        String projectItemId = (String) context.get("projectItemId");
        String projectId = (String) context.get("projectId");
        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String tagId = (String) context.get("tagId");
        String tagName = (String) context.get("tagName");
        String oldOpportunityStageId = (String) context.get("oldOpportunityStageId");
        String newOpportunityStageId = (String) context.get("newOpportunityStageId");
        String oldProjectItemStatusId = (String) context.get("oldProjectItemStatusId");
        String newProjectItemStatusId = (String) context.get("newProjectItemStatusId");
        String projectItemAssigneeId = (String) context.get("projectItemAssigneeId");
        String oldProjectItemAssigneeId = (String) context.get("oldProjectItemAssigneeId");
        String newProjectItemAssigneeId = (String) context.get("newProjectItemAssigneeId");
        String oldProjectItemPriorityId = (String) context.get("oldProjectItemPriorityId");
        String newProjectItemPriorityId = (String) context.get("newProjectItemPriorityId");
        String oldProjectItemTypeId = (String) context.get("oldProjectItemTypeId");
        String newProjectItemTypeId = (String) context.get("newProjectItemTypeId");
        String issueIdTo = (String) context.get("issueIdTo");
        String quoteId = (String) context.get("quoteId");
        String noteInfo = (String) context.get("noteInfo");
        String description = (String) context.get("description");
        String requirementStatus = (String) context.get("requirementStatus");
        String noteName = (String) context.get("noteName");
        String noteTypeId = (String) context.get("noteTypeId");
        String isNoteInternal = (String) context.get("isNoteInternal");
        String actorType = (String) context.get("actorType");
        String actorId = (String) context.get("actorId");
        String actorUrl = (String) context.get("actorUrl");
        String actorName = (String) context.get("actorName");
        String objectType = (String) context.get("objectType");
        String objectId = (String) context.get("objectId");
        String objectUrl = (String) context.get("objectUrl");
        String objectName = (String) context.get("objectName");
        String startTime = (String) context.get("startTime");
        String endTime = (String) context.get("endTime");
        String to = (String) context.get("to");
        String cc = (String) context.get("cc");
        String bcc = (String) context.get("bcc");
        String venue = (String) context.get("venue");
        String phoneNumber = (String) context.get("phoneNumber");
        String subject = (String) context.get("subject");
        String content = (String) context.get("content");
        String oldStatus = (String) context.get("oldStatus");
        String newStatus = (String) context.get("newStatus");
        String oldQuoteType = (String) context.get("oldQuoteType");
        String newQuoteType = (String) context.get("newQuoteType");
        String solicitationNumber = (String) context.get("solicitationNumber");
        String referenceNumber = (String) context.get("referenceNumber");
        String contentName = (String) context.get("contentName");
        String productId = (String) context.get("productId");
        String quoteUnitPrice = (String) context.get("quoteUnitPrice");
        String opportunityUnitPrice = (String) context.get("opportunityUnitPrice");
        String quantity = (String) context.get("quantity");
        String discountAmount = (String) context.get("discountAmount");
        String discountPercentage = (String) context.get("discountPercentage");
        String invoiceId = (String) context.get("invoiceId");
        String amount = (String) context.get("amount");
        String contactPartyName = (String) context.get("contactPartyName");
        String oldContactPartyName = (String) context.get("oldContactPartyName");
        String emailRecipients = (String) context.get("emailRecipients");
        String callLogSummary = (String) context.get("callLogSummary");
        String roleTypeDescription = (String) context.get("roleTypeDescription");
        String opportunityLinkedTaskId = (String) context.get("opportunityLinkedTaskId");
        String opportunityEventId = (String) context.get("opportunityEventId");
        String partyId = (String) context.get("partyId");
        String emailAddress = (String) context.get("emailAddress");

        String oldAmount = (String) context.get("oldAmount");
        String newAmount = (String) context.get("newAmount");
        String oldCurrency = (String) context.get("oldCurrency");
        String newCurrency = (String) context.get("newCurrency");
        String oldPriority = (String) context.get("oldPriority");
        String newPriority = (String) context.get("newPriority");
        try {
            // Create the Activity... The API uses a Fluent Generator pattern
            Activity activity =
                    activity()
                            .verb(verb)
                            .set("taskId", taskId)
                            .set("custRequestId", custRequestId)
                            .set("salesOpportunityId", salesOpportunityId)
                            .set("projectItemId", projectItemId)
                            .set("projectId", projectId)
                            .set("quoteId", quoteId)
                            .set("partyId", partyId)
                            .set("invoiceId", invoiceId)
                            .set("noteInfo", noteInfo)
                            .set("description", description)
                            .set("requirementStatus", requirementStatus)
                            .set("noteName", noteName)
                            .set("noteTypeId", noteTypeId)
                            .set("isNoteInternal", isNoteInternal)
                            .actor(
                                    object(actorType)
                                            .id(actorId)
                                            .url(actorUrl)
                                            .displayName(actorName)
                            )
                            .object(
                                    object(objectType)
                                            .id(objectId)
                                            .url(objectUrl)
                                            .displayName(objectName)
                                            .set("to", to)
                                            .set("cc", cc)
                                            .set("bcc", bcc)
                                            .set("venue", venue)
                                            .set("phoneNumber", phoneNumber)
                                            .summary(subject)
                                            .content(content)
                                            .set("oldStatus", oldStatus)
                                            .set("newStatus", newStatus)
                                            .set("oldQuoteType", oldQuoteType)
                                            .set("newQuoteType", newQuoteType)
                                            .set("solicitationNumber", solicitationNumber)
                                            .set("referenceNumber", referenceNumber)
                                            .set("contentName", contentName)
                                            .set("productId", productId)
                                            .set("quoteUnitPrice", quoteUnitPrice)
                                            .set("tagId", tagId)
                                            .set("tagName", tagName)
                                            .set("opportunityUnitPrice", opportunityUnitPrice)
                                            .set("newOpportunityStageId", newOpportunityStageId)
                                            .set("oldOpportunityStageId", oldOpportunityStageId)
                                            .set("newProjectItemStatusId", newProjectItemStatusId)
                                            .set("oldProjectItemStatusId", oldProjectItemStatusId)
                                            .set("projectItemAssigneeId", projectItemAssigneeId)
                                            .set("newProjectItemAssigneeId", newProjectItemAssigneeId)
                                            .set("oldProjectItemAssigneeId", oldProjectItemAssigneeId)
                                            .set("newProjectItemPriorityId", newProjectItemPriorityId)
                                            .set("oldProjectItemPriorityId", oldProjectItemPriorityId)
                                            .set("newProjectItemTypeId", newProjectItemTypeId)
                                            .set("oldProjectItemTypeId", oldProjectItemTypeId)
                                            .set("issueIdTo", issueIdTo)
                                            .set("quantity", quantity)
                                            .set("discountAmount", discountAmount)
                                            .set("discountPercentage", discountPercentage)
                                            .set("amount", amount)
                                            .set("contactPartyName", contactPartyName)
                                            .set("oldContactPartyName", oldContactPartyName)
                                            .set("callLogSummary", callLogSummary)
                                            .set("emailRecipients", emailRecipients)
                                            .set("roleTypeDescription", roleTypeDescription)
                                            .set("opportunityLinkedTaskId", opportunityLinkedTaskId)
                                            .set("opportunityEventId", opportunityEventId)
                                            .set("emailAddress", emailAddress)
                                            .set("projectItemId", projectItemId)
                                            .set("projectId", projectId)
                                            .set("oldCurrency", oldCurrency)
                                            .set("newCurrency", newCurrency)
                                            .set("oldPriority", oldPriority)
                                            .set("newPriority", newPriority)
                                            .set("oldAmount", oldAmount)
                                            .set("newAmount", newAmount)
                            )
                            .startTime(startTime == null ? null : DateTime.parse(startTime))
                            .endTime(endTime == null ? null : DateTime.parse(endTime))
                            .get();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            activity.writeTo(out, io);
            byte[] byteArray = out.toByteArray();

            String activityId = delegator.getNextSeqId("Activity");
            GenericValue act = delegator.makeValue("Activity");
            act.set("activityId", activityId);
            act.set("activity", byteArray);
            if (verb.contains("task")) {
                act.set("activityType", "task_activity");
                act.set("activityFor", taskId);
            }
            if (verb.contains("quote")) {
                act.set("activityType", "quote_activity");
                act.set("activityFor", quoteId);
            }
            if (verb.contains("Payment")) {
                act.set("activityType", "payment_activity");
                act.set("activityFor", invoiceId);
            }
            if (verb.contains("opportunity")) {
                act.set("activityType", "opportunity_activity");
                act.set("activityFor", salesOpportunityId);
            }
            if (verb.contains("issue")) {
                act.set("activityType", "project_item_activity");
                act.set("activityFor", projectItemId);
            }
            if (verb.contains("project")) {
                act.set("activityType", "project_activity");
                act.set("activityFor", projectId);
            }
            if (verb.contains("party") || verb.contains("employer") || verb.contains("call log summary")
                    || verb.contains("send an email") || verb.contains("contact")) {
                act.set("activityType", "party_activity");
                act.set("activityFor", partyId);
            }
            if (verb.contains("Converted")) {
                act.set("activityType", "deal_activity");
                act.set("activityFor", salesOpportunityId);
            }
            if (verb.contains("custRequest")) {
                act.set("activityType", "customer_request_activity");
                act.set("activityFor", custRequestId);
            }
            if (verb.contains("priority") || verb.contains("currency") || verb.contains("amount")) {
                act.set("activityType", "deal_update_activity");
                act.set("activityFor", salesOpportunityId);
            }
            act.create();

            serviceResult.put("activityId", activityId);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read an activity using activityId from the context and return the activity details
     * in serviceeResult.
     *
     * @param dctx
     * @param context
     * @return serviceResult
     */
    public static Map<String, Object> readActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();

        String activityId = (String) context.get("activityId");
        String verb = "";
        String taskId = "";
        String quoteId = "";
        String noteInfo = "";
        String description = "";
        String requirementStatus = "";
        String noteName = "";
        String noteTypeId = "";
        String isNoteInternal = "";
        String actorType = "";
        String actorId = "";
        String actorUrl = "";
        String actorName = "";
        String objectType = "";
        String objectId = "";
        String objectUrl = "";
        String objectName = "";
        String startTime = "";
        String endTime = "";
        String to = "";
        String cc = "";
        String bcc = "";
        String venue = "";
        String phoneNumber = "";
        String subject = "";
        String content = "";
        String oldStatus = "";
        String newStatus = "";
        String oldQuoteType = "";
        String newQuoteType = "";
        String solicitationNumber = "";
        String referenceNumber = "";
        String contentName = "";
        String productId = "";
        String quoteUnitPrice = "";
        String opportunityUnitPrice = "";
        String oldOpportunityStageId = "";
        String newOpportunityStageId = "";
        String oldProjectItemStatusId = "";
        String newProjectItemStatusId = "";
        String oldProjectItemAssigneeId = "";
        String newProjectItemAssigneeId = "";
        String oldProjectItemPriorityId = "";
        String newProjectItemPriorityId = "";
        String issueIdTo = "";
        String quantity = "";
        String discountAmount = "";
        String discountPercentage = "";
        String invoiceId = "";
        String amount = "";
        String contactPartyName = "";
        String roleTypeDescription = "";
        String opportunityLinkedTaskId = "";
        String opportunityEventId = "";
        String oldAmount = "";
        String newAmount = "";
        String oldCurrency = "";
        String newCurrency = "";
        String oldPriority = "";
        String newPriority = "";
        String emailAddress = "";

        Activity activity;

        try {
            GenericValue act = EntityQuery.use(delegator)
                    .from("Activity")
                    .where(UtilMisc.toMap("activityId", activityId))
                    .queryOne();

            if (UtilValidate.isNotEmpty(act)) {
                byte[] byteArray = (byte[]) act.get("activity");
                ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
                activity = io.readAsActivity(in);

                verb = activity.verb().toString();
                taskId = activity.getString("taskId", "") == null ? "" : activity.getString("taskId", "");
                quoteId = activity.getString("quoteId", "") == null ? "" : activity.getString("quoteId", "");
                invoiceId = activity.getString("invoiceId", "") == null ? "" : activity.getString("invoiceId", "");
                noteInfo = activity.getString("noteInfo", "") == null ? "" : activity.getString("noteInfo", "");
                description = activity.getString("description", "") == null ? "" : activity.getString("description", "");
                requirementStatus = activity.getString("requirementStatus", "") == null ? "" : activity.getString("requirementStatus", "");
                noteName = activity.getString("noteName", "") == null ? "" : activity.getString("noteName", "");
                noteTypeId = activity.getString("noteTypeId", "") == null ? "" : activity.getString("noteTypeId", "");
                isNoteInternal = activity.getString("isNoteInternal", "") == null ? "" : activity.getString("isNoteInternal", "");

                Iterable<LinkValue> lvs = activity.actor();
                LinkValue lv = Iterables.getFirst(lvs, null);
                ASObject aso = (ASObject) lv;
                actorType = aso.objectType() == null ? "" : aso.objectType().toString();
                actorId = aso.id() == null ? "" : aso.id().toString();
                actorUrl = aso.url() == null ? "" : aso.url().toString();
                actorName = aso.displayName() == null ? "" : aso.displayName().toString();

                lvs = activity.object();
                lv = Iterables.getFirst(lvs, null);
                aso = (ASObject) lv;
                objectType = aso.objectType() == null ? "" : aso.objectType().toString();
                objectId = aso.id() == null ? "" : aso.id().toString();
                objectUrl = aso.url() == null ? "" : aso.url().toString();
                objectName = aso.displayName() == null ? "" : aso.displayName().toString();
                to = aso.getString("to", "") == null ? "" : aso.getString("to", "");
                cc = aso.getString("cc", "") == null ? "" : aso.getString("cc", "");
                bcc = aso.getString("bcc", "") == null ? "" : aso.getString("bcc", "");
                venue = aso.getString("venue", "") == null ? "" : aso.getString("venue", "");
                phoneNumber = aso.getString("phoneNumber", "") == null ? "" : aso.getString("phoneNumber", "");
                subject = aso.summary() == null ? "" : aso.summary().toString();
                content = aso.content() == null ? "" : aso.content().toString();
                oldStatus = aso.getString("oldStatus", "") == null ? "" : aso.getString("oldStatus", "");
                newStatus = aso.getString("newStatus", "") == null ? "" : aso.getString("newStatus", "");
                oldQuoteType = aso.getString("oldQuoteType", "") == null ? "" : aso.getString("oldQuoteType", "");
                newQuoteType = aso.getString("newQuoteType", "") == null ? "" : aso.getString("newQuoteType", "");
                solicitationNumber = aso.getString("solicitationNumber", "") == null ? "" : aso.getString("solicitationNumber", "");
                referenceNumber = aso.getString("referenceNumber", "") == null ? "" : aso.getString("referenceNumber", "");
                contentName = aso.getString("contentName", "") == null ? "" : aso.getString("contentName", "");
                productId = aso.getString("productId", "") == null ? "" : aso.getString("productId", "");
                quoteUnitPrice = aso.getString("quoteUnitPrice", "") == null ? "" : aso.getString("quoteUnitPrice", "");
                opportunityUnitPrice = aso.getString("opportunityUnitPrice", "") == null ? "" : aso.getString("opportunityUnitPrice", "");
                oldOpportunityStageId = aso.getString("oldOpportunityStageId", "") == null ? "" : aso.getString("oldOpportunityStageId", "");
                newOpportunityStageId = aso.getString("newOpportunityStageId", "") == null ? "" : aso.getString("newOpportunityStageId", "");
                oldProjectItemStatusId = aso.getString("oldProjectItemStatusId", "") == null ? "" : aso.getString("oldProjectItemStatusId", "");
                newProjectItemStatusId = aso.getString("newProjectItemStatusId", "") == null ? "" : aso.getString("newProjectItemStatusId", "");
                oldProjectItemAssigneeId = aso.getString("oldProjectItemAssigneeId", "") == null ? "" : aso.getString("oldProjectItemAssigneeId", "");
                newProjectItemAssigneeId = aso.getString("newProjectItemAssigneeId", "") == null ? "" : aso.getString("newProjectItemAssigneeId", "");
                oldProjectItemPriorityId = aso.getString("oldProjectItemPriorityId", "") == null ? "" : aso.getString("oldProjectItemPriorityId", "");
                newProjectItemPriorityId = aso.getString("newProjectItemPriorityId", "") == null ? "" : aso.getString("newProjectItemPriorityId", "");
                issueIdTo = aso.getString("issueIdTo", "") == null ? "" : aso.getString("issueIdTo", "");
                quantity = aso.getString("quantity", "") == null ? "" : aso.getString("quantity", "");
                discountAmount = aso.getString("discountAmount", "") == null ? "" : aso.getString("discountAmount", "");
                discountPercentage = aso.getString("discountPercentage", "") == null ? "" : aso.getString("discountPercentage", "");
                amount = aso.getString("amount", "") == null ? "" : aso.getString("amount", "");
                contactPartyName = aso.getString("contactPartyName", "") == null ? "" : aso.getString("contactPartyName", "");
                roleTypeDescription = aso.getString("roleTypeDescription", "") == null ? "" : aso.getString("roleTypeDescription", "");
                opportunityLinkedTaskId = aso.getString("opportunityLinkedTaskId", "") == null ? "" : aso.getString("opportunityLinkedTaskId", "");
                opportunityEventId = aso.getString("opportunityEventId", "") == null ? "" : aso.getString("opportunityEventId", "");

                oldCurrency = aso.getString("oldCurrency", "") == null ? "" : aso.getString("oldCurrency", "");
                newCurrency = aso.getString("newCurrency", "") == null ? "" : aso.getString("newCurrency", "");
                oldPriority = aso.getString("oldPriority", "") == null ? "" : aso.getString("oldPriority", "");
                newPriority = aso.getString("newPriority", "") == null ? "" : aso.getString("newPriority", "");
                oldAmount = aso.getString("oldAmount", "") == null ? "" : aso.getString("oldAmount", "");
                newAmount = aso.getString("newAmount", "") == null ? "" : aso.getString("newAmount", "");

                emailAddress = aso.getString("emailAddress", "") == null ? "" : aso.getString("emailAddress", "");

                DateTime st = activity.startTime();
                if (st != null) {
                    startTime = st.toString();
                }
                DateTime et = activity.endTime();
                if (et != null) {
                    endTime = et.toString();
                }
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        serviceResult.put("verb", verb);
        serviceResult.put("taskId", taskId);
        serviceResult.put("quoteId", quoteId);
        serviceResult.put("noteInfo", noteInfo);
        serviceResult.put("description", description);
        serviceResult.put("requirementStatus", requirementStatus);
        serviceResult.put("noteName", noteName);
        serviceResult.put("noteTypeId", noteTypeId);
        serviceResult.put("isNoteInternal", isNoteInternal);
        serviceResult.put("actorType", actorType);
        serviceResult.put("actorId", actorId);
        serviceResult.put("actorUrl", actorUrl);
        serviceResult.put("actorName", actorName);
        serviceResult.put("objectType", objectType);
        serviceResult.put("objectId", objectId);
        serviceResult.put("objectUrl", objectUrl);
        serviceResult.put("objectName", objectName);
        serviceResult.put("startTime", startTime);
        serviceResult.put("endTime", endTime);
        serviceResult.put("to", to);
        serviceResult.put("cc", cc);
        serviceResult.put("bcc", bcc);
        serviceResult.put("venue", venue);
        serviceResult.put("phoneNumber", phoneNumber);
        serviceResult.put("subject", subject);
        serviceResult.put("content", content);
        serviceResult.put("oldStatus", oldStatus);
        serviceResult.put("newStatus", newStatus);
        serviceResult.put("oldQuoteType", oldQuoteType);
        serviceResult.put("newQuoteType", newQuoteType);
        serviceResult.put("solicitationNumber", solicitationNumber);
        serviceResult.put("referenceNumber", referenceNumber);
        serviceResult.put("contentName", contentName);
        serviceResult.put("productId", productId);
        serviceResult.put("quoteUnitPrice", quoteUnitPrice);
        serviceResult.put("opportunityUnitPrice", opportunityUnitPrice);
        serviceResult.put("newOpportunityStageId", newOpportunityStageId);
        serviceResult.put("oldOpportunityStageId", oldOpportunityStageId);
        serviceResult.put("newProjectItemStatusId", newProjectItemStatusId);
        serviceResult.put("oldProjectItemStatusId", oldProjectItemStatusId);
        serviceResult.put("newProjectItemAssigneeId", newProjectItemAssigneeId);
        serviceResult.put("oldProjectItemAssigneeId", oldProjectItemAssigneeId);
        serviceResult.put("newProjectItemPriorityId", newProjectItemPriorityId);
        serviceResult.put("oldProjectItemPriorityId", oldProjectItemPriorityId);
        serviceResult.put("issueIdTo", issueIdTo);
        serviceResult.put("quantity", quantity);
        serviceResult.put("discountAmount", discountAmount);
        serviceResult.put("discountPercentage", discountPercentage);
        serviceResult.put("invoiceId", invoiceId);
        serviceResult.put("amount", amount);
        serviceResult.put("roleTypeDescription", roleTypeDescription);
        serviceResult.put("opportunityLinkedTaskId", opportunityLinkedTaskId);
        serviceResult.put("opportunityEventId", opportunityEventId);
        serviceResult.put("contactPartyName", contactPartyName);
        serviceResult.put("oldAmount", oldAmount);
        serviceResult.put("newAmount", newAmount);
        serviceResult.put("oldCurrency", oldCurrency);
        serviceResult.put("newCurrency", newCurrency);
        serviceResult.put("oldPriority", oldPriority);
        serviceResult.put("newPriority", newPriority);
        serviceResult.put("emailAddress", emailAddress);
        return serviceResult;
    }

    public static Map<String, Object> prepareActivityInfo(GenericValue act) {
        Map<String, Object> activityInfo = ServiceUtil.returnSuccess();

        String activityId = act.getString("activityId");
        String verb = "";
        String taskId = "";
        String quoteId = "";
        String noteInfo = "";
        String description = "";
        String requirementStatus = "";
        String noteName = "";
        String noteTypeId = "";
        String isNoteInternal = "";
        String actorType = "";
        String actorId = "";
        String actorUrl = "";
        String actorName = "";
        String objectType = "";
        String objectId = "";
        String objectUrl = "";
        String objectName = "";
        String startTime = "";
        String endTime = "";
        String to = "";
        String cc = "";
        String bcc = "";
        String venue = "";
        String phoneNumber = "";
        String subject = "";
        String content = "";
        String oldStatus = "";
        String newStatus = "";
        String oldQuoteType = "";
        String newQuoteType = "";
        String solicitationNumber = "";
        String referenceNumber = "";
        String contentName = "";
        String productId = "";
        String quoteUnitPrice = "";
        String opportunityUnitPrice = "";
        String oldOpportunityStageId = "";
        String newOpportunityStageId = "";
        String oldProjectItemStatusId = "";
        String newProjectItemStatusId = "";
        String oldProjectItemAssigneeId = "";
        String newProjectItemAssigneeId = "";
        String oldProjectItemPriorityId = "";
        String newProjectItemPriorityId = "";
        String issueIdTo = "";
        String quantity = "";
        String discountAmount = "";
        String discountPercentage = "";
        String invoiceId = "";
        String amount = "";
        String contactPartyName = "";
        String roleTypeDescription = "";
        String opportunityLinkedTaskId = "";
        String opportunityEventId = "";
        String oldAmount = "";
        String newAmount = "";
        String oldCurrency = "";
        String newCurrency = "";
        String oldPriority = "";
        String newPriority = "";
        String emailAddress = "";

        Activity activity;

        try {
            if (UtilValidate.isNotEmpty(act)) {
                byte[] byteArray = (byte[]) act.get("activity");
                ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
                activity = io.readAsActivity(in);

                verb = activity.verb().toString();
                taskId = activity.getString("taskId", "") == null ? "" : activity.getString("taskId", "");
                quoteId = activity.getString("quoteId", "") == null ? "" : activity.getString("quoteId", "");
                invoiceId = activity.getString("invoiceId", "") == null ? "" : activity.getString("invoiceId", "");
                noteInfo = activity.getString("noteInfo", "") == null ? "" : activity.getString("noteInfo", "");
                description = activity.getString("description", "") == null ? "" : activity.getString("description", "");
                requirementStatus = activity.getString("requirementStatus", "") == null ? "" : activity.getString("requirementStatus", "");
                noteName = activity.getString("noteName", "") == null ? "" : activity.getString("noteName", "");
                noteTypeId = activity.getString("noteTypeId", "") == null ? "" : activity.getString("noteTypeId", "");
                isNoteInternal = activity.getString("isNoteInternal", "") == null ? "" : activity.getString("isNoteInternal", "");

                Iterable<LinkValue> lvs = activity.actor();
                LinkValue lv = Iterables.getFirst(lvs, null);
                ASObject aso = (ASObject) lv;
                actorType = aso.objectType() == null ? "" : aso.objectType().toString();
                actorId = aso.id() == null ? "" : aso.id().toString();
                actorUrl = aso.url() == null ? "" : aso.url().toString();
                actorName = aso.displayName() == null ? "" : aso.displayName().toString();

                lvs = activity.object();
                lv = Iterables.getFirst(lvs, null);
                aso = (ASObject) lv;
                objectType = aso.objectType() == null ? "" : aso.objectType().toString();
                objectId = aso.id() == null ? "" : aso.id().toString();
                objectUrl = aso.url() == null ? "" : aso.url().toString();
                objectName = aso.displayName() == null ? "" : aso.displayName().toString();
                to = aso.getString("to", "") == null ? "" : aso.getString("to", "");
                cc = aso.getString("cc", "") == null ? "" : aso.getString("cc", "");
                bcc = aso.getString("bcc", "") == null ? "" : aso.getString("bcc", "");
                venue = aso.getString("venue", "") == null ? "" : aso.getString("venue", "");
                phoneNumber = aso.getString("phoneNumber", "") == null ? "" : aso.getString("phoneNumber", "");
                subject = aso.summary() == null ? "" : aso.summary().toString();
                content = aso.content() == null ? "" : aso.content().toString();
                oldStatus = aso.getString("oldStatus", "") == null ? "" : aso.getString("oldStatus", "");
                newStatus = aso.getString("newStatus", "") == null ? "" : aso.getString("newStatus", "");
                oldQuoteType = aso.getString("oldQuoteType", "") == null ? "" : aso.getString("oldQuoteType", "");
                newQuoteType = aso.getString("newQuoteType", "") == null ? "" : aso.getString("newQuoteType", "");
                solicitationNumber = aso.getString("solicitationNumber", "") == null ? "" : aso.getString("solicitationNumber", "");
                referenceNumber = aso.getString("referenceNumber", "") == null ? "" : aso.getString("referenceNumber", "");
                contentName = aso.getString("contentName", "") == null ? "" : aso.getString("contentName", "");
                productId = aso.getString("productId", "") == null ? "" : aso.getString("productId", "");
                quoteUnitPrice = aso.getString("quoteUnitPrice", "") == null ? "" : aso.getString("quoteUnitPrice", "");
                opportunityUnitPrice = aso.getString("opportunityUnitPrice", "") == null ? "" : aso.getString("opportunityUnitPrice", "");
                oldOpportunityStageId = aso.getString("oldOpportunityStageId", "") == null ? "" : aso.getString("oldOpportunityStageId", "");
                newOpportunityStageId = aso.getString("newOpportunityStageId", "") == null ? "" : aso.getString("newOpportunityStageId", "");
                oldProjectItemStatusId = aso.getString("oldProjectItemStatusId", "") == null ? "" : aso.getString("oldProjectItemStatusId", "");
                newProjectItemStatusId = aso.getString("newProjectItemStatusId", "") == null ? "" : aso.getString("newProjectItemStatusId", "");
                oldProjectItemAssigneeId = aso.getString("oldProjectItemAssigneeId", "") == null ? "" : aso.getString("oldProjectItemAssigneeId", "");
                newProjectItemAssigneeId = aso.getString("newProjectItemAssigneeId", "") == null ? "" : aso.getString("newProjectItemAssigneeId", "");
                oldProjectItemPriorityId = aso.getString("oldProjectItemPriorityId", "") == null ? "" : aso.getString("oldProjectItemPriorityId", "");
                newProjectItemPriorityId = aso.getString("newProjectItemPriorityId", "") == null ? "" : aso.getString("newProjectItemPriorityId", "");
                issueIdTo = aso.getString("issueIdTo", "") == null ? "" : aso.getString("issueIdTo", "");
                quantity = aso.getString("quantity", "") == null ? "" : aso.getString("quantity", "");
                discountAmount = aso.getString("discountAmount", "") == null ? "" : aso.getString("discountAmount", "");
                discountPercentage = aso.getString("discountPercentage", "") == null ? "" : aso.getString("discountPercentage", "");
                amount = aso.getString("amount", "") == null ? "" : aso.getString("amount", "");
                contactPartyName = aso.getString("contactPartyName", "") == null ? "" : aso.getString("contactPartyName", "");
                roleTypeDescription = aso.getString("roleTypeDescription", "") == null ? "" : aso.getString("roleTypeDescription", "");
                opportunityLinkedTaskId = aso.getString("opportunityLinkedTaskId", "") == null ? "" : aso.getString("opportunityLinkedTaskId", "");
                opportunityEventId = aso.getString("opportunityEventId", "") == null ? "" : aso.getString("opportunityEventId", "");

                oldCurrency = aso.getString("oldCurrency", "") == null ? "" : aso.getString("oldCurrency", "");
                newCurrency = aso.getString("newCurrency", "") == null ? "" : aso.getString("newCurrency", "");
                oldPriority = aso.getString("oldPriority", "") == null ? "" : aso.getString("oldPriority", "");
                newPriority = aso.getString("newPriority", "") == null ? "" : aso.getString("newPriority", "");
                oldAmount = aso.getString("oldAmount", "") == null ? "" : aso.getString("oldAmount", "");
                newAmount = aso.getString("newAmount", "") == null ? "" : aso.getString("newAmount", "");

                emailAddress = aso.getString("emailAddress", "") == null ? "" : aso.getString("emailAddress", "");

                DateTime st = activity.startTime();
                if (st != null) {
                    startTime = st.toString();
                }
                DateTime et = activity.endTime();
                if (et != null) {
                    endTime = et.toString();
                }
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        activityInfo.put("activityId", activityId);
        activityInfo.put("verb", verb);
        activityInfo.put("taskId", taskId);
        activityInfo.put("quoteId", quoteId);
        activityInfo.put("noteInfo", noteInfo);
        activityInfo.put("description", description);
        activityInfo.put("requirementStatus", requirementStatus);
        activityInfo.put("noteName", noteName);
        activityInfo.put("noteTypeId", noteTypeId);
        activityInfo.put("isNoteInternal", isNoteInternal);
        activityInfo.put("actorType", actorType);
        activityInfo.put("actorId", actorId);
        activityInfo.put("actorUrl", actorUrl);
        activityInfo.put("actorName", actorName);
        activityInfo.put("objectType", objectType);
        activityInfo.put("objectId", objectId);
        activityInfo.put("objectUrl", objectUrl);
        activityInfo.put("objectName", objectName);
        activityInfo.put("startTime", startTime);
        activityInfo.put("endTime", endTime);
        activityInfo.put("to", to);
        activityInfo.put("cc", cc);
        activityInfo.put("bcc", bcc);
        activityInfo.put("venue", venue);
        activityInfo.put("phoneNumber", phoneNumber);
        activityInfo.put("subject", subject);
        activityInfo.put("content", content);
        activityInfo.put("oldStatus", oldStatus);
        activityInfo.put("newStatus", newStatus);
        activityInfo.put("oldQuoteType", oldQuoteType);
        activityInfo.put("newQuoteType", newQuoteType);
        activityInfo.put("solicitationNumber", solicitationNumber);
        activityInfo.put("referenceNumber", referenceNumber);
        activityInfo.put("contentName", contentName);
        activityInfo.put("productId", productId);
        activityInfo.put("quoteUnitPrice", quoteUnitPrice);
        activityInfo.put("opportunityUnitPrice", opportunityUnitPrice);
        activityInfo.put("newOpportunityStageId", newOpportunityStageId);
        activityInfo.put("oldOpportunityStageId", oldOpportunityStageId);
        activityInfo.put("newProjectItemStatusId", newProjectItemStatusId);
        activityInfo.put("oldProjectItemStatusId", oldProjectItemStatusId);
        activityInfo.put("newProjectItemAssigneeId", newProjectItemAssigneeId);
        activityInfo.put("oldProjectItemAssigneeId", oldProjectItemAssigneeId);
        activityInfo.put("newProjectItemPriorityId", newProjectItemPriorityId);
        activityInfo.put("oldProjectItemPriorityId", oldProjectItemPriorityId);
        activityInfo.put("issueIdTo", issueIdTo);
        activityInfo.put("quantity", quantity);
        activityInfo.put("discountAmount", discountAmount);
        activityInfo.put("discountPercentage", discountPercentage);
        activityInfo.put("invoiceId", invoiceId);
        activityInfo.put("amount", amount);
        activityInfo.put("roleTypeDescription", roleTypeDescription);
        activityInfo.put("opportunityLinkedTaskId", opportunityLinkedTaskId);
        activityInfo.put("opportunityEventId", opportunityEventId);
        activityInfo.put("contactPartyName", contactPartyName);
        activityInfo.put("oldAmount", oldAmount);
        activityInfo.put("newAmount", newAmount);
        activityInfo.put("oldCurrency", oldCurrency);
        activityInfo.put("newCurrency", newCurrency);
        activityInfo.put("oldPriority", oldPriority);
        activityInfo.put("newPriority", newPriority);
        activityInfo.put("emailAddress", emailAddress);
        return activityInfo;
    }

    /**
     * Save information about a create quote activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCreateQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Create quote");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectType", context.get("quoteType"));
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("objectUrl", context.get("quoteUrl"));
            ctx.put("objectName", context.get("quoteName"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a create quote activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readCreateQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Create quote")) {
                serviceResult.put("creatorType", resp.get("actorType"));
                serviceResult.put("creatorId", resp.get("actorId"));
                serviceResult.put("creatorUrl", resp.get("actorUrl"));
                serviceResult.put("creatorName", resp.get("actorName"));
                serviceResult.put("quoteType", resp.get("objectType"));
                serviceResult.put("quoteId", resp.get("objectId"));
                serviceResult.put("quoteUrl", resp.get("objectUrl"));
                serviceResult.put("quoteName", resp.get("objectName"));
                serviceResult.put("createdTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Save information about an update quote activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Update quote");
            ctx.put("actorType", context.get("updaterType"));
            ctx.put("actorId", context.get("updaterId"));
            ctx.put("actorUrl", context.get("updaterUrl"));
            ctx.put("oldQuoteType", context.get("oldQuoteType"));
            ctx.put("newQuoteType", context.get("newQuoteType"));
            ctx.put("actorName", context.get("updaterName"));
            ctx.put("objectType", context.get("quoteType"));
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("objectUrl", context.get("quoteUrl"));
            ctx.put("objectName", context.get("quoteName"));
            ctx.put("startTime", context.get("updatedTime"));
            ctx.put("endTime", context.get("updatedTime"));
            ctx.put("solicitationNumber", context.get("solicitationNumber"));
            ctx.put("referenceNumber", context.get("referenceNumber"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a update quote activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readUpdateQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Update quote")) {
                serviceResult.put("updaterType", resp.get("actorType"));
                serviceResult.put("updaterId", resp.get("actorId"));
                serviceResult.put("updaterUrl", resp.get("actorUrl"));
                serviceResult.put("updaterName", resp.get("actorName"));
                serviceResult.put("quoteType", resp.get("objectType"));
                serviceResult.put("quoteId", resp.get("objectId"));
                serviceResult.put("quoteUrl", resp.get("objectUrl"));
                serviceResult.put("quoteName", resp.get("objectName"));
                serviceResult.put("updatedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a note added to a quote.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddNoteToQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add note to quote");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("noteInfo", context.get("noteInfo"));
            ctx.put("noteName", context.get("noteName"));
            ctx.put("noteTypeId", context.get("noteTypeId"));
            ctx.put("isNoteInternal", context.get("isNoteInternal"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("adderId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Quote note");
            ctx.put("objectId", context.get("noteId"));
            ctx.put("objectUrl", context.get("quoteUrl"));
            ctx.put("objectName", context.get("quoteName"));
            ctx.put("startTime", context.get("addedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a note added to a quote.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readAddNoteToQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");

            if (UtilValidate.isNotEmpty(verb) && verb.equals("Add note to quote")) {
                serviceResult.put("quoteId", resp.get("quoteId"));
                serviceResult.put("noteInfo", resp.get("noteInfo"));
                serviceResult.put("noteName", resp.get("noteName"));
                serviceResult.put("noteTypeId", resp.get("noteTypeId"));
                serviceResult.put("isNoteInternal", resp.get("isNoteInternal"));
                serviceResult.put("adderType", resp.get("actorType"));
                serviceResult.put("adderId", resp.get("actorId"));
                serviceResult.put("adderUrl", resp.get("actorUrl"));
                serviceResult.put("adderName", resp.get("actorName"));
                serviceResult.put("quoteUrl", resp.get("objectUrl"));
                serviceResult.put("quoteName", resp.get("objectName"));
                serviceResult.put("addedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a mail sent for a quote.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerSendQuoteEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Send quote email");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("actorType", context.get("senderType"));
            ctx.put("actorId", context.get("senderId"));
            ctx.put("actorUrl", context.get("senderUrl"));
            ctx.put("actorName", context.get("senderName"));
            ctx.put("objectType", "Quote email");
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("startTime", context.get("sentTime"));
            ctx.put("endTime", context.get("sentTime"));
            ctx.put("to", context.get("sendTo"));
            ctx.put("cc", context.get("sendCC"));
            ctx.put("bcc", context.get("sendBCC"));
            ctx.put("subject", context.get("subject"));
            ctx.put("content", context.get("content"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a mail sent for a quote.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readSendQuoteEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Send quote email")) {
                serviceResult.put("quoteId", resp.get("quoteId"));
                serviceResult.put("senderType", resp.get("actorType"));
                serviceResult.put("senderId", resp.get("actorId"));
                serviceResult.put("senderUrl", resp.get("actorUrl"));
                serviceResult.put("senderName", resp.get("actorName"));
                serviceResult.put("sentTime", resp.get("startTime"));
                serviceResult.put("sendTo", resp.get("to"));
                serviceResult.put("sendCC", resp.get("cc"));
                serviceResult.put("sendBCC", resp.get("bcc"));
                serviceResult.put("subject", resp.get("subject"));
                serviceResult.put("content", resp.get("content"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Save information about a create task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCreateTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Create task");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectType", context.get("taskType"));
            ctx.put("objectId", context.get("taskId"));
            ctx.put("taskId", context.get("taskId"));
            ctx.put("objectUrl", context.get("taskUrl"));
            ctx.put("objectName", context.get("taskName"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a create task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readCreateTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Create task")) {
                serviceResult.put("creatorType", resp.get("actorType"));
                serviceResult.put("creatorId", resp.get("actorId"));
                serviceResult.put("creatorUrl", resp.get("actorUrl"));
                serviceResult.put("creatorName", resp.get("actorName"));
                serviceResult.put("taskType", resp.get("objectType"));
                serviceResult.put("taskId", resp.get("objectId"));
                serviceResult.put("taskUrl", resp.get("objectUrl"));
                serviceResult.put("taskName", resp.get("objectName"));
                serviceResult.put("createdTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Save information about a update task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Update task");
            ctx.put("actorType", context.get("updaterType"));
            ctx.put("actorId", context.get("updaterId"));
            ctx.put("actorUrl", context.get("updaterUrl"));
            ctx.put("actorName", context.get("updaterName"));
            ctx.put("objectType", context.get("taskType"));
            ctx.put("objectId", context.get("taskId"));
            ctx.put("taskId", context.get("taskId"));
            ctx.put("objectUrl", context.get("taskUrl"));

            ctx.put("startTime", context.get("updatedTime"));
            ctx.put("endTime", context.get("updatedTime"));

            String taskName = (String) context.get("taskName");
            String taskId = (String) context.get("taskId");

            if (UtilValidate.isNotEmpty(taskName)) {
                ctx.put("objectName", context.get("taskName"));
            } else {
                GenericValue task = EntityQuery.use(delegator).from("TaskHeader").where("taskId", taskId).queryOne();
                String nameOfTask = task.getString("taskName");
                ctx.put("objectName", nameOfTask);
            }

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a update task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readUpdateTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Update task")) {
                serviceResult.put("updaterType", resp.get("actorType"));
                serviceResult.put("updaterId", resp.get("actorId"));
                serviceResult.put("updaterUrl", resp.get("actorUrl"));
                serviceResult.put("updaterName", resp.get("actorName"));
                serviceResult.put("taskType", resp.get("objectType"));
                serviceResult.put("taskId", resp.get("objectId"));
                serviceResult.put("taskUrl", resp.get("objectUrl"));
                serviceResult.put("taskName", resp.get("objectName"));
                serviceResult.put("updatedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Save information about a modify task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerModifyTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Modify task");
            ctx.put("actorType", context.get("modifierType"));
            ctx.put("actorId", context.get("modifierId"));
            ctx.put("actorUrl", context.get("modifierUrl"));
            ctx.put("actorName", context.get("modifierName"));
            ctx.put("objectType", context.get("taskType"));
            ctx.put("objectId", context.get("taskId"));
            ctx.put("taskId", context.get("taskId"));
            ctx.put("objectUrl", context.get("taskUrl"));
            ctx.put("objectName", context.get("taskName"));
            ctx.put("startTime", context.get("modifiedTime"));
            ctx.put("endTime", context.get("modifiedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a modify task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readModifyTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Modify task")) {
                serviceResult.put("modifierType", resp.get("actorType"));
                serviceResult.put("modifierId", resp.get("actorId"));
                serviceResult.put("modifierUrl", resp.get("actorUrl"));
                serviceResult.put("modifierName", resp.get("actorName"));
                serviceResult.put("taskType", resp.get("objectType"));
                serviceResult.put("taskId", resp.get("objectId"));
                serviceResult.put("taskUrl", resp.get("objectUrl"));
                serviceResult.put("taskName", resp.get("objectName"));
                serviceResult.put("modifiedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Save information about a update task status activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateTaskStatusActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Update task status");
            ctx.put("actorType", context.get("updaterType"));
            ctx.put("actorId", context.get("updaterId"));
            ctx.put("actorUrl", context.get("updaterUrl"));
            ctx.put("actorName", context.get("updaterName"));
            ctx.put("objectType", context.get("taskType"));
            ctx.put("objectId", context.get("taskId"));
            ctx.put("taskId", context.get("taskId"));
            ctx.put("objectUrl", context.get("taskUrl"));
            ctx.put("objectName", context.get("taskName"));
            ctx.put("startTime", context.get("updatedTime"));
            ctx.put("endTime", context.get("updatedTime"));
            ctx.put("newStatus", context.get("newStatus"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a update task status activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readUpdateTaskStatusActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Update task status")) {
                serviceResult.put("updaterType", resp.get("actorType"));
                serviceResult.put("updaterId", resp.get("actorId"));
                serviceResult.put("updaterUrl", resp.get("actorUrl"));
                serviceResult.put("updaterName", resp.get("actorName"));
                serviceResult.put("taskType", resp.get("objectType"));
                serviceResult.put("taskId", resp.get("objectId"));
                serviceResult.put("taskUrl", resp.get("objectUrl"));
                serviceResult.put("taskName", resp.get("objectName"));
                serviceResult.put("updatedTime", resp.get("startTime"));
                serviceResult.put("newStatus", resp.get("newStatus"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a note added to a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddNoteToTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add note to task");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("noteInfo", context.get("noteInfo"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("adderId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Task note");
            ctx.put("objectId", context.get("taskId"));
            ctx.put("startTime", context.get("addedTime"));
            ctx.put("endTime", context.get("addedTime"));

            String taskName = (String) context.get("taskName");
            String taskId = (String) context.get("taskId");
            if (UtilValidate.isNotEmpty(taskName)) {
                ctx.put("objectName", context.get("taskName"));
            } else {
                GenericValue task = EntityQuery.use(delegator).from("TaskHeader").where("taskId", taskId).queryOne();
                String nameOfTask = task.getString("taskName");
                ctx.put("objectName", nameOfTask);
            }
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a note added to a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readAddNoteToTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");

            if (UtilValidate.isNotEmpty(verb) && verb.equals("Add note to task")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("noteId", resp.get("noteId"));
                serviceResult.put("adderType", resp.get("actorType"));
                serviceResult.put("adderId", resp.get("actorId"));
                serviceResult.put("adderUrl", resp.get("actorUrl"));
                serviceResult.put("adderName", resp.get("actorName"));
                serviceResult.put("taskUrl", resp.get("objectUrl"));
                serviceResult.put("taskName", resp.get("objectName"));
                serviceResult.put("addedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a mail sent for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Send task email");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("actorType", context.get("senderType"));
            ctx.put("actorId", context.get("senderId"));
            ctx.put("actorUrl", context.get("senderUrl"));
            ctx.put("actorName", context.get("senderName"));
            ctx.put("startTime", context.get("sentTime"));
            ctx.put("endTime", context.get("sentTime"));
            ctx.put("to", context.get("sendTo"));
            ctx.put("cc", context.get("sendCC"));
            ctx.put("bcc", context.get("sendBCC"));
            ctx.put("subject", context.get("subject"));
            ctx.put("content", context.get("content"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a mail sent for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Send task email")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("senderType", resp.get("actorType"));
                serviceResult.put("senderId", resp.get("actorId"));
                serviceResult.put("senderUrl", resp.get("actorUrl"));
                serviceResult.put("senderName", resp.get("actorName"));
                serviceResult.put("sentTime", resp.get("startTime"));
                serviceResult.put("sendTo", resp.get("to"));
                serviceResult.put("sendCC", resp.get("cc"));
                serviceResult.put("sendBCC", resp.get("bcc"));
                serviceResult.put("subject", resp.get("subject"));
                serviceResult.put("content", resp.get("content"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a phone call made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskPhoneActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Make task related phone call");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("actorType", context.get("callerType"));
            ctx.put("actorId", context.get("callerId"));
            ctx.put("actorUrl", context.get("callerUrl"));
            ctx.put("actorName", context.get("callerName"));
            ctx.put("objectType", context.get("calleeType"));
            ctx.put("objectId", context.get("calleeId"));
            ctx.put("objectUrl", context.get("calleeUrl"));
            ctx.put("objectName", context.get("calleeName"));
            ctx.put("startTime", context.get("startTime"));
            ctx.put("endTime", context.get("endTime"));
            ctx.put("phoneNumber", context.get("phoneNumber"));
            ctx.put("subject", context.get("subject"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a phone call made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskPhoneActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Make task related phone call")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("callerType", resp.get("actorType"));
                serviceResult.put("callerId", resp.get("actorId"));
                serviceResult.put("callerUrl", resp.get("actorUrl"));
                serviceResult.put("callerName", resp.get("actorName"));
                serviceResult.put("calleeType", resp.get("objectType"));
                serviceResult.put("calleeId", resp.get("objectId"));
                serviceResult.put("calleeUrl", resp.get("objectUrl"));
                serviceResult.put("calleeName", resp.get("objectName"));
                serviceResult.put("startTime", resp.get("startTime"));
                serviceResult.put("endTime", resp.get("endTime"));
                serviceResult.put("phoneNumber", resp.get("phoneNumber"));
                serviceResult.put("subject", resp.get("subject"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a fax made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskFaxActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Send task related fax");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("actorType", context.get("callerType"));
            ctx.put("actorId", context.get("callerId"));
            ctx.put("actorUrl", context.get("callerUrl"));
            ctx.put("actorName", context.get("callerName"));
            ctx.put("objectType", context.get("calleeType"));
            ctx.put("objectId", context.get("calleeId"));
            ctx.put("objectUrl", context.get("calleeUrl"));
            ctx.put("objectName", context.get("calleeName"));
            ctx.put("startTime", context.get("startTime"));
            ctx.put("endTime", context.get("endTime"));
            ctx.put("phoneNumber", context.get("faxNumber"));
            ctx.put("subject", context.get("subject"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a fax made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskFaxActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Send task related fax")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("callerType", resp.get("actorType"));
                serviceResult.put("callerId", resp.get("actorId"));
                serviceResult.put("callerUrl", resp.get("actorUrl"));
                serviceResult.put("callerName", resp.get("actorName"));
                serviceResult.put("calleeType", resp.get("objectType"));
                serviceResult.put("calleeId", resp.get("objectId"));
                serviceResult.put("calleeUrl", resp.get("objectUrl"));
                serviceResult.put("calleeName", resp.get("objectName"));
                serviceResult.put("startTime", resp.get("startTime"));
                serviceResult.put("endTime", resp.get("endTime"));
                serviceResult.put("faxNumber", resp.get("phoneNumber"));
                serviceResult.put("subject", resp.get("subject"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a gift made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskGiftActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Send/receive task related gift");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("actorType", context.get("giverType"));
            ctx.put("actorId", context.get("giverId"));
            ctx.put("actorUrl", context.get("giverUrl"));
            ctx.put("actorName", context.get("giverName"));
            ctx.put("objectType", context.get("reciverType"));
            ctx.put("objectId", context.get("receiverId"));
            ctx.put("objectUrl", context.get("receiverUrl"));
            ctx.put("objectName", context.get("receiverName"));
            ctx.put("startTime", context.get("giftTime"));
            ctx.put("endTime", context.get("giftTime"));
            ctx.put("subject", context.get("giftDescription"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a gift made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskGiftActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Send/receive task related gift")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("giverType", resp.get("actorType"));
                serviceResult.put("giverId", resp.get("actorId"));
                serviceResult.put("giverUrl", resp.get("actorUrl"));
                serviceResult.put("giverName", resp.get("actorName"));
                serviceResult.put("receiverType", resp.get("reciverType"));
                serviceResult.put("receiverId", resp.get("objectId"));
                serviceResult.put("receiverUrl", resp.get("objectUrl"));
                serviceResult.put("receiverName", resp.get("objectName"));
                serviceResult.put("giftTime", resp.get("startTime"));
                serviceResult.put("giftDescription", resp.get("subject"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a meeting held for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskMeetingActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Hold task related meeting");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("venue", context.get("venue"));
            ctx.put("phoneNumber", context.get("phoneNumber"));
            ctx.put("actorType", context.get("organizerType"));
            ctx.put("actorId", context.get("organizerId"));
            ctx.put("actorUrl", context.get("organizerUrl"));
            ctx.put("actorName", context.get("organizerName"));
            ctx.put("to", context.get("participants"));
            ctx.put("startTime", context.get("startTime"));
            ctx.put("endTime", context.get("endTime"));
            ctx.put("subject", context.get("subject"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a meeting held for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskMeetingActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Hold task related meeting")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("venue", resp.get("venue"));
                serviceResult.put("phoneNumber", resp.get("phoneNumber"));
                serviceResult.put("organizerType", resp.get("actorType"));
                serviceResult.put("organizerId", resp.get("actorId"));
                serviceResult.put("organizerUrl", resp.get("actorUrl"));
                serviceResult.put("organizerName", resp.get("actorName"));
                serviceResult.put("participants", resp.get("to"));
                serviceResult.put("startTime", resp.get("startTime"));
                serviceResult.put("endTime", resp.get("endTime"));
                serviceResult.put("subject", resp.get("subject"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about an appointment made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskAppointmentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Make task related appointment");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("venue", context.get("venue"));
            ctx.put("phoneNumber", context.get("phoneNumber"));
            ctx.put("actorType", context.get("organizerType"));
            ctx.put("actorId", context.get("organizerId"));
            ctx.put("actorUrl", context.get("organizerUrl"));
            ctx.put("actorName", context.get("organizerName"));
            ctx.put("objectType", context.get("consultantType"));
            ctx.put("objectId", context.get("consultantId"));
            ctx.put("objectUrl", context.get("consultantUrl"));
            ctx.put("objectName", context.get("consultantName"));
            ctx.put("to", context.get("participants"));
            ctx.put("startTime", context.get("startTime"));
            ctx.put("endTime", context.get("endTime"));
            ctx.put("subject", context.get("subject"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about an appointment made for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskAppointmentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Make task related appointment")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("venue", resp.get("venue"));
                serviceResult.put("phoneNumber", resp.get("phoneNumber"));
                serviceResult.put("organizerType", resp.get("actorType"));
                serviceResult.put("organizerId", resp.get("actorId"));
                serviceResult.put("organizerUrl", resp.get("actorUrl"));
                serviceResult.put("organizerName", resp.get("actorName"));
                serviceResult.put("consultantType", resp.get("objectType"));
                serviceResult.put("consultantId", resp.get("objectId"));
                serviceResult.put("consultantUrl", resp.get("objectUrl"));
                serviceResult.put("consultantName", resp.get("objectName"));
                serviceResult.put("participants", resp.get("to"));
                serviceResult.put("startTime", resp.get("startTime"));
                serviceResult.put("endTime", resp.get("endTime"));
                serviceResult.put("subject", resp.get("subject"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a letter written for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerTaskLetterActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Write task related letter");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("actorType", context.get("writerType"));
            ctx.put("actorId", context.get("writerId"));
            ctx.put("actorUrl", context.get("writerUrl"));
            ctx.put("actorName", context.get("writerName"));
            ctx.put("objectType", context.get("addresseeType"));
            ctx.put("objectId", context.get("addresseeId"));
            ctx.put("objectUrl", context.get("addresseeUrl"));
            ctx.put("objectName", context.get("addresseeName"));
            ctx.put("startTime", context.get("postedTime"));
            ctx.put("endTime", context.get("postedTime"));
            ctx.put("subject", context.get("subject"));
            ctx.put("content", context.get("content"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a letter written for a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readTaskLetterActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Write task related letter")) {
                serviceResult.put("taskId", resp.get("taskId"));
                serviceResult.put("writerType", resp.get("actorType"));
                serviceResult.put("writerId", resp.get("actorId"));
                serviceResult.put("writerUrl", resp.get("actorUrl"));
                serviceResult.put("writerName", resp.get("actorName"));
                serviceResult.put("addresseeType", resp.get("objectType"));
                serviceResult.put("addresseeId", resp.get("objectId"));
                serviceResult.put("addresseeUrl", resp.get("objectUrl"));
                serviceResult.put("addresseeName", resp.get("objectName"));
                serviceResult.put("postedTime", resp.get("startTime"));
                serviceResult.put("subject", resp.get("subject"));
                serviceResult.put("content", resp.get("content"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register information about a quote status change.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerQuoteStatusChangeActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Change quote status");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("actorType", context.get("actorType"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("actorUrl", context.get("actorUrl"));
            ctx.put("actorName", context.get("actorName"));
            ctx.put("oldStatus", context.get("oldStatus"));
            ctx.put("newStatus", context.get("newStatus"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Read information about a quote status change.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> readQuoteStatusChangeActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("activityId", context.get("activityId"));
            Map<String, Object> resp = dispatcher.runSync("readActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            String verb = (String) resp.get("verb");
            if (UtilValidate.isNotEmpty(verb) && verb.equals("Change quote status")) {
                serviceResult.put("quoteId", resp.get("quoteId"));
                serviceResult.put("actorType", resp.get("actorType"));
                serviceResult.put("actorId", resp.get("actorId"));
                serviceResult.put("actorUrl", resp.get("actorUrl"));
                serviceResult.put("actorName", resp.get("actorName"));
                serviceResult.put("oldStatus", resp.get("oldStatus"));
                serviceResult.put("newStatus", resp.get("newStatus"));
                serviceResult.put("changedTime", resp.get("startTime"));
            } else {
                String msg = "Wrong activity type";
                Debug.logError(msg, module);
                return ServiceUtil.returnError(msg);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a requirement added to a task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddRequirementToTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add requirement to task");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("description", context.get("description"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("adderId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Task requirement");
            ctx.put("objectId", context.get("taskId"));
            ctx.put("startTime", context.get("addedTime"));
            ctx.put("endTime", context.get("addedTime"));
            String taskName = (String) context.get("taskName");
            String taskId = (String) context.get("taskId");
            if (UtilValidate.isNotEmpty(taskName)) {
                ctx.put("objectName", context.get("taskName"));
            } else {
                GenericValue task = EntityQuery.use(delegator).from("TaskHeader").where("taskId", taskId).queryOne();
                String nameOfTask = task.getString("taskName");
                ctx.put("objectName", nameOfTask);
            }

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a requirement mark for task.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerMarkTaskRequirementActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Mark task requirement");
            ctx.put("taskId", context.get("taskId"));
            ctx.put("description", context.get("requirementInfo"));
            ctx.put("requirementStatus", context.get("requirementStatus"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Task requirement");
            ctx.put("objectId", context.get("taskId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            String taskName = (String) context.get("taskName");
            String taskId = (String) context.get("taskId");
            if (UtilValidate.isNotEmpty(taskName)) {
                ctx.put("objectName", context.get("taskName"));
            } else {
                GenericValue task = EntityQuery.use(delegator).from("TaskHeader").where("taskId", taskId).queryOne();
                String nameOfTask = task.getString("taskName");
                ctx.put("objectName", nameOfTask);
            }
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register upload quote document.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUploadQuoteDocumentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Upload quote document");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("contentName", context.get("contentName"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add quote item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddQuoteItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add quote item");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("productId", context.get("productId"));
            ctx.put("quoteUnitPrice", context.get("quoteUnitPrice"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register update quote item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateQuoteItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Update quote item");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("productId", context.get("productId"));
            ctx.put("quoteUnitPrice", context.get("quoteUnitPrice"));
            ctx.put("quantity", context.get("quantity"));
            ctx.put("discountAmount", context.get("discountAmount"));
            ctx.put("discountPercentage", context.get("discountPercentage"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("quoteId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register a convert quote to order activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerConvertQuoteToOrderActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Convert quote to order");
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("convertedTime"));
            ctx.put("endTime", context.get("convertedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Save information about a capture payment activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCapturePaymentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Capture Payment");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectId", context.get("invoiceId"));
            ctx.put("invoiceId", context.get("invoiceId"));
            ctx.put("amount", context.get("amount"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Save information about a create quote activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCreateOpportunityActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Create opportunity");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectType", context.get("opportunityType"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("objectUrl", context.get("opportunityUrl"));
            ctx.put("objectName", context.get("opportunityName"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add opportunity item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddOpportunityItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add opportunity item");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("productId", context.get("productId"));
            ctx.put("opportunityUnitPrice", context.get("unitPrice"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * /**
     * Register remove opportunity item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerRemoveOpportunityItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Delete opportunity item");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("productId", context.get("productId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * /** Register add opportunity item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddOpportunityRoleActivity(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contactPartyName = "";
        Delegator delegator = dctx.getDelegator();

        if (UtilValidate.isNotEmpty(context.get("contactPartyId"))) {
            contactPartyName = AxPartyHelper.getPartyName(delegator, (String) context.get("contactPartyId"));
        }

        String roleTypeDescription = "";
        if (UtilValidate.isNotEmpty(context.get("roleTypeId"))) {
            roleTypeDescription = (String) context.get("roleTypeId");
            GenericValue roleType = EntityQuery.use(delegator)
                    .from("RoleType")
                    .where(UtilMisc.toMap("roleTypeId", (String) context.get("roleTypeId")))
                    .queryOne();
            if (UtilValidate.isNotEmpty(roleType)) {
                roleTypeDescription = roleType.getString("description");
            }
        }

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add opportunity role");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("contactPartyName", contactPartyName);
            ctx.put("roleTypeDescription", roleTypeDescription);
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add opportunity item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerRemoveOpportunityRoleActivity(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contactPartyName = "";
        Delegator delegator = dctx.getDelegator();

        if (UtilValidate.isNotEmpty(context.get("contactPartyId"))) {
            contactPartyName = AxPartyHelper.getPartyName(delegator, (String) context.get("contactPartyId"));
        }

        String roleTypeDescription = "";
        if (UtilValidate.isNotEmpty(context.get("roleTypeId"))) {
            roleTypeDescription = (String) context.get("roleTypeId");
            GenericValue roleType = EntityQuery.use(delegator)
                    .from("RoleType")
                    .where(UtilMisc.toMap("roleTypeId", (String) context.get("roleTypeId")))
                    .queryOne();
            if (UtilValidate.isNotEmpty(roleType)) {
                roleTypeDescription = roleType.getString("description");
            }
        }

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Remove opportunity role");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("contactPartyName", contactPartyName);
            ctx.put("roleTypeDescription", roleTypeDescription);
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add opportunity and task association.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddOpportunityLinkTaskActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "opportunity association");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("opportunityLinkedTaskId", context.get("opportunityLinkedTaskId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add opportunity and event association.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddOpportunityCreateEventActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "opportunity event");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("opportunityEventId", context.get("opportunityEventId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register update opportunity details.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateOpportunityDetailsActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "opportunity details");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a note added to a opportunity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddNoteToOpportunityActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add note to opportunity");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("noteInfo", context.get("noteInfo"));
            ctx.put("noteName", context.get("noteName"));
            ctx.put("noteTypeId", context.get("noteTypeId"));
            ctx.put("isNoteInternal", context.get("isNoteInternal"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("adderId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Opportunity note");
            ctx.put("objectId", context.get("noteId"));
            ctx.put("objectUrl", context.get("opportunityUrl"));
            ctx.put("objectName", context.get("opportunityName"));
            ctx.put("startTime", context.get("addedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a mail sent for a opportunity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerSendOpportunityEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Send opportunity email");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("actorType", context.get("senderType"));
            ctx.put("actorId", context.get("senderId"));
            ctx.put("actorUrl", context.get("senderUrl"));
            ctx.put("actorName", context.get("senderName"));
            ctx.put("objectType", "Opportunity email");
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("sentTime"));
            ctx.put("endTime", context.get("sentTime"));
            ctx.put("to", context.get("sendTo"));
            ctx.put("cc", context.get("sendCC"));
            ctx.put("bcc", context.get("sendBCC"));
            ctx.put("subject", context.get("subject"));
            ctx.put("content", context.get("content"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register upload opportunity document.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUploadOpportunityDocumentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Upload opportunity document");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("contentName", context.get("contentName"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("salesOpportunityId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about stage added to a opportunity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerStageToOpportunityActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated opportunity stage");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("oldOpportunityStageId", context.get("oldOpportunityStageId"));
            ctx.put("newOpportunityStageId", context.get("newOpportunityStageId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about opportunity to quote.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerOpportunityToQuoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Convert opportunity to quote");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("quoteId", context.get("quoteId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Save information about a create project item activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Create issue");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectId", context.get("projectItemId"));
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("objectUrl", context.get("projectItemUrl"));
            ctx.put("objectName", context.get("projectItemName"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about status update to a issue.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemStatusActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated issue status");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("oldProjectItemStatusId", context.get("oldProjectItemStatusId"));
            ctx.put("newProjectItemStatusId", context.get("newProjectItemStatusId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about assignee update to a issue.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemAssigneeActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated issue assignee");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("oldProjectItemAssigneeId", context.get("oldProjectItemAssigneeId"));
            ctx.put("newProjectItemAssigneeId", context.get("newProjectItemAssigneeId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a note added to a project item.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddNoteToProjectItemActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add note to issue");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("noteInfo", context.get("noteInfo"));
            ctx.put("noteTypeId", context.get("noteTypeId"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", context.get("adderId"));
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Issue note");
            ctx.put("objectId", context.get("noteId"));
            ctx.put("startTime", context.get("addedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register upload project item document.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUploadProjectItemDocumentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Upload issue document");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("contentName", context.get("contentName"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("projectItemId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about Priority update to a issue.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemPriorityActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated issue priority");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("oldProjectItemPriorityId", context.get("oldProjectItemPriorityId"));
            ctx.put("newProjectItemPriorityId", context.get("newProjectItemPriorityId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about Association issue to a main issue.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemAssociationActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String isIssueLink = (String) context.get("isIssueLink");

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            if (UtilValidate.isNotEmpty(isIssueLink) && "Y".equals(isIssueLink)) {
                ctx.put("verb", "Associated issue");
            } else {
                ctx.put("verb", "Removed association issue");
            }
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("issueIdTo", context.get("issueIdTo"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about Description to issue.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerProjectItemDescriptionActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String isIssueLink = (String) context.get("isIssueLink");

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Add description to issue");
            ctx.put("projectItemId", context.get("projectItemId"));
            ctx.put("description", context.get("description"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register add Collection.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCreateCollectionActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Create Collection");
            ctx.put("actorType", context.get("creatorType"));
            ctx.put("actorId", context.get("creatorId"));
            ctx.put("actorUrl", context.get("creatorUrl"));
            ctx.put("actorName", context.get("creatorName"));
            ctx.put("objectType", context.get("collectionTypeId"));
            ctx.put("objectId", context.get("collectionId"));
            ctx.put("collectionId", context.get("collectionId"));
            ctx.put("objectUrl", context.get("collectionUrl"));
            ctx.put("objectName", context.get("name"));
            ctx.put("startTime", context.get("createdTime"));
            ctx.put("endTime", context.get("createdTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register activity to convert lead to deal activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerConvertLeadToDealActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            String contactPartyName = AxPartyHelper.getPartyName(delegator, (String) context.get("leadPartyId"));
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Converted lead to deal");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("partyId", context.get("leadPartyId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("convertedTime"));
            ctx.put("endTime", context.get("convertedTime"));
            ctx.put("contactPartyName", contactPartyName);
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register update currency for opportunity
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateCurrencyForDeal(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated opportunity currency");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("oldCurrency", context.get("oldCurrency"));
            ctx.put("newCurrency", context.get("newCurrency"));
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register update estimated amount for opportunity
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdateEstimatedAmountForDeal(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated opportunity estimated amount");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("oldAmount", context.get("oldAmount"));
            ctx.put("newAmount", context.get("newAmount"));
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register update estimated amount for opportunity
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdatePriorityForDeal(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Updated opportunity priority");
            ctx.put("salesOpportunityId", context.get("salesOpportunityId"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("oldPriority", context.get("oldPriority"));
            ctx.put("newPriority", context.get("newPriority"));
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register upload customer request document.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUploadCustRequestDocumentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "Upload document to custRequest");
            ctx.put("custRequestId", context.get("custRequestId"));
            ctx.put("contentName", context.get("contentName"));
            ctx.put("actorId", context.get("actorId"));
            ctx.put("objectId", context.get("custRequestId"));
            ctx.put("startTime", context.get("changedTime"));
            ctx.put("endTime", context.get("addedTime"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a mail sent for a customer.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerSendCustomerEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", context.get("userLogin"));
            ctx.put("verb", "send an email to customer");
            ctx.put("partyId", context.get("customerId"));
            ctx.put("actorType", context.get("senderType"));
            ctx.put("actorId", context.get("senderId"));
            ctx.put("actorUrl", context.get("senderUrl"));
            ctx.put("actorName", context.get("senderName"));
            ctx.put("objectType", "Customer invite email");
            ctx.put("objectId", context.get("customerId"));
            ctx.put("startTime", context.get("sentTime"));
            ctx.put("endTime", context.get("sentTime"));
            ctx.put("to", context.get("sendTo"));
            ctx.put("cc", context.get("sendCC"));
            ctx.put("bcc", context.get("sendBCC"));
            ctx.put("subject", context.get("subject"));
            ctx.put("content", context.get("content"));

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }

            serviceResult.put("activityId", resp.get("activityId"));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

}
