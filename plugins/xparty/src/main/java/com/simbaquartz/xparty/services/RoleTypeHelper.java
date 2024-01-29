package com.simbaquartz.xparty.services;

import static com.simbaquartz.xparty.hierarchy.HierarchyRelationshipUtils.module;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class RoleTypeHelper {

  public static class RoleTypeErrorMessages {
    public static final String SYSTEM_ERROR =
        "Something went wrong, please check log for more details.";
    public static final String ERROR_REMOVING_ROLE_PERMISSIONS =
        "Error while removing profile role associated permissions, details:";
    public static final String ERROR_ADDING_ROLE_PERMISSIONS =
        "Error while adding profile role associated permissions, details:";
  }

  public static Map<String, Object> addRoleManagerRelation(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method addRoleManagerRelation", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String roleTypeId = (String) context.get("roleTypeId");
    String groupPartyId = (String) context.get("groupPartyId");
    String roleType = (String) context.get("roleType");

    try {
      GenericValue roleTypeManager = delegator.makeValue("RoleManager");
      roleTypeManager.set("roleTypeId", roleTypeId);
      roleTypeManager.set("groupPartyId", groupPartyId);
      roleTypeManager.set("roleType", roleType);
      roleTypeManager.create();

    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while adding entry to RoleManagerRelation details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> removeRoleManagerRelation(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method removeRoleManagerRelation", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String roleTypeId = (String) context.get("roleTypeId");
    String groupPartyId = (String) context.get("groupPartyId");

    try {
      GenericValue deleteRole =
          EntityQuery.use(delegator)
              .from("RoleManager")
              .where("groupPartyId", groupPartyId, "roleTypeId", roleTypeId)
              .queryFirst();
      if (UtilValidate.isNotEmpty(deleteRole)) {
        deleteRole.remove();
      }
    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while deleting record from RoleManager details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> addRolePermissions(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method addRolePermissions", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    List<String> permissions = (List) context.get("permissions");
    String roleTypeId = (String) context.get("roleTypeId");
    List rolePermissions = new LinkedList();
    try {
      for (String permission : permissions) {
        GenericValue rolePermission = delegator.makeValue("RolePermission");
        rolePermission.set("permissionId", permission);
        rolePermission.set("roleTypeId", roleTypeId);
        rolePermissions.add(rolePermission);
      }
      if (UtilValidate.isNotEmpty(rolePermissions)) {
        delegator.storeAll(rolePermissions);
      }
    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while adding entry to RolePermission details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  public static Map<String, Object> removeRolePermissions(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method removeRolePermissions", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String roleTypeId = (String) context.get("roleTypeId");
    try {
      List<GenericValue> deleteRolePermissions =
          EntityQuery.use(delegator)
              .from("RolePermission")
              .where("roleTypeId", roleTypeId)
              .queryList();
      delegator.removeAll(deleteRolePermissions);
    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while deleting records from RolePermission details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> updateRolePermissions(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering service updateRolePermissions", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    List<String> permissions = (List) context.get("permissions");
    String roleTypeId = (String) context.get("roleTypeId");
    String groupPartyId = (String) context.get("groupPartyId");
    try {
      Map<String, Object> removeRolePermissionsCtx =
          UtilMisc.toMap(
              "userLogin", userLogin, "roleTypeId", roleTypeId, "groupPartyId", groupPartyId);
      Map<String, Object> removeRolePermissionsResp =
          dispatcher.runSync("removeRolePermissions", removeRolePermissionsCtx);
      if (ServiceUtil.isError(removeRolePermissionsResp)) {
        String errorMsg = ServiceUtil.getErrorMessage(removeRolePermissionsResp);
        Debug.logError(RoleTypeErrorMessages.ERROR_REMOVING_ROLE_PERMISSIONS + errorMsg, module);
        return ServiceUtil.returnError(
            RoleTypeErrorMessages.ERROR_REMOVING_ROLE_PERMISSIONS + errorMsg);
      }

      Map<String, Object> addRolePermissionsCtx =
          UtilMisc.toMap(
              "userLogin", userLogin, "roleTypeId", roleTypeId, "permissions", permissions);
      Map<String, Object> addRolePermissionsResp =
          dispatcher.runSync("addRolePermissions", addRolePermissionsCtx);
      if (ServiceUtil.isError(addRolePermissionsResp)) {
        String errorMsg = ServiceUtil.getErrorMessage(addRolePermissionsResp);
        Debug.logError(RoleTypeErrorMessages.ERROR_ADDING_ROLE_PERMISSIONS + errorMsg, module);
        return ServiceUtil.returnError(
            RoleTypeErrorMessages.ERROR_ADDING_ROLE_PERMISSIONS + errorMsg);
      }

    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred in updateRolePermissions service, details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return result;
  }

  public static Map<String, Object> getRolePermissions(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getRolePermissions", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String roleTypeId = (String) context.get("roleTypeId");
    try {
      List<GenericValue> rolePermissions =
          EntityQuery.use(delegator)
              .from("RolePermission")
              .where("roleTypeId", roleTypeId)
              .queryList();
      result.put("permissions", rolePermissions);
      // format the code here
    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while fetching records from RolePermission details" + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }
    result.put("roleTypeId", roleTypeId);
    return result;
  }

  public static Map<String, Object> cloneRolePermissions(
      DispatchContext dctx, Map<String, Object> context) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method cloneRolePermissions", module);

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String roleTypeId = (String) context.get("roleTypeId");
    String cloneFrom = (String) context.get("cloneFrom");
    List rolePermissions = new LinkedList();

    try {
      List<GenericValue> rolePermissionsToCloneFrom =
          EntityQuery.use(delegator)
              .from("RolePermission")
              .where("roleTypeId", cloneFrom)
              .queryList();
      if (UtilValidate.isNotEmpty(rolePermissionsToCloneFrom)) {
        for (GenericValue rolePermissionToCloneFrom : rolePermissionsToCloneFrom) {
          GenericValue rolePermission = delegator.makeValue("RolePermission");
          rolePermission.set("permissionId", rolePermissionToCloneFrom.get("permissionId"));
          rolePermission.set("roleTypeId", roleTypeId);
          rolePermissions.add(rolePermission);
        }
      }

      if (UtilValidate.isNotEmpty(rolePermissions)) {
        delegator.storeAll(rolePermissions);
      }
    } catch (Exception e) {
      Debug.logError(
          e,
          "An error occurred while adding entry to RolePermission via cloning details"
              + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }
}
