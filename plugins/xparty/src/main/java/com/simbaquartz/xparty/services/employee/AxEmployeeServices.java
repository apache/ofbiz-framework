package com.simbaquartz.xparty.services.employee;

import com.fidelissd.zcp.xcommon.util.AxPhoneNumberUtil;
import com.fidelissd.zcp.xcommon.util.AxUtilValidate;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyContactHelper;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** Created by Admin on 8/14/17. */
public class AxEmployeeServices {
  private static final String module = AxEmployeeServices.class.getName();
  public static final String ROLE_INTERNAL_ORG = "INTERNAL_ORGANIZATIO";
  public static final String TEAM = "TEAM";

  public static class CustomFieldErrorMessages {
    public static final String SYSTEM_ERROR_RESP = "Something went wrong, please try later.";
    public static final String ERROR_CREATE_RELATION =
        "Error while creating relationship for team and internal organization";
    public static final String ERROR_CREATE_ROLE = "Error while creating role for team";
    public static final String ERROR_CREATE_TEAM = "Error while creating team";
    public static final String ERROR_ADD_SECURITY_GROUP =
        "Error while adding user to security group";
    public static final String SYSTEM_ERROR =
        "Something went wrong, please check log for more details.";
  }

  public Map<String, Object> createEmployee(DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String companyId = (String) context.get("companyId");
    String phoneNumber = (String) context.get("phoneNumber");
    String email = (String) context.get("email");

    String firstName = (String) context.get("firstName");
    String lastName = (String) context.get("lastName");

    Map<String, Object> createEmployeeMap = new HashMap<>();
    createEmployeeMap.put("userLogin", userLogin);
    createEmployeeMap.put("companyId", companyId);
    createEmployeeMap.put("firstName", firstName);
    createEmployeeMap.put("lastName", lastName);
    createEmployeeMap.put("email", email);
    createEmployeeMap.put("phone", phoneNumber);

    Map<String, Object> createProductDefaultPriceResult = null;
    try {
      createProductDefaultPriceResult = dispatcher.runSync("createNewEmployee", createEmployeeMap);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
    }
    if (ServiceUtil.isError(createProductDefaultPriceResult)) {
      String errorMessage = "Unable to create employee, Please contact support";
      Debug.logError(errorMessage, module);
      return ServiceUtil.returnError(errorMessage);
    }
    String employeeId = (String) createProductDefaultPriceResult.get("employeeId");
    serviceResult.put("employeeId", employeeId);
    return serviceResult;
  }

  public Map<String, Object> updateEmployee(DispatchContext dctx, Map<String, Object> context)
      throws GenericEntityException, GenericServiceException {
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String companyId = (String) context.get("companyId");
    String employeeId = (String) context.get("employeeId");
    String phone = (String) context.get("phoneNumber");
    String email = (String) context.get("email");
    String firstName = (String) context.get("firstName");
    String lastName = (String) context.get("lastName");

    if (UtilValidate.isNotEmpty(firstName) || UtilValidate.isNotEmpty(lastName)) {
      Map personRecord = AxPartyHelper.getPersonDetails(delegator, employeeId);
      if (UtilValidate.isNotEmpty(personRecord)) {
        Map<String, Object> updateEmployeeCtx = new HashMap<>();
        updateEmployeeCtx.put("userLogin", userLogin);
        updateEmployeeCtx.put("partyId", employeeId);
        if (UtilValidate.isNotEmpty(firstName)) {
          updateEmployeeCtx.put("firstName", firstName);
        }
        if (UtilValidate.isNotEmpty(lastName)) {
          updateEmployeeCtx.put("lastName", lastName);
        }

        Map<String, Object> updateEmployeeServiceResponse = null;

        try {
          updateEmployeeServiceResponse = dispatcher.runSync("updatePerson", updateEmployeeCtx);
        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(updateEmployeeServiceResponse)) {
          return ServiceUtil.returnError(
              ServiceUtil.getErrorMessage(updateEmployeeServiceResponse));
        }
      }
    }

    if (UtilValidate.isNotEmpty(email)) {
      GenericValue employeePrimaryEmailContactMech =
          PartyContactHelper.getPartyPrimaryEmailContactMech(delegator, employeeId);
      if (UtilValidate.isNotEmpty(employeePrimaryEmailContactMech)) {
        Map<String, Object> updateEmailCtx = new HashMap<>();

        updateEmailCtx.put("userLogin", userLogin);
        updateEmailCtx.put("partyId", employeeId);
        updateEmailCtx.put("emailAddress", email);
        updateEmailCtx.put(
            "contactMechId", employeePrimaryEmailContactMech.getString("contactMechId"));

        Map<String, Object> updateEmailServiceResponse = null;
        try {
          updateEmailServiceResponse = dispatcher.runSync("axUpdateEmailAddress", updateEmailCtx);
        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(updateEmailServiceResponse)) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(updateEmailServiceResponse));
        }
      }
    }

    if (UtilValidate.isNotEmpty(phone)) {
      // validate phone
      if (!AxUtilValidate.isValidPhoneNumber(phone, delegator)) {
        return ServiceUtil.returnError(
            "Invalid phone number, please provide a valid 10 digit number and try again.");
      }

      GenericValue latestTelecomNumber =
          PartyWorker.findPartyLatestTelecomNumber(employeeId, delegator);
      if (UtilValidate.isNotEmpty(latestTelecomNumber)) {
        String phoneContactMechId = latestTelecomNumber.getString("contactMechId");

        // prepare area code and contact number
        GenericValue companyPrimaryAddress =
            PartyWorker.findPartyLatestPostalAddress(companyId, delegator);

        // get country code using company's address
        String countryRegionCode = "";
        countryRegionCode = AxPartyHelper.getPostalAddressCountryGeoCode(companyPrimaryAddress);
        Map phoneNumberInfo = AxPhoneNumberUtil.preparePhoneNumberInfo(phone, countryRegionCode);
        boolean isValidNumber = (boolean) phoneNumberInfo.get("isValidNumber");

        // checks if the number is a valid one for the country, if not return error
        if (!isValidNumber) {
          Debug.logWarning(
              "Not a valid phone number for country ["
                  + countryRegionCode
                  + "] please provide a valid phone number and try again.",
              module);
          return ServiceUtil.returnError(
              "Not a valid phone number for country ["
                  + countryRegionCode
                  + "] please provide a valid phone number and try again.");
        }

        Map<String, Object> updateContactCtx = new HashMap<>();
        String countryCode = "+" + (String) phoneNumberInfo.get("countryCode");
        String phoneAreaCode = (String) phoneNumberInfo.get("areaCode");
        String phoneContactNumber = (String) phoneNumberInfo.get("contactNumber");

        updateContactCtx.put("countryCode", countryCode);
        updateContactCtx.put("areaCode", phoneAreaCode);
        updateContactCtx.put("contactNumber", phoneContactNumber);
        updateContactCtx.put("userLogin", userLogin);
        updateContactCtx.put("contactMechId", phoneContactMechId);

        Map<String, Object> updateContactServiceResponse = null;
        try {
          updateContactServiceResponse =
              dispatcher.runSync("axUpdateTelecomNumber", updateContactCtx);
        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(updateContactServiceResponse)) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(updateContactServiceResponse));
        }
      }
    }
    return serviceResult;
  }

  public static Map<String, Object> getStoreEmployee(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String storeId = (String) context.get("storeId");
    String employeeId = (String) context.get("employeeId");
    Map employeeDocument = new HashMap<>();

    Map storeEmployee = AxPartyHelper.getPersonDetails(delegator, employeeId);
    if (UtilValidate.isNotEmpty(storeEmployee)) {
      employeeDocument.put("employeeId", storeEmployee.get("partyId"));
      employeeDocument.put("firstName", storeEmployee.get("firstName"));
      employeeDocument.put("lastName", storeEmployee.get("lastName"));
      employeeDocument.put("createdStamp", storeEmployee.get("createdStamp"));
      employeeDocument.put("lastModifiedDate", storeEmployee.get("lastUpdatedStamp"));
    }
    Map customerPrimaryContactDetails =
        AxPartyHelper.getPartyPrimaryContactDetails(
            delegator, HierarchyUtils.getPartyByPartyId(delegator, employeeId));
    // phone
    String phone = (String) customerPrimaryContactDetails.get("phone");
    employeeDocument.put("phone", phone);

    // email
    String email = (String) customerPrimaryContactDetails.get("email");
    employeeDocument.put("email", email);

    serviceResult.put("employeeDetails", employeeDocument);
    return serviceResult;
  }

  public static Map<String, Object> createTeam(DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    String teamName = (String) context.get("teamName");
    String createdBy = (String) context.get("createdBy");
    String partyGroupId = (String) context.get("partyGroupId");
    Map createTeamMap = new HashMap<>();
    createTeamMap.put("groupName", teamName);
    createTeamMap.put("userLogin", userLogin);
    Map<String, Object> createTeamResp;
    String teamPartyId;
    try {
      createTeamResp = dispatcher.runSync("createPartyGroup", createTeamMap);
      if (ServiceUtil.isError(createTeamResp)) {
        Debug.logError(
            CustomFieldErrorMessages.ERROR_CREATE_TEAM,
            ServiceUtil.getErrorMessage(createTeamResp),
            module);
        return ServiceUtil.returnError(CustomFieldErrorMessages.SYSTEM_ERROR_RESP);
      }
      teamPartyId = (String) createTeamResp.get("partyId");
      Map<String, Object> createPartyRoleCtx = new HashMap<String, Object>();
      createPartyRoleCtx.put("userLogin", userLogin);
      createPartyRoleCtx.put("partyId", teamPartyId);
      createPartyRoleCtx.put("roleTypeId", TEAM);
      Map<String, Object> createRoleResult = null;
      createRoleResult = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
      if (ServiceUtil.isError(createRoleResult)) {
        Debug.logError(
            CustomFieldErrorMessages.ERROR_CREATE_ROLE,
            ServiceUtil.getErrorMessage(createRoleResult),
            module);
        return ServiceUtil.returnError(CustomFieldErrorMessages.SYSTEM_ERROR_RESP);
      }

      // create the relationship of team and account party group
      Map<String, Object> createPartyRelationshipCtx = new HashMap<String, Object>();
      createPartyRelationshipCtx.put("partyIdFrom", partyGroupId);
      createPartyRelationshipCtx.put("partyIdTo", teamPartyId);
      createPartyRelationshipCtx.put("roleTypeIdFrom", ROLE_INTERNAL_ORG);
      createPartyRelationshipCtx.put("roleTypeIdTo", TEAM);
      createPartyRelationshipCtx.put("partyRelationshipTypeId", TEAM);
      createPartyRelationshipCtx.put("userLogin", userLogin);

      Map<String, Object> createRelationshipResult = null;
      createRelationshipResult =
          dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
      if (ServiceUtil.isError(createRelationshipResult)) {
        Debug.logError(
            CustomFieldErrorMessages.ERROR_CREATE_RELATION,
            ServiceUtil.getErrorMessage(createRelationshipResult),
            module);
        return ServiceUtil.returnError(CustomFieldErrorMessages.SYSTEM_ERROR_RESP);
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, "An error occurred while creating team", module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("teamPartyId", teamPartyId);
    return serviceResult;
  }

  public static Map<String, Object> getTeams(DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    Delegator delegator = dctx.getDelegator();
    String partyGroupId = (String) context.get("partyGroupId");
    Map createTeamMap = new HashMap<>();
    createTeamMap.put("userLogin", userLogin);

    try {
      List<EntityExpr> conds = new LinkedList<>();
      conds.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyGroupId));
      conds.add(
          EntityCondition.makeCondition(
              "roleTypeIdFrom", EntityOperator.EQUALS, ROLE_INTERNAL_ORG));
      conds.add(EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, TEAM));
      conds.add(
          EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, TEAM));

      List<GenericValue> teams =
          EntityQuery.use(delegator).from("PartyRelationship").where(conds).queryList();
      List<Map> teamsList = new LinkedList<>();
      if (UtilValidate.isNotEmpty(teams)) {
        for (GenericValue team : teams) {
          String teamPartyId = team.getString("partyIdTo");
          GenericValue teamData =
              EntityQuery.use(delegator)
                  .from("PartyGroup")
                  .where("partyId", teamPartyId)
                  .queryOne();
          teamsList.add(
              UtilMisc.toMap("partyId", teamPartyId, "teamName", teamData.get("groupName")));
        }
      }

      serviceResult.put("teams", teamsList);
    } catch (GenericEntityException e) {
      Debug.logError(e, "An error occurred while creating team", module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * Returns the position title for a party, expects an option employerPartyId, if provided uses the
   * relationship between two to figure out the positionTitle.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> getPartyPositionTitle(
      DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");

    GenericValue partyAttribute =
        EntityQuery.use(delegator)
            .from("PartyAttribute")
            .where(UtilMisc.toMap("partyId", partyId, "attrName", "Designation"))
            .queryOne();
    if (UtilValidate.isNotEmpty(partyAttribute)) {
      serviceResult.put("designation", partyAttribute.getString("attrValue"));
    }
    return serviceResult;
  }

  public static Map<String, Object> fsdGetAllDesignation(
      DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
    Delegator delegator = dctx.getDelegator();
    List<Map<String, Object>> allDesignation = FastList.newInstance();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    try {
      List<GenericValue> customerDesignationList =
          EntityQuery.use(delegator)
              .select("attrValue")
              .from("PartyAttribute")
              .where(UtilMisc.toMap("attrName", "Designation"))
              .distinct()
              .queryList();

      for (GenericValue customerDesignation : customerDesignationList) {
        Map<String, Object> designation = FastMap.newInstance();
        String attrValue = customerDesignation.getString("attrValue");
        if (UtilValidate.isNotEmpty(attrValue)) {
          designation.put("name", attrValue);
          allDesignation.add(designation);
        }
      }
      serviceResult.put("allDesignation", allDesignation);
      serviceResult.put("count", allDesignation.size());
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fsdSetPartyDesignationService(
      DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String designation = (String) context.get("designation");

    GenericValue partyAttribute =
        EntityQuery.use(delegator)
            .from("PartyAttribute")
            .where(UtilMisc.toMap("partyId", partyId, "attrName", "Designation"))
            .queryOne();

    if (UtilValidate.isEmpty(partyAttribute)) {
      Map<String, Object> createPartyAttributeCtx = FastMap.newInstance();
      createPartyAttributeCtx.put("userLogin", userLogin);
      createPartyAttributeCtx.put("partyId", partyId);
      createPartyAttributeCtx.put("attrName", "Designation");
      createPartyAttributeCtx.put("attrValue", designation);

      Map<String, Object> createPartyAttributeResponse;
      try {
        createPartyAttributeResponse =
            dispatcher.runSync("createPartyAttribute", createPartyAttributeCtx);
        if (!ServiceUtil.isSuccess(createPartyAttributeResponse)) {
          return createPartyAttributeResponse;
        }
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }

      return ServiceUtil.returnSuccess();
    } else {
      Map<String, Object> updatePartyAttributeCtx = FastMap.newInstance();
      updatePartyAttributeCtx.put("userLogin", userLogin);
      updatePartyAttributeCtx.put("partyId", partyId);
      updatePartyAttributeCtx.put("attrName", "Designation");
      updatePartyAttributeCtx.put("attrValue", designation);

      Map<String, Object> updatePartyAttributeResponse;
      try {
        updatePartyAttributeResponse =
            dispatcher.runSync("updatePartyAttribute", updatePartyAttributeCtx);
        if (!ServiceUtil.isSuccess(updatePartyAttributeResponse)) {
          return updatePartyAttributeResponse;
        }
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }

      return ServiceUtil.returnSuccess();
    }
  }

  public static Map<String, Object> fsdSetPartyDepartmentService(
      DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String department = (String) context.get("department");

    Map<String, Object> createPartyAttributeCtx = FastMap.newInstance();
    createPartyAttributeCtx.put("userLogin", userLogin);
    createPartyAttributeCtx.put("partyId", partyId);
    createPartyAttributeCtx.put("attrName", "Department");
    createPartyAttributeCtx.put("attrValue", department);

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

    return ServiceUtil.returnSuccess();
  }

  public static Map<String, Object> fsdRemovePartyDesignationService(
      DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String Designation = (String) context.get("designation");

    GenericValue partyAttribute =
        EntityQuery.use(delegator)
            .from("PartyAttribute")
            .where(UtilMisc.toMap("partyId", partyId, "attrName", "Designation"))
            .queryOne();

    if (UtilValidate.isNotEmpty(partyAttribute)) {
      partyAttribute.remove();
      return ServiceUtil.returnSuccess("Party Designation Removed");
    } else {
      return ServiceUtil.returnError("Couldn't remove Party Designation");
    }
  }
}
