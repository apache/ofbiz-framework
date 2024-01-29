

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

import com.fidelissd.hierarchy.HierarchyUtils
import com.fidelissd.hierarchy.role.InternalPersonRoles;

public Map createOwnerRelationship() {
    final String module = "createOwnerRelationship";

    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    String partyId  = parameters.partyId;
    String partyGroupPartyId = parameters.partyGroupPartyId;

    if(!HierarchyUtils.checkPartyRole(delegator, partyId, InternalPersonRoles.OWNER.toString()))
    {
        Map createPartyRoleResponse = dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : partyId , roleTypeId : InternalPersonRoles.OWNER.toString()]);
        if(!ServiceUtil.isSuccess(createPartyRoleResponse))
        {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
        }
    }

    Map serviceResult = dispatcher.runSync("createPartyRelationship", [userLogin : system, partyIdFrom : partyGroupPartyId , partyIdTo : partyId, roleTypeIdFrom : "_NA_", roleTypeIdTo : InternalPersonRoles.OWNER.toString(), partyRelationshipTypeId : "OWNER"]);
    return serviceResult;
}
