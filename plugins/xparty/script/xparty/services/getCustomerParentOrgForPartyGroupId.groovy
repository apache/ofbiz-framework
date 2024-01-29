/******************************************************************************************
 * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 * Unauthorized copying of this file, via any medium is strictly prohibited               *
 * Proprietary and confidential                                                           *
 * Written by Forrest Rae <forrest.rae@fidelissd.com>, December2015                       *
 ******************************************************************************************/


import com.fidelissd.hierarchy.role.CustomerRoles
import com.fidelissd.hierarchy.role.HierarchyRoleUtils
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

public Map getCustomerParentOrgForPartyGroupId() {
    final String module = "getCustomerParentOrgForPartyGroupId";
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    // First check and see if partyId exists
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, parameters.partyGroupPartyId)
    if(UtilValidate.isEmpty(party))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist);

    // Check and make sure this is a PARTY_GROUP.
    if(HierarchyUtils.getPartyType(party) != "GOVERNMENT_LOC" &&
            HierarchyUtils.getPartyType(party) != "GOVERNMENT_AGENCY" &&
            HierarchyUtils.getPartyType(party) != "GOVERNMENT_ORG" &&
            HierarchyUtils.getPartyType(party) != "PARTY_GROUP")
    {
        return ServiceUtil.returnError(uiLabelMap.WrongPartyTypePerson);
    }

    // Search for relationships
    EntityExpr condnPartyIdTo = EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, parameters.partyGroupPartyId);
    List exprListSubOrg = [
            condnPartyIdTo,
            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, HierarchyRoleUtils.roleTypeIds(CustomerRoles.class)),
            EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.IN, HierarchyRoleUtils.partyRelationshipTypeIds(CustomerRoles.class))
    ];

    EntityConditionList mainCond = EntityCondition.makeCondition(exprListSubOrg, EntityOperator.AND);
    List partyRelationships = EntityUtil.filterByDate(delegator.findList("PartyRelationship", mainCond, null, null, null, false));
    if(UtilValidate.isEmpty(partyRelationships))
        return ServiceUtil.returnFailure(uiLabelMap.PartyHasNoParentOrg);

    GenericValue parentOrg = delegator.findOne("Party", [partyId : partyRelationships.first().partyIdFrom], true );
    
    if(UtilValidate.isEmpty(parentOrg))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist);
        
    result.put("parentOrg", parentOrg);
    return result;
}
