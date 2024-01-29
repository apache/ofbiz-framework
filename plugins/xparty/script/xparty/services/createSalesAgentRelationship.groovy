


import com.fidelissd.hierarchy.HierarchyUtils
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

public Map createSalesAgentRelationship() {
    final String module = "createSalesAgentRelationship";
    Map serviceResult = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    String partyId  = parameters.partyId;
    String partyGroupPartyId = parameters.partyGroupPartyId;

    if(!HierarchyUtils.checkPartyRole(delegator, partyId, "AGENT"))
    {
        Map createPartyRoleResponse = dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : partyId , roleTypeId : "AGENT"]);

        if(!ServiceUtil.isSuccess(createPartyRoleResponse))
        {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
        }
    }

    serviceResult = dispatcher.runSync("createPartyRelationship", [userLogin : system, partyIdFrom : partyGroupPartyId , partyIdTo : partyId, roleTypeIdFrom : "_NA_", roleTypeIdTo : "AGENT" , partyRelationshipTypeId : "AGENT"]);
    return serviceResult;
}
