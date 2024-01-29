

import org.apache.ofbiz.service.ServiceUtil;

public Map isSubordinatePartyIdInManagerOrg() {
    final String module = "isSubordinatePartyIdInManagerOrg";
    Map result = ServiceUtil.returnSuccess();

    Map serviceResult = runService("isSubordinatePartyIdInManagerOrgPermissionCheck", [managerPartyId : parameters.managerPartyId,  subordinatePartyId: parameters.subordinatePartyId]);
    result.put("isInOrg", serviceResult.hasPermission);
    return result;
}
