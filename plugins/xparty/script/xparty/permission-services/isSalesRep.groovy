

import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map isSalesRep()
{
    final String module = "isSalesRep";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);

    // First check that party is a SALES_REP
    if (!HierarchyUtils.checkPartyRole(delegator, userLogin.partyId, "SALES_REP"))
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
