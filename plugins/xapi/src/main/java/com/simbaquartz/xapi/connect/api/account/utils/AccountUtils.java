package com.simbaquartz.xapi.connect.api.account.utils;

import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class AccountUtils {

  private static final String module = AccountUtils.class.getName();

  private static final String SUBSCRIPTION_LICENSE_ACTIVE = "LICENSE_ACTIVE";

  /**
   * Check if the user has active license
   */
  public static boolean isUserLicenseActive(Delegator delegator, LocalDispatcher dispatcher, String
      accountPartyId) {
    try {

      GenericValue partyAttribute = delegator.findOne("PartyAttribute", UtilMisc.toMap("partyId",
          accountPartyId, "attrName", SUBSCRIPTION_LICENSE_ACTIVE),false);

      if (UtilValidate.isNotEmpty(partyAttribute)) {
        return UtilValidate.isNotEmpty(partyAttribute.get("attrValue")) && "Y"
            .equalsIgnoreCase(partyAttribute.getString("attrValue"));
      }
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
    }
    return false;
  }

  public static void setUserLicenseActive(LocalDispatcher dispatcher, Delegator delegator,
      String accountId) {

    try {
      GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("partyId",
          accountId).queryFirst();
      setUserLicenseActive(dispatcher, userLogin);
    } catch (GenericEntityException e) {
      Debug.logError(
          "An error occurred while invoking createOrUpdatePartyAttribute service, details: " + e
              .getMessage(), module);
      if (Debug.verboseOn()) {
        Debug.logVerbose("Exiting method setUserLicenseActive", module);
      }

      Debug.logError(e, module);
    }
  }

  public static void setUserLicenseActive(LocalDispatcher dispatcher, GenericValue userLogin) {

    try {
      Map<String, Object> createUpdatePartyAttrCtx = UtilMisc.toMap(
          "userLogin", userLogin,
          "partyId", userLogin.get("partyId"),
          "attrName", SUBSCRIPTION_LICENSE_ACTIVE,
          "attrValue", "Y");

      Map<String, Object> createPartyAttrResp = dispatcher
          .runSync("createOrUpdatePartyAttribute", createUpdatePartyAttrCtx);
      if (ServiceUtil.isError(createPartyAttrResp)) {
        Debug.logError(ServiceUtil.getErrorMessage(createPartyAttrResp), module);
      }
    } catch (GenericServiceException e) {
      Debug.logError(
          "An error occurred while invoking createOrUpdatePartyAttribute service, details: " + e
              .getMessage(), module);
      if (Debug.verboseOn()) {
        Debug.logVerbose("Exiting method setUserLicenseActive", module);
      }

      Debug.logError(e, module);
    }

  }

  public static void setUserLicenseInactive(LocalDispatcher dispatcher, GenericValue userLogin) {
    try {
      Map<String, Object> createUpdatePartyAttrCtx = UtilMisc.toMap(
          "userLogin", userLogin,
          "partyId", userLogin.get("partyId"),
          "attrName", SUBSCRIPTION_LICENSE_ACTIVE,
          "attrValue", "N");

      Map<String, Object> createPartyAttrResp = dispatcher
          .runSync("createOrUpdatePartyAttribute", createUpdatePartyAttrCtx);
      if (ServiceUtil.isError(createPartyAttrResp)) {
        Debug.logError(ServiceUtil.getErrorMessage(createPartyAttrResp), module);
      }
    } catch (GenericServiceException e) {
      Debug.logError(
          "An error occurred while invoking createOrUpdatePartyAttribute service, details: " + e
              .getMessage(), module);
      if (Debug.verboseOn()) {
        Debug.logVerbose("Exiting method setUserLicenseActive", module);
      }

      Debug.logError(e, module);
    }

  }
}