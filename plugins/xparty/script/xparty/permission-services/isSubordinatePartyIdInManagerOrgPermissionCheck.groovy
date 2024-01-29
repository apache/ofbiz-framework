

import com.fidelissd.hierarchy.HierarchyUtils
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

public Map isSubordinatePartyIdInManagerOrgPermissionCheck()
{
    final String module = "isSubordinatePartyIdInManagerOrg";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    // First check and see if managerPartyId exists
    GenericValue managerParty = HierarchyUtils.getPartyByPartyId(delegator, parameters.managerPartyId)
    if (UtilValidate.isEmpty(managerParty))
    {
        result.put("failMessage", uiLabelMap.PartyDoesNotExist);
        result.put("hasPermission", hasPermission);
        return result;
    }
    // Second check and see if subordinatePartyId exists
    GenericValue subordinateParty = HierarchyUtils.getPartyByPartyId(delegator, parameters.subordinatePartyId)
    if (UtilValidate.isEmpty(subordinateParty))
    {
        result.put("failMessage", uiLabelMap.PartyDoesNotExist);
        result.put("hasPermission", hasPermission);
        return result;
    }

    // Third Check that managerPartyId is a manager
    if (!HierarchyUtils.checkPartyRole(managerParty, "MANAGER"))
    {
        result.put("failMessage", uiLabelMap.PartyNotManager);
        result.put("hasPermission", hasPermission);
        return result;
    }

    // Because getSubordinatesForPartyId gets called here, the calling userLogin must meet the permission-service requirements of getSubordinatesForPartyId.
    Map serviceResult = runService("getSubordinatesForPartyId", [partyId: managerParty.partyId, recurse: "Y"]);
    List subordinatePartyIdList = serviceResult.subordinatePartyIdList;

    if (subordinatePartyIdList.contains(subordinateParty.partyId))
        hasPermission = true;

    if (!hasPermission)
    {
        result.put("failMessage", uiLabelMap.HierarchyPermissionError);
    }

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
