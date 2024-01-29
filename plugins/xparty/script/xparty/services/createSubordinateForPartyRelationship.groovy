


import com.fidelissd.hierarchy.HierarchyUtils
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

public Map createSubordinateForPartyRelationship() {
    final String module = "createSubordinateForPartyRelationship";
    Map result = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);
    
    String managerPartyId  = parameters.managerPartyId;
    String subordinatePartyId = parameters.subordinatePartyId;
    String subordinateRoleTypeId = parameters.subordinateRoleTypeId;

    if(!HierarchyUtils.checkPartyRole(delegator, subordinatePartyId, subordinateRoleTypeId))
    {
        Map createPartyRoleResponse = dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : subordinatePartyId , roleTypeId : subordinateRoleTypeId]);
        
        if(!ServiceUtil.isSuccess(createPartyRoleResponse))
        {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
        }
    }
    
    Map serviceResult = dispatcher.runSync("createPartyRelationship", [userLogin : system, partyIdFrom : managerPartyId , partyIdTo : subordinatePartyId, roleTypeIdFrom : "MANAGER", roleTypeIdTo : subordinateRoleTypeId , partyRelationshipTypeId : "REPORTS_TO"]);
    return result;
}
