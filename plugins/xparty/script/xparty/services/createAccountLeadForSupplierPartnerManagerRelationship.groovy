


import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

import com.fidelissd.hierarchy.HierarchyUtils;

public Map createAccountLeadForSupplierPartnerManagerRelationship() {
    final String module = "createAccountLeadForSupplierPartnerManagerRelationship";
    String accountLeadPartyId  = parameters.accountLeadPartyId;
    String managerPartyId = parameters.managerPartyId;
    String roleTypeIdTo = parameters.roleTypeIdTo;
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    // Manager Party must have MANAGER Role
    if(!HierarchyUtils.checkPartyRole(delegator, managerPartyId, "MANAGER")) {
        return ServiceUtil.returnFailure("Person does not have the MANAGER Role");
    }

    //check if the managerPartyId has roleTypeIdTo role, and create if not
    if(!HierarchyUtils.checkPartyRole(delegator, managerPartyId, roleTypeIdTo)) {
        dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : managerPartyId , roleTypeId : roleTypeIdTo]);
    }

    //check if the accountLeadPartyId has ACCOUNT_LEAD role, and create if not
    if(!HierarchyUtils.checkPartyRole(delegator, accountLeadPartyId, "ACCOUNT_LEAD")) {
        dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : accountLeadPartyId , roleTypeId : "ACCOUNT_LEAD"]);
    }

    //Create relationship
    Map serviceResult = runService("createPartyRelationship" , [userLogin : system,
                                                                partyIdTo : managerPartyId ,
                                                                partyIdFrom : accountLeadPartyId,
                                                                roleTypeIdFrom : "ACCOUNT_LEAD",
                                                                roleTypeIdTo : roleTypeIdTo,
                                                                partyRelationshipTypeId : "ACCOUNT"]);
    return serviceResult;
}
