package com.simbaquartz.xparty.services;

import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyPostalAddressHelper;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
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

/** Offers party search related services. */
public class AxPartySearchServices {

  public static final String module = AxPartySearchServices.class.getName();

  public static class AxPartySearchServicesErrorMessages {
    public static final String PARTY_ID_OR_PARTY_OBJ_REQUIRED =
        "At least one of partyId or partyObj are required to invoke this service, please check your input and try again.";
  }

  /**
   * Prepares searchable text, displayName, searchText, email, photoUrl for party, example for a
   * person record makes a searchText using the format partyId / displayName / email / externalId
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> populateBasicInformationForParty(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();

    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String partyId = (String) context.get("partyId");
    GenericValue partyObj = (GenericValue) context.get("partyObj");
    Boolean overrideExistingValues = (Boolean) context.get("overrideExistingValues");
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    if (UtilValidate.isEmpty(partyId) && UtilValidate.isEmpty(partyObj)) {
      Debug.logError(AxPartySearchServicesErrorMessages.PARTY_ID_OR_PARTY_OBJ_REQUIRED, module);
      return ServiceUtil.returnError(
          AxPartySearchServicesErrorMessages.PARTY_ID_OR_PARTY_OBJ_REQUIRED);
    }

    try {
      if (UtilValidate.isEmpty(partyObj)) {
        partyObj = HierarchyUtils.getPartyByPartyId(delegator, partyId);
        if (overrideExistingValues || UtilValidate.isNotEmpty(partyObj)) {
          // add display name to party entity
          String displayName = partyObj.getString("displayName");
          if (overrideExistingValues || UtilValidate.isEmpty(displayName)) {
            displayName = AxPartyHelper.getPartyName(delegator, partyId);
            partyObj.set("displayName", displayName);
          } else {
            Debug.logInfo(
                "Display name:"
                    + displayName
                    + ", already exists, doing nothing. Use overrideExistingValues flag to override values.",
                module);
          }

          // Check and add photo url to party entity if needed
          String photoUrl = partyObj.getString("photoUrl");

          if (overrideExistingValues || UtilValidate.isEmpty(photoUrl)) {
            Map<String, Object> generatePublicResourceUrlResp = null;
            try {
              generatePublicResourceUrlResp =
                  dispatcher.runSync(
                      "generatePublicResourceUrl",
                      UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
            } catch (GenericServiceException e) {
              Debug.logError(
                  "An error occurred while invoking generatePublicResourceUrl service, details: "
                      + e.getMessage(),
                  module);
            }

            if (UtilValidate.isNotEmpty(generatePublicResourceUrlResp)) {
              String publicResourceUrl =
                  (String) generatePublicResourceUrlResp.get("publicResourceUrl");
              partyObj.set("photoUrl", publicResourceUrl);
            }
          } else {
            Debug.logInfo(
                "Photo Url :"
                    + photoUrl
                    + ", already exists, doing nothing. Use overrideExistingValues flag to override values.",
                module);
          }

          // Check and add email to party entity
          String partyExistingEmail = partyObj.getString("email");
          if (overrideExistingValues || UtilValidate.isEmpty(partyExistingEmail)) {
            GenericValue partyEmail = AxPartyHelper.findPartyLatestEmailAddress(partyId, delegator);
            if (UtilValidate.isNotEmpty(partyEmail)) {
              partyExistingEmail = partyEmail.getString("infoString");
              partyObj.set("email", partyExistingEmail);
            }

          } else {
            Debug.logInfo(
                "Email :"
                    + partyExistingEmail
                    + ", already exists, doing nothing. Use overrideExistingValues flag to override values.",
                module);
          }

          // prepare searchText
          String partyExistingSearchText = partyObj.getString("searchText");
          if (overrideExistingValues || UtilValidate.isEmpty(partyExistingSearchText)) {
            String partyExternalId = partyObj.getString("externalId");
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(partyId + " / " + displayName);
            if (UtilValidate.isNotEmpty(partyExistingEmail)) {
              stringBuffer.append(" / " + partyExistingEmail);
            }
            if (UtilValidate.isNotEmpty(partyExternalId)) {
              stringBuffer.append(" / " + partyExternalId);
            }
            String searchableText = stringBuffer.toString();
            partyObj.set("searchText", searchableText);

          } else {
            Debug.logInfo(
                "Search text :"
                    + partyExistingSearchText
                    + ", already exists, doing nothing. Use overrideExistingValues flag to override values.",
                module);
          }

          Double rating = partyObj.getDouble("rating");
          if (overrideExistingValues || UtilValidate.isEmpty(rating)) {
            List<GenericValue> partyFeedbackList =
                EntityQuery.use(delegator)
                    .from("PartyFeedback")
                    .where("partyId", partyId)
                    .queryList();
            Double netRating = 0.0;
            if (UtilValidate.isNotEmpty(partyFeedbackList) && partyFeedbackList.size() > 0) {
              long total =
                  partyFeedbackList.stream().mapToLong(x -> x.getLong("experienceRating")).sum();
              netRating = ((double) total) / partyFeedbackList.size();
            }
            partyObj.set("rating", netRating);

          } else {
            Debug.logInfo(
                "Rating :"
                    + partyExistingSearchText
                    + ", already exists, doing nothing. Use overrideExistingValues flag to override values.",
                module);
          }

          // location details
          Map partyPrimaryAddressMap =
              PartyPostalAddressHelper.getPrimaryAddress(partyId, delegator);
          if (UtilValidate.isNotEmpty(partyPrimaryAddressMap)) {
            String contactMechId = (String) partyPrimaryAddressMap.get("contactMechId");
            String formattedAddress = (String) partyPrimaryAddressMap.get("formattedAddress");
            if (UtilValidate.isNotEmpty(formattedAddress)) {
              // update location and locationContactMechId
              partyObj.set("location", formattedAddress);
              partyObj.set("locationContactMechId", contactMechId);
            }
          }
          delegator.store(partyObj);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError("An error occurred while fetching party details: " + e.getMessage(), module);
    }
    return result;
  }
}
