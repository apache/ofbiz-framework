

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import com.fidelissd.hierarchy.HierarchyUtils;

private GenericValue getAccountLeadForPartyGroup(GenericValue partyGroup) {
    final String module = "getAccountLead";
    List partyRelationships = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", [partyIdTo: partyGroup.partyId, partyRelationshipTypeId: "ACCOUNT", roleTypeIdFrom: "ACCOUNT_LEAD"], null, false));
    if(UtilValidate.isEmpty(partyRelationships))
        return null;
    return partyRelationships.first().getRelatedOne("FromParty", true);
}


public Map getAccountLeadForPartyId() {
    final String module = "getAccountLeadForPartyId";
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    // First check and see if partyId exists
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, parameters.partyId)
    if(UtilValidate.isEmpty(party))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist);

    if( UtilValidate.isEmpty(partyId) )
        return ServiceUtil.returnError(uiLabelMap.HierarchyInvalidArgumentError);

    // Get PartyGroup for the partyId
    Map serviceResult = runService("getPartyGroupForPartyId", [partyId : partyId]);
    GenericValue partyIdPartyGroup = serviceResult.organizationPartyGroup;

    // Get the Account Lead for that partyId
    GenericValue accountLead = getAccountLeadForPartyGroup(partyIdPartyGroup);
    if(UtilValidate.isEmpty(accountLead))
        return ServiceUtil.returnError(uiLabelMap.PartyIdNotInPartyGroup)
    result.put("accountLead", accountLead);
    return result;
}
