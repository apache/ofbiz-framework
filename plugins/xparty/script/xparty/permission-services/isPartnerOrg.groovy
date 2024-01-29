


import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map isPartnerOrg()
{
    final String module = "isPartnerOrg";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);

    // Check if party is part of the Partner Organization PartyGroup
    Map serviceResult = runService("getPartyGroupForPartyId", [partyId: userLogin.partyId]);
    // Check that we got success and not failure.
    if(ServiceUtil.isFailure(serviceResult)) {
        //return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "HierarchyPartnerLoginError", locale));
        return serviceResult;
    }

    if (!HierarchyUtils.checkPartyRole(serviceResult.organizationPartyGroup, "PARTNER"))
    {
        result.put("failMessage", uiLabelMap.PartyNotPartnerOrg);
        result.put("hasPermission", hasPermission);
            return result;
    }
    else
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
