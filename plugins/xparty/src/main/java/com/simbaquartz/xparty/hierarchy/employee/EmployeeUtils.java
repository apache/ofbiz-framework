package com.simbaquartz.xparty.hierarchy.employee;


import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.role.CustomerPersonRoles;
import com.simbaquartz.xparty.hierarchy.role.EmployerPersonRoles;
import com.simbaquartz.xparty.hierarchy.role.HierarchyRoleUtils;
import com.simbaquartz.xparty.hierarchy.role.InternalPersonRoles;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.fidelissd.zcp.xcommon.collections.FastList;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class EmployeeUtils {
  public static final String module = EmployeeUtils.class.getName();
  public static final String resource = "HierarchyUiLabels";
  public static final String resource_error = "HierarchyErrorUiLabels";

  public static Map<String, Object> createNewStaff(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    String partyId = (String) context.get("partyId");
    String primaryEmailAddress = (String) context.get("primaryEmailAddress");
    String firstName = (String) context.get("firstName");
    String lastName = (String) context.get("lastName");
    String selectedRoleId = (String) context.get("role");

    String middleName = (String) context.get("middleName");
    String nickName = (String) context.get("nickname");
    if (UtilValidate.isNotEmpty(middleName))
      middleName = middleName.substring(0, 1).toUpperCase() + middleName.substring(1);
    String suffix = (String) context.get("suffix");
    String addToMyContactsFlag = (String) context.get("addToMyContactsFlag");
    String personalTitle = (String) context.get("personalTitle");
    String partyRegion = (String) context.get("primaryPartyRegion");
    String primaryPhoneCountryCode = (String) context.get("primaryPhoneCountryCode");
    String primaryPhoneAreaCode = (String) context.get("primaryPhoneAreaCode");
    String primaryPhoneContactNumber = (String) context.get("primaryPhoneContactNumber");
    String primaryPhoneExtension = (String) context.get("primaryPhoneExtension");
    String primaryWebAddress = (String) context.get("primaryWebAddress");
    String primaryFaxCountryCode = (String) context.get("primaryFaxCountryCode");
    String primaryFaxAreaCode = (String) context.get("primaryFaxAreaCode");
    String primaryFaxContactNumber = (String) context.get("primaryFaxContactNumber");
    String primaryFaxExtension = (String) context.get("primaryFaxExtension");
    String accountLeadPartyId = (String) context.get("accountLeadPartyId");
    String addToAccountLeadsContactsFlag = (String) context.get("addToAccountLeadsContactsFlag");
    String chooseSupplierOrPartner = (String) context.get("chooseSupplierOrPartner");
    String managerPartyId = (String) context.get("managerPartyId");
    String supplierPartyId = (String) context.get("supplierPartyId");
    String partnerPartyId = (String) context.get("partnerPartyId");
    String designation = (String) context.get("designation");
    String orgPartyId = (String) context.get("orgPartyId");
    List<Map<String, Object>> emails = UtilGenerics.toList(context.get("emails"));
    List<Map<String, Object>> phones = UtilGenerics.toList(context.get("phones"));

    try {

      firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
      lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1);

      // create person
      Map createPersonCtx =
          UtilMisc.toMap(
              "userLogin",
              userLogin,
              "personalTitle",
              personalTitle,
              "firstName",
              firstName,
              "middleName",
              middleName,
              "lastName",
              lastName,
              "nickname",
              nickName,
              "suffix",
              suffix);
      Map<String, Object> createPersonResult = null;
      try {
        createPersonResult = dispatcher.runSync("createPerson", createPersonCtx);
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonResult));
      }
      String supplierContactPartyId = (String) createPersonResult.get("partyId");

      String enumRoleTypeId = null, partyRelationshipTypeId = null;
      if (selectedRoleId != null) {
        EmployerPersonRoles role = EmployerPersonRoles.valueOf(selectedRoleId);
        enumRoleTypeId = role.name();
        partyRelationshipTypeId = role.getPartyRelationshipTypeId();
        boolean isSuccess = addRole(dctx, selectedRoleId, supplierContactPartyId, userLogin);
        if (!isSuccess) {
          return ServiceUtil.returnError("Unable to add role");
        }

        GenericValue supplierParty =
            HierarchyUtils.getPartyByPartyId(dctx.getDelegator(), supplierContactPartyId);
        // add default role type
        if (!HierarchyUtils.checkPartyRole(supplierParty, "SALES_REP")) {
          boolean hasRoleAdded = addRole(dctx, "SALES_REP", supplierContactPartyId, userLogin);
          if (!hasRoleAdded) {
            return ServiceUtil.returnError("Unable to add role");
          }
        }
      }

      String supplierOrPartnerPartyId = null;
      if (chooseSupplierOrPartner.equals("SUPPLIER")) {
        supplierOrPartnerPartyId = supplierPartyId;
      } else {
        supplierOrPartnerPartyId = partnerPartyId;
      }

      Map createSupplierPartnerContactCtx =
          UtilMisc.toMap(
              "userLogin",
              userLogin,
              "partyIdFrom",
              supplierOrPartnerPartyId,
              "partyIdTo",
              supplierContactPartyId,
              "roleTypeIdTo",
              enumRoleTypeId,
              "partyRelationshipTypeId",
              partyRelationshipTypeId);
      Map<String, Object> createSupplierPartnerContactRelResult = null;
      try {
        createSupplierPartnerContactRelResult =
            dispatcher.runSync("createPartyRelationship", createSupplierPartnerContactCtx);
      } catch (GenericServiceException e) {
        return ServiceUtil.returnError(
            ServiceUtil.getErrorMessage(createSupplierPartnerContactRelResult));
      }

      if ((UtilValidate.isNotEmpty(addToMyContactsFlag) && (addToMyContactsFlag == "Y"))
          || (UtilValidate.isNotEmpty(addToAccountLeadsContactsFlag)
              && (addToAccountLeadsContactsFlag == "Y")
              && UtilValidate.isNotEmpty(accountLeadPartyId))) {
        if (!enumRoleTypeId.equals(EmployerPersonRoles.CONTACT.name())) {
          // CONTACT role needs to be added.
          boolean isSuccess =
              addRole(dctx, EmployerPersonRoles.CONTACT.name(), supplierContactPartyId, userLogin);
          if (!isSuccess) {
            return ServiceUtil.returnError("Unable to add role");
          }
        }
      }

      // check if newly created staff to be added as contact for logged in person
      if (UtilValidate.isNotEmpty(addToMyContactsFlag) && (addToMyContactsFlag == "Y")) {
        if (!HierarchyUtils.checkPartyRole(userLogin, "_NA_")) {
          boolean isSuccess = addRole(dctx, "_NA_", userLogin.getString("partyId"), userLogin);
          if (!isSuccess) {
            return ServiceUtil.returnError("Unable to add role");
          }
        }

        Map createSalesRepMap =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyIdFrom",
                userLogin.getString("partyId"),
                "partyIdTo",
                supplierContactPartyId,
                "roleTypeIdTo",
                EmployerPersonRoles.CONTACT.name(),
                "partyRelationshipTypeId",
                EmployerPersonRoles.CONTACT.getPartyRelationshipTypeId());
        Map<String, Object> createSalesRepRelResult = null;
        try {
          createSalesRepRelResult =
              dispatcher.runSync("createPartyRelationship", createSalesRepMap);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createSalesRepRelResult));
        }
      }
      // Check if relationship needs to be established with Accounts_lead
      if (UtilValidate.isNotEmpty(addToAccountLeadsContactsFlag)
          && (addToAccountLeadsContactsFlag == "Y")
          && UtilValidate.isNotEmpty(accountLeadPartyId)) {
        Map createAccountLeadRelMap =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyIdFrom",
                accountLeadPartyId,
                "partyIdTo",
                supplierContactPartyId,
                "roleTypeIdFrom",
                "ACCOUNT_LEAD",
                "roleTypeIdTo",
                "CONTACT",
                "partyRelationshipTypeId",
                "ACCOUNT");
        Map<String, Object> createAccountLeadRelResult = null;
        try {
          createAccountLeadRelResult =
              dispatcher.runSync("createPartyRelationship", createAccountLeadRelMap);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountLeadRelResult));
        }
      }

      if (UtilValidate.isNotEmpty(phones)) {
        for (Map phone : phones) {
          Map createContactPhoneCtx =
              UtilMisc.toMap(
                  "userLogin",
                  userLogin,
                  "partyId",
                  supplierContactPartyId,
                  "countryCode",
                  phone.get("countryCode"),
                  "areaCode",
                  phone.get("areaCode"),
                  "contactNumber",
                  phone.get("phone"),
                  "extension",
                  phone.get("extension"));
          Map<String, Object> createContactPhoneResult = null;
          try {
            createContactPhoneResult =
                dispatcher.runSync("createPartyTelecomNumber", createContactPhoneCtx);
          } catch (GenericServiceException e) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactPhoneResult));
          }
        }
      }

      // check for contact information and add it
      // create primary phone number
      if (UtilValidate.isNotEmpty(primaryPhoneContactNumber)) {

        Map createContactPhoneCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "countryCode",
                primaryPhoneCountryCode,
                "areaCode",
                primaryPhoneAreaCode,
                "contactNumber",
                primaryPhoneContactNumber,
                "extension",
                primaryPhoneExtension,
                "contactMechPurposeTypeId",
                "PRIMARY_PHONE");
        Map<String, Object> createContactPhoneResult = null;
        try {
          createContactPhoneResult =
              dispatcher.runSync("createPartyTelecomNumber", createContactPhoneCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactPhoneResult));
        }
        String contactMechId = (String) createContactPhoneResult.get("contactMechId");
        if (UtilValidate.isNotEmpty(partyRegion)) {
          Map createContactPhoneRegionCtx =
              UtilMisc.toMap(
                  "userLogin",
                  userLogin,
                  "contactMechId",
                  contactMechId,
                  "partyRegion",
                  partyRegion);
          Map<String, Object> createContactPhoneRegionResult = null;
          try {
            createContactPhoneRegionResult =
                dispatcher.runSync(
                    "createTelecomNumberRegionForParty", createContactPhoneRegionCtx);
          } catch (GenericServiceException e) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createContactPhoneRegionResult));
          }
        }
      }

      if (UtilValidate.isNotEmpty(emails)) {
        for (Map email : emails) {
          Map createContactEmailCtx =
              UtilMisc.toMap(
                  "userLogin",
                  userLogin,
                  "partyId",
                  supplierContactPartyId,
                  "emailAddress",
                  ((String) email.get("email")).toLowerCase(),
                  "contactMechPurposeTypeId",
                  email.get("contactMechPurposeTypeId"));
          Map<String, Object> createContactEmailResult = null;
          try {
            createContactEmailResult =
                dispatcher.runSync("createPartyEmailAddress", createContactEmailCtx);
          } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactEmailResult));
          }
        }
      }

      if (UtilValidate.isNotEmpty(primaryEmailAddress)) {
        primaryEmailAddress = primaryEmailAddress.toLowerCase();
        Map createContactEmailCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "emailAddress",
                primaryEmailAddress);
        Map<String, Object> createContactEmailResult = null;
        try {
          createContactEmailResult =
              dispatcher.runSync("createPartyEmailAddress", createContactEmailCtx);
        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactEmailResult));
        }
        // Create ContactMechPurpose for Email

        Map createContactMechCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "contactMechId",
                createContactEmailResult.get("contactMechId"),
                "contactMechPurposeTypeId",
                EmailTypesEnum.PRIMARY.getTypeId());
        Map<String, Object> emailContactMechPurposeResp = null;
        try {
          emailContactMechPurposeResp =
              dispatcher.runSync("createPartyContactMechPurpose", createContactMechCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(emailContactMechPurposeResp));
        }
      }

      // create web address if not empty
      if (UtilValidate.isNotEmpty(primaryWebAddress)) {
        Map createContactWebCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "infoString",
                primaryWebAddress,
                "contactMechTypeId",
                "WEB_ADDRESS");
        Map<String, Object> createContactWebResult = null;
        try {
          createContactWebResult =
              dispatcher.runSync("createPartyContactMech", createContactWebCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactWebResult));
        }

        // Create ContactMechPurpose for Web-Address
        Map websiteContactMechPurposeCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "contactMechId",
                createContactWebResult.get("contactMechId"),
                "contactMechPurposeTypeId",
                "PRIMARY_WEB_URL");
        Map<String, Object> websiteContactMechPurposeResp = null;
        try {
          websiteContactMechPurposeResp =
              dispatcher.runSync("createPartyContactMechPurpose", websiteContactMechPurposeCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(
              ServiceUtil.getErrorMessage(websiteContactMechPurposeResp));
        }
      }

      // create fax phone number
      if (UtilValidate.isNotEmpty(primaryFaxContactNumber)) {
        Map createContactFaxCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyId",
                supplierContactPartyId,
                "countryCode",
                primaryFaxCountryCode,
                "areaCode",
                primaryFaxAreaCode,
                "contactNumber",
                primaryFaxContactNumber,
                "extension",
                primaryFaxExtension,
                "contactMechPurposeTypeId",
                "FAX_NUMBER");
        Map<String, Object> createContactFaxResult = null;
        try {
          createContactFaxResult =
              dispatcher.runSync("createPartyTelecomNumber", createContactFaxCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactFaxResult));
        }
      }
      serviceResult.put("partyId", supplierContactPartyId);

      // add manager to reporting staff relationship if manager exists
      if (UtilValidate.isNotEmpty(managerPartyId)) {
        Map createReporteeRelationCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyIdFrom",
                managerPartyId,
                "partyIdTo",
                supplierContactPartyId,
                "roleTypeIdFrom",
                "MANAGER",
                "roleTypeIdTo",
                selectedRoleId,
                "partyRelationshipTypeId",
                "REPORTS_TO");
        Map<String, Object> createReporteeRelationResult = null;
        try {
          createReporteeRelationResult =
              dispatcher.runSync("createPartyRelationship", createReporteeRelationCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createReporteeRelationResult));
        }
      }
      if (UtilValidate.isNotEmpty(designation)) {
        Map createReporteeRelationCtx =
            UtilMisc.toMap(
                "userLogin",
                userLogin,
                "partyIdFrom",
                orgPartyId,
                "partyIdTo",
                supplierContactPartyId,
                "roleTypeIdFrom",
                "INTERNAL_ORGANIZATIO",
                "roleTypeIdTo",
                "EMPLOYEE",
                "partyRelationshipTypeId",
                "EMPLOYEMENT",
                "positionTitle",
                designation);
        Map<String, Object> createReporteeRelationResult = null;
        try {
          createReporteeRelationResult =
              dispatcher.runSync("createPartyRelationship", createReporteeRelationCtx);
        } catch (GenericServiceException e) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createReporteeRelationResult));
        }
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  private static boolean addRole(
      DispatchContext dctx, String roleTypeId, String partyId, GenericValue userLoginToUse) {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map createSelectedRoleCtx =
        UtilMisc.toMap("userLogin", userLoginToUse, "partyId", partyId, "roleTypeId", roleTypeId);
    // invoke the service
    Map<String, Object> createSelectedRoleResult = null;
    try {
      createSelectedRoleResult = dispatcher.runSync("createPartyRole", createSelectedRoleCtx);
    } catch (GenericServiceException e) {
      Debug.logError(ServiceUtil.getErrorMessage(createSelectedRoleResult), module);
      return false;
    }
    return true;
  }

  public static Map<String, Object> getOrgAllPersons(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    String partyGroupPartyId = (String) context.get("partyGroupPartyId");
    if (UtilValidate.isEmpty(partyGroupPartyId)) {
      Debug.logError("partyGroupPartyId parameter cannot be null.", module);
      return ServiceUtil.returnError("partyGroupPartyId parameter cannot be null.");
    }
    Class enumClass = null;
    try {
      enumClass = getRolesEnumClassForPartyGroupId(dispatcher, userLogin, partyGroupPartyId);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
    }
    if (enumClass == null) {
      return ServiceUtil.returnError("partyGroupPartyId is not a valid Group Party Id.");
    }
    List<String> relationshipTypeIds = HierarchyRoleUtils.partyRelationshipTypeIds(enumClass);
    Set<String> relationshipTypeIdSet = new HashSet<String>(relationshipTypeIds);

    EntityExpr condnPartyIdTo =
        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyGroupPartyId);
    EntityExpr condnRoleTypeIdFrom =
        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "_NA_");

    List<EntityCondition> relationshipCondsList = FastList.newInstance();
    for (String relationshipTypeId : relationshipTypeIdSet) {
      relationshipCondsList.add(
          EntityCondition.makeCondition(
              "partyRelationshipTypeId", EntityOperator.EQUALS, relationshipTypeId));
    }

    EntityConditionList<EntityCondition> exprListpartyRelationshipTypeIdOr =
        EntityCondition.makeCondition(relationshipCondsList, EntityOperator.OR);
    EntityConditionList<EntityCondition> mainCond =
        EntityCondition.makeCondition(
            UtilMisc.toList(condnRoleTypeIdFrom, condnPartyIdTo, exprListpartyRelationshipTypeIdOr),
            EntityOperator.AND);
    List<GenericValue> partyRelationships = FastList.newInstance();
    try {
      partyRelationships =
          EntityUtil.filterByDate(
              delegator.findList("PartyRelationship", mainCond, null, null, null, true));
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    List<GenericValue> orgAllPersons = new LinkedList<GenericValue>();
    Set<String> uniquePartyIds = new TreeSet<>();
    if (UtilValidate.isNotEmpty(partyRelationships)) {
      for (GenericValue partyRelEntry : partyRelationships) {
        String partyIdTo = partyRelEntry.getString("partyIdTo");
        if (uniquePartyIds.contains(partyIdTo)) {
          continue;
        }
        uniquePartyIds.add(partyIdTo);
        GenericValue personInfo = null;
        try {
          personInfo = delegator.findOne("Person", UtilMisc.toMap("partyId", partyIdTo), true);
        } catch (GenericEntityException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isNotEmpty(personInfo)) {
          orgAllPersons.add(personInfo);
        }
      }
    }
    serviceResult.put("orgPersonsList", orgAllPersons);
    return serviceResult;
  }

  private static Class getRolesEnumClassForPartyGroupId(
      LocalDispatcher dispatcher, GenericValue userLogin, String partyGroupPartyId)
      throws GenericServiceException {
    // Check if Internal Org
    Map isInternalOrgCheckCtx =
        UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyGroupPartyId);
    Boolean isInternalPermission = false;
    Map<String, Object> isInternalOrgCheckResp =
        dispatcher.runSync(
            "doesPartyGroupPartyIdEqualInternalOrgPartyIdPermissionCheck", isInternalOrgCheckCtx);
    isInternalPermission = (Boolean) isInternalOrgCheckResp.get("hasPermission");
    if (ServiceUtil.isSuccess(isInternalOrgCheckResp) && isInternalPermission == true) {
      return InternalPersonRoles.class;
    }

    // 1. isPartyGroupPartyIdCustomerPermissionCheck
    Map isCustomerCheckCtx =
        UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyGroupPartyId);
    Map<String, Object> isCustomerCheckResp =
        dispatcher.runSync("isPartyGroupPartyIdCustomerPermissionCheck", isCustomerCheckCtx);
    Boolean isCustomerPermission = (Boolean) isCustomerCheckResp.get("hasPermission");
    if (ServiceUtil.isSuccess(isCustomerCheckResp) && isCustomerPermission == true) {
      return CustomerPersonRoles.class;
    }

    // 2. isPartyGroupPartyIdPartnerPermissionCheck
    Map isPartnerCheckCtx =
        UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyGroupPartyId);
    Map<String, Object> isPartnerCheckResp =
        dispatcher.runSync("isPartyGroupPartyIdPartnerPermissionCheck", isPartnerCheckCtx);
    Boolean isPartnerPermission = (Boolean) isPartnerCheckResp.get("hasPermission");
    if (ServiceUtil.isSuccess(isPartnerCheckResp) && isPartnerPermission == true) {
      return EmployerPersonRoles.class;
    }

    // 3. isPartyGroupPartyIdSupplierPermissionCheck
    Map isSupplierCheckCtx =
        UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyGroupPartyId);
    Map<String, Object> isSupplierCheckResp =
        dispatcher.runSync("isPartyGroupPartyIdSupplierPermissionCheck", isSupplierCheckCtx);
    Boolean isSupplierPermission = (Boolean) isSupplierCheckResp.get("hasPermission");
    if (ServiceUtil.isSuccess(isSupplierCheckResp) && isSupplierPermission == true) {
      return EmployerPersonRoles.class;
    }
    return null;
  }

  /**
   * Assigns a manager party to a company(partyGroup) employee. If employee already has a manager
   * that relationship is dropped first before making a new relationship with the new manager.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> assignEmployeeManager(
      DispatchContext dctx, Map<String, Object> context) {
     Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();

    String employeePartyId = (String) context.get("employeePartyId");
    String managerPartyId = (String) context.get("managerPartyId");

    GenericValue employeeParty = HierarchyUtils.getPartyByPartyId(delegator, employeePartyId);
    if (UtilValidate.isEmpty(employeeParty)) {
      return ServiceUtil.returnError(
          "Unable to find a valid employee record using supplied id. Please validate the employee identifier and try again.");
    }

    GenericValue managerParty = HierarchyUtils.getPartyByPartyId(delegator, employeePartyId);
    if (UtilValidate.isEmpty(managerParty)) {
      return ServiceUtil.returnError(
          "Unable to find a valid manager record using supplied id. Please validate the manager identifier and try again.");
    }

    // make sure employee doesn't have an existing manager relationship, if so drop that first
    GenericValue partyManagerRelationship = getEmployeeManager(delegator, employeePartyId);
    if (UtilValidate.isNotEmpty(partyManagerRelationship)) {
      // drop the relationship
      try {
        delegator.removeValue(partyManagerRelationship);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
        ServiceUtil.returnError(
            "An error occurred while trying to remove existing manager relationship. Please check log for more details.");
      }
    }

    // make the new relationship
    try {
      dispatcher.runSync(
          "createManagerForPartyRelationship",
          UtilMisc.toMap(
              "userLogin",
              userLogin,
              "subordinatePartyId",
              employeePartyId,
              "managerPartyId",
              managerPartyId));
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      ServiceUtil.returnError(
          "An error occurred while trying to create new manager relationship. Please check log for more details.");
    }

    return serviceResult;
  }

  /**
   * Removes an existing assigned reporting manager from an employee's relationship.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> removeEmployeeManager(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();

    String employeePartyId = (String) context.get("employeePartyId");

    GenericValue employeeParty = HierarchyUtils.getPartyByPartyId(delegator, employeePartyId);
    if (UtilValidate.isEmpty(employeeParty)) {
      return ServiceUtil.returnError(
          "Unable to find a valid employee record using supplied id. Please validate the employee identifier and try again.");
    }
    GenericValue partyManagerRelationship = getEmployeeManager(delegator, employeePartyId);
    if (UtilValidate.isNotEmpty(partyManagerRelationship)) {
      // drop the relationship
      try {
        delegator.removeValue(partyManagerRelationship);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
        ServiceUtil.returnError(
            "An error occurred while trying to remove existing manager relationship. Please check log for more details.");
      }
    } else {
      Debug.logWarning(
          "No manager exists for the input employee party id : "
              + employeePartyId
              + " doing nothing.",
          module);
    }

    return serviceResult;
  }

  /**
   * Returns PartyRelationship manager record for the employee.
   *
   * @param delegator
   * @param employeePartyId
   * @return
   */
  public static GenericValue getEmployeeManager(Delegator delegator, String employeePartyId) {
    EntityExpr condnPartyIdTo =
        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, employeePartyId);

    List exprListManager =
        UtilMisc.toList(
            condnPartyIdTo,
            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ORG_MEMBER"),
            EntityCondition.makeCondition(
                "partyRelationshipTypeId", EntityOperator.EQUALS, "REPORTS_TO"));

    EntityConditionList exprListManagerAnd =
        EntityCondition.makeCondition(exprListManager, EntityOperator.AND);

    List exprListAccountLead =
        UtilMisc.toList(
            condnPartyIdTo,
            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_LEAD"),
            EntityCondition.makeCondition(
                "partyRelationshipTypeId", EntityOperator.EQUALS, "ACCOUNT"));

    EntityConditionList exprListAccountLeadAnd =
        EntityCondition.makeCondition(exprListAccountLead, EntityOperator.AND);

    EntityConditionList mainCond =
        EntityCondition.makeCondition(
            UtilMisc.toList(exprListManagerAnd, exprListAccountLeadAnd), EntityOperator.OR);

    GenericValue partyRelationship = null;
    try {
      partyRelationship =
          EntityUtil.getFirst(
              EntityUtil.filterByDate(
                  delegator.findList("PartyRelationship", mainCond, null, null, null, false)));
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return partyRelationship;
  }

  /**
   * Returns manager's partyId for the employee.
   *
   * @param delegator
   * @param employeePartyId
   * @return
   */
  public static String getEmployeeManagerPartyId(Delegator delegator, String employeePartyId) {
    String managerPartyId = "";
    EntityExpr condnPartyIdTo =
        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, employeePartyId);
    String member= AccountUserRoleTypesEnum.MEMBER.getRole();
    List exprListManager =
        UtilMisc.toList(
            condnPartyIdTo,
            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, member),
            EntityCondition.makeCondition(
                "partyRelationshipTypeId", EntityOperator.EQUALS, "REPORTS_TO"));

    EntityConditionList exprListManagerAnd =
        EntityCondition.makeCondition(exprListManager, EntityOperator.AND);

    List exprListAccountLead =
        UtilMisc.toList(
            condnPartyIdTo,
            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT_LEAD"),
            EntityCondition.makeCondition(
                "partyRelationshipTypeId", EntityOperator.EQUALS, "ACCOUNT"));

    EntityConditionList exprListAccountLeadAnd =
        EntityCondition.makeCondition(exprListAccountLead, EntityOperator.AND);

    EntityConditionList mainCond =
        EntityCondition.makeCondition(
            UtilMisc.toList(exprListManagerAnd, exprListAccountLeadAnd), EntityOperator.OR);

    GenericValue partyRelationship = null;
    try {
      partyRelationship =
          EntityUtil.getFirst(
              EntityUtil.filterByDate(
                  delegator.findList("PartyRelationship", mainCond, null, null, null, false)));
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    if (UtilValidate.isNotEmpty(partyRelationship)) {
      managerPartyId = partyRelationship.getString("partyIdFrom");
    }

    return managerPartyId;
  }

  /**
   * Assign manager role to an existing party
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> addManagerRoleToSuppParty(
      DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String employeePartyId = (String) context.get("employeePartyId");
    String supplierPartyId = (String) context.get("supplierPartyId");

    GenericValue employeeParty = HierarchyUtils.getPartyByPartyId(delegator, employeePartyId);
    if (UtilValidate.isEmpty(employeeParty)) {
      return ServiceUtil.returnError(
          "Unable to find a valid employee record. Please validate the employee identifier and try again.");
    }

    // create party role as MANAGER
    Map createPartyRoleCtx =
        UtilMisc.toMap(
            "userLogin", userLogin,
            "partyId", employeePartyId,
            "roleTypeId", "MANAGER");
    Map<String, Object> createPartyRoleResp =
        dispatcher.runSync("createPartyRole", createPartyRoleCtx);
    if (!ServiceUtil.isSuccess(createPartyRoleResp)) {
      return createPartyRoleResp;
    }

    // delete existing relationship
    List<GenericValue> partyRelationshipRecord = null;
    try {
      partyRelationshipRecord =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where(
                  "partyIdTo",
                  employeePartyId,
                  "partyIdFrom",
                  supplierPartyId,
                  "partyRelationshipTypeId",
                  "EMPLOYMENT")
              .queryList();

      if (UtilValidate.isNotEmpty(partyRelationshipRecord)) {
        for (GenericValue record : partyRelationshipRecord) {
          delegator.removeValue(record);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    // create relationship
    Map createPartyRelationshipCtx =
        UtilMisc.toMap(
            "userLogin",
            userLogin,
            "partyIdFrom",
            supplierPartyId,
            "partyIdTo",
            employeePartyId,
            "roleTypeIdTo",
            "MANAGER",
            "roleTypeIdFrom",
            "_NA_",
            "partyRelationshipTypeId",
            "EMPLOYMENT");

    Map<String, Object> createPartyRelationshipResp =
        dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
    if (!ServiceUtil.isSuccess(createPartyRelationshipResp)) {
      return createPartyRelationshipResp;
    }

    return serviceResult;
  }

  public static Map<String, Object> removeManagerRoleToSuppParty(
      DispatchContext dctx, Map<String, Object> context)
      throws GenericServiceException, GenericEntityException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String partyId = (String) context.get("partyId");
    String supplierPartyId = (String) context.get("supplierPartyId");

    try {
      List<GenericValue> partyRelationshipsReportsTo =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where("partyIdFrom", partyId, "roleTypeIdFrom", "MANAGER")
              .queryList();

      if (UtilValidate.isNotEmpty(partyRelationshipsReportsTo)) {
        for (GenericValue record : partyRelationshipsReportsTo) {
          delegator.removeValue(record);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    // remove relationship of manager with supplier
    try {
      List<GenericValue> partyRelationships =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where(
                  "partyIdFrom", supplierPartyId, "partyIdTo", partyId, "roleTypeIdTo", "MANAGER")
              .queryList();
      if (UtilValidate.isNotEmpty(partyRelationships)) {
        for (GenericValue record : partyRelationships) {
          delegator.removeValue(record);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    // remove relationship of manager with other parties if exists
    try {
      List<GenericValue> partyRelationshipsTo =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where("partyIdTo", partyId, "roleTypeIdTo", "MANAGER")
              .queryList();
      if (UtilValidate.isNotEmpty(partyRelationshipsTo)) {
        for (GenericValue record : partyRelationshipsTo) {
          delegator.removeValue(record);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    GenericValue partyRoleRecords =
        EntityQuery.use(delegator)
            .from("PartyRole")
            .where("partyId", partyId, "roleTypeId", "SALES_REP")
            .queryOne();
    if (UtilValidate.isEmpty(partyRoleRecords)) {
      // //create party role as SALES_REP
      Map createPartyRoleCtx =
          UtilMisc.toMap(
              "userLogin", userLogin,
              "partyId", partyId,
              "roleTypeId", "SALES_REP");
      Map<String, Object> createPartyRoleResp =
          dispatcher.runSync("createPartyRole", createPartyRoleCtx);
      if (!ServiceUtil.isSuccess(createPartyRoleResp)) {
        return createPartyRoleResp;
      }
    }

    // build a relationship
    Map createPartyRelationshipCtx =
        UtilMisc.toMap(
            "userLogin",
            userLogin,
            "partyIdFrom",
            supplierPartyId,
            "partyIdTo",
            partyId,
            "roleTypeIdTo",
            "SALES_REP",
            "roleTypeIdFrom",
            "_NA_",
            "partyRelationshipTypeId",
            "EMPLOYMENT");

    Map createSalesRepResult =
        dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);

    if (!ServiceUtil.isSuccess(createSalesRepResult)) {
      return createSalesRepResult;
    }
    try {
      GenericValue partyRoleToRemove =
          EntityQuery.use(delegator)
              .from("PartyRole")
              .where("partyId", partyId, "roleTypeId", "MANAGER")
              .queryOne();
      if (UtilValidate.isNotEmpty(partyRoleToRemove)) {
        delegator.removeValue(partyRoleToRemove);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }
}
