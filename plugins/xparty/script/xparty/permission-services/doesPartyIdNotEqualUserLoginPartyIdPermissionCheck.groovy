

import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public Map doesPartyIdNotEqualUserLoginPartyIdPermissionCheck()
{
    final String module = "doesPartyIdNotEqualUserLoginPartyIdPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    // Call doesPartyIdEqualUserLoginPartyIdPermissionCheck, and then negate the result.
    Debug.logInfo("Calling doesPartyIdEqualUserLoginPartyIdPermissionCheck and result will be negated.", module);
    Map serviceResult = runService("doesPartyIdEqualUserLoginPartyIdPermissionCheck", [partyId: partyId]);
    hasPermission = !serviceResult.hasPermission;
    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}