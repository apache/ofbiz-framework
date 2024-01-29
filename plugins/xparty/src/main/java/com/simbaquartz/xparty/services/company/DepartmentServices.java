package com.simbaquartz.xparty.services.company;


import com.simbaquartz.xparty.hierarchy.role.AccountRoles;
import com.simbaquartz.xparty.services.account.AccountServices;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.collections.FastSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.simbaquartz.xparty.helpers.PartyContactHelper.module;

public class DepartmentServices {

  public static class DepartmentServicesErrorMessages {
    public static final String SYSTEM_ERROR_RESP = "Something went wrong, please try later.";

    public static final String ERROR_CREATE_ROLE = "Error creating role ";
    public static final String ERROR_CREATE_RELATION =
            "Error while creating relationship in party and company account";
  }

  /**
   * Service to add details for department, details include department name, owner of the
   * department, list of members for the department Also if parentDepartmentId is present then
   * create relation of department with sub -department
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public Map<String, Object> createDepartment(DispatchContext dctx, Map<String, Object> context)
          throws GenericEntityException, GenericServiceException {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String name = (String) context.get("name");
    String ownerPartyId = (String) context.get("ownerPartyId");
    String parentDepartmentId = (String) context.get("parentDepartmentId");
    String description = (String) context.get("description");
    String orgGroupPartyId = (String) context.get("orgGroupPartyId");
    List<String> members = (List) context.get("members");

    // create party group
    Map<String, Object> createPartyGroupCtx =
            UtilMisc.toMap(
                    "groupName",
                    name,
                    "comments",
                    description,
                    "userLogin",
                    userLogin,
                    "partyTypeId",
                    "DEPARTMENT");

    Map<String, Object> createPartyGroupCtxResponse =
            dispatcher.runSync("createPartyGroup", createPartyGroupCtx);
    String departmentId = (String) createPartyGroupCtxResponse.get("partyId");

    // create role of party group as department
    Map createPartyRoleCtx =
            UtilMisc.toMap(
                    "userLogin",
                    userLogin,
                    "partyId",
                    departmentId,
                    "roleTypeId",
                    AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
    Map<String, Object> createPartyRoleCtxResponse = null;
    try {
      createPartyRoleCtxResponse = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    if (ServiceUtil.isError(createPartyRoleCtxResponse)) {
      Debug.logError(
              DepartmentServicesErrorMessages.ERROR_CREATE_ROLE,
              ServiceUtil.getErrorMessage(createPartyRoleCtxResponse),
              module);
      return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
    }

    // create the relationship of OWNER between department and account party group
    Map<String, Object> createDepartmentRelationshipCtx = new HashMap<String, Object>();

    createDepartmentRelationshipCtx.put("partyIdTo", departmentId);
    createDepartmentRelationshipCtx.put(
            "partyRelationshipTypeId", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
    createDepartmentRelationshipCtx.put("userLogin", userLogin);

    if (UtilValidate.isNotEmpty(parentDepartmentId)) {
      createDepartmentRelationshipCtx.put("partyIdFrom", parentDepartmentId);
      createDepartmentRelationshipCtx.put(
              "roleTypeIdFrom", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
      createDepartmentRelationshipCtx.put(
              "roleTypeIdTo", AccountRoles.SUB_DEPARTMENT.getPartyRelationshipTypeId());
    } else {
      createDepartmentRelationshipCtx.put("partyIdFrom", orgGroupPartyId);
      createDepartmentRelationshipCtx.put(
              "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
      createDepartmentRelationshipCtx.put(
              "roleTypeIdTo", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
    }
    // Created relationship and role of department and company i.e. department is OWNER of the
    // INTERNAL_ORGANIZATIO with type as OWNER
    Map<String, Object> createDepartmentRelationshipResp = null;
    createDepartmentRelationshipResp =
            dispatcher.runSync("createPartyRelationshipAndRole", createDepartmentRelationshipCtx);
    if (ServiceUtil.isError(createDepartmentRelationshipResp)) {
      Debug.logError(
              DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
              ServiceUtil.getErrorMessage(createDepartmentRelationshipResp),
              module);
      return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
    }

    // create the relationship of head of department between between hod and department
    Map<String, Object> createDepartmentOwnerRelationshipCtx = new HashMap<String, Object>();
    createDepartmentOwnerRelationshipCtx.put("partyIdFrom", departmentId);
    createDepartmentOwnerRelationshipCtx.put("partyIdTo", ownerPartyId);
    createDepartmentOwnerRelationshipCtx.put(
            "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
    createDepartmentOwnerRelationshipCtx.put(
            "roleTypeIdTo", AccountRoles.HOD.getPartyRelationshipTypeId());
    createDepartmentOwnerRelationshipCtx.put(
            "partyRelationshipTypeId", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
    createDepartmentOwnerRelationshipCtx.put("userLogin", userLogin);

    // Created relationship and role of person and company i.e. person is OWNER of the
    // INTERNAL_ORGANIZATIO with type as OWNER
    Map<String, Object> createDepartmentOwnerRelationshipResp = null;
    createDepartmentOwnerRelationshipResp =
            dispatcher.runSync("createPartyRelationshipAndRole", createDepartmentOwnerRelationshipCtx);
    if (ServiceUtil.isError(createDepartmentOwnerRelationshipResp)) {
      Debug.logError(
              DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
              ServiceUtil.getErrorMessage(createDepartmentOwnerRelationshipResp),
              module);
      return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
    }

    if (ServiceUtil.isError(createPartyGroupCtxResponse)) {
      Debug.logError(ServiceUtil.getErrorMessage(createPartyGroupCtxResponse), module);
      return ServiceUtil.returnError(
              AccountServices.AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
    }

    // Creating relationship of members with department
    for (String employeeId : CollectionUtils.emptyIfNull(members)) {
      Map<String, Object> createDeptMemberRelationshipCtx = new HashMap<String, Object>();
      createDeptMemberRelationshipCtx.put("partyIdFrom", departmentId);
      createDeptMemberRelationshipCtx.put("partyIdTo", employeeId);
      createDeptMemberRelationshipCtx.put(
              "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put(
              "roleTypeIdTo", AccountRoles.DEPARTMENT_MEMBER.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put(
              "partyRelationshipTypeId", AccountRoles.DEPARTMENT_MEMBER.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put("userLogin", userLogin);

      // Created relationship and role of person and Department i.e. person is member of the
      // department
      Map<String, Object> createAccountEmployeeRelationshipResult = null;
      createAccountEmployeeRelationshipResult =
              dispatcher.runSync("createPartyRelationshipAndRole", createDeptMemberRelationshipCtx);
      if (ServiceUtil.isError(createAccountEmployeeRelationshipResult)) {
        Debug.logError(
                DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
                ServiceUtil.getErrorMessage(createAccountEmployeeRelationshipResult),
                module);
        return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
      }

      if (ServiceUtil.isError(createPartyGroupCtxResponse)) {
        Debug.logError(ServiceUtil.getErrorMessage(createPartyGroupCtxResponse), module);
        return ServiceUtil.returnError(
                AccountServices.AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
      }
    }

    serviceResult.put("departmentId", departmentId);
    return serviceResult;
  }

  /**
   * Service to fetch list of all departments for the company of logged in user
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public Map<String, Object> getCompanyDepartments(
          DispatchContext dctx, Map<String, Object> context)
          throws GenericEntityException, GenericServiceException {
    Delegator delegator = dctx.getDelegator();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    String orgGroupPartyId = (String) context.get("orgGroupPartyId");
    String keyword = (String) context.get("keyword");
    Integer startIndex = (Integer) context.get("startIndex");
    Integer viewSize = (Integer) context.get("viewSize");
    List<Map> departments = FastList.newInstance();
    long resultSize = 0;
    try {

      if (UtilValidate.isEmpty(keyword)) keyword = "";

      if (UtilValidate.isEmpty(viewSize) || viewSize < 0) viewSize = 10;

      if (UtilValidate.isEmpty(startIndex) || startIndex <= 0) startIndex = 0;

      int lowIndex = startIndex + 1;
      int highIndex = (startIndex) + viewSize;

      EntityFindOptions efo = new EntityFindOptions();
      efo.setMaxRows(highIndex);
      efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
      efo.setDistinct(true);

      // order by
      List<String> orderBy = FastList.newInstance();
      orderBy.add("fromDate");

      Set<String> fieldToSelect = FastSet.newInstance();
      fieldToSelect.add("partyIdTo");
      fieldToSelect.add("fromDate");
      List<EntityCondition> condList = FastList.newInstance();
      condList.add(EntityCondition.makeCondition("partyIdFrom", orgGroupPartyId));
      condList.add(EntityCondition.makeCondition("roleTypeIdTo", "DEPARTMENT"));
      if (UtilValidate.isNotEmpty(keyword)) {
        condList.add(EntityCondition.makeCondition(
                EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("groupName"), EntityOperator.LIKE, "%" + keyword.toUpperCase() + "%"),
                EntityOperator.OR,
                EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("comments"), EntityOperator.LIKE, "%" + keyword.toUpperCase() + "%"))
        );
      }
      EntityCondition cond = EntityCondition.makeCondition(condList);

      TransactionUtil.begin();
      EntityListIterator searchResult =
              delegator.find("PartyRelationshipAndDetail", cond, null, fieldToSelect, orderBy, efo);
      List<GenericValue> departmentIdList = null;
      if (UtilValidate.isNotEmpty(searchResult)) {
        departmentIdList = searchResult.getPartialList(lowIndex, highIndex - lowIndex + 1);
        if (UtilValidate.isNotEmpty(departmentIdList)) {
          List<String> departmentIds =
                  departmentIdList
                          .stream()
                          .map(x -> (String) x.get("partyIdTo"))
                          .collect(Collectors.toList());
          List<GenericValue> departmentGvs = null;
          if (UtilValidate.isNotEmpty(departmentIds)) {
            departmentGvs =
                    EntityQuery.use(delegator)
                            .from("DepartmentDetails")
                            .where(
                                    EntityCondition.makeCondition(
                                            EntityCondition.makeCondition(
                                                    "partyId", EntityOperator.IN, departmentIds)))
                            .queryList();
          }
          // get total count for pagination
          resultSize =
                  EntityQuery.use(delegator).from("PartyRelationshipAndDetail").where(cond).queryCount();
          if (UtilValidate.isNotEmpty(departmentGvs)) {
            List<GenericValue> departmentAndHod =
                    departmentGvs
                            .stream()
                            .filter(
                                    x ->
                                            AccountRoles.HOD
                                                    .getPartyRelationshipTypeId()
                                                    .equals(x.getString("roleTypeIdTo")))
                            .collect(Collectors.toList());
            for (GenericValue department : departmentAndHod) {
              Map departmentMap = FastMap.newInstance();
              departmentMap.put("name", department.getString("groupName"));
              departmentMap.put("id", department.getString("partyId"));
              departmentMap.put("description", department.getString("comments"));
              Map ownerDetails =
                      UtilMisc.toMap(
                              "id", department.getString("partyIdTo"),
                              "displayName", department.getString("toDisplayName"),
                              "email", department.getString("toEmail"),
                              "photoUrl", department.getString("toPhotoUrl"));

              departmentMap.put("owner", ownerDetails);

              List<Map> departmentMembers =
                      departmentGvs
                              .stream()
                              .filter(
                                      x ->
                                              AccountRoles.DEPARTMENT_MEMBER
                                                      .getPartyRelationshipTypeId()
                                                      .equals(x.getString("roleTypeIdTo"))
                                                      && department.getString("partyId").equals(x.getString("partyId")))
                              .map(
                                      x ->
                                              UtilMisc.toMap(
                                                      "id", x.getString("partyIdTo"),
                                                      "displayName", x.getString("toDisplayName"),
                                                      "email", x.getString("toEmail"),
                                                      "photoUrl", x.getString("toPhotoUrl")))
                              .collect(Collectors.toList());
              departmentMap.put("members", departmentMembers);
              departments.add(departmentMap);
            }
          }
          searchResult.close();
          TransactionUtil.commit();
        }
      }
    } catch (GenericEntityException ex) {
      Debug.logError(ex, module);
      return ServiceUtil.returnError(ex.getMessage());
    }
    serviceResult.put("departments", departments);
    serviceResult.put("resultSize", resultSize);
    return serviceResult;
  }

  /**
   * Service to fetch complete details of the department based on department Id
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public Map<String, Object> getCompanyDepartment(DispatchContext dctx, Map<String, Object> context)
          throws GenericEntityException, GenericServiceException {
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    String departmentId = (String) context.get("departmentId");
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    Map departmentMap = FastMap.newInstance();
    try {
      List<GenericValue> departmentGvs =
              EntityQuery.use(delegator)
                      .from("DepartmentDetails")
                      .where("partyId", departmentId)
                      .queryList();
      if (UtilValidate.isNotEmpty(departmentGvs)) {
        GenericValue departmentAndHod =
                departmentGvs
                        .stream()
                        .filter(
                                x ->
                                        AccountRoles.HOD
                                                .getPartyRelationshipTypeId()
                                                .equals(x.getString("roleTypeIdTo")))
                        .findFirst()
                        .orElse(null);
        if (UtilValidate.isNotEmpty(departmentAndHod)) {
          departmentMap.put("name", departmentAndHod.getString("groupName"));
          departmentMap.put("id", departmentAndHod.getString("partyId"));
          departmentMap.put("description", departmentAndHod.getString("comments"));
          Map ownerDetails =
                  UtilMisc.toMap(
                          "id", departmentAndHod.getString("partyIdTo"),
                          "displayName", departmentAndHod.getString("toDisplayName"),
                          "email", departmentAndHod.getString("toEmail"),
                          "photoUrl", departmentAndHod.getString("toPhotoUrl"));

          departmentMap.put("owner", ownerDetails);
          Set<GenericValue> departmentMemberGvs =
                  departmentGvs
                          .stream()
                          .filter(
                                  x ->
                                          AccountRoles.DEPARTMENT_MEMBER
                                                  .getPartyRelationshipTypeId()
                                                  .equals(x.getString("roleTypeIdTo"))
                                                  && departmentAndHod
                                                  .getString("partyId")
                                                  .equals(x.getString("partyId")))
                          .collect(
                                  Collectors.toCollection(
                                          // distinct elements are stored into new SET
                                          () ->
                                                  new TreeSet<>(
                                                          Comparator.comparing(
                                                                  p -> p.getString("partyIdTo"))))).descendingSet(); // Id comparison
          if (UtilValidate.isNotEmpty(departmentMemberGvs)) {
            List<Map> departmentMembers =
                    departmentMemberGvs
                            .stream()
                            .filter(
                                    x ->
                                            AccountRoles.DEPARTMENT_MEMBER
                                                    .getPartyRelationshipTypeId()
                                                    .equals(x.getString("roleTypeIdTo"))
                                                    && departmentAndHod
                                                    .getString("partyId")
                                                    .equals(x.getString("partyId")))
                            .map(
                                    x ->
                                            UtilMisc.toMap(
                                                    "id", x.getString("partyIdTo"),
                                                    "displayName", x.getString("toDisplayName"),
                                                    "email", x.getString("toEmail"),
                                                    "photoUrl", x.getString("toPhotoUrl")))
                            .collect(Collectors.toList());
            departmentMap.put("members", departmentMembers);
          }
          // Fetch Sub Departments
          List<String> subDepartmentsGv =
                  departmentGvs
                          .stream()
                          .filter(
                                  x ->
                                          AccountRoles.SUB_DEPARTMENT
                                                  .getPartyRelationshipTypeId()
                                                  .equals(x.getString("roleTypeIdTo"))
                                                  && departmentAndHod
                                                  .getString("partyId")
                                                  .equals(x.getString("partyId")))
                          .map(x -> x.getString("partyIdTo")).distinct()
                          .collect(Collectors.toList());
          Map getDepartmentsResp;
          List<Map> subDepartments = FastList.newInstance();
          for (String subDepartmentId : CollectionUtils.emptyIfNull(subDepartmentsGv)) {
            getDepartmentsResp =
                    dispatcher.runSync(
                            "getCompanyDepartment",
                            UtilMisc.toMap("userLogin", userLogin, "departmentId", subDepartmentId));
            if (ServiceUtil.isError(getDepartmentsResp)) {
              // handle error here
              String serviceError = ServiceUtil.getErrorMessage(getDepartmentsResp);
              Debug.logError(
                      "An error occured while fetching department, details: " + serviceError, module);
              if (Debug.verboseOn()) Debug.logVerbose("Exiting method getDepartment", module);
              return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getDepartmentsResp));
            }
            if (UtilValidate.isNotEmpty(getDepartmentsResp)
                    && UtilValidate.isNotEmpty(getDepartmentsResp.get("departmentDetails"))) {
              subDepartments.add((Map) getDepartmentsResp.get("departmentDetails"));
            }
          }
          if (UtilValidate.isNotEmpty(subDepartments)) {
            departmentMap.put("subDepartments", subDepartments);
          }
        }
      }

    } catch (GenericEntityException ex) {
      Debug.logError(ex, module);
      return ServiceUtil.returnError(ex.getMessage());
    }
    serviceResult.put("departmentDetails", departmentMap);
    return serviceResult;
  }
  /**
   * Service to update the details for department, details include department name, owner of the
   * department, list of members for the department Also if parentDepartmentId is present then
   * update relation of department with sub -department
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public Map<String, Object> updateCompanyDepartment(DispatchContext dctx, Map<String, Object> context)
          throws GenericEntityException, GenericServiceException {
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String name = (String) context.get("name");
    String description = (String) context.get("description");
    String departmentId = (String) context.get("departmentId");
    String ownerPartyId = (String) context.get("ownerPartyId");
    String parentDepartmentId = (String) context.get("parentDepartmentId");
    List<String> members = (List) context.get("members");

    // create party group
    Map<String, Object> updatePartyGroupCtx =
            UtilMisc.toMap(
                    "partyId",
                    departmentId,
                    "groupName",
                    name,
                    "comments",
                    description,
                    "userLogin",
                    userLogin);

    Map<String, Object> updatePartyGroupCtxResponse =
            dispatcher.runSync("updatePartyGroup", updatePartyGroupCtx);

    // delete all members from department
    List<GenericValue> deptRelationships = EntityQuery.use(delegator).from("PartyRelationship")
            .where("partyIdFrom", departmentId,
                    "roleTypeIdTo", AccountRoles.DEPARTMENT_MEMBER.getPartyRelationshipTypeId())
            .queryList();
    if(UtilValidate.isNotEmpty(deptRelationships)) {
      delegator.removeAll(deptRelationships);
    }

    // Creating relationship of members with department
    for (String employeeId : CollectionUtils.emptyIfNull(members)) {
      Map<String, Object> createDeptMemberRelationshipCtx = new HashMap<String, Object>();
      createDeptMemberRelationshipCtx.put("partyIdFrom", departmentId);
      createDeptMemberRelationshipCtx.put("partyIdTo", employeeId);
      createDeptMemberRelationshipCtx.put(
              "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put(
              "roleTypeIdTo", AccountRoles.DEPARTMENT_MEMBER.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put(
              "partyRelationshipTypeId", AccountRoles.DEPARTMENT_MEMBER.getPartyRelationshipTypeId());
      createDeptMemberRelationshipCtx.put("userLogin", userLogin);

      // Created relationship and role of person and Department i.e. person is member of the
      // department
      Map<String, Object> createAccountEmployeeRelationshipResult = null;
      createAccountEmployeeRelationshipResult =
              dispatcher.runSync("createPartyRelationshipAndRole", createDeptMemberRelationshipCtx);
      if (ServiceUtil.isError(createAccountEmployeeRelationshipResult)) {
        Debug.logError(
                DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
                ServiceUtil.getErrorMessage(createAccountEmployeeRelationshipResult),
                module);
        return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
      }

      if (ServiceUtil.isError(updatePartyGroupCtxResponse)) {
        Debug.logError(ServiceUtil.getErrorMessage(updatePartyGroupCtxResponse), module);
        return ServiceUtil.returnError(
                AccountServices.AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
      }
    }

    //Update Parent Department
    if (UtilValidate.isNotEmpty(parentDepartmentId)) {
      // delete prent department relation with department
      GenericValue parentDeptRelationships = EntityQuery.use(delegator).from("PartyRelationship")
              .where(EntityCondition.makeCondition(EntityCondition.makeCondition("partyIdTo", departmentId),
                      EntityCondition.makeCondition("roleTypeIdTo",EntityOperator.IN,UtilMisc.toList(AccountRoles.SUB_DEPARTMENT.getPartyRelationshipTypeId(),
                              AccountRoles.DEPARTMENT.getPartyRelationshipTypeId()))))
              .queryFirst();
      if(UtilValidate.isNotEmpty(parentDeptRelationships)) {
        parentDeptRelationships.remove();
      }
      // create the relationship of OWNER between department and account party group/Parent department
      Map<String, Object> createParentDepartmentRelationshipCtx = new HashMap<String, Object>();

      createParentDepartmentRelationshipCtx.put("partyIdTo", departmentId);
      createParentDepartmentRelationshipCtx.put(
              "partyRelationshipTypeId", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
      createParentDepartmentRelationshipCtx.put("userLogin", userLogin);

      if (UtilValidate.isNotEmpty(parentDepartmentId)) {
        createParentDepartmentRelationshipCtx.put("partyIdFrom", parentDepartmentId);
        createParentDepartmentRelationshipCtx.put(
                "roleTypeIdFrom", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
        createParentDepartmentRelationshipCtx.put(
                "roleTypeIdTo", AccountRoles.SUB_DEPARTMENT.getPartyRelationshipTypeId());
      }
      Map<String, Object> createDepartmentRelationshipResp = null;
      createDepartmentRelationshipResp =
              dispatcher.runSync("createPartyRelationshipAndRole", createParentDepartmentRelationshipCtx);
      if (ServiceUtil.isError(createDepartmentRelationshipResp)) {
        Debug.logError(
                DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
                ServiceUtil.getErrorMessage(createDepartmentRelationshipResp),
                module);
        return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
      }
    }
    //Update Owner of the Department
    if (UtilValidate.isNotEmpty(ownerPartyId)) {
      // delete prent department relation with department
      GenericValue deptOwnerRelationship = EntityQuery.use(delegator).from("PartyRelationship")
              .where("partyIdFrom", departmentId,
                      "roleTypeIdTo", AccountRoles.HOD.getPartyRelationshipTypeId())
              .queryFirst();
      if(UtilValidate.isNotEmpty(deptOwnerRelationship)) {
        deptOwnerRelationship.remove();
      }
      Map<String, Object> createDepartmentOwnerRelationshipCtx = new HashMap<String, Object>();
      createDepartmentOwnerRelationshipCtx.put("partyIdFrom", departmentId);
      createDepartmentOwnerRelationshipCtx.put("partyIdTo", ownerPartyId);
      createDepartmentOwnerRelationshipCtx.put(
              "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
      createDepartmentOwnerRelationshipCtx.put(
              "roleTypeIdTo", AccountRoles.HOD.getPartyRelationshipTypeId());
      createDepartmentOwnerRelationshipCtx.put(
              "partyRelationshipTypeId", AccountRoles.DEPARTMENT.getPartyRelationshipTypeId());
      createDepartmentOwnerRelationshipCtx.put("userLogin", userLogin);

      // Created relationship and role of person and company i.e. person is OWNER of the
      // INTERNAL_ORGANIZATIO with type as OWNER
      Map<String, Object> createDepartmentOwnerRelationshipResp = null;
      createDepartmentOwnerRelationshipResp =
              dispatcher.runSync("createPartyRelationshipAndRole", createDepartmentOwnerRelationshipCtx);
      if (ServiceUtil.isError(createDepartmentOwnerRelationshipResp)) {
        Debug.logError(
                DepartmentServicesErrorMessages.ERROR_CREATE_RELATION,
                ServiceUtil.getErrorMessage(createDepartmentOwnerRelationshipResp),
                module);
        return ServiceUtil.returnError(DepartmentServicesErrorMessages.SYSTEM_ERROR_RESP);
      }
    }
    return serviceResult;
  }


}
