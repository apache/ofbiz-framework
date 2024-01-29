


import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public Map isPartyGroupPartyIdNotSupplierPermissionCheck()
{
    final String module = "isPartyGroupPartyIdSupplierPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    GenericValue partyRole = delegator.findOne("PartyRole", ["partyId": partyGroupPartyId, "roleTypeId": "SUPPLIER"], true);

    if(partyRole == null)
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
