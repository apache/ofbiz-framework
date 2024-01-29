package com.simbaquartz.xparty.services;

import com.simbaquartz.xparty.helpers.AxPartyHelper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public class CreditScoreService {

    public static final String module = CreditScoreService.class.getName();

    public static Map<String, Object> updatePartyCreditScore(DispatchContext dctx,
                                                             Map<String, ? extends Object> context)
            throws GenericEntityException, GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");
        String partyContentTypeId = (String) context.get("partyContentTypeId");
        String partyId = (String) context.get("partyId");
        GenericValue creditScore = null;
        GenericValue newCreditScore = null;
        Timestamp now = UtilDateTime.nowTimestamp();

        try {
            List andConditionsList = new LinkedList();
            andConditionsList.add(
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            andConditionsList.add(
                    EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            EntityCondition condition =
                    EntityCondition.makeCondition(andConditionsList, EntityOperator.AND);
            creditScore = EntityQuery.use(delegator).from("PartyCreditScore").where(condition).queryOne();

            // check to see if partyCreditScore object exists, if so set thruDate and create new Credit score
            if (!UtilValidate.isEmpty(creditScore)) {
                creditScore.set("thruDate", now);
                creditScore.store();
            }
            // Create new CreditScore
            Map<String, Object> newPartyMap = UtilMisc.toMap("partyId", partyId, "score", context.get("score"), "note", context.get("note"), "fromDate", now, "createdDate", now, "lastModifiedDate", now);
            newPartyMap.put("createdByUserLogin", userLogin.get("userLoginId"));
            newPartyMap.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
            newCreditScore = delegator.makeValue("PartyCreditScore", newPartyMap);
            newCreditScore.create();

        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }
        return result;
    }

    public static Map<String, Object> getPartyCreditScoreHistory(DispatchContext dctx,
                                                                 Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");
        Timestamp fromDateTs = (Timestamp) context.get("fromDate");
        Timestamp thruDateTs = (Timestamp) context.get("thruDate");

        // default 30 days date range if no input date range
        if (UtilValidate.isEmpty(fromDateTs) || UtilValidate.isEmpty(thruDateTs)) {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            thruDateTs = Timestamp.valueOf(currentLocalDateTime);
            currentLocalDateTime = currentLocalDateTime.minusDays(30);
            fromDateTs = Timestamp.valueOf(currentLocalDateTime);
        }

        if (startIndex == null || startIndex <= 0) {
            startIndex = 0;
        }
        if (viewSize == null || viewSize < 0) {
            viewSize = 10;
        }

        int recordsCount = 0;
        int lowIndex = 0;
        int highIndex = 0;
        List<GenericValue> partyCreditScore;
        try {
            EntityConditionList<EntityExpr> dateCondition = EntityCondition.makeCondition(
                    UtilMisc.toList(
                            EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTs),
                            EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDateTs)),
                    EntityOperator.AND);
            EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(
                    UtilMisc.toList(
                            dateCondition,
                            EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId)
                    )
            );
            PagedList<GenericValue> pagedCreditScore = EntityQuery.use(delegator)
                    .from("PartyCreditScore")
                    .where(conditions)
                    .cursorScrollInsensitive()
                    .orderBy("-fromDate")
                    .queryPagedList(startIndex, viewSize);

            if (UtilValidate.isNotEmpty(pagedCreditScore)) {
                recordsCount = pagedCreditScore.getSize();
                lowIndex = pagedCreditScore.getStartIndex();
                highIndex = pagedCreditScore.getEndIndex();
                partyCreditScore = pagedCreditScore.getData();
                result.put("recordsCount", recordsCount);
                result.put("lowIndex", lowIndex);
                result.put("highIndex", highIndex);
                result.put("startIndex", startIndex);
                result.put("records", preparePartyCreditScoreRecords(delegator, partyCreditScore));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    private static List<Map<String, Object>> preparePartyCreditScoreRecords(Delegator delegator, List<GenericValue> partyCreditScore) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (UtilValidate.isNotEmpty(partyCreditScore)) {
            partyCreditScore.forEach(partyCreditScoreGv -> {
                String partyId = partyCreditScoreGv.getString("partyId");
                Map<String, Object> partyCreditScoreInfo = new HashMap<>();
                partyCreditScoreInfo.put("partyId", partyId);
                partyCreditScoreInfo.put("score", partyCreditScoreGv.getBigDecimal("score"));
                partyCreditScoreInfo.put("createdAt", partyCreditScoreGv.getTimestamp("createdDate"));
                partyCreditScoreInfo.put("lastModifiedDate", partyCreditScoreGv.getTimestamp("lastModifiedDate"));
                partyCreditScoreInfo.put("note", partyCreditScoreGv.get("note"));

                try {
                    String createdByUserLogin = (String) partyCreditScoreGv.get("createdByUserLogin");
                    GenericValue createdByParty = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", createdByUserLogin).queryOne();
                    partyCreditScoreInfo.put("createdBy", AxPartyHelper.getPartyBasicDetails(delegator, createdByParty.getString("partyId")));
                } catch (GenericEntityException e) {
                    e.printStackTrace();
                }
                result.add(partyCreditScoreInfo);
            });
        }
        return result;
    }

    public static Map<String, Object> getPartyLatestCreditScore(DispatchContext dispatchContext, Map<String, Object> context){
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dispatchContext.getDelegator();
        String customerId = (String) context.get("customerId");

        try {
            GenericValue partyCreditScoreGv = EntityQuery.use(delegator)
                    .from("PartyCreditScore")
                    .where("partyId", customerId)
                    .orderBy("-fromDate")
                    .queryFirst();
            if (UtilValidate.isNotEmpty(partyCreditScoreGv)) {
                List<Map<String, Object>> responsePartyCreditScoreRecords = preparePartyCreditScoreRecords(delegator, Collections.singletonList(partyCreditScoreGv));
                serviceResult.put("result", responsePartyCreditScoreRecords.get(0));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }
}
