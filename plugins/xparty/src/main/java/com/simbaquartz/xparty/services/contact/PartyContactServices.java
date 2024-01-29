package com.simbaquartz.xparty.services.contact;

import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.services.contact.PhoneTypesEnum;
import com.simbaquartz.xparty.services.location.PostalAddressTypesEnum;
import java.util.List;
import java.util.Map;
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

/** Party contact (email/phone/address) related services */
public class PartyContactServices {
  private static final String module = PartyContactServices.class.getName();

  /**
   * Sets the primary purpose {@link PhoneTypesEnum} for the input Email and party
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericServiceException
   * @throws GenericEntityException
   */
  public static Map<String, Object> makePrimaryPhone(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericServiceException, GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String contactMechId = (String) context.get("contactMechId");

    // fetch all primary phone
    List<GenericValue> primaryPhones =
        EntityQuery.use(delegator)
            .from("PartyContactMechPurpose")
            .where(
                "partyId", partyId, "contactMechPurposeTypeId", PhoneTypesEnum.PRIMARY.getTypeId())
            .queryList();
    if (UtilValidate.isNotEmpty(primaryPhones)) {
      for (GenericValue primaryPhone : primaryPhones) {
        primaryPhone.remove();
      }
    }

    // make contactMech primary
    Map addPhonePurposeCtx =
        UtilMisc.toMap(
            "userLogin",
            userLogin,
            "partyId",
            partyId,
            "contactMechPurposeTypeId",
            PhoneTypesEnum.PRIMARY.getTypeId(),
            "contactMechId",
            contactMechId);

    try {
      dispatcher.runSync("createPartyContactMechPurpose", addPhonePurposeCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Sets the primary purpose {@link EmailTypesEnum}
   * for the input Email and party.
   * Removes existing primary entries
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericServiceException
   * @throws GenericEntityException
   */
  public static Map<String, Object> makePrimaryEmail(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericServiceException, GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String contactMechId = (String) context.get("contactMechId");

    // fetch all primary email
    List<GenericValue> primaryEmails =
        EntityQuery.use(delegator)
            .from("PartyContactMechPurpose")
            .where(
                "partyId", partyId, "contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId())
            .queryList();
    if (UtilValidate.isNotEmpty(primaryEmails)) {
      for (GenericValue primaryEmail : primaryEmails) {
        primaryEmail.remove();
      }
    }

    // make contactMech primary
    Map addEmailPurposeCtx =
        UtilMisc.toMap(
            "userLogin",
            userLogin,
            "partyId",
            partyId,
            "contactMechPurposeTypeId",
            EmailTypesEnum.PRIMARY.getTypeId(),
            "contactMechId",
            contactMechId);

    try {
      dispatcher.runSync("createPartyContactMechPurpose", addEmailPurposeCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Sets the primary purpose {@link PostalAddressTypesEnum} for the input PostalAddress and party. Removes any existing primary addresses.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericServiceException
   * @throws GenericEntityException
   */
  public static Map<String, Object> makePrimaryAddress(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericServiceException, GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String contactMechId = (String) context.get("contactMechId");

    // fetch all primary addresses
    List<GenericValue> primaryEmails =
        EntityQuery.use(delegator)
            .from("PartyContactMechPurpose")
            .where(
                "partyId",
                partyId,
                "contactMechPurposeTypeId",
                PostalAddressTypesEnum.PRIMARY.getTypeId())
            .queryList();
    if (UtilValidate.isNotEmpty(primaryEmails)) {
      for (GenericValue primaryEmail : primaryEmails) {
        primaryEmail.remove();
      }
    }

    // make contactMech primary
    Map addEmailPurposeCtx =
        UtilMisc.toMap(
            "userLogin",
            userLogin,
            "partyId",
            partyId,
            "contactMechPurposeTypeId",
            PostalAddressTypesEnum.PRIMARY.getTypeId(),
            "contactMechId",
            contactMechId);

    try {
      dispatcher.runSync("createPartyContactMechPurpose", addEmailPurposeCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }
}
