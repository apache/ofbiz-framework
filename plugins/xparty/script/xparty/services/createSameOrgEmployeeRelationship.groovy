


import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map createSameOrgEmployeeRelationship()
{
    final String module = "createSameOrgEmployeeRelationship";
    Map result = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    //uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    String partyId = parameters.partyId;
    String roleTypeId = parameters.roleTypeId;

    // Check that userLogin.partyId is a manager
    if (!HierarchyUtils.checkPartyRole(delegator, userLogin.partyId, "MANAGER"))
    {
        return ServiceUtil.returnFailure(uiLabelMap.PartyNotManager);
    }

    // Get the partyGroupId for userLogin.  Should be the same as parameters.partyId as we already called isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck
    result = runService("getPartyGroupForPartyId", [partyId: userLogin.partyId]);
    String partyGroupPartyId = result.organizationPartyGroup.partyId;

    if (!HierarchyUtils.checkPartyRole(delegator, partyId, roleTypeId))
    {
        result = dispatcher.runSync("createPartyRole", [userLogin: system, partyId: partyId, roleTypeId: roleTypeId]);
    }

    result = dispatcher.runSync("createPartyRelationship", [userLogin: system, partyIdFrom: partyGroupPartyId, roleTypeIdFrom: "_NA_", partyIdTo: partyId, roleTypeIdTo: roleTypeId, partyRelationshipTypeId: "EMPLOYMENT"]);
    return result;
}
