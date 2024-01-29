


import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

import com.fidelissd.hierarchy.HierarchyUtils;
import com.fidelissd.hierarchy.role.PartnerRoles;
import com.fidelissd.hierarchy.role.InternalPersonRoles;

public Map createAccountLeadForSupplierPartnerPartyGroupRelationship() {
    final String module = "createAccountLeadForSupplierPartnerPartyGroupRelationship";
    String accountLeadPartyId  = parameters.accountLeadPartyId;
    String partyIdTo = parameters.supplierPartyGroupPartyId;
    String roleTypeIdTo = parameters.roleTypeIdTo;
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    if(!(roleTypeIdTo.equals(PartnerRoles.SUPPLIER.toString()) || roleTypeIdTo.equals(PartnerRoles.PARTNER.toString()))) {
        return ServiceUtil.returnFailure("roleTypeIdTo argument does not have the SUPPLIER or PARTNER Role");
    }
    //check if the accountLeadPartyId has ACCOUNT_LEAD role, and create if not
    if(!HierarchyUtils.checkPartyRole(delegator, accountLeadPartyId, InternalPersonRoles.ACCOUNT_LEAD.toString())) {
        dispatcher.runSync("createPartyRole", [userLogin : system, partyId  : accountLeadPartyId , roleTypeId : InternalPersonRoles.ACCOUNT_LEAD.toString()]);
    }
    //Create relationship
    Map serviceResult = dispatcher.runSync("createPartyRelationship" , [userLogin : system, partyIdTo : partyIdTo , partyIdFrom : accountLeadPartyId, roleTypeIdFrom : InternalPersonRoles.ACCOUNT_LEAD.toString(), roleTypeIdTo : roleTypeIdTo ,partyRelationshipTypeId : "ACCOUNT"]);
    return serviceResult;
}
