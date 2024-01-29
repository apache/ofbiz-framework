package com.simbaquartz.xparty.helpers;

import com.simbaquartz.xparty.ContactMethodTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Party contact helper. Offers contact mechanism related helper utilities, email, phone, addresses
 * etc.
 */
public class PartyContactHelper {

  public static final String module = PartyContactHelper.class.getName();

  // START: Email related methods
  /**
   * Returns party's latest primary email contact mech.Null if nothing exists
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static GenericValue getPartyPrimaryEmailContactMech(Delegator delegator, String partyId) {
    GenericValue partyLatestContactMech =
        PartyWorker.findPartyLatestContactMech(
            partyId, ContactMethodTypesEnum.EMAIL.getTypeId(), delegator);

    return partyLatestContactMech;
  }

  public static String getEmailAddressForPartyId(String partyId, Delegator delegator) {
    String emailAddressString = null;
    GenericValue sendMailToPartyContactMech =
        PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
    if (UtilValidate.isNotEmpty(sendMailToPartyContactMech)) {
      emailAddressString = sendMailToPartyContactMech.getString("infoString");
    }

    return emailAddressString;
  }

  public static List<String> getEmailAddressesForPartyIds(
      List<String> partyIds, Delegator delegator) {
    List<String> emailAddressesForPartyIds = new ArrayList<>();

    for (String partyId : CollectionUtils.emptyIfNull(partyIds)) {
      emailAddressesForPartyIds.add(AxPartyHelper.getPartyEmail(delegator, partyId));
    }

    return emailAddressesForPartyIds;
  }
  // END: Email related methods

  // START: Phone related methods
  public static Map getLatestPrimaryTelecomNumber(
      Delegator delegator, LocalDispatcher dispatcher, String partyId, boolean useCache) {
    Map telecomNumber = FastMap.newInstance();
    try {
      GenericValue partyContactMechPurposePrimary =
          EntityQuery.use(delegator)
              .from("PartyContactWithPurpose")
              .where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_PHONE")
              .filterByDate(
                  "contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyContactMechPurposePrimary)) {
        telecomNumber.putAll(
            partyContactMechPurposePrimary.getRelatedOne("TelecomNumber", useCache));
        telecomNumber.put("extension", partyContactMechPurposePrimary.getString("extension"));
      } else {
        // possibly a phone not set as default, try to find one if found set as default
        GenericValue partyLatestPhone =
            PartyWorker.findPartyLatestContactMech(
                partyId, ContactMethodTypesEnum.PHONE.getTypeId(), delegator);
        if (UtilValidate.isNotEmpty(partyLatestPhone)) {
          telecomNumber = partyLatestPhone;
          // also make it primary
          Map<String, Object> makePrimaryPhoneContext = FastMap.newInstance();
          makePrimaryPhoneContext.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
          makePrimaryPhoneContext.put("partyId", partyId);
          makePrimaryPhoneContext.put("contactMechId", partyLatestPhone.getString("contactMechId"));

          try {
            dispatcher.runSync("makePrimaryPhone", makePrimaryPhoneContext);
          } catch (GenericServiceException e) {
            Debug.logError(
                e,
                "An error occurred while invoking makePrimaryPhone service, details: "
                    + e.getMessage(),
                "PartyApiServiceImpl");
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method makePrimaryPhone", module);
            return null;
          }
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return telecomNumber;
  }
  // END: Phone related methods

}
