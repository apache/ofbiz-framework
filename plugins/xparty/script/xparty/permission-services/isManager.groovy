

import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map isManager()
{
    final String module = "isManager";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);

    // Check that party is a manager
    if (!HierarchyUtils.checkPartyRole(delegator, userLogin.partyId, "MANAGER"))
    {
        result.put("failMessage", uiLabelMap.PartyNotManager);
        result.put("hasPermission", hasPermission);
        return result;
    }
    else
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
