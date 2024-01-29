

import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;

public Map upgradeSupplierToPartnerRelationship() {
    final String module = "upgradeSupplierToPartnerRelationship";
    Map result = ServiceUtil.returnSuccess();
    String supplierPartyId = parameters.supplierPartyId;
    String internalOrgPartyId = UtilProperties.getPropertyValue("general", "ORGANIZATION_PARTY");
    
    partyRoles = delegator.findByAnd("PartyRole" , [partyId : supplierPartyId , roleTypeId : "PARTNER"] , null,false);
    
    //if required role is not applied already, apply one
    if(UtilValidate.isEmpty(partyRoles))
    {
        createRoleResult = runService("createPartyRole" , [partyId : supplierPartyId , roleTypeId : "PARTNER"]);
    }
    
    result = runService("createPartyRelationship", [userLogin : userLogin, partyIdTo : supplierPartyId, roleTypeIdTo : "PARTNER", partyRelationshipTypeId : "PARTNERSHIP", partyIdFrom : internalOrgPartyId, roleTypeIdFrom:"DISTRIBUTOR"]);
    return result;
}