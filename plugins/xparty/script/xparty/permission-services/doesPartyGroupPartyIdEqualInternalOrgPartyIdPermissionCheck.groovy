


import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public Map doesPartyGroupPartyIdEqualInternalOrgPartyIdPermissionCheck()
{
    final String module = "doesPartyGroupPartyIdEqualInternalOrgPartyIdPermissionCheck";
    Boolean hasPermission = false;
    Map<String, Object> result = ServiceUtil.returnSuccess();

    String InternalOrgPartyId = EntityUtilProperties.getPropertyValue("general.properties", "ORGANIZATION_PARTY", delegator);
    hasPermission = (parameters.partyGroupPartyId == InternalOrgPartyId);

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
