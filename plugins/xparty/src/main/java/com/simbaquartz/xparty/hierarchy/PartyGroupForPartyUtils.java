/*****************************************************************************************
 * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Forrest Rae <forrest.rae@fidelissd.com>, November 2015
 ************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************************/

package com.simbaquartz.xparty.hierarchy;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;
import com.simbaquartz.xparty.hierarchy.role.*;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityFieldMap;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.*;

public class PartyGroupForPartyUtils {

    public static final String module = PartyGroupForPartyUtils.class.getName();
    public static final String resource = "XpartyUiLabels";
    public static final String resource_error = "XpartyErrorUiLabels";

    // This utility gets the PartyGroup relationship for a partyId of a Person who is a particular type of user as defined by the role enum that is passed in via enumClass.
    private static <T extends Enum<T> & HierarchyRolesEnum> List<GenericValue> getPartyRelationshipForPartyIdWithRoleTypeEnum(Delegator delegator, String partyId, Class<T> enumClass) {
        List<EntityFieldMap> expressionListAnd = new ArrayList<>();
        final EnumSet<T> enums = EnumSet.allOf(enumClass);
        Iterator<T> enumsIt = enums.iterator();

        while (enumsIt.hasNext()) {
            T role = enumsIt.next();
            if (role.isEmploymentRole()) {
                Map<String, String> searchCriteria =
                        UtilMisc.toMap(
                                //"roleTypeIdFrom", "_NA_",
                                "partyIdTo", partyId,
                                "roleTypeIdTo", role.name(),
                                "partyRelationshipTypeId", role.getPartyRelationshipTypeId());
                EntityFieldMap loopCondition = EntityCondition.makeCondition(searchCriteria, EntityOperator.AND);
                expressionListAnd.add(loopCondition);
            }
        }
        EntityConditionList mainCond = EntityCondition.makeCondition(expressionListAnd, EntityOperator.OR);

        List<GenericValue> partyRelationships = null;
        try {
            Set<String> fieldsToSelect = UtilMisc.toSet("partyIdFrom", "roleTypeIdTo");
            partyRelationships = delegator.findList("PartyRelationship", mainCond, fieldsToSelect, null, null, false);
        } catch (GenericEntityException e) {
            return null;
        }

        return EntityUtil.filterByDate(partyRelationships);
    }

    /**
     * Deprecated. don't use this
     * @param delegator
     * @param party
     * @return
     */
    @Deprecated
    public static GenericValue getPartyGroupForPartyId_depr(Delegator delegator, GenericValue party) {
        Map employeeConditionMap = UtilMisc.toMap("roleTypeIdFrom", "_NA_",
                "partyIdTo", party.getString("partyId"),
                "roleTypeIdTo", "EMPLOYEE",
                "roleTypeIdFrom", "_NA_",
                "partyRelationshipTypeId", "EMPLOYMENT");

        GenericValue partyGroup = null;
        try {
            GenericValue partyGroupRelationship = EntityQuery.use(delegator).from("PartyRelationship").where(employeeConditionMap).queryFirst();

            if (UtilValidate.isNotEmpty(partyGroupRelationship)) {
                partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyGroupRelationship.getString("partyIdFrom")).queryOne();
            }
        } catch (GenericEntityException e) {
            Debug.logError("An error occurred while trying to retrieve party group relationship.", module);
        }

        return partyGroup;
    }

    // getPartyGroupForPartyId service.
    public static Map<String, Object> getPartyGroupForPartyIdService(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String partyId = (String) context.get("partyId");
        Map<String, Object> result = new HashMap<>();

        ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("XpartyErrorUiLabels", locale);
        uiLabelMap.addBottomResourceBundle("CommonUiLabels");

        // First check and see if partyId exists
        GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
        if (UtilValidate.isEmpty(party))
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "PartyDoesNotExist", locale));

        // Second check and see if partyId is a PartyGroup
        if (HierarchyUtils.isPartyGroup(party)) {
            result.put("organizationPartyGroup", party);
            return result;
        }

        GenericValue partyGroup = getPartyGroupForPartyId(party);
        if (UtilValidate.isNotEmpty(partyGroup)) {
            result.put("organizationPartyGroup", partyGroup);
        } else
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource_error, "XpartyPartnerLoginError", locale));

        return result;
    }

    // Utility method called by getPartyGroupForPartyId service.
    public static GenericValue getPartyGroupForPartyId(GenericValue party) {
        if (UtilValidate.isEmpty(party))
            return null;

        Delegator delegator = party.getDelegator();

        // Get a list of all the PartyGroups this person is related too.
        //List<GenericValue> partyRelationships = new FastList<GenericValue>();
        LinkedHashSet<GenericValue> partyRelationships = new LinkedHashSet<GenericValue>(); // LinkedHashSet makes sure it's unique.
        partyRelationships.addAll(getPartyRelationshipForPartyIdWithRoleTypeEnum(delegator, party.get("partyId").toString(), EmployerPersonRoles.class));
        partyRelationships.addAll(getPartyRelationshipForPartyIdWithRoleTypeEnum(delegator, party.get("partyId").toString(), AuthenticatedUserRoles.class));

        if (UtilValidate.isEmpty(partyRelationships)) {
            Debug.logError("ERROR: getPartyGroupForPartyId() partyRelationships is null.", module);
            return null;
        }

        // Then check the roles for each of these party groups.
        GenericValue partyGroup = null;
        for (GenericValue partyRelationship : partyRelationships) {
            // Get the PartyGroup
            try {
                partyGroup = delegator.findOne("PartyGroup", UtilMisc.toMap("partyId", partyRelationship.get("partyIdFrom").toString()), true);
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: partyRelationships is greater than 1 for partyIdFrom: " + partyRelationship.get("partyIdFrom").toString(), module);
                return null;
            }

            if (UtilValidate.isNotEmpty(partyGroup)) {
                // Check and see if this is a
                if (HierarchyUtils.checkPartyRolesAnd(partyGroup, HierarchyRoleUtils.roleTypeIds(PartyGroupTypes.class))) {
                    // We shouldn't have more than one.
                    if (partyRelationships.size() > 1) {
                        Debug.logError("ERROR: partyRelationships is greater than 1 for partyIdFrom: " + partyRelationship.get("partyIdFrom").toString(), module);
                        return null;
                    }
                    return partyGroup;
                } else if (HierarchyUtils.checkPartyRole(partyGroup, PartyGroupTypes.INTERNAL_ORGANIZATIO.name())) {
                    // We shouldn't have more than one.
                    //if (partyRelationships.size() > 1)
                    //    return null;

                    return partyGroup;
                } else if (HierarchyUtils.checkPartyRole(partyGroup, PartyGroupTypes.ORGANIZATION_ROLE.name())) {
                    // We shouldn't have more than one.
                    if (partyRelationships.size() > 1)
                        return null;

                    return partyGroup;
                } else if (HierarchyUtils.checkPartyRolesOr(partyGroup, HierarchyRoleUtils.roleTypeIds(CustomerRoles.class))) {
                    // If partyId is a CONTRACTING_OFFICER, it likely has multiple relationships.
                    // Just return the first
                    // TODO Traverse up the hierarchy and return the highest.
                    return partyGroup;
                }
            }
        }

        return null;
    }

    public static Long getPartyGroupMembersCount(Delegator delegator, String accountId) {

        //Default count 1 as single user will be always there for the party
        Long partyGroupMembersCount = 1l;
        List<GenericValue> partyRelationships = new LinkedList<>();
        partyRelationships.addAll(
            getPartyRelationshipForPartyIdWithRoleTypeEnum(delegator, accountId,
                EmployerPersonRoles.class));

        if (UtilValidate.isNotEmpty(partyRelationships)) {
            partyRelationships = EntityUtil.filterByDate(partyRelationships);

            if (UtilValidate.isNotEmpty(partyRelationships)) {

                // LinkedHashSet makes sure it's unique.
                LinkedHashSet<GenericValue> partyGroupMembersRelationships = new LinkedHashSet<GenericValue>();

                partyGroupMembersRelationships.addAll(partyRelationships);
                return (long) partyGroupMembersRelationships.size();
            }
        }

        return partyGroupMembersCount;
    }
}