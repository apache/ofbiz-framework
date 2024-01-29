

import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map isInternalOrg()
{
    final String module = "isInternalOrg";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);

    if(userLogin.partyId.equals("system")) {
        // Always allow system user
        result.put("hasPermission", true)
        return result
    }

    // Check if party is part of the Internal Organization PartyGroup
    Map serviceResult = runService("getPartyGroupForPartyId", [partyId: userLogin.partyId]);
    // Check that we got success and not failure.
    if(ServiceUtil.isFailure(serviceResult)) {
        //return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "HierarchyPartnerLoginError", locale));
        return serviceResult;
    }

    if (!HierarchyUtils.checkPartyRole(serviceResult.organizationPartyGroup, "INTERNAL_ORGANIZATIO"))
    {
        result.put("failMessage", uiLabelMap.PartyNotInternalOrg);
        result.put("hasPermission", hasPermission);
        return result;
    }
    else
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
