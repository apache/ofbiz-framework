package com.simbaquartz.xparty.services.communication;

import static com.fidelissd.zcp.xcommon.util.AxUtilFormat.formatCreatedTimestamp;

import com.simbaquartz.xparty.helpers.AxPartyHelper;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

/** Phone call services and related utility classes for a party (Person/Company) */
public class PartyPhoneCallServices {
  private static final String module = PartyPhoneCallServices.class.getName();

  /**
   * Returns the call logs for the input party id. Fetches both incoming and outgoing call logs.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static Map<String, Object> getPartyCallLogs(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    List<Map<String, Object>> callLogs = FastList.newInstance();

    //    Prepare condition to find all CoummnicationEvent of type phone where input party id is
    // either from (Caller) or to (Receiver)
    List<EntityCondition> adjExprs = new LinkedList<>();
    List<EntityCondition> exprs = new LinkedList<>();
    exprs.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
    exprs.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));

    adjExprs.add(EntityCondition.makeCondition(exprs, EntityOperator.OR));

    adjExprs.add(
        EntityCondition.makeCondition(
            "commEventCommunicationEventTypeId", EntityOperator.EQUALS, "PHONE_COMMUNICATION"));

    EntityCondition cond = EntityCondition.makeCondition(adjExprs, EntityOperator.AND);

    String orderBy = "-commEventDatetimeStarted";

    // TODO: @MSS Needs pagination implementation.
    List<GenericValue> partyCallRecords =
        EntityQuery.use(delegator)
            .from("CommEventAndParty")
            .where(cond)
            .orderBy(orderBy)
            .queryList();
    if (UtilValidate.isNotEmpty(partyCallRecords)) {
      for (GenericValue record : partyCallRecords) {
        Map<String, Object> partyEventMap = FastMap.newInstance();
        String eventId = record.getString("eventId");
        partyEventMap.put("eventId", eventId);
        Timestamp eventCreatedDate = record.getTimestamp("eventCreatedDate");
        partyEventMap.put("eventCreatedDate", eventCreatedDate);

        Timestamp startedAt = record.getTimestamp("commEventDatetimeStarted");
        Timestamp endedAt = record.getTimestamp("commEventDatetimeEnded");
        partyEventMap.put("startedAt", startedAt);
        partyEventMap.put("endedAt", endedAt);

        // caller details
        String callerPartyId = record.getString("partyIdFrom");

        if (UtilValidate.isNotEmpty(callerPartyId)) {
          Map callerDetails =
              UtilMisc.toMap(
                  "id", callerPartyId,
                  "displayName", record.getString("callerDisplayName"),
                  "email", record.getString("callerEmail"),
                  "photoUrl", record.getString("callerPhotoUrl"));

          partyEventMap.put("caller", callerDetails);
          partyEventMap.put("recipient", AxPartyHelper.getPartyName(delegator, partyId));
        }

        // call receiver details
        String receiverPartyId = record.getString("partyIdTo");

        if (UtilValidate.isNotEmpty(receiverPartyId)) {
          Map receiverDetails =
              UtilMisc.toMap(
                  "id", receiverPartyId,
                  "displayName", record.getString("receiverDisplayName"),
                  "email", record.getString("receiverEmail"),
                  "photoUrl", record.getString("receiverPhotoUrl"));

          partyEventMap.put("receiver", receiverDetails);
        }

        partyEventMap.put("eventName", record.getString("eventName"));
        partyEventMap.put("eventTypeId", record.getString("eventTypeId"));
        String description = record.getString("eventDescription");
        if (UtilValidate.isNotEmpty(description)) {
          description = description.replaceAll("\\<.*?\\>", "");
          partyEventMap.put("description", description);
          partyEventMap.put("descriptionHtml", description);
        }

        String descriptionJSON = record.getString("eventDescriptionJson");
        if (UtilValidate.isNotEmpty(descriptionJSON)) {
          partyEventMap.put("descriptionJson", descriptionJSON);
        }

        partyEventMap.put("callLogDuration", record.getString("commEventCallLogDuration"));
        String fullCreatedDateTimeFormat = formatCreatedTimestamp(eventCreatedDate);
        partyEventMap.put("createdTime", fullCreatedDateTimeFormat);

        callLogs.add(partyEventMap);
      }
    }

    result.put("callLogs", callLogs);
    return result;
  }
}
