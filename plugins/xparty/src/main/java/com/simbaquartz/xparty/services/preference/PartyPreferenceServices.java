package com.simbaquartz.xparty.services.preference;

import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * For managing party preference related services. Use this to store party level preferences,
 * example is notification enabled, work week etc.
 */
public class PartyPreferenceServices {

  public static final String module = PartyPreferenceServices.class.getName();

  public static Map<String, Object> createOrUpdatePartyPreference(DispatchContext dctx, Map context)
      throws GenericEntityException, GenericServiceException {

    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    String prefName = (String) context.get("prefName");
    String prefValue = (String) context.get("prefValue");

    GenericValue partyPreference =
        EntityQuery.use(delegator)
            .from("PartyPreference")
            .where(UtilMisc.toMap("partyId", partyId, "prefName", prefName))
            .queryOne();
    if (UtilValidate.isEmpty(partyPreference)) {
      GenericValue createPartyPreferenceCtx = delegator.makeValue("PartyPreference");
      createPartyPreferenceCtx.set("partyId", partyId);
      createPartyPreferenceCtx.set("prefName", prefName);
      createPartyPreferenceCtx.set("prefValue", prefValue);

      delegator.create(createPartyPreferenceCtx);

    } else {
      partyPreference.set("partyId", partyId);
      if (UtilValidate.isNotEmpty(prefName)) {
        partyPreference.set("prefName", prefName);
      }
      if (UtilValidate.isNotEmpty(prefValue)) {
        partyPreference.set("prefValue", prefValue);
      }

      delegator.store(partyPreference);
    }
    return ServiceUtil.returnSuccess();
  }

  public static Map<String, Object> getPartyPreferences(DispatchContext dctx, Map context)
      throws GenericEntityException, GenericServiceException {

    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    String prefName = (String) context.get("prefName");

    Map filteringCriteria =UtilMisc.toMap("partyId", partyId);
    if(UtilValidate.isNotEmpty(prefName))
      filteringCriteria.put("prefName", prefName);


    List<GenericValue> partyPreferences =
        EntityQuery.use(delegator)
            .from("PartyPreference")
            .where(filteringCriteria)
            .queryList();

    Map result = ServiceUtil.returnSuccess();
    result.put("preferences", partyPreferences);
    return result;
  }
}
