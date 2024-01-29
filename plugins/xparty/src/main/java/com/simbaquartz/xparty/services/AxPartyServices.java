package com.simbaquartz.xparty.services;

import static com.fidelissd.zcp.xcommon.util.AxUtilFormat.formatCreatedTimestamp;
import static com.fidelissd.zcp.xcommon.util.AxUtilFormat.formatDateLong;
import static com.fidelissd.zcp.xcommon.util.AxUtilFormat.formatDateMedium;
import static com.fidelissd.zcp.xcommon.util.DateUtil.displayTimeZoneOffset;
import static com.simbaquartz.xparty.hierarchy.HierarchyRelationshipUtils.module;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.util.AxUtilFormat;
import com.simbaquartz.xgeo.utils.GeoUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyContactHelper;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFieldValue;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** Created by Joban on 7/5/17. */
public class AxPartyServices {

  public static final String LEAD_POC = "LEAD_POC";
  public static final String LEAD = "LEAD";
  public static final String CONT_TYPE_WARRANTY = "WARRANTY";
  public static final String CONT_TYPE_SPECIAL_CONDITIONS = "TERMS_AND_CONDS";
  public static final String SOLR_RECORD_ITEM_DELIMITER = "\\^";
  public static final String SOLR_RECORD_VALUE_DELIMITER = ":";
  private static final String solrCoreName =
      UtilProperties.getPropertyValue("SolrConnector.properties", "solr.core.name");
  private static final String solrSearchServer =
      UtilProperties.getPropertyValue("SolrConnector.properties", "solr.instance");
  private static int DEFAULT_PAGE_SIZE = 10;

  public static class AxPartyServicesErrorMessages {
    public static final String SYSTEM_ERROR_RESP = "Something went wrong, please try later.";
    public static final String ERROR_CREATE_RELATION =
        "Error while creating relationship for party and party poc";
    public static final String ERROR_CREATE_ROLE = "Error while creating role for party poc";
    public static final String ERROR_CREATING_USER_EMAIL = "Error while creating party poc email";
    public static final String ERROR_UPDATING_USER_EMAIL = "Error while updating party poc email";
    public static final String ERROR_CREATING_USER_PHONE = "Error while creating party poc phone";
    public static final String ERROR_UPDATING_USER_PHONE = "Error while updating party poc phone";
    public static final String PARTY_POC_DETAILS_NOT_UPDATED =
        "Error while updating party poc details";
    public static final String ERROR_CREATING_USER = "Error while creating user account";
  }

  public Map<String, Object> axUpdatePartyEmailAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = new HashMap<>();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String contactMechId = (String) context.get("contactMechId");
    String email = (String) context.get("email");
    String partyId = (String) context.get("partyId");
    String emailVerified = (String) context.get("verified");
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    Map<String, Object> axUpdatePartyEmailAddressCtx = new HashMap<>();

    axUpdatePartyEmailAddressCtx.put("partyId", partyId);
    axUpdatePartyEmailAddressCtx.put("userLogin", userLogin);
    axUpdatePartyEmailAddressCtx.put("contactMechId", contactMechId);
    axUpdatePartyEmailAddressCtx.put("infoString", email);
    if (UtilValidate.isNotEmpty(emailVerified)) {
      axUpdatePartyEmailAddressCtx.put("verified", emailVerified);
    }

    axUpdatePartyEmailAddressCtx.put("contactMechTypeId", "EMAIL_ADDRESS");

    Map<String, Object> axUpdatePartyEmailAddressServiceResponse;
    try {
      axUpdatePartyEmailAddressServiceResponse =
          dispatcher.runSync("updatePartyContactMech", axUpdatePartyEmailAddressCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    if (ServiceUtil.isError(axUpdatePartyEmailAddressServiceResponse)) {
      return ServiceUtil.returnError(
          ServiceUtil.getErrorMessage(axUpdatePartyEmailAddressServiceResponse));
    }
    serviceResult.put(
        "oldContactMechId", axUpdatePartyEmailAddressServiceResponse.get("contactMechId"));
    return serviceResult;
  }


  public static Map<String, Object> updateTelecomNumber(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult;
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String contactMechId = (String) context.get("contactMechId");
    String countryCode = (String) context.get("countryCode");
    String areaCode = (String) context.get("areaCode");
    String contactNumber = (String) context.get("contactNumber");
    String phoneVerified = (String) context.get("phoneVerified");

    try {
      GenericValue telecomNumber =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(telecomNumber)) {
        if (UtilValidate.isNotEmpty(countryCode)) {
          telecomNumber.set("countryCode", countryCode);
        }
        if (UtilValidate.isNotEmpty(areaCode)) {
          telecomNumber.set("areaCode", areaCode);
        }
        if (UtilValidate.isNotEmpty(contactNumber)) {
          telecomNumber.set("contactNumber", contactNumber);
        }
        telecomNumber.store();
      }
      GenericValue phoneVerifiedRecord =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(phoneVerifiedRecord)) {
        if (UtilValidate.isNotEmpty(phoneVerified)) {
          phoneVerifiedRecord.set("verified", phoneVerified.toUpperCase());
        }
        phoneVerifiedRecord.store();
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return ServiceUtil.returnSuccess();
  }

  public static Map<String, Object> updateEmailAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult;
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String storeId = (String) context.get("storeId");
    String contactMechId = (String) context.get("contactMechId");
    String infoString = (String) context.get("emailAddress");
    String emailVerified = (String) context.get("emailVerified");
    String allowSolicitation = (String) context.get("allowSolicitation");
    if (UtilValidate.isNotEmpty(infoString)
        || UtilValidate.isNotEmpty(emailVerified)
        || UtilValidate.isNotEmpty(allowSolicitation)) {
      try {
        GenericValue emailAddress =
            EntityQuery.use(delegator)
                .from("ContactMech")
                .where("contactMechId", contactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(emailAddress)) {
          if (UtilValidate.isNotEmpty(infoString)) {
            emailAddress.set("infoString", infoString);
          }
          emailAddress.store();
        }
        GenericValue emailVerifiedRecord =
            EntityQuery.use(delegator)
                .from("PartyContactMech")
                .where("contactMechId", contactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(emailVerifiedRecord)) {
          if (UtilValidate.isNotEmpty(emailVerified)) {
            emailVerifiedRecord.set("verified", emailVerified.toUpperCase());
          }
          if (UtilValidate.isNotEmpty(allowSolicitation)) {
            emailVerifiedRecord.set("allowSolicitation", allowSolicitation.toUpperCase());
          }
          emailVerifiedRecord.store();
        }
      } catch (Exception e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }
    }
    return ServiceUtil.returnSuccess();
  }

  public static Map getEnvHost(DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    Map envVar = System.getenv();
    String EnvHost = (String) envVar.get("ENV");
    serviceResult.put("envHostVar", envVar);
    serviceResult.put("ENV", EnvHost);
    return serviceResult;
  }

  /**
   * * Get designations.
   *
   * <p>This service gets the position titles from the party Relationship tables
   *
   * @param context
   * @return
   */
  public static Map<String, Object> getPartyDesignations(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    String keyword = (String) context.get("keyword");
    List<Map> workspaceList = new LinkedList<>();
    try {
      List<EntityCondition> exprList = new LinkedList<>();

      exprList.add(
          EntityCondition.makeCondition(
              "roleTypeIdFrom", EntityOperator.EQUALS, "INTERNAL_ORGANIZATIO"));
      exprList.add(
          EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "EMPLOYEE"));
      exprList.add(
          EntityCondition.makeCondition(
              "partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT"));

      EntityConditionList<EntityCondition> workspaceExprList =
          EntityCondition.makeCondition(exprList, EntityOperator.AND);

      List<GenericValue> designations =
          EntityQuery.use(delegator)
              .select("positionTitle")
              .from("PartyRelationship")
              .where(workspaceExprList)
              .distinct()
              .queryList();

      int count = 0;
      if (UtilValidate.isNotEmpty(designations)) {
        for (GenericValue designation : designations) {
          Map<String, Object> designationMap = new HashMap<>();
          designationMap.put("positionTitle", designation.getString("positionTitle"));
          workspaceList.add(designationMap);
          count++;

          // fetching 20 records only
          if (count > 19) {
            break;
          }
        }
      }
      result.put("result", workspaceList);

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> createTelecomNumberRegionForParty(
      DispatchContext dctx, Map<String, ? extends Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyRegion = (String) context.get("partyRegion");
    try {
      GenericValue userPreference = delegator.makeValue("UserPreference");
      userPreference.set("userLoginId", contactMechId);
      userPreference.set("userPrefTypeId", "defaultCountryGeoId");
      userPreference.set("userPrefGroupTypeId", "APPL_PREFERENCES");
      userPreference.set("userPrefValue", partyRegion);
      delegator.create(userPreference);

    } catch (Exception e) {
      Debug.logError(e, e.getMessage(), module);
    }
    return result;
  }

  public static Map<String, Object> fetchPartyWebAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    try {
      GenericValue webAddress =
          EntityQuery.use(delegator)
              .from("PartyAndContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(webAddress)) {
        Map webUrlDetailsMap = new HashMap();
        GenericValue contactMechAttr =
            EntityQuery.use(delegator)
                .from("ContactMechAttribute")
                .where("contactMechId", contactMechId, "attrName", "LEAD_WEBLINK_DESCRIPTION")
                .queryOne();
        webUrlDetailsMap.put("contactMechId", webAddress.getString("contactMechId"));
        webUrlDetailsMap.put("webLink", webAddress.getString("infoString"));
        webUrlDetailsMap.put("createdDate", webAddress.getString("createdDate"));
        webUrlDetailsMap.put("lastModifiedDate", webAddress.getString("lastModifiedDate"));
        if (UtilValidate.isNotEmpty(contactMechAttr)) {
          webUrlDetailsMap.put("description", contactMechAttr.get("attrValue"));
        }
        result.put("webLinkDetails", webUrlDetailsMap);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> fetchPartyWebAddresses(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");

    try {
      List andConditionsList = new LinkedList();
      andConditionsList.add(
          EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
      andConditionsList.add(
          EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "WEB_ADDRESS"));

      andConditionsList.add(
          EntityCondition.makeCondition(
              "fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
      andConditionsList.add(
          EntityCondition.makeCondition(
              EntityCondition.makeCondition(
                  "thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
              EntityOperator.OR,
              EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
      EntityCondition conditions =
          EntityCondition.makeCondition(andConditionsList, EntityOperator.AND);

      List<GenericValue> webAddresses =
          EntityQuery.use(delegator)
              .from("PartyAndContactMech")
              .where(conditions)
              .orderBy("-createdDate")
              .queryList();
      if (UtilValidate.isNotEmpty(webAddresses)) {
        List<Map> webUrlList = new LinkedList<>();
        for (GenericValue webAddress : webAddresses) {
          Map webUrlDetailsMap = new HashMap();
          String contactMechId = webAddress.getString("contactMechId");
          GenericValue contactMechAttr =
              EntityQuery.use(delegator)
                  .from("ContactMechAttribute")
                  .where("contactMechId", contactMechId, "attrName", "LEAD_WEBLINK_DESCRIPTION")
                  .queryOne();
          webUrlDetailsMap.put("contactMechId", contactMechId);
          webUrlDetailsMap.put("webLink", webAddress.getString("infoString"));
          webUrlDetailsMap.put("createdDate", webAddress.getString("createdDate"));
          webUrlDetailsMap.put("lastModifiedDate", webAddress.getString("lastModifiedDate"));
          if (UtilValidate.isNotEmpty(contactMechAttr)) {
            webUrlDetailsMap.put("description", contactMechAttr.get("attrValue"));
          }
          webUrlList.add(webUrlDetailsMap);
        }
        result.put("webLinksList", webUrlList);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> updatePartyWebLink(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String contactMechId = (String) context.get("contactMechId");
    String url = (String) context.get("url");
    String description = (String) context.get("description");

    try {
      if (UtilValidate.isNotEmpty(url)) {
        Map<String, Object> updateLeadLinkContext = new HashMap<>();
        updateLeadLinkContext.put("userLogin", userLogin);
        updateLeadLinkContext.put("contactMechId", contactMechId);
        updateLeadLinkContext.put("partyId", partyId);
        updateLeadLinkContext.put("contactMechTypeId", "WEB_ADDRESS");
        updateLeadLinkContext.put("infoString", url);
        Debug.log(
            "Invoking service updatePartyContactMech with input context : " + updateLeadLinkContext,
            module);
        Map updatePartyLinkUrlResp =
            dispatcher.runSync("updatePartyContactMech", updateLeadLinkContext);
        if (ServiceUtil.isError(updatePartyLinkUrlResp)) {
          String errorMessage = ServiceUtil.getErrorMessage(updatePartyLinkUrlResp);
          Debug.logError(
              "An error occurred while invoking updatePartyContactMech service, details: "
                  + errorMessage,
              module);
          return ServiceUtil.returnError(errorMessage);
        }
        contactMechId = (String) updatePartyLinkUrlResp.get("contactMechId");
      }

      // updating contactMech attribute aka description of the link for current
      if (UtilValidate.isNotEmpty(description)) {
        GenericValue contactMechAttr =
            EntityQuery.use(delegator)
                .from("ContactMechAttribute")
                .where("contactMechId", contactMechId, "attrName", "LEAD_WEBLINK_DESCRIPTION")
                .queryOne();
        if (UtilValidate.isNotEmpty(contactMechAttr)) {
          contactMechAttr.set("attrValue", description);
          contactMechAttr.store();
        } else {
          Map createPartyLinkedInUrlAttrResponse =
              dispatcher.runSync(
                  "createContactMechAttribute",
                  UtilMisc.toMap(
                      "userLogin",
                      userLogin,
                      "contactMechId",
                      contactMechId,
                      "attrName",
                      "LEAD_WEBLINK_DESCRIPTION",
                      "attrValue",
                      description));
          if (ServiceUtil.isError(createPartyLinkedInUrlAttrResponse)) {
            Debug.logError(ServiceUtil.getErrorMessage(createPartyLinkedInUrlAttrResponse), module);
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createPartyLinkedInUrlAttrResponse));
          }
        }
      }
      result.put("contactMechId", contactMechId);
    } catch (GenericEntityException | GenericServiceException e) {
      Debug.logError(e, "Error occurred updating party web link", module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  /**
   * Creates an party poc
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> createPartyPoc(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method createPartyPoc", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();

    LocalDispatcher dispatcher = dctx.getDispatcher();

    try {
      // input details from the user
      String fullName = (String) context.get("fullName");
      String partyId = (String) context.get("partyId");
      Map<String, Object> emailAddressMap = (Map<String, Object>) context.get("email");
      Map<String, Object> phoneMap = (Map<String, Object>) context.get("phone");

      GenericValue userLogin = (GenericValue) context.get("userLogin");

      // creating a person using full name as first name & person user login
      Map<String, Object> createPersonCtx =
          UtilMisc.toMap(
              "firstName", fullName,
              "displayName", fullName);

      Map<String, Object> createPersonResp = dispatcher.runSync("createPerson", createPersonCtx);

      String pocPartyId = null;
      if (ServiceUtil.isError(createPersonResp)) {
        String errorMessage = ServiceUtil.getErrorMessage(createPersonResp);
        Debug.logError(errorMessage, module);
        return ServiceUtil.returnError(AxPartyServicesErrorMessages.ERROR_CREATING_USER);
      }

      pocPartyId = (String) createPersonResp.get("partyId");

      // create the relationship of owner person and account party group
      Map<String, Object> createPartyRoleCtx = new HashMap<String, Object>();
      createPartyRoleCtx.put("partyId", pocPartyId);
      createPartyRoleCtx.put("roleTypeId", LEAD_POC);
      createPartyRoleCtx.put("userLogin", userLogin);

      // Creating role of lead poc i.e. LEAD_POC
      Map<String, Object> createRoleResult = null;
      createRoleResult = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
      if (ServiceUtil.isError(createRoleResult)) {
        Debug.logError(
            AxPartyServicesErrorMessages.ERROR_CREATE_ROLE,
            ServiceUtil.getErrorMessage(createRoleResult),
            module);
        return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
      }

      // create the relationship of owner person and account party group
      Map<String, Object> createPartyRelationshipCtx = new HashMap<String, Object>();
      createPartyRelationshipCtx.put("partyIdFrom", partyId);
      createPartyRelationshipCtx.put("partyIdTo", pocPartyId);
      createPartyRelationshipCtx.put("roleTypeIdFrom", LEAD);
      createPartyRelationshipCtx.put("roleTypeIdTo", LEAD_POC);
      createPartyRelationshipCtx.put("partyRelationshipTypeId", LEAD_POC);
      createPartyRelationshipCtx.put("userLogin", userLogin);

      // Created relationship lead poc and lead i.e. lead as LEAD and lead poc as LEAD_POC
      Map<String, Object> createRelationshipResult = null;
      createRelationshipResult =
          dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
      if (ServiceUtil.isError(createRelationshipResult)) {
        Debug.logError(
            AxPartyServicesErrorMessages.ERROR_CREATE_RELATION,
            ServiceUtil.getErrorMessage(createRelationshipResult),
            module);
        return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
      }

      if (UtilValidate.isNotEmpty(emailAddressMap)) {
        String email = (String) emailAddressMap.get("emailAddress");

        Map<String, Object> createEmailAddressContext = new HashMap<String, Object>();
        createEmailAddressContext.put("userLogin", userLogin);
        createEmailAddressContext.put("partyId", pocPartyId);
        createEmailAddressContext.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
        createEmailAddressContext.put("emailAddress", email);
        createEmailAddressContext.put("isVerified", "N");

        Map<String, Object> createEmailAddressResp = null;
        createEmailAddressResp =
            dispatcher.runSync("createPartyEmailAddress", createEmailAddressContext);
        if (ServiceUtil.isError(createEmailAddressResp)) {
          Debug.logError(AxPartyServicesErrorMessages.ERROR_CREATING_USER_EMAIL, module);
          return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
        }
      }
      if (UtilValidate.isNotEmpty(phoneMap)) {
        Map<String, Object> createPhoneContext = new HashMap<String, Object>();
        createPhoneContext.put("userLogin", userLogin);
        createPhoneContext.put("partyId", pocPartyId);
        createPhoneContext.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
        if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
          createPhoneContext.put("areaCode", phoneMap.get("areaCode"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
          createPhoneContext.put("contactNumber", phoneMap.get("contactNumber"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
          createPhoneContext.put("countryCode", phoneMap.get("countryCode"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("extension"))) {
          createPhoneContext.put("extension", phoneMap.get("extension"));
        }

        Map<String, Object> createPhoneAddressResp = null;
        createPhoneAddressResp = dispatcher.runSync("createPartyTelecomNumber", createPhoneContext);
        if (ServiceUtil.isError(createPhoneAddressResp)) {
          Debug.logError(AxPartyServicesErrorMessages.ERROR_CREATING_USER_PHONE, module);
          return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
        }
      }

      result.put("partyId", pocPartyId);
    } catch (GenericServiceException e) {
      Debug.logError(e, "An error occurred while creating party POC " + e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> updatePartyPoc(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method updatePartyPoc", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();

    LocalDispatcher dispatcher = dctx.getDispatcher();

    try {
      // input details from the user
      String fullName = (String) context.get("fullName");
      String partyId = (String) context.get("partyId");
      Map<String, Object> emailAddressMap = (Map<String, Object>) context.get("email");
      Map<String, Object> phoneMap = (Map<String, Object>) context.get("phone");

      GenericValue userLogin = (GenericValue) context.get("userLogin");

      // updating party full name
      Map<String, Object> updatePartyPerson = new HashMap<>();

      if (UtilValidate.isNotEmpty(fullName)) {
        updatePartyPerson.put("firstName", fullName);
        updatePartyPerson.put("displayName", fullName);
        updatePartyPerson.put("lastName", "");
      }

      if (UtilValidate.isNotEmpty(updatePartyPerson)) {
        updatePartyPerson.put("userLogin", userLogin);
        updatePartyPerson.put("partyId", partyId);
        Map partyDataSourceResp = dispatcher.runSync("updatePerson", updatePartyPerson);
        if (ServiceUtil.isError(partyDataSourceResp)) {
          Debug.logError(
              ServiceUtil.getErrorMessage(partyDataSourceResp),
              AxPartyServicesErrorMessages.PARTY_POC_DETAILS_NOT_UPDATED,
              module);
          return ServiceUtil.returnError(
              AxPartyServicesErrorMessages.PARTY_POC_DETAILS_NOT_UPDATED);
        }
      }

      if (UtilValidate.isNotEmpty(emailAddressMap)) {
        String email = (String) emailAddressMap.get("emailAddress");
        String contactMechId = (String) emailAddressMap.get("contactMechId");

        Map<String, Object> updateEmailAddressContext = new HashMap<String, Object>();
        updateEmailAddressContext.put("userLogin", userLogin);
        updateEmailAddressContext.put("emailAddress", email);
        updateEmailAddressContext.put("contactMechId", contactMechId);
        updateEmailAddressContext.put("partyId", partyId);

        Map<String, Object> createEmailAddressResp = null;
        createEmailAddressResp =
            dispatcher.runSync("createUpdatePartyEmailAddress", updateEmailAddressContext);
        if (ServiceUtil.isError(createEmailAddressResp)) {
          Debug.logError(AxPartyServicesErrorMessages.ERROR_UPDATING_USER_EMAIL, module);
          return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
        }
      }
      if (UtilValidate.isNotEmpty(phoneMap)) {
        Map<String, Object> updatePhoneContext = new HashMap<String, Object>();
        updatePhoneContext.put("userLogin", userLogin);
        updatePhoneContext.put("partyId", partyId);
        if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
          updatePhoneContext.put("areaCode", phoneMap.get("areaCode"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
          updatePhoneContext.put("contactNumber", phoneMap.get("contactNumber"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
          updatePhoneContext.put("countryCode", phoneMap.get("countryCode"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("extension"))) {
          updatePhoneContext.put("extension", phoneMap.get("extension"));
        }
        if (UtilValidate.isNotEmpty(phoneMap.get("contactMechId"))) {
          updatePhoneContext.put("contactMechId", phoneMap.get("contactMechId"));
        }

        Map<String, Object> createPhoneAddressResp = null;
        createPhoneAddressResp =
            dispatcher.runSync("createUpdatePartyTelecomNumber", updatePhoneContext);
        if (ServiceUtil.isError(createPhoneAddressResp)) {
          Debug.logError(AxPartyServicesErrorMessages.ERROR_UPDATING_USER_PHONE, module);
          return ServiceUtil.returnError(AxPartyServicesErrorMessages.SYSTEM_ERROR_RESP);
        }
      }

      result.put("partyId", partyId);
    } catch (GenericServiceException e) {
      Debug.logError(
          e, "An error occurred while updating party POC details" + e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> deletePartyPoc(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method deletePartyPoc", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    try {
      String partyId = (String) context.get("partyId");

      List conditionsList = new LinkedList();

      conditionsList.add(
          EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));
      conditionsList.add(
          EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, LEAD));
      conditionsList.add(
          EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, LEAD_POC));
      conditionsList.add(
          EntityCondition.makeCondition(
              "partyRelationshipTypeId", EntityOperator.EQUALS, LEAD_POC));

      GenericValue partyRelationship =
          EntityQuery.use(delegator).from("PartyRelationship").where(conditionsList).queryOne();
      List<GenericValue> partyRoles =
          EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId).queryList();

      // deleting party relationship
      if (UtilValidate.isNotEmpty(partyRelationship)) partyRelationship.remove();
      if (UtilValidate.isNotEmpty(partyRoles)) delegator.removeAll(partyRoles);

      List<GenericValue> partyContactMechs =
          EntityQuery.use(delegator).from("PartyContactMech").where("partyId", partyId).queryList();
      ArrayList<String> partyContactMechIds = new ArrayList<>();
      for (GenericValue partyContactMech : partyContactMechs) {
        partyContactMechIds.add(partyContactMech.getString("contactMechId"));
      }
      List<GenericValue> contactMechs =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where(
                  EntityCondition.makeCondition(
                      "contactMechId", EntityOperator.IN, partyContactMechIds))
              .queryList();
      List<GenericValue> partyContactMechPurposes =
          EntityQuery.use(delegator)
              .from("PartyContactMechPurpose")
              .where(
                  EntityCondition.makeCondition(
                      "contactMechId", EntityOperator.IN, partyContactMechIds))
              .queryList();
      List<GenericValue> telecomNumbers =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where(
                  EntityCondition.makeCondition(
                      "contactMechId", EntityOperator.IN, partyContactMechIds))
              .queryList();

      // deleting contact mech
      if (UtilValidate.isNotEmpty(partyContactMechs)) delegator.removeAll(partyContactMechs);
      if (UtilValidate.isNotEmpty(partyContactMechPurposes))
        delegator.removeAll(partyContactMechPurposes);
      if (UtilValidate.isNotEmpty(telecomNumbers)) delegator.removeAll(telecomNumbers);
      if (UtilValidate.isNotEmpty(contactMechs)) delegator.removeAll(contactMechs);

      // removing party detils
      GenericValue person =
          EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryFirst();
      List<GenericValue> partyStatus =
          EntityQuery.use(delegator).from("PartyStatus").where("partyId", partyId).queryList();
      List<GenericValue> partyNameHistory =
          EntityQuery.use(delegator).from("PartyNameHistory").where("partyId", partyId).queryList();
      if (UtilValidate.isNotEmpty(person)) person.remove();

      if (UtilValidate.isNotEmpty(partyStatus)) delegator.removeAll(partyStatus);
      if (UtilValidate.isNotEmpty(partyNameHistory)) delegator.removeAll(partyNameHistory);

      // setting party status as PARTY_DISABLED to disable getting party poc while fetching all POCs
      GenericValue party =
          EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryFirst();
      if (UtilValidate.isNotEmpty(party)) {
        party.remove();
      }

      result.put("partyId", partyId);
    } catch (Exception e) {
      Debug.logError(
          e, "An error occurred while deleting party POC details" + e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> getPartyPocs(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getPartyPocs", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    List<Map<String, Object>> resultList = new LinkedList<>();
    try {
      // input details from the user
      String partyId = (String) context.get("partyId");

      List conditionsList = new LinkedList();

      conditionsList.add(
          EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
      conditionsList.add(
          EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, LEAD));
      conditionsList.add(
          EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, LEAD_POC));
      conditionsList.add(
          EntityCondition.makeCondition(
              "partyRelationshipTypeId", EntityOperator.EQUALS, LEAD_POC));

      List<GenericValue> partyRelationships =
          EntityQuery.use(delegator).from("PartyRelationship").where(conditionsList).queryList();
      if (UtilValidate.isNotEmpty(partyRelationships)) {
        for (GenericValue partyRelationship : partyRelationships) {
          Map<String, Object> partyDetailsMap = new HashMap<>();
          String pocPartyId = (String) partyRelationship.get("partyIdTo");

          List andConditionsList = new LinkedList();
          andConditionsList.add(
              EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, pocPartyId));

          andConditionsList.add(
              EntityCondition.makeCondition(
                  "fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
          andConditionsList.add(
              EntityCondition.makeCondition(
                  EntityCondition.makeCondition(
                      "thruDate",
                      EntityOperator.GREATER_THAN_EQUAL_TO,
                      UtilDateTime.nowTimestamp()),
                  EntityOperator.OR,
                  EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));

          List<GenericValue> partyAndContactMechList =
              EntityQuery.use(delegator)
                  .from("PartyAndContactMech")
                  .where(andConditionsList)
                  .queryList();
          if (UtilValidate.isNotEmpty(partyAndContactMechList)) {
            for (GenericValue partyAndContactMech : partyAndContactMechList) {
              if ("PARTY_DISABLED".equalsIgnoreCase(partyAndContactMech.getString("statusId"))) {
                continue;
              }

              if ("EMAIL_ADDRESS"
                  .equalsIgnoreCase(partyAndContactMech.getString("contactMechTypeId"))) {
                Map contactMechDetailsMap = new HashMap();
                String contactMechId = partyAndContactMech.getString("contactMechId");
                contactMechDetailsMap.put("contactMechId", contactMechId);
                contactMechDetailsMap.put(
                    "emailAddress", partyAndContactMech.getString("infoString"));
                contactMechDetailsMap.put(
                    "createdDate", partyAndContactMech.getString("createdDate"));
                contactMechDetailsMap.put(
                    "lastModifiedDate", partyAndContactMech.getString("lastModifiedDate"));
                partyDetailsMap.put("email", contactMechDetailsMap);
              } else if ("TELECOM_NUMBER"
                  .equalsIgnoreCase(partyAndContactMech.getString("contactMechTypeId"))) {
                Map contactMechDetailsMap = new HashMap();
                String contactMechId = partyAndContactMech.getString("contactMechId");
                contactMechDetailsMap.put("contactMechId", contactMechId);
                contactMechDetailsMap.put("areaCode", partyAndContactMech.getString("tnAreaCode"));
                contactMechDetailsMap.put(
                    "countryCode", partyAndContactMech.getString("tnCountryCode"));
                contactMechDetailsMap.put(
                    "contactNumber", partyAndContactMech.getString("tnContactNumber"));
                contactMechDetailsMap.put("extension", partyAndContactMech.getString("extension"));
                contactMechDetailsMap.put(
                    "createdDate", partyAndContactMech.getString("createdDate"));
                contactMechDetailsMap.put(
                    "lastModifiedDate", partyAndContactMech.getString("lastModifiedDate"));
                partyDetailsMap.put("phone", contactMechDetailsMap);
              }
            }
          }
          GenericValue person =
              EntityQuery.use(delegator).from("Person").where("partyId", pocPartyId).queryOne();
          partyDetailsMap.put("partyId", pocPartyId);
          if (UtilValidate.isNotEmpty(person)) {
            partyDetailsMap.put("fullName", person.getString("firstName"));
          }
          resultList.add(partyDetailsMap);
        }
      }
      result.put("partyPocList", resultList);
    } catch (Exception e) {
      Debug.logError(
          e, "An error occurred while getting party POC details" + e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> splitMergedRecords(
      String record, String itemDelimiter, String valueDelimiter) {
    Map<String, Object> parsedMap = FastMap.newInstance();

    String[] items = record.split(itemDelimiter);

    for (String item : items) {
      String[] itemKeyValue = item.split(valueDelimiter);

      try {
        if (itemKeyValue[0].equals("noteDateTime")) {
          String noteDateTime =
              itemKeyValue[1].trim() + ":" + itemKeyValue[2].trim() + ":" + itemKeyValue[3].trim();
          parsedMap.put(itemKeyValue[0].trim(), noteDateTime);
        } else {
          parsedMap.put(itemKeyValue[0].trim(), itemKeyValue[1].trim());
        }
      } catch (ArrayIndexOutOfBoundsException aiobe) {
        Debug.logWarning("Unable to parse key value pairs for: " + item, module);
      }
    }

    return parsedMap;
  }

  public static String prettyDate(Timestamp dateTimeToFormat) {
    String formattedDate = "";
    if (UtilValidate.isNotEmpty(dateTimeToFormat)) {
      formattedDate = AxUtilFormat.formatDate(dateTimeToFormat);
    }

    return formattedDate;
  }

  /**
   * Service to get data suource with particular dataSourceId
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getDataSource(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String dataSourceTypeId = (String) context.get("dataSourceTypeId");
    String dataSourceId = (String) context.get("dataSourceId");

    try {
      List<EntityCondition> cond = new LinkedList<>();
      cond.add(
          EntityCondition.makeCondition(
              "dataSourceTypeId", EntityOperator.EQUALS, dataSourceTypeId));
      if (UtilValidate.isNotEmpty(dataSourceId)) {
        cond.add(
            EntityCondition.makeCondition("dataSourceId", EntityOperator.EQUALS, dataSourceId));
      }
      List<GenericValue> dataSources =
          EntityQuery.use(delegator)
              .select("dataSourceId", "dataSourceTypeId", "description")
              .from("DataSource")
              .where(cond)
              .cache(true)
              .queryList();

      result.put("dataSource", dataSources);
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
    }
    return result;
  }

  /**
   * Sets the timezone for a party. Sets the userlogin.lastTimezone and adds a partyAttribute named
   * timeZone
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> setPartyTimezone(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String timezoneId = (String) context.get("timezoneId");

    // check if userlogin exists for party id
    GenericValue userLoginRec =
        EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryFirst();
    if (UtilValidate.isNotEmpty(userLoginRec)) {
      // if so then only update the timezone with new timezone
      userLoginRec.set("lastTimeZone", timezoneId);
      userLoginRec.store();
    }

    Map<String, Object> createPartyAttributeCtx = FastMap.newInstance();
    createPartyAttributeCtx.put("userLogin", userLogin);
    createPartyAttributeCtx.put("partyId", partyId);
    createPartyAttributeCtx.put("attrName", "Timezone");
    createPartyAttributeCtx.put("attrValue", timezoneId);

    Map<String, Object> createPartyAttributeResponse;
    try {
      createPartyAttributeResponse =
          dispatcher.runSync("createOrUpdatePartyAttribute", createPartyAttributeCtx);
      if (ServiceUtil.isSuccess(createPartyAttributeResponse)) {
        return createPartyAttributeResponse;
      }
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Returns Timezone details for the party. If a userlogin is found uses the
   * userLogin.lastTimezone, else tries the partyAttribute named Timezone.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getPartyTimezone(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    Map<String, Object> timeZoneDetails = FastMap.newInstance();
    int displayStyle = java.util.TimeZone.LONG;
    String timeZone = null;
    TimeZone timeZoneId = null;

    // check if userlogin exists for party id
    GenericValue partyAtrribute =
        EntityQuery.use(delegator)
            .from("PartyAttribute")
            .where("partyId", partyId, "attrName", "Timezone")
            .queryOne();

    GenericValue userLoginRec =
        EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryFirst();
    if (UtilValidate.isNotEmpty(userLoginRec)) {
      if (UtilValidate.isNotEmpty(userLoginRec.getString("lastTimeZone"))) {
        timeZone = (String) userLoginRec.get("lastTimeZone");
        timeZoneId = UtilDateTime.toTimeZone(timeZone);
      } else {
        // see if party attribute exists
        if (UtilValidate.isNotEmpty(partyAtrribute)) {
          timeZoneId = UtilDateTime.toTimeZone(partyAtrribute.getString("attrValue"));
          // update the lastTimezone on the userLogin record as well
          userLogin.set("lastTimeZone", timeZoneId.getID());
          userLogin.store();
        }
      }
    } else {
      // see if party attribute exists
      if (UtilValidate.isNotEmpty(partyAtrribute)) {
        timeZoneId = UtilDateTime.toTimeZone(partyAtrribute.getString("attrValue"));
      }
    }

    if (UtilValidate.isNotEmpty(timeZoneId)) {

      String timeZoneStr = timeZoneId.toString();

      TimeZone zone = UtilDateTime.toTimeZone(timeZoneStr);
      String name =
          timeZoneId.getDisplayName(zone.useDaylightTime(), displayStyle, Locale.getDefault());

      timeZoneDetails.put("id", timeZoneId.getID());
      timeZoneDetails.put("timeZoneName", name);
      timeZoneDetails.put("gmtOffset", displayTimeZoneOffset(timeZoneId));
    }

    result.put("timeZoneDetails", timeZoneDetails);

    return result;
  }

  public static Map<String, Object> getPartyNotes(DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    List<Map<String, Object>> partyNotes = FastList.newInstance();
    Delegator delegator = dctx.getDelegator();

    String partyId = (String) context.get("partyId");
    Integer viewSize = (Integer) context.get("viewSize");
    Integer startIndex = (Integer) context.get("startIndex");

    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    List<GenericValue> notes =
        EntityQuery.use(delegator)
            .from("PartyNoteView")
            .where("targetPartyId", partyId)
            .orderBy("-noteDateTime")
            .queryList();
    if (UtilValidate.isNotEmpty(notes)) {
      for (GenericValue note : notes) {
        Map<String, Object> noteMap = FastMap.newInstance();
        noteMap.putAll(note);
        // add info of note creator
        if (UtilValidate.isNotEmpty(note.getString("noteParty"))) {
          String notePartyName = AxPartyHelper.getPartyName(delegator, note.getString("noteParty"));

          Map<String, Object> generatePublicResourceUrlResp =
              dispatcher.runSync(
                  "generatePublicResourceUrl",
                  UtilMisc.toMap("userLogin", userLogin, "partyId", note.getString("noteParty")));
          if (UtilValidate.isNotEmpty(generatePublicResourceUrlResp)) {
            String publicResourceUrl =
                (String) generatePublicResourceUrlResp.get("publicResourceUrl");
            noteMap.put("logoImageUrl", publicResourceUrl);
            String thumbNailUrl = (String) generatePublicResourceUrlResp.get("thumbNailUrl");
            noteMap.put("thumbNailUrl", thumbNailUrl);
          }

          noteMap.put("partyName", notePartyName);
        }
        partyNotes.add(noteMap);
      }
    }

    result.put("partyNotes", partyNotes);
    return result;
  }

  public static Map<String, Object> getPartyEmailLogs(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException, ParseException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    List<Map<String, Object>> emailLogs = FastList.newInstance();

    GenericValue getCustomerEmail =
        PartyWorker.findPartyLatestContactMech(
            partyId, "EMAIL_ADDRESS", delegator); // get latest email contact
    if (UtilValidate.isNotEmpty(getCustomerEmail)) {
      EntityCondition emailLogCond =
          EntityCondition.makeCondition(
              EntityOperator.OR,
              EntityCondition.makeCondition(
                  "toString",
                  EntityOperator.LIKE,
                  "%" + getCustomerEmail.getString("infoString") + "%"),
              EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId),
              EntityCondition.makeCondition(
                  "ccString",
                  EntityOperator.LIKE,
                  "%" + getCustomerEmail.getString("infoString") + "%"));

      EntityCondition emailLogAndCond =
          EntityCondition.makeCondition(
              EntityOperator.AND,
              emailLogCond,
              EntityCondition.makeCondition(
                  "communicationEventTypeId", EntityOperator.EQUALS, "EMAIL_COMMUNICATION"));

      List<GenericValue> commEvent =
          EntityQuery.use(delegator)
              .from("CommunicationEvent")
              .where(emailLogAndCond)
              .orderBy("-lastUpdatedStamp")
              .queryList();
      if (UtilValidate.isNotEmpty(commEvent)) {
        for (GenericValue event : commEvent) {
          Map<String, Object> partyEventAsso = FastMap.newInstance();
          Timestamp mailSentTimestamp =
              UtilDateTime.toTimestamp(event.getTimestamp("datetimeStarted"));
          String formattedSentDateTime = prettyDate(mailSentTimestamp);
          String formattedUsDateTime = formatDateMedium(mailSentTimestamp);
          partyEventAsso.put("communicationEventId", event.getString("communicationEventId"));
          if (UtilValidate.isNotEmpty(event.getString("subject"))) {
            partyEventAsso.put("description", event.getString("subject"));
          }
          partyEventAsso.put("formattedSentDateTime", formattedSentDateTime);
          partyEventAsso.put("formattedUsDateTime", formattedUsDateTime);

          emailLogs.add(partyEventAsso);
        }
      }
    }

    result.put("emailLogs", emailLogs);
    return result;
  }

  /**
   * Returns the employees reporting to the input party id. Fetches all parties that have a
   * relationship employment or reports to for the input party id.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static Map<String, Object> getPartyEmployees(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    String searchText = (String) context.get("searchText");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    List<Map<String, Object>> partyEmployeeList = FastList.newInstance();
    List<Map> groups = FastList.newInstance();
    List<Map<Object, Object>> unclassifiedPartyMaps = FastList.newInstance();
    int totalEmployees = 0;

    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
    boolean isPerson = false;

    if (UtilValidate.isNotEmpty(party)) {
      List<GenericValue> employeePersons = FastList.newInstance();
      String partyTypeId = party.getString("partyTypeId");

      switch (partyTypeId) {
        case "PERSON":
          isPerson = true;
          Map<String, Object> serviceResult =
              dispatcher.runSync(
                  "getSubordinatesForPartyId",
                  UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
          List<String> subordinatePartyIdList =
              (List<String>) serviceResult.get("subordinatePartyIdList");

          if (UtilValidate.isNotEmpty(subordinatePartyIdList)) {
            for (String relatedPartyId : subordinatePartyIdList) {
              if (!relatedPartyId.equals(partyId)) {
                // ignore party entries with party.partyId as the response includes the partyIdFrom
                // as well
                GenericValue employeePerson =
                    delegator.findOne("Person", UtilMisc.toMap("partyId", relatedPartyId), false);

                if (UtilValidate.isNotEmpty(employeePerson)) {
                  employeePersons.add(employeePerson);
                }
              }
            }
          }
          break;
        case "PARTY_GROUP":
          Map<String, Object> getEmployeePersonsResult =
              dispatcher.runSync(
                  "getEmployeePersons",
                  UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyId));
          employeePersons =
              (List<GenericValue>) getEmployeePersonsResult.get("employeePersonsList");
          break;
        default:
          break;
      }

      for (GenericValue employeePerson : employeePersons) {
        String employeePartyId = employeePerson.getString("partyId");
        String employeeName =
            AxPartyHelper.getPartyName(delegator, employeePerson.getString("partyId"));

        List<GenericValue> employeeRoles = FastList.newInstance();
        switch (partyTypeId) {
          case "PERSON":
            employeeRoles =
                EntityUtil.filterByDate(
                    delegator.findByAnd(
                        "PartyRelationship",
                        UtilMisc.toMap(
                            "partyIdFrom",
                            partyId,
                            "partyRelationshipTypeId",
                            "REPORTS_TO",
                            "partyIdTo",
                            employeePerson.getString("partyId")),
                        null,
                        false));
            break;
          case "PARTY_GROUP":
            employeeRoles =
                EntityUtil.filterByDate(
                    delegator.findByAnd(
                        "PartyRelationship",
                        UtilMisc.toMap(
                            "partyIdFrom",
                            partyId,
                            "partyRelationshipTypeId",
                            "EMPLOYMENT",
                            "partyIdTo",
                            employeePerson.getString("partyId")),
                        null,
                        false));
            break;
          default:
            break;
        }

        // remove duplicates
        Set<GenericValue> uniquePartyGroupIdSet = new HashSet<GenericValue>();
        uniquePartyGroupIdSet.addAll(employeeRoles);
        List<GenericValue> partyGroupIds = FastList.newInstance();
        partyGroupIds.addAll(uniquePartyGroupIdSet);

        // if party is added as recipient
        Map partyNameDetailsMap =
            AxPartyHelper.getPartyNameAndLogoDetails(delegator, employeePartyId, true);

        // get party destination
        Map<String, Object> fsdGetPartyDesignationResponse =
            dispatcher.runSync(
                "getPartyPositionTitle",
                UtilMisc.toMap("partyId", employeePartyId, "userLogin", userLogin));
        String partyDesignation = "";
        if (ServiceUtil.isSuccess(fsdGetPartyDesignationResponse)) {
          partyDesignation = (String) fsdGetPartyDesignationResponse.get("designation");
        }

        boolean isManager = HierarchyUtils.checkPartyRole(employeePerson, "MANAGER");

        Map<String, Object> partyEmployee = FastMap.newInstance();
        partyEmployee.put("partyId", employeePartyId);
        partyEmployee.put("employeeName", employeeName);
        partyEmployee.put("employeeRoles", partyGroupIds);
        partyEmployee.put("isManager", isManager);
        partyEmployee.put("designation", partyDesignation);
        partyEmployee.put("displayName", (String) partyNameDetailsMap.get("partyName"));
        partyEmployee.put("nameInitials", (String) partyNameDetailsMap.get("partyInitials"));
        partyEmployee.put(
            "partyLogoContentId", (String) partyNameDetailsMap.get("partyLogoContentId"));

        partyEmployeeList.add(partyEmployee);
      }
    }

    if (UtilValidate.isNotEmpty(searchText)) {
      List<EntityCondition> mainAndConds = new LinkedList<EntityCondition>();
      List<EntityExpr> orExprs = new ArrayList<EntityExpr>();
      String entityName = "PartyRoleNameDetail";
      List<String> searchFieldsList = FastList.newInstance();
      searchFieldsList.add("partyId");
      searchFieldsList.add("firstName");
      searchFieldsList.add("middleName");
      searchFieldsList.add("lastName");
      searchFieldsList.add("nickname");
      Boolean searchDistinct = true;
      String fieldValue = searchText;

      if (UtilValidate.isNotEmpty(searchFieldsList) && UtilValidate.isNotEmpty(fieldValue)) {
        Object searchValue = "%" + fieldValue.toUpperCase() + "%";

        for (String fieldName : searchFieldsList) {
          orExprs.add(
              EntityCondition.makeCondition(
                  EntityFunction.UPPER(EntityFieldValue.makeFieldValue(fieldName)),
                  EntityOperator.LIKE,
                  searchValue));
        }
      }

      List<GenericValue> autocompleteOptions = FastList.newInstance();
      if (UtilValidate.isNotEmpty(orExprs) && UtilValidate.isNotEmpty(entityName)) {
        mainAndConds.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        mainAndConds.add(EntityCondition.makeCondition("partyTypeId", "PERSON"));

        EntityCondition entityConditionList =
            EntityCondition.makeCondition(mainAndConds, EntityOperator.AND);

        String viewSizeStr =
            UtilProperties.getPropertyValue("widget", "widget.autocompleter.defaultViewSize");
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxRows(1000);
        findOptions.setDistinct(searchDistinct);

        autocompleteOptions =
            delegator.findList(entityName, entityConditionList, null, null, findOptions, false);
      }

      // augment result to have display name
      List<Map<String, Object>> filteredPartyList = FastList.newInstance();
      if (UtilValidate.isNotEmpty(autocompleteOptions)
          && UtilValidate.isNotEmpty(partyEmployeeList)) {
        for (GenericValue autocompleteOption : autocompleteOptions) {
          // narrow down the list of party employees
          for (Map partyEmployee : partyEmployeeList) {
            String partyEmployeePartyId = (String) partyEmployee.get("partyId");
            if (partyEmployeePartyId.equals(autocompleteOption.getString("partyId"))) {
              filteredPartyList.add(partyEmployee);
            }
          }
        }
      }

      List<String> uniqueParties = FastList.newInstance();
      List<Map<String, Object>> searchList = FastList.newInstance();
      for (Map filteredParty : filteredPartyList) {
        if (!uniqueParties.contains((String) filteredParty.get("partyId"))) {
          uniqueParties.add((String) filteredParty.get("partyId"));
          searchList.add(filteredParty);
        }
      }
      totalEmployees = totalEmployees + searchList.size();
      result.put("searchList", searchList);
    } else {
      if (isPerson) {
        // To prepare reporting staff data for the person
        List<Map> personReportingStaffMaps = FastList.newInstance();
        for (Map employee : partyEmployeeList) {
          Map personReportingStaffMap = FastMap.newInstance();
          personReportingStaffMap.putAll(employee);

          GenericValue employeeParty =
              HierarchyUtils.getPartyByPartyId(delegator, (String) employee.get("partyId"));
          Map partyNameDetailsMap = AxPartyHelper.getPartyNameAndLogoDetails(employeeParty, true);
          String employeeEmailId =
              PartyContactHelper.getEmailAddressForPartyId(
                  (String) partyNameDetailsMap.get("partyId"), delegator);
          // get party destination
          Map fsdGetPartyDesignationResponse =
              dispatcher.runSync(
                  "getPartyPositionTitle",
                  UtilMisc.toMap(
                      "partyId", (String) employee.get("partyId"), "userLogin", userLogin));
          String partyDesignation = "";
          if (ServiceUtil.isSuccess(fsdGetPartyDesignationResponse)) {
            partyDesignation = (String) fsdGetPartyDesignationResponse.get("designation");
          }

          personReportingStaffMap.put("displayName", (String) partyNameDetailsMap.get("partyName"));
          personReportingStaffMap.put("employeeEmailId", employeeEmailId);
          personReportingStaffMap.put("designation", partyDesignation);
          personReportingStaffMap.put(
              "nameInitials", (String) partyNameDetailsMap.get("partyInitials"));
          personReportingStaffMap.put(
              "partyLogoContentId", (String) partyNameDetailsMap.get("partyLogoContentId"));
          boolean isManager = HierarchyUtils.checkPartyRole(employeeParty, "MANAGER");
          personReportingStaffMap.put("isManager", isManager);
          personReportingStaffMaps.add(personReportingStaffMap);
        }
        totalEmployees = totalEmployees + personReportingStaffMaps.size();
        result.put("reportingEmployees", personReportingStaffMaps);
      } else {
        List<String> employeePartyIds = FastList.newInstance();
        for (Map employee : partyEmployeeList) {
          employeePartyIds.add((String) employee.get("partyId"));
        }
        List<GenericValue> classificationGroups =
            EntityQuery.use(delegator)
                .from("SuppPartyClassificationGroup")
                .where("supplierPartyId", partyId)
                .orderBy("sequenceNum")
                .cache(false)
                .queryList();

        for (GenericValue classificationGroup : classificationGroups) {

          Map<String, Object> group = FastMap.newInstance();
          group.putAll(classificationGroup);
          String classificationGroupName = classificationGroup.getString("groupName");
          String classificationGroupDescription = classificationGroup.getString("description");
          classificationGroupName = classificationGroupName.replace("'", "\\'");
          if (UtilValidate.isNotEmpty(classificationGroupDescription)) {
            classificationGroupDescription = classificationGroupDescription.replace("'", "\\'");
          }
          group.put("groupName", classificationGroupName);
          group.put("groupNameUnparsed", classificationGroup.getString("groupName"));
          group.put("description", classificationGroupDescription);

          String groupId = classificationGroup.getString("classificationGroupId");

          List employeeClassificationForParties = FastList.newInstance();

          List<GenericValue> classificationForParties =
              EntityQuery.use(delegator)
                  .from("SupplierPartyClassification")
                  .where("classificationGroupId", groupId)
                  .filterByDate()
                  .queryList();

          if (UtilValidate.isNotEmpty(classificationForParties)) {
            for (GenericValue classificationForParty : classificationForParties) {
              if (employeePartyIds.contains(classificationForParty.getString("partyId"))) {

                String employeePartyId = classificationForParty.getString("partyId");

                List<GenericValue> employeeRoles = FastList.newInstance();
                switch (party.getString("partyTypeId")) {
                  case "PERSON":
                    employeeRoles =
                        EntityUtil.filterByDate(
                            delegator.findByAnd(
                                "PartyRelationship",
                                UtilMisc.toMap(
                                    "partyIdFrom",
                                    partyId,
                                    "partyRelationshipTypeId",
                                    "REPORTS_TO",
                                    "partyIdTo",
                                    employeePartyId),
                                null,
                                false));
                    break;
                  case "PARTY_GROUP":
                    employeeRoles =
                        EntityUtil.filterByDate(
                            delegator.findByAnd(
                                "PartyRelationship",
                                UtilMisc.toMap(
                                    "partyIdFrom",
                                    partyId,
                                    "partyRelationshipTypeId",
                                    "EMPLOYMENT",
                                    "partyIdTo",
                                    employeePartyId),
                                null,
                                false));
                    break;
                  default:
                    break;
                }

                GenericValue employeeParty =
                    HierarchyUtils.getPartyByPartyId(delegator, employeePartyId);
                Map partyNameDetailsMap =
                    AxPartyHelper.getPartyNameAndLogoDetails(
                        delegator, classificationForParty.getString("partyId"), true);
                String employeeEmailId =
                    PartyContactHelper.getEmailAddressForPartyId(
                        (String) partyNameDetailsMap.get("partyId"), delegator);

                // get party destination
                Map fsdGetPartyDesignationResponse =
                    dispatcher.runSync(
                        "getPartyPositionTitle",
                        UtilMisc.toMap("partyId", employeePartyId, "userLogin", userLogin));
                String partyDesignation = "";
                if (ServiceUtil.isSuccess(fsdGetPartyDesignationResponse)) {
                  partyDesignation = (String) fsdGetPartyDesignationResponse.get("designation");
                }

                String lastEngagedDate =
                    AxPartyHelper.getLastEngagedDateWithParty(dispatcher, employeePartyId);

                Map partyEmployee = FastMap.newInstance();
                partyEmployee.put("employeeEmailId", employeeEmailId);
                partyEmployee.put("displayName", (String) partyNameDetailsMap.get("partyName"));
                partyEmployee.put("designation", partyDesignation);
                partyEmployee.put(
                    "nameInitials", (String) partyNameDetailsMap.get("partyInitials"));
                partyEmployee.put(
                    "partyLogoContentId", (String) partyNameDetailsMap.get("partyLogoContentId"));

                partyEmployee.put("partyId", classificationForParty.getString("partyId"));
                partyEmployee.put("employeeRoles", employeeRoles);
                boolean isManager = HierarchyUtils.checkPartyRole(employeeParty, "MANAGER");
                partyEmployee.put("isManager", isManager);
                partyEmployee.put("lastEngagedDate", lastEngagedDate);
                partyEmployee.put(
                    "partyClassificationGroupId",
                    classificationForParty.getString("classificationGroupId"));

                employeeClassificationForParties.add(partyEmployee);
              }
            }
          }
          // sort alphabatically
          employeeClassificationForParties =
              UtilMisc.sortMaps(employeeClassificationForParties, UtilMisc.toList("displayName"));
          group.put("employeeClassificationForParties", employeeClassificationForParties);
          totalEmployees = totalEmployees + employeeClassificationForParties.size();
          groups.add(group);
        }

        // unclassified parties
        List<GenericValue> classificationForParties =
            EntityQuery.use(delegator)
                .from("SupplierPartyClassification")
                .filterByDate()
                .queryList();
        List<String> employeeParties =
            FastList.newInstance(); // list of employees in the form of partyIds
        for (Map employee : partyEmployeeList) {
          employeeParties.add((String) employee.get("partyId"));
        }

        // list of classified employees
        List<String> classifiedParties = FastList.newInstance();
        if (UtilValidate.isNotEmpty(classificationForParties)) {
          for (GenericValue classificationForParty : classificationForParties) {
            if (!classifiedParties.contains(
                classificationForParty.getString("partyId"))) { // uniqueness check
              classifiedParties.add(classificationForParty.getString("partyId"));
            }
          }
        }

        List<String> unclassifiedParties = FastList.newInstance();
        for (String employeeParty : employeeParties) {
          if (!classifiedParties.contains(employeeParty)) {
            unclassifiedParties.add(employeeParty);
          }
        }

        for (String unclassifiedParty : unclassifiedParties) {
          for (Map employee : partyEmployeeList) {
            Map unclassifiedPartyMap = FastMap.newInstance();
            String employeePartyIdStr = (String) employee.get("partyId");
            if (employeePartyIdStr.equals(unclassifiedParty)) {
              unclassifiedPartyMap.putAll(employee);

              GenericValue employeeParty =
                  HierarchyUtils.getPartyByPartyId(delegator, (String) employee.get("partyId"));
              Map partyNameDetailsMap =
                  AxPartyHelper.getPartyNameAndLogoDetails(
                      delegator, (String) employee.get("partyId"), true);
              String employeeEmailId =
                  PartyContactHelper.getEmailAddressForPartyId(
                      (String) partyNameDetailsMap.get("partyId"), delegator);
              // get party destination
              Map fsdGetPartyDesignationResponse =
                  dispatcher.runSync(
                      "getPartyPositionTitle",
                      UtilMisc.toMap(
                          "partyId", (String) employee.get("partyId"), "userLogin", userLogin));
              String partyDesignation = "";
              if (ServiceUtil.isSuccess(fsdGetPartyDesignationResponse)) {
                partyDesignation = (String) fsdGetPartyDesignationResponse.get("designation");
              }

              String lastEngagedDate =
                  AxPartyHelper.getLastEngagedDateWithParty(
                      dispatcher, (String) employee.get("partyId"));
              unclassifiedPartyMap.put(
                  "displayName", (String) partyNameDetailsMap.get("partyName"));
              unclassifiedPartyMap.put("employeeEmailId", employeeEmailId);
              unclassifiedPartyMap.put("designation", partyDesignation);
              unclassifiedPartyMap.put("lastEngagedDate", lastEngagedDate);
              unclassifiedPartyMap.put(
                  "nameInitials", (String) partyNameDetailsMap.get("partyInitials"));
              unclassifiedPartyMap.put(
                  "partyLogoContentId", (String) partyNameDetailsMap.get("partyLogoContentId"));
              boolean isManager = HierarchyUtils.checkPartyRole(employeeParty, "MANAGER");
              unclassifiedPartyMap.put("isManager", isManager);
              unclassifiedPartyMaps.add(unclassifiedPartyMap);
            }
          }
        }

        // sort alphabatically
        unclassifiedPartyMaps =
            UtilMisc.sortMaps(unclassifiedPartyMaps, UtilMisc.toList("displayName"));
        totalEmployees = totalEmployees + unclassifiedPartyMaps.size();
        result.put("classifiedEmployees", groups);
        result.put("unclassifiedEmployees", unclassifiedPartyMaps);
      }
    }

    result.put("totalEmployees", totalEmployees);
    return result;
  }

  /**
   * * Get party terms
   *
   * @param context
   * @return
   */
  public static Map<String, Object> getPartyTerms(DispatchContext dctx, Map<String, Object> context)
      throws Exception, GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    List<Map<String, Object>> terms = FastList.newInstance();
    List contentTypeIdsCondsList = new ArrayList();
    contentTypeIdsCondsList.add(
        EntityCondition.makeCondition(
            "partyContentTypeId", EntityOperator.EQUALS, CONT_TYPE_WARRANTY));
    contentTypeIdsCondsList.add(
        EntityCondition.makeCondition(
            "partyContentTypeId", EntityOperator.EQUALS, CONT_TYPE_SPECIAL_CONDITIONS));
    EntityConditionList<EntityExpr> exprListContentTypeIdsOr =
        EntityCondition.makeCondition(contentTypeIdsCondsList, EntityOperator.OR);

    EntityConditionList<EntityCondition> partyContentsMainCond =
        EntityCondition.makeCondition(
            UtilMisc.toList(
                exprListContentTypeIdsOr,
                EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId)),
            EntityOperator.AND);

    List<GenericValue> partyContents =
        EntityQuery.use(delegator)
            .from("PartyContent")
            .where(partyContentsMainCond)
            .orderBy("sequenceNum")
            .filterByDate()
            .queryList();
    if (UtilValidate.isNotEmpty(partyContents)) {
      for (GenericValue partyContent : partyContents) {
        // Get Content -> Data resource -> Electronic Text
        Map<String, Object> contentMap = FastMap.newInstance();
        GenericValue content = partyContent.getRelatedOne("Content", false);
        GenericValue dataResource = content.getRelatedOne("DataResource", false);
        GenericValue electronicText = dataResource.getRelatedOne("ElectronicText", false);
        if (UtilValidate.isNotEmpty(electronicText)) {

          contentMap.put("contentId", partyContent.getString("contentId"));
          contentMap.put("contentDescription", content.getString("description"));
          contentMap.put("textData", electronicText.getString("textData"));
          contentMap.put("fromDate", partyContent.getTimestamp("fromDate"));
          contentMap.put("partyContentTypeId", partyContent.getString("partyContentTypeId"));

          // get description for party content type
          GenericValue partyContentType = partyContent.getRelatedOne("PartyContentType", false);
          if (UtilValidate.isNotEmpty(partyContentType)) {
            contentMap.put("typeDescription", partyContentType.getString("description"));
          }
        }
        terms.add(contentMap);
      }
    }

    result.put("terms", terms);
    return result;
  }

  /**
   * * Get party messages
   *
   * @param context
   * @return
   */
  public static Map<String, Object> getPartyMessages(
      DispatchContext dctx, Map<String, Object> context) throws Exception, GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    List<Map<String, Object>> messages = FastList.newInstance();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();

    List<GenericValue> msgContents =
        EntityQuery.use(delegator)
            .from("PartyContent")
            .where("partyId", partyId, "partyContentTypeId", "BROADCAST_MSG")
            .orderBy("-lastUpdatedStamp")
            .queryList();
    List<Map<String, Object>> messagesLogsList = FastList.newInstance();
    Boolean hasLogs = false;

    for (GenericValue msg : msgContents) {
      Map<String, Object> msgMap = FastMap.newInstance();
      msgMap.put("contentId", msg.getString("contentId"));
      GenericValue content = msg.getRelatedOne("Content", false);
      msgMap.put("messageName", content.getString("description"));
      String prettyFormattedLastModified = prettyDate(content.getTimestamp("lastUpdatedStamp"));
      String lastModifiedFormattedDate =
          formatCreatedTimestamp(content.getTimestamp("lastUpdatedStamp"));

      msgMap.put("prettyFormattedLastModified", prettyFormattedLastModified);
      msgMap.put("lastModifiedFormattedDate", lastModifiedFormattedDate);

      if (UtilValidate.isNotEmpty(content.getString("dataResourceId"))) {
        msgMap.put("dataResourceId", content.getString("dataResourceId"));

        GenericValue messageBody =
            EntityQuery.use(delegator)
                .from("ElectronicText")
                .where("dataResourceId", content.getString("dataResourceId"))
                .queryOne();
        if (UtilValidate.isNotEmpty(messageBody.getString("textData"))) {
          msgMap.put("message", messageBody.getString("textData"));
        }
      }
      msgMap.put("contentType", msg.getString("partyContentTypeId"));
      msgMap.put("fromDate", msg.getTimestamp("fromDate"));
      msgMap.put("createdBy", msg.getString("createdByUserLogin"));
      if (UtilValidate.isNotEmpty(msg.getString("createdByUserLogin"))) {
        msgMap.put(
            "createdByPartyName",
            AxPartyHelper.getPartyName(delegator, msg.getString("createdByUserLogin")));
      }

      // get list of message tags
      Map<String, Object> getMsgTagsMap = FastMap.newInstance();
      getMsgTagsMap.put("userLogin", userLogin);
      getMsgTagsMap.put("messageId", msg.getString("contentId"));
      getMsgTagsMap.put("supplierPartyId", partyId);

      Map<String, Object> getMsgTagsMapResponse =
          dispatcher.runSync("getSupplierMessageTags", getMsgTagsMap);
      if (!ServiceUtil.isSuccess(getMsgTagsMapResponse)) {
        Debug.logError(
            "An Error occurred while createPerson service call: "
                + ServiceUtil.getErrorMessage(getMsgTagsMapResponse),
            module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getMsgTagsMapResponse));
      }

      List<Map<String, Object>> tags =
          (List<Map<String, Object>>) getMsgTagsMapResponse.get("messageTagList");
      msgMap.put("tags", tags);

      // get message attachments
      List<GenericValue> msgAttachments =
          EntityQuery.use(delegator)
              .from("PartyMessageContent")
              .where("partyId", partyId, "messageId", msg.getString("contentId"))
              .queryList();
      List<Map<String, Object>> msgAttachmentList = FastList.newInstance();
      if (UtilValidate.isNotEmpty(msgAttachments)) {
        for (GenericValue msgAttachment : msgAttachments) {
          Map<String, Object> attachmentMap = FastMap.newInstance();
          attachmentMap.put("fromDate", msgAttachment.getTimestamp("fromDate"));
          attachmentMap.put("contentId", msgAttachment.getString("contentId"));
          GenericValue contentRec = msgAttachment.getRelatedOne("Content", true);
          attachmentMap.put("contentName", contentRec.getString("contentName"));

          if (UtilValidate.isNotEmpty(msgAttachment.getString("contentId"))) {
            Map fsdEnrichContentCtx =
                UtilMisc.toMap(
                    "userLogin", userLogin, "contentId", msgAttachment.getString("contentId"));
            Map fsdEnrichContentCtxResponse = FastMap.newInstance();
            try {
              fsdEnrichContentCtxResponse =
                  dispatcher.runSync("extEnrichContent", fsdEnrichContentCtx, 300, true);
            } catch (Exception ex) {
              Debug.logError(ex, module);
            }

            // fetching thumbNail Url
            Map<String, Object> getThumbNailUrlCtx = FastMap.newInstance();
            getThumbNailUrlCtx.put("contentId", msgAttachment.getString("contentId"));
            Map<String, Object> getThumbNailUrlResult =
                dispatcher.runSync("getThumbNailUrl", getThumbNailUrlCtx);
            String thumbNailUrl = (String) getThumbNailUrlResult.get("thumbNailUrl");
            attachmentMap.put("thumbNailUrl", thumbNailUrl);
          }
          msgAttachmentList.add(attachmentMap);
        }
      }

      // get message logs
      List<GenericValue> communicationEvents =
          EntityQuery.use(delegator)
              .from("SupplierMessageCommunication")
              .where("messageId", msg.getString("contentId"))
              .queryList();

      if (UtilValidate.isNotEmpty(communicationEvents)) {
        for (GenericValue communicationEvent : communicationEvents) {
          Map<String, Object> messagesLogsMap = FastMap.newInstance();
          String communicationEventId = communicationEvent.getString("communicationEventId");

          GenericValue commEventPurpose =
              EntityQuery.use(delegator)
                  .from("CommunicationEventPurpose")
                  .where(
                      "communicationEventPrpTypId",
                      "BRDCST_MSG_TO_CM",
                      "communicationEventId",
                      communicationEventId)
                  .queryOne();
          GenericValue communicationEventRec =
              EntityQuery.use(delegator)
                  .from("CommunicationEvent")
                  .where("communicationEventId", communicationEventId)
                  .queryOne();

          if (UtilValidate.isNotEmpty(commEventPurpose)) {
            Timestamp logCreatedTs = commEventPurpose.getTimestamp("createdStamp");
            String lastRunOnFormatted = formatDateLong(logCreatedTs);
            messagesLogsMap.put("communicationEventId", communicationEventId);
            messagesLogsMap.put("description", commEventPurpose.getString("description"));
            messagesLogsMap.put("partyId", communicationEventRec.getString("partyIdTo"));
            messagesLogsMap.put("lastRun", lastRunOnFormatted);
            messagesLogsList.add(messagesLogsMap);
            hasLogs = true;
          }
        }
      }
      msgMap.put("msgAttachmentList", msgAttachmentList);
      msgMap.put("totalAttachments", msgAttachmentList.size());
      msgMap.put("hasLogs", hasLogs);
      // get message recurrence info
      GenericValue msgRecurrenceInfo =
          EntityQuery.use(delegator)
              .from("MessageRecurrenceInfo")
              .where("messageId", msg.getString("contentId"))
              .queryOne();
      if (UtilValidate.isNotEmpty(msgRecurrenceInfo)) {
        GenericValue recurrenceTypeInfo = msgRecurrenceInfo.getRelatedOne("Enumeration", true);
        msgMap.put("recurrenceTypeDescription", recurrenceTypeInfo.getString("description"));
        if (UtilValidate.isNotEmpty(msgRecurrenceInfo.getTimestamp("nextScheduledRun"))) {
          String nextScheduledRunFormatted =
              formatDateLong(msgRecurrenceInfo.getTimestamp("nextScheduledRun"));
          msgMap.put("nextScheduledRunFormatted", nextScheduledRunFormatted);
        }
      }
      messages.add(msgMap);
    }

    result.put("messages", messages);
    return result;
  }

  /**
   * Create party id from email address.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> createPartyIdFromEmailAddress(
      DispatchContext dctx, Map<String, ? extends Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String emailAddress = (String) context.get("emailAddress");
    String firstName = null;
    String lastName = null;
    List<GenericValue> toBeStored = new LinkedList<GenericValue>();
    Map<String, Object> findPartyFromEmailAddressResp = null;
    emailAddress = emailAddress.trim();

    if (UtilValidate.isNotEmpty(emailAddress)) {
      try {
        findPartyFromEmailAddressResp =
            dispatcher.runSync(
                "findPartyFromEmailAddress",
                UtilMisc.<String, Object>toMap(
                    "address", emailAddress, "caseInsensitive", "Y", "userLogin", userLogin));

        if (findPartyFromEmailAddressResp.get("partyId") == null) {
          String name =
              emailAddress.substring(
                  0,
                  emailAddress.contains("@")
                      ? emailAddress.lastIndexOf("@")
                      : emailAddress.length() - 1);
          if (UtilValidate.isNotEmpty(name)) {
            if (name.contains(".")) {
              firstName = name.substring(0, name.lastIndexOf("."));
              lastName = name.substring(name.lastIndexOf(".") + 1);
            } else {
              firstName = name;
            }
          }
          Map<String, Object> createPersonCtx = FastMap.newInstance();
          createPersonCtx.put("userLogin", userLogin);
          if (UtilValidate.isNotEmpty(firstName)) {
            createPersonCtx.put("firstName", firstName);
          }
          if (UtilValidate.isNotEmpty(lastName)) {
            createPersonCtx.put("lastName", lastName);
          }
          Map<String, Object> createPersonResponse =
              dispatcher.runSync("createPerson", createPersonCtx);
          if (!ServiceUtil.isSuccess(createPersonResponse)) {
            Debug.logError(
                "An Error occurred while createPerson service call: "
                    + ServiceUtil.getErrorMessage(createPersonResponse),
                module);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonResponse));
          }
          String partyId = (String) createPersonResponse.get("partyId");
          if (UtilValidate.isNotEmpty(partyId)) {
            result.put("partyId", partyId);
            String contactMechTypeId = "EMAIL_ADDRESS";
            String contactMechId = delegator.getNextSeqId("ContactMech");
            if (UtilValidate.isNotEmpty(contactMechId)) {
              GenericValue tempContactMech =
                  delegator.makeValue(
                      "ContactMech",
                      UtilMisc.toMap(
                          "contactMechId",
                          contactMechId,
                          "contactMechTypeId",
                          contactMechTypeId,
                          "infoString",
                          emailAddress));
              toBeStored.add(tempContactMech);
              GenericValue tempPartyContactMech =
                  delegator.makeValue(
                      "PartyContactMech",
                      UtilMisc.toMap(
                          "partyId",
                          partyId,
                          "contactMechId",
                          contactMechId,
                          "fromDate",
                          UtilDateTime.nowTimestamp()));
              toBeStored.add(tempPartyContactMech);
            }
            delegator.storeAll(toBeStored);

            if (UtilValidate.isNotEmpty(contactMechId)) {
              result.put("contactMechId", contactMechId);
              result.put("infoString", emailAddress);
            }
          }

          Map<String, Object> createPartyClassificationCtx = FastMap.newInstance();
          createPartyClassificationCtx.put("partyId", partyId);
          createPartyClassificationCtx.put("partyClassificationGroupId", "UNCLASSIFIED_CONTACT");
          createPartyClassificationCtx.put("userLogin", userLogin);

          Map createPartyClassificationResponse =
              dispatcher.runSync("createPartyClassification", createPartyClassificationCtx);
        }
      } catch (GenericServiceException | GenericEntityException e) {
        Debug.logError(
            "An Exception occurred while calling the createPartyIdFromEmailAddress service:"
                + e.getMessage(),
            module);
        return ServiceUtil.returnError(e.getMessage());
      }
    }
    return result;
  }

  public static Map<String, Object> getAllPartyDetails(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();

    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    Map<String, Object> partyDetails = FastMap.newInstance();
    String partyId = (String) context.get("partyId");
    Map<String, Object> getAllPartyDetailsCtx = new HashMap<>();
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    getAllPartyDetailsCtx.put("partyId", partyId);
    getAllPartyDetailsCtx.put("userLogin", userLogin);

    Map getCustomerResponse = null;

    try {

      getCustomerResponse = dispatcher.runSync("fsdGetPartyDetails", getAllPartyDetailsCtx);
      Map fsdGetPartyDetails = (Map) getCustomerResponse.get("partyDetails");
      partyDetails.put("partyObj", fsdGetPartyDetails.get("partyObj"));
      partyDetails.put("partyId", fsdGetPartyDetails.get("partyId"));
      partyDetails.put("partyName", fsdGetPartyDetails.get("partyName"));
      partyDetails.put("postalAddresses", fsdGetPartyDetails.get("postalAddresses"));
      partyDetails.put("phoneNumbers", fsdGetPartyDetails.get("phoneNumbers"));
      partyDetails.put("emailAddress", fsdGetPartyDetails.get("emailAddress"));
      partyDetails.put("webAddress", fsdGetPartyDetails.get("webAddress"));
      partyDetails.put("firstName", fsdGetPartyDetails.get("firstName"));
      partyDetails.put("middleName", fsdGetPartyDetails.get("middleName"));
      partyDetails.put("lastName", fsdGetPartyDetails.get("lastName"));
      partyDetails.put("displayName", fsdGetPartyDetails.get("displayName"));

//      Map<String, Object> generatePublicResourceUrlResp =
//          dispatcher.runSync(
//              "generatePublicResourceUrl",
//              UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));

//      if (UtilValidate.isNotEmpty(generatePublicResourceUrlResp)) {
//        String publicResourceUrl = (String) generatePublicResourceUrlResp.get("publicResourceUrl");
//        String thumbNailUrl = (String) generatePublicResourceUrlResp.get("thumbNailUrl");
//        partyDetails.put("publicResourceUrl", publicResourceUrl);
//        partyDetails.put("thumbNailUrl", thumbNailUrl);
//      }

      GenericValue partyRelationshipRecord = null;
      try {
        List<EntityExpr> conds = new LinkedList<>();
        conds.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));
        conds.add(
            EntityCondition.makeCondition(
                EntityCondition.makeCondition(
                    "partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT"),
                EntityOperator.OR,
                EntityCondition.makeCondition(
                    "partyRelationshipTypeId", EntityOperator.EQUALS, "OWNER")));
        List<EntityExpr> exprs = UtilMisc.toList(conds);

        partyRelationshipRecord =
            EntityQuery.use(delegator)
                .select("partyIdFrom")
                .from("PartyRelationship")
                .where(exprs)
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyRelationshipRecord)) {
          partyDetails.put("organizationPartyId", partyRelationshipRecord.getString("partyIdFrom"));
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, "Error finding PartyRelationship", module);
      }

      // Fetch Party Attributes
      List<GenericValue> partyAttributes =
          EntityQuery.use(delegator).from("PartyAttribute").where("partyId", partyId).queryList();
      if (UtilValidate.isNotEmpty(partyAttributes)) partyDetails.put("attributes", partyAttributes);
    } catch (Exception e) {
      Debug.logError(e, e.getMessage(), module);
    }
    result.put("partyDetails", partyDetails);

    return result;
  }

  /**
   * * Get list of parties who has given role
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> extGetAllPartiesHavingRole(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    List<Map<String, Object>> partyList = FastList.newInstance();

    Map<String, Object> inputMap = FastMap.newInstance();
    inputMap.put("userLogin", context.get("userLogin"));
    inputMap.put("partyId", context.get("partyId"));
    inputMap.put("partyName", context.get("partyName"));
    inputMap.put("partyCity", context.get("partyCity"));
    inputMap.put("partyStateProvinceGeoId", context.get("partyStateProvinceGeoId"));
    inputMap.put("partyCountryGeoId", context.get("partyCountryGeoId"));
    inputMap.put("partyPostalCode", context.get("partyPostalCode"));
    inputMap.put("selectedRoleType", context.get("roleTypeId"));
    inputMap.put("viewSize", context.get("viewSize"));

    Map<String, Object> serviceResult = null;
    try {
      serviceResult = dispatcher.runSync("searchParties", inputMap);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    partyList = (List<Map<String, Object>>) serviceResult.get("searchResults");
    List phoneNumbers = FastList.newInstance();
    List emailAddress = FastList.newInstance();
    List postalAddresses = FastList.newInstance();
    List<Map<String, Object>> xpartyList = FastList.newInstance();

    for (Map party : partyList) {
      Map<String, Object> partyMap = FastMap.newInstance();
      String partyId = (String) party.get("partyId");
      String groupName = AxPartyHelper.getPartyName(delegator, partyId);
      if (UtilValidate.isNotEmpty(groupName)) {
        partyMap.put("partyId", partyId);
        partyMap.put("partyName", groupName);
      }
      GenericValue partyRcd = HierarchyUtils.getPartyByPartyId(delegator, partyId);

      // fetch postal Addresses
      try {
        postalAddresses = AxPartyHelper.getPostalAddresses(delegator, partyRcd);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      partyMap.put("postalAddresses", postalAddresses);

      // fetch phone numbers
      try {
        phoneNumbers = AxPartyHelper.getPartyContactNumbers(delegator, partyRcd);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      partyMap.put("phoneNumbers", phoneNumbers);

      // fetch email addresses
      try {
        emailAddress = AxPartyHelper.getEmailAddresses(partyRcd);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      partyMap.put("emailAddress", emailAddress);

      // fetch web addresses
      List<GenericValue> webAddress = null;
      try {
        webAddress = AxPartyHelper.getPartyWebAddresses(delegator, partyRcd);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      partyMap.put("webAddress", webAddress);

      // fetch parent party Id
      /*Map<String, Object> ex = populateParentPartyDetails(delegator, partyMap, partyId);
      if (ex != null) return ex;*/

      xpartyList.add(partyMap);
    }

    result.put("partyList", xpartyList);
    return result;
  }

  /**
   * Fetch the Postal Address details.
   *
   * @param context
   * @return
   */
  public static Map<String, Object> fetchPartyPostalAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyId = (String) context.get("partyId");
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    try {
      GenericValue postalAddress =
          EntityQuery.use(delegator)
              .from("PostalAddress")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(postalAddress)) {
        Map<String, Object> postalAddressMap = new HashMap<>();
        postalAddressMap.put("contactMechId", postalAddress.getString("contactMechId"));
        postalAddressMap.put("toName", postalAddress.getString("toName"));
        postalAddressMap.put("attnName", postalAddress.getString("attnName"));
        postalAddressMap.put("address1", postalAddress.getString("address1"));
        postalAddressMap.put("address2", postalAddress.getString("address2"));
        postalAddressMap.put("city", postalAddress.getString("city"));
        postalAddressMap.put("stateProvinceGeoId", postalAddress.getString("stateProvinceGeoId"));
        postalAddressMap.put("postalCode", postalAddress.getString("postalCode"));
        postalAddressMap.put("countryGeoId", postalAddress.getString("countryGeoId"));
        postalAddressMap.put("directions", postalAddress.getString("directions"));
        postalAddressMap.put("latitude", postalAddress.getDouble("latitude"));
        postalAddressMap.put("longitude", postalAddress.getDouble("longitude"));

        // check if details are enriched
        injectEnrichedPostalAdress(dispatcher, userLogin, postalAddress, postalAddressMap);

        GenericValue partyContactMechPurpose =
            EntityQuery.use(delegator)
                .from("PartyContactMechPurpose")
                .where(
                    "partyId", partyId, "contactMechId", postalAddress.getString("contactMechId"))
                .filterByDate()
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
          String contactMechPurposeTypeId =
              partyContactMechPurpose.getString("contactMechPurposeTypeId");
          postalAddressMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
          if ("OTHER_ADDRESS".equalsIgnoreCase(contactMechPurposeTypeId)) {
            if (UtilValidate.isNotEmpty(partyContactMechPurpose.get("label")))
              postalAddressMap.put("label", partyContactMechPurpose.get("label"));
          } else {
            GenericValue contactMechPurposeType =
                EntityQuery.use(delegator)
                    .from("ContactMechPurposeType")
                    .where("contactMechPurposeTypeId", contactMechPurposeTypeId)
                    .queryFirst();
            if (UtilValidate.isNotEmpty(contactMechPurposeType)
                && UtilValidate.isNotEmpty(contactMechPurposeType.get("description"))) {
              postalAddressMap.put("label", contactMechPurposeType.get("description"));
            }
          }
        }
        result.put("addressDetails", postalAddressMap);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  private static void injectEnrichedPostalAdress(
      LocalDispatcher dispatcher,
      GenericValue userLogin,
      GenericValue postalAddress,
      Map postalAddressMap) {
    // check if details are enriched
    if (UtilValidate.isEmpty(postalAddress.get("latitude"))
        || UtilValidate.isEmpty(postalAddress.get("longitude"))) {
      Map<String, Object> enrichGeoDetailsForPostalAddressResp = null;
      try {
        enrichGeoDetailsForPostalAddressResp =
            dispatcher.runSync(
                "enrichGeoDetailsForPostalAddress",
                UtilMisc.toMap(
                    "contactMechId", postalAddress.get("contactMechId"), "userLogin", userLogin));
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
      }
      if (ServiceUtil.isError(enrichGeoDetailsForPostalAddressResp)) {
        String serviceError = ServiceUtil.getErrorMessage(enrichGeoDetailsForPostalAddressResp);
        Debug.logError(
            "An error occurred while enrichGeoDetailsForPostalAddress, details: " + serviceError,
            module);
        return;
      }
      postalAddress =
          (GenericValue) enrichGeoDetailsForPostalAddressResp.get("updatedPostalAddress");
    }
    postalAddressMap.put("formattedAddress", postalAddress.get("formattedAddress"));
    postalAddressMap.put("googleUrl", postalAddress.get("googleUrl"));
    postalAddressMap.put("staticMapUrl", postalAddress.get("staticMapUrl"));
    Double destinationLat = postalAddress.getDouble("latitude");
    postalAddressMap.put("latitude", destinationLat);
    Double destinationLong = postalAddress.getDouble("longitude");
    postalAddressMap.put("longitude", destinationLong);
    String timeZoneId = postalAddress.getString("timeZoneId");
    postalAddressMap.put("timeZoneId", timeZoneId);
    if (UtilValidate.isNotEmpty(timeZoneId)) {
      postalAddressMap.put("timezone", GeoUtil.getTimeZoneObjectMapById(timeZoneId));
    }
  }

  public static Map<String, Object> fetchPartyTelecomNumber(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyId = (String) context.get("partyId");
    try {
      GenericValue telecomNumber =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(telecomNumber)) {
        Map phoneMap = FastMap.newInstance();
        String phoneContactMechId = telecomNumber.getString("contactMechId");
        String phoneCountryCode = telecomNumber.getString("countryCode");
        String phoneAreaCode = telecomNumber.getString("areaCode");
        String phoneContactNumber = telecomNumber.getString("contactNumber");
        phoneMap.put("contactMechId", phoneContactMechId);
        phoneMap.put("countryCode", phoneCountryCode);
        phoneMap.put("areaCode", phoneAreaCode);
        phoneMap.put("contactNumber", phoneContactNumber);

        GenericValue partyContactMech =
            EntityQuery.use(delegator)
                .select("extension")
                .from("PartyContactMech")
                .where("contactMechId", contactMechId, "partyId", partyId)
                .filterByDate()
                .queryOne();
        String phoneExtension = null;
        if (UtilValidate.isNotEmpty(partyContactMech)) {
          phoneExtension = partyContactMech.getString("extension");
          phoneMap.put("extension", phoneExtension);
        }
        String formattedPhoneNumber =
            AxPartyHelper.getFormattedPhoneNumber(
                delegator,
                phoneContactMechId,
                phoneCountryCode,
                phoneAreaCode,
                phoneContactNumber,
                phoneExtension);
        phoneMap.put("formattedPhoneNumber", formattedPhoneNumber);

        GenericValue partyContactMechPurpose =
            EntityQuery.use(delegator)
                .from("PartyContactMechPurpose")
                .where("partyId", partyId, "contactMechId", contactMechId)
                .filterByDate()
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
          String contactMechPurposeTypeId =
              partyContactMechPurpose.getString("contactMechPurposeTypeId");
          phoneMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
          if ("OTHER_PHONE".equalsIgnoreCase(contactMechPurposeTypeId)) {
            if (UtilValidate.isNotEmpty(partyContactMechPurpose.get("label")))
              phoneMap.put("label", partyContactMechPurpose.get("label"));
          } else {
            GenericValue contactMechPurposeType =
                EntityQuery.use(delegator)
                    .from("ContactMechPurposeType")
                    .where("contactMechPurposeTypeId", contactMechPurposeTypeId)
                    .queryFirst();
            if (UtilValidate.isNotEmpty(contactMechPurposeType)
                && UtilValidate.isNotEmpty(contactMechPurposeType.get("description"))) {
              phoneMap.put("label", contactMechPurposeType.get("description"));
            }
          }
        }
        result.put("phoneDetails", phoneMap);
      }
    } catch (GenericEntityException e) {
      Debug.logError(
          e,
          "An error occurred while trying to fetch the phone number of a party: # " + partyId,
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> fetchPartyEmail(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyId = (String) context.get("partyId");
    try {
      GenericValue emailAddress =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(emailAddress)) {
        Map emailDetailsMap = FastMap.newInstance();
        emailDetailsMap.put("contactMechId", emailAddress.getString("contactMechId"));
        emailDetailsMap.put("emailAddress", emailAddress.getString("infoString"));
        emailDetailsMap.put("createdStamp", emailAddress.getString("createdStamp"));
        emailDetailsMap.put("lastUpdatedStamp", emailAddress.getString("lastUpdatedStamp"));
        GenericValue partyContactMechPurpose =
            EntityQuery.use(delegator)
                .from("PartyContactMechPurpose")
                .where("partyId", partyId, "contactMechId", emailAddress.getString("contactMechId"))
                .filterByDate()
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
          String contactMechPurposeTypeId =
              partyContactMechPurpose.getString("contactMechPurposeTypeId");
          emailDetailsMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
          if ("OTHER_EMAIL".equalsIgnoreCase(contactMechPurposeTypeId)) {
            if (UtilValidate.isNotEmpty(partyContactMechPurpose.get("label")))
              emailDetailsMap.put("label", partyContactMechPurpose.get("label"));
          } else {
            GenericValue contactMechPurposeType =
                EntityQuery.use(delegator)
                    .from("ContactMechPurposeType")
                    .where("contactMechPurposeTypeId", contactMechPurposeTypeId)
                    .queryFirst();
            if (UtilValidate.isNotEmpty(contactMechPurposeType)
                && UtilValidate.isNotEmpty(contactMechPurposeType.get("description"))) {
              emailDetailsMap.put("label", contactMechPurposeType.get("description"));
            }
          }
        }
        result.put("emailDetails", emailDetailsMap);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> fetchPartyLinkedInAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyId = (String) context.get("partyId");
    try {
      GenericValue linkedInAddress =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(linkedInAddress)) {
        Map linkedInUrlDetailsMap = FastMap.newInstance();
        linkedInUrlDetailsMap.put("contactMechId", linkedInAddress.getString("contactMechId"));
        linkedInUrlDetailsMap.put("linkedInUrl", linkedInAddress.getString("infoString"));
        linkedInUrlDetailsMap.put("createdStamp", linkedInAddress.getString("createdStamp"));
        linkedInUrlDetailsMap.put(
            "lastUpdatedStamp", linkedInAddress.getString("lastUpdatedStamp"));
        result.put("linkedInDetails", linkedInUrlDetailsMap);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> fetchPartyTwitterOrFacebookInfo(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contactMechId = (String) context.get("contactMechId");
    String partyId = (String) context.get("partyId");
    try {
      GenericValue twitterUserName =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(twitterUserName)) {
        Map twitterDetailsMap = FastMap.newInstance();
        twitterDetailsMap.put("contactMechId", twitterUserName.getString("contactMechId"));
        twitterDetailsMap.put("infoString", twitterUserName.getString("infoString"));
        twitterDetailsMap.put("createdStamp", twitterUserName.getString("createdStamp"));
        twitterDetailsMap.put("lastUpdatedStamp", twitterUserName.getString("lastUpdatedStamp"));
        result.put("result", twitterDetailsMap);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  /**
   * Service to fetch the last contacted details for the party
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fetchPartyLastContacted(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    try {

      Map<String, Object> lastContactedDetails = new HashMap<>();

      // fetching last contacted details
      EntityCondition commEventCond =
          EntityCondition.makeCondition(
              EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId),
              EntityOperator.OR,
              EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId));

      GenericValue lastCommunicationEvent =
          EntityQuery.use(delegator)
              .from("CommunicationEvent")
              .where(commEventCond)
              .orderBy("-lastUpdatedStamp")
              .queryFirst();

      if (UtilValidate.isNotEmpty(lastCommunicationEvent)) {
        GenericValue commEvent =
            EntityQuery.use(delegator)
                .from("CommunicationEventType")
                .where(
                    "communicationEventTypeId",
                    lastCommunicationEvent.get("communicationEventTypeId"))
                .queryOne();

        if (UtilValidate.isNotEmpty(commEvent)) {
          String lastContactedVia = commEvent.getString("description");
          Timestamp lastContacted = lastCommunicationEvent.getTimestamp("lastUpdatedStamp");
          String partyIdTo = lastCommunicationEvent.getString("partyIdTo");
          String partyIdFrom = lastCommunicationEvent.getString("partyIdFrom");
          String lastContactedById;
          if (UtilValidate.isNotEmpty(partyIdTo) && partyId.equalsIgnoreCase(partyIdTo)) {
            lastContactedById = partyIdTo;
          } else {
            lastContactedById = partyIdFrom;
          }
          GenericValue lastContactedBy =
              HierarchyUtils.getPartyByPartyId(delegator, lastContactedById);

          lastContactedDetails.put("lastContactedVia", lastContactedVia);
          lastContactedDetails.put("lastContacted", lastContacted);
          if (UtilValidate.isNotEmpty(lastContactedBy)) {
            lastContactedDetails.put("lastContactedBy", lastContactedBy);
          }
        }
      }
      result.put("lastContactedDetails", lastContactedDetails);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> deletePartyAttribute(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String attrName = (String) context.get("attrName");
    String partyId = (String) context.get("partyId");
    try {
      EntityCondition makeCondition =
          EntityCondition.makeCondition(
              EntityCondition.makeCondition("partyId", partyId),
              EntityCondition.makeCondition(
                  EntityFunction.UPPER_FIELD("attrName"),
                  EntityOperator.EQUALS,
                  attrName.toUpperCase()));
      GenericValue partyAttribute =
          EntityQuery.use(delegator).from("PartyAttribute").where(makeCondition).queryFirst();
      if (UtilValidate.isNotEmpty(partyAttribute)) {
        partyAttribute.remove();
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }
}
