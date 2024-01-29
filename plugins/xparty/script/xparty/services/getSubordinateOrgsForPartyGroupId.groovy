


import com.simbaquartz.xcommon.collections.FastList
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map getSubordinateOrgsForPartyGroupId() {
    final String module = "getSubordinateOrgsForPartyGroupId";
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    Map getSubordinateOrgPartiesResult = dispatcher.runSync("getRelatedParties", [userLogin : system, "partyIdFrom": parameters.partyGroupPartyId, "recurse": parameters.recurse, "roleTypeIdTo": parameters.roleTypeIdTo, "partyRelationshipTypeId": "ORG_ROLLUP"]);
    if (ServiceUtil.isError(getSubordinateOrgPartiesResult))
    {
        Debug.logError(ServiceUtil.getErrorMessage(getSubordinateOrgPartiesResult), module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getSubordinateOrgPartiesResult));
    }
    List<String> relatedParties = getSubordinateOrgPartiesResult.relatedPartyIdList;

    // If recurse, we need to follow GOVERNMENT_ORG relationships
    if(parameters.recurse.equals("Y"))
    {
        List<String> relatedPartiesToAdd = FastList.newInstance();

        for (String relatedPartyId : relatedParties)
        {
            // If user is an GOVERNMENT_ORG, follow the relationship.
            Map getGovOrgPartyRelationshipsForPartyGroupIdResult;
            if (HierarchyUtils.checkPartyRole(delegator, relatedPartyId, "GOVERNMENT_ORG"))
            {
                // Get the GOVERNMENT_ORG relationships.
                getGovOrgPartyRelationshipsForPartyGroupIdResult = dispatcher.runSync("getGovOrgPartyRelationshipsForPartyGroupId", [userLogin: system, "partyId": relatedPartyId]);

                // Iterate over each relationship.
                for (GenericValue partyRelationship : getGovOrgPartyRelationshipsForPartyGroupIdResult.partyRelationships)
                {
                    //Get the org party to which this relatedPartyId reports to 
                    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyRelationship.partyIdTo);
                    // Determine if they're a PartyGroup, rather than a Person 
                    if(HierarchyUtils.isPartyGroup(party))
                    {
                        // Get the related parties to this Org.
                        Map getRelatedPartiesToAccountResult = dispatcher.runSync("getRelatedParties", [userLogin: system, "partyIdFrom": partyRelationship.partyIdTo, "recurse": parameters.recurse, "roleTypeIdTo": parameters.roleTypeIdTo, "partyRelationshipTypeId": "ORG_ROLLUP"]);
                        // Add them to the list.
                        relatedPartiesToAdd.addAll(getRelatedPartiesToAccountResult.relatedPartyIdList);
                    }
                }
            }
        }

        relatedParties.addAll(relatedPartiesToAdd);
    }

    // Return a unique list of the relatedParties.
    serviceResult.put("subordinatePartyIdList", relatedParties.unique())
    return serviceResult;
}
