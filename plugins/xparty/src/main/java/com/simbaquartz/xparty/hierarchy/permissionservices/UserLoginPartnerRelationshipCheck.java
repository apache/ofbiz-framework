

package com.simbaquartz.xparty.hierarchy.permissionservices;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.ofbiz.entity.util.EntityUtil.filterByDate;

public class UserLoginPartnerRelationshipCheck {

    public static final String module = UserLoginPartnerRelationshipCheck.class.getName();
    public static final String resource = "HierarchyUiLabels";
    public static final String resourceError = "HierarchyErrorUiLabels";

    private static GenericValue getEmploymentOrAgentRelationship(String partyId, GenericValue partyGroup) throws Exception {
        final String module = "getEmploymentOrAgentRelationship";
        Delegator delegator = partyGroup.getDelegator();

        // Search for relationships
        EntityExpr condnRoleTypeIdFrom = EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "_NA_");
        EntityExpr condnPartyIdTo = EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId);

        EntityConditionList<EntityExpr> exprListpartyRelationshipTypeIdOr = EntityCondition.makeCondition(
                UtilMisc.toList(
                        EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT"),
                        EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "AGENT")),
                EntityOperator.OR);

        EntityConditionList<EntityCondition> mainCond = EntityCondition.makeCondition(
                UtilMisc.toList(condnRoleTypeIdFrom, condnPartyIdTo, exprListpartyRelationshipTypeIdOr),
                EntityOperator.AND
        );
        List<GenericValue> partyRelationships = filterByDate(delegator.findList("PartyRelationship", mainCond, null, null, null, false));

        if(UtilValidate.isEmpty(partyRelationships))
            throw new NullPointerException();

        return partyRelationships.get(0);
    }


    private static GenericValue getPartnerPartyGroupToInternalOrgRelationship(GenericValue partyGroup) throws Exception {
        final String module = "getEmploymentOrAgentRelationship";
        Delegator delegator = partyGroup.getDelegator();
        String internalOrgPartyId = UtilProperties.getPropertyValue("general", "ORGANIZATION_PARTY");

        List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship",
                UtilMisc.toMap("partyIdFrom", internalOrgPartyId,
                "roleTypeIdFrom", "DISTRIBUTOR",
                "partyRelationshipTypeId", "PARTNERSHIP",
                "partyIdTo", partyGroup.get("partyId"),
                "roleTypeIdTo", "PARTNER"),
                null, false);
        partyRelationships = EntityUtil.filterByDate(partyRelationships);

        if(UtilValidate.isEmpty(partyRelationships))
            throw new NullPointerException();

        return partyRelationships.get(0).getRelatedOne("FromParty", true);
    }

    public static Map<String, Object> userLoginPartnerRelationshipCheckService(DispatchContext dctx, Map<String, ? extends Object> context) {
        final String module = "userLoginPartnerRelationshipCheck";
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue system = HierarchyUtils.getUserLogin(delegator, "system");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> result;
        try {
            //result = dispatcher.runSync("isPartnerOrg", UtilMisc.toMap("userLogin", context.get("userLogin"))); // Is userLogin a partner?
            result = dispatcher.runSync("isPartnerOrg", context); // Is userLogin a partner?
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        // Check the result.
        if(ServiceUtil.isFailure(result)) {
            //return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale));
            return result;
        }
        boolean isPartnerOrg = (Boolean) result.get("hasPermission"); // save off boolean so we can clear the result map.
        // Clear the result map so it cane be reused, and doesn't throw org.apache.ofbiz.service.GenericServiceException: Outgoing
        // result (in runSync : userLoginPartnerRelationshipCheck) does not match expected requirements (Unknown parameter found:
        // [userLoginPartnerRelationshipCheck.failMessage]Unknown parameter found: [userLoginPartnerRelationshipCheck.hasPermission])
        result.clear();

        try {
            result = dispatcher.runSync("isInternalOrg", context); // Is userLogin a Internal?
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        if(ServiceUtil.isFailure(result)) {
            //return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale));
            return result;
        }
        boolean isInternalOrg = (Boolean) result.get("hasPermission"); // save off boolean so we can clear the result map.
        result.clear(); // Clear the result map so it cane be reused.

        // Check that this person isn't magically both part of the Partner and Internal Orgs.
        // We don't support customer login at this point.
        if(!(isPartnerOrg ^ isInternalOrg)){
            Debug.logWarning(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale), module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale));
        }

        if(isPartnerOrg)
        {
            try {
                result = dispatcher.runSync("getPartyGroupForPartyId", UtilMisc.toMap("userLogin", userLogin, "partyId", (String) userLogin.get("partyId"))); // Get userLogin's PartyGroup.
            } catch (GenericServiceException e) {
                Debug.logError(e, e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
            GenericValue partyIdPartyGroup = (GenericValue) result.get("organizationPartyGroup"); // save off organizationPartyGroup so we can clear the result map.
            result.clear(); // Clear the result map so it cane be reused.

            // Do the following in a try/catch block just in case something wacky happens, we can returnError with NullPointerException due to there not being a relationship.
            try
            {
                // Get a List of current EMPLOYMENT/AGENT relationships between userLogin and their Org.
                GenericValue partyGroupRelationship = getEmploymentOrAgentRelationship((String) userLogin.get("partyId"), partyIdPartyGroup);
                // If List is empty, return Failure.  This will cause the userLogin service to return failure and the user won't be logged in.
                if (UtilValidate.isEmpty(partyGroupRelationship))
                {
                    Debug.logError(UtilProperties.getMessage(resourceError, "HierarchyLoginFailureNotPartner", locale), module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "HierarchyLoginFailureNotPartner", locale));
                }

                // Get a List of current PARTNERSHIP relationships between userLogin's Org and the ORGANIZATION_PARTY
                GenericValue partyGroupFsdRelationship = getPartnerPartyGroupToInternalOrgRelationship(partyIdPartyGroup); // TODO Looks like this returned PartyGroup
                // If List is empty, return Failure.  This will cause the userLogin service to return failure and the user won't be logged in.
                if (UtilValidate.isEmpty(partyGroupFsdRelationship))
                {
                    Debug.logError(UtilProperties.getMessage(resourceError, "HierarchyLoginFailureNotPartner", locale), module);
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resourceError, "HierarchyLoginFailureNotPartner", locale));
                }
            }
            catch (Exception e)
            {
                Debug.logError(e, e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        }

        result.put("userLogin", userLogin);
        return result;
    }
}
