


import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public Map isPartyGroupPartyIdPartnerPermissionCheck()
{
    final String module = "isPartyGroupPartyIdPartnerPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    GenericValue partyRole = delegator.findOne("PartyRole", ["partyId": partyGroupPartyId, "roleTypeId": "PARTNER"], true);

    if(partyRole != null)
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
