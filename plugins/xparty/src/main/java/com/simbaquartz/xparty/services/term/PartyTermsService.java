package com.simbaquartz.xparty.services.term;

import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastList;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Party Terms related services.
 */
public class PartyTermsService {
  private static final String module = PartyTermsService.class.getName();

  /**
   * Updates the sequence for terms, pass in the new sequence of terms (contentIds) to set. Returns
   * the updated sequence of terms List(GenericValue).
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static Map<String, Object> setPartyTermSequence(
      DispatchContext dctx, Map<String, ? extends Object> context)
      throws GenericEntityException, GenericServiceException {
    Delegator delegator = dctx.getDelegator();
    Map sequenceNumsMap = UtilMisc.toMap();
    long sequenceCounter = 1;

    Map result = ServiceUtil.returnSuccess();

    List contentIdsCondsList = FastList.newInstance();
    String partyId = (String) context.get("partyId");
    List<String> newSequenceOfTerms = (List<String>) context.get("newSequenceOfTerms");

    for (String newTermSequenceContentId : newSequenceOfTerms) {
      contentIdsCondsList.add(
          EntityCondition.makeCondition(
              "contentId", EntityOperator.EQUALS, newTermSequenceContentId));
      sequenceNumsMap.put(newTermSequenceContentId, sequenceCounter++);
    }

    EntityConditionList<EntityExpr> exprListContentIdsOr =
        EntityCondition.makeCondition(contentIdsCondsList, EntityOperator.OR);
    EntityConditionList<EntityCondition> mainCond =
        EntityCondition.makeCondition(
            UtilMisc.toList(
                exprListContentIdsOr,
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId)),
            EntityOperator.AND);

    List<GenericValue> updatedSequenceOfTerms;
    try {
      updatedSequenceOfTerms =
          delegator.findList(
              "PartyContent", mainCond, null, UtilMisc.toList("sequenceNum"), null, false);

      if (updatedSequenceOfTerms != null && updatedSequenceOfTerms.size() > 0) {
        for (GenericValue partyContentGv : updatedSequenceOfTerms) {
          String partyContentId = partyContentGv.getString("contentId");
          partyContentGv.set("sequenceNum", new Long((long) sequenceNumsMap.get(partyContentId)));
        }

        delegator.storeAll(updatedSequenceOfTerms);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    result.put("updatedSequenceOfTerms", updatedSequenceOfTerms);
    return result;
  }
}
