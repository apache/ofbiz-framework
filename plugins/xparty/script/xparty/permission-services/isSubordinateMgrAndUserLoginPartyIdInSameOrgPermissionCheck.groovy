

import org.apache.ofbiz.service.ServiceUtil

public Map isSubordinateMgrAndUserLoginPartyIdInSameOrgPermissionCheck()
{
    final String module = "isSubordinateMgrAndUserLoginPartyIdInSameOrgPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    String managerPartyId = parameters.managerPartyId;
    String subordinatePartyId = parameters.subordinatePartyId;

    Map serviceResultManager = runService("isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck", [partyId: managerPartyId]);
    Map serviceResultSubordinate = runService("isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck", [partyId: subordinatePartyId]);

    hasPermission = serviceResultManager.hasPermission && serviceResultSubordinate.hasPermission;

    result.put("hasPermission", hasPermission);
    return result;
}