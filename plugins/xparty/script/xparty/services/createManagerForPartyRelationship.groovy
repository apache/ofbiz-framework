import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import com.simbaquartz.xcommon.util.hierarchy.HierarchyUtils
import com.simbaquartz.xcommon.enums.AccountUserRoleTypesEnum

public Map createManagerForPartyRelationship() {
    final String module = "createManagerForPartyRelationship";
    Map serviceResult = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);
    
    String loginPartyId = userLogin.partyId;
    String managerPartyId = parameters.managerPartyId;
    String subordinatePartyId = parameters.subordinatePartyId;
    
    // Check that managerPartyId is a manager
    if (!HierarchyUtils.checkPartyRole(delegator, managerPartyId, AccountUserRoleTypesEnum.MEMBER.getRole()))
    {
        return ServiceUtil.returnError(uiLabelMap.PartyNotManager);
    }

	//check if subordinate already has a sales rep role, if not create one
	Debug.logInfo("Checking for sales rep role.", module);
	if (!HierarchyUtils.checkPartyRole(delegator, subordinatePartyId, "SALES_REP"))
    {
    	Debug.logInfo("Sales rep role not present for subordinate party id, creating one.", module);
        try
        {
	        serviceResult = dispatcher.runSync("createPartyRole", [userLogin : system, partyId : subordinatePartyId, roleTypeId : "SALES_REP"]);
            
            if (ServiceUtil.isError(serviceResult))
            {
                Debug.logError("An error occurred while trying to create role for party id : " + subordinatePartyId +
                        " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

                return ServiceUtil.returnError("Unable to create sales representative role for sub ordinate");
            }
        } catch (Exception e)
        {
                Debug.logError("An error occurred while trying to create role for party id : " + subordinatePartyId +
                        " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

            return ServiceUtil.returnError("Unable to create sales representative role for sub ordinate. Details : " + ServiceUtil.getErrorMessage(serviceResult));
        }
    }
    
    serviceResult = dispatcher.runSync("createPartyRelationship", [userLogin : system, partyIdFrom : managerPartyId, partyIdTo : subordinatePartyId, roleTypeIdFrom : AccountUserRoleTypesEnum.MEMBER.getRole(), roleTypeIdTo : "SALES_REP", partyRelationshipTypeId : "REPORTS_TO"]);
    return serviceResult;
}
