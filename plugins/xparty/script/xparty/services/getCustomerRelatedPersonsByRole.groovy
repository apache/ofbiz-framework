


import com.fidelissd.hierarchy.role.CustomerPersonRoles
import com.fidelissd.hierarchy.role.HierarchyRoleUtils
import com.simbaquartz.xcommon.collections.FastList
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map getCustomerRelatedPersonsByRole() {
    final String module = "getCustomerRelatedPersonsByRole";
    Map serviceResult = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    // First, check and see if roleTypeId is defined in CustomerPersonRoles
    if(!HierarchyRoleUtils.roleTypeIds(CustomerPersonRoles.class).contains(parameters.roleTypeId))
        return ServiceUtil.returnFailure(UtilProperties.getMessage("HierarchyErrorUiLabels", "CustomerPersonRoleDoesNotExist", UtilMisc.toMap("roleTypeId", parameters.roleTypeId), locale));

    // Second check and see if partyGroupPartyId exists
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, parameters.partyGroupPartyId);
    if(UtilValidate.isEmpty(party))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist);
    
    // Third Check and make sure this is a PARTY_GROUP.
    if(HierarchyUtils.getPartyType(party) != "GOVERNMENT_LOC" &&
            HierarchyUtils.getPartyType(party) != "GOVERNMENT_AGENCY" &&
            HierarchyUtils.getPartyType(party) != "GOVERNMENT_ORG" &&
            HierarchyUtils.getPartyType(party) != "PARTY_GROUP")
    {
        return ServiceUtil.returnError(uiLabelMap.WrongPartyTypePerson);
    }

    Map getRelatedPartiesResult = dispatcher.runSync("getRelatedParties", [userLogin : system, "partyIdFrom": parameters.partyGroupPartyId, "recurse": "Y", "includeFromToSwitched" : "N", partyRelationshipTypeId:parameters.partyRelationshipTypeId]);
    if (ServiceUtil.isError(getRelatedPartiesResult))
    {
        Debug.logError(ServiceUtil.getErrorMessage(getRelatedPartiesResult), module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getRelatedPartiesResult));
    }
    
    // Get a unique list of the relatedParties.
    List<String> relatedParties = getRelatedPartiesResult.relatedPartyIdList.unique();

    //filter out parties with only a given role
    FastList<GenericValue> relatedPersonsList = FastList.newInstance();
    relatedParties.each { String relatedPartyId ->
        GenericValue relatedParty = HierarchyUtils.getPartyByPartyId(delegator, relatedPartyId);
        
        //party has eligible role
        if( HierarchyUtils.checkPartyRole(delegator, relatedPartyId, parameters.roleTypeId) && HierarchyUtils.isPerson(relatedParty) ){
            GenericValue personWithRole = delegator.findOne("Person", [partyId : relatedPartyId], false);
            
            relatedPersonsList.add(personWithRole);
        }
    }
    
    serviceResult.put("relatedPersonsList", relatedPersonsList);
    return serviceResult;
}
