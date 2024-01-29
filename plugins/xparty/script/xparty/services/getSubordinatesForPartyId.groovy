


import com.simbaquartz.xcommon.collections.FastList
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import com.simbaquartz.xcommon.util.hierarchy.HierarchyUtils;

public Map getSubordinatesForPartyId() {
    final String module = "getSubordinatesForPartyId";
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    Map getRelatedPartiesResult = dispatcher.runSync("getRelatedParties", ["userLogin" : userLogin, "partyIdFrom": parameters.partyId, "recurse": parameters.recurse, "roleTypeIdTo": parameters.roleTypeIdTo, "partyRelationshipTypeId": "REPORTS_TO"]);
    if (ServiceUtil.isError(getRelatedPartiesResult))
    {
        Debug.logError(ServiceUtil.getErrorMessage(getRelatedPartiesResult), module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getRelatedPartiesResult));
    }
    List<String> relatedParties = getRelatedPartiesResult.relatedPartyIdList;

    // If recurse, we need to follow ACCOUNT_LEAD relationships
    if(parameters.recurse.equals("Y"))
    {
        List<String> relatedPartiesToAdd = FastList.newInstance();

        for (String relatedPartyId : relatedParties)
        {
            // If user is an ACCOUNT_LEAD, follow the relationship.
            Map getAccountLeadPartyRelationshipsForPartyIdResult;
            if (HierarchyUtils.checkPartyRole(delegator, relatedPartyId, "ACCOUNT_LEAD"))
            {
                // Get the ACCOUNT_LEAD relationships.
                getAccountLeadPartyRelationshipsForPartyIdResult = dispatcher.runSync("getAccountLeadPartyRelationshipsForPartyId", [userLogin: userLogin, "partyId": relatedPartyId]);

                // Iterate over each relationship.
                for (GenericValue partyRelationship : getAccountLeadPartyRelationshipsForPartyIdResult.partyRelationships)
                {
                    //Get the party to which this relatedPartyId is an ACCOUNT_LEAD for.
                    GenericValue party = partyRelationship.getRelatedOne("ToParty", true);
                    // Determine if they're a Person, rather than a PartyGroup
                    if(HierarchyUtils.isPerson(party))
                    {
                        // Get the related parties to this Person.
                        Map getRelatedPartiesToAccountResult = dispatcher.runSync("getRelatedParties", [userLogin: system, "partyIdFrom": partyRelationship.partyIdTo, "recurse": parameters.recurse, "roleTypeIdTo": parameters.roleTypeIdTo, "partyRelationshipTypeId": "REPORTS_TO"]);
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
