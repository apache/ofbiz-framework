package com.simbaquartz.xparty.services;

import com.simbaquartz.xparty.helpers.ExtPartyRelationshipHelper;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** For tracking the party follower services. */
public class PartyFollowerServices {
  private static final String module = PartyFollowerServices.class.getName();
  private static final String PARTY_FOLLOWER_ROLE_ID = "PARTY_FOLLOWER";

  /**
   * Returns the parties in a relationship of type PARTY_FOLLOWER with the input party id, weeds out
   * expired relationships using fromDate and thruDate on the relationship.
   */
  public static Map<String, Object> getPartyFollowers(
      DispatchContext dctx, Map<String, Object> context) {

    Map<String, Object> result = ServiceUtil.returnSuccess();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String partyIdTo = (String) context.get("partyId");
    List<Map> partyFollowers =
        ExtPartyRelationshipHelper.getActivePartyRelationshipsFromParty(
            dispatcher, partyIdTo, PARTY_FOLLOWER_ROLE_ID);

    result.put("followers", partyFollowers);

    return result;
  }
}
