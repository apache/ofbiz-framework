

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.service.ServiceUtil

public Map doesPartyIdEqualUserLoginPartyIdPermissionCheck()
{
    final String module = "doesPartyIdEqualUserLoginPartyIdPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    //uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    String callerPartyId = userLogin.partyId;

    if (callerPartyId == parameters.partyId)
    {
        hasPermission = true;
    }
    else
    {
        result.put("failMessage", uiLabelMap.HierarchyPermissionError);
    }

    Debug.logInfo("hasPermission: " + hasPermission, module);

    result.put("hasPermission", hasPermission);
    return result;
}
