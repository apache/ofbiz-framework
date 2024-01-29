

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionList
import org.apache.ofbiz.entity.condition.EntityExpr
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil

public Map specialConditionPermissionCheck()
{
    final String module = "specialConditionPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    //uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    if(userLogin.partyId.equals("system")) {
        // Always allow system user
        result.put("hasPermission", true)
        return result
    }

    String callerPartyId = userLogin.partyId;

    // First get the Internal Organizations
    List internalOrganizationPartyRoles = delegator.findByAnd("PartyRole", [roleTypeId: "INTERNAL_ORGANIZATIO"], null, false);

    //Iterate through all internal organizations building a list of Managers and Owners
    for (internalOrganizationPartyRole in internalOrganizationPartyRoles)
    {
        // If the userLogin.partyId in the role of Manager has an Employment relationship to INTERNAL_ORGANIZATION in the role of _NA_
        // If the userLogin.partyId in the role of Owner has an Owner relationship to INTERNAL_ORGANIZATION in the role of _NA_
        EntityExpr condnPartyIdTo = EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, callerPartyId);

        List exprListRoleType = [
                EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "MANAGER"),
                EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "OWNER")
        ];
        EntityConditionList exprListOr = EntityCondition.makeCondition(exprListRoleType, EntityOperator.OR);

        EntityExpr condnPartyRelationTypeId = EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT");
        EntityExpr condnPartyIdFrom = EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, internalOrganizationPartyRole.partyId);
        EntityExpr condnRoleTypeIdFrom = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "_NA_");

        EntityConditionList mainCond = EntityCondition.makeCondition(
                [condnPartyIdTo, exprListOr, condnPartyRelationTypeId, condnPartyIdFrom, condnRoleTypeIdFrom],
                EntityOperator.AND
        );

        List managersAndOwners = EntityUtil.filterByDate(delegator.findList("PartyRelationship", mainCond, null, null, null, false));

        if (UtilValidate.isNotEmpty(managersAndOwners))
        {
            hasPermission = true;
            break;
        }
    }

    if (!hasPermission)
    {
        result.put("failMessage", uiLabelMap.HierarchyPermissionError);
    }

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
