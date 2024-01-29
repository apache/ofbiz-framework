

package com.simbaquartz.xparty.hierarchy;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.util.*;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.*;
import org.apache.ofbiz.entity.Delegator;

import com.simbaquartz.xparty.hierarchy.role.CustomerPersonRoles;

public class HierarchyRelationshipUtils
{
    public static final String module = HierarchyRelationshipUtils.class.getName();
    public static final String resource = "HierarchyRelationshipUtils";

    // Not a service, utility routine.
    public static Map<String, Object> createGovCustPersonRelationship(HashMap context, String partyGroupPartyId, String partyId, CustomerPersonRoles role)
    {
        final String module = "createGovCustNonEmployeeRelationship";
        DispatchContext dctx = (DispatchContext) context.get("dctx");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> serviceResult = new HashMap<>();
        GenericValue system = HierarchyUtils.getUserLogin(delegator, "system");

        //check if subordinate already has role, if not create one
        if (!HierarchyUtils.checkPartyRole(delegator, partyId, role.name()))
        {
            //Role not present for partyId, creating one.
            try
            {
                serviceResult = dispatcher.runSync("createPartyRole", UtilMisc.toMap("userLogin", system, "partyId", partyId, "roleTypeId", role.name()));
                if (ServiceUtil.isError(serviceResult))
                {
                    return serviceResult;
                }
            }
            catch (GenericServiceException e)
            {
                Debug.logError(e, e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "PartyCannotCopyPartyContactMech",
                        UtilMisc.toMap("errorString", e.getMessage()), locale));
            }
        }

        // Create the relationship.
        try
        {
            serviceResult = dispatcher.runSync("createPartyRelationship", UtilMisc.toMap("partyIdFrom", partyGroupPartyId, "partyIdTo", partyId, "roleTypeIdFrom", "_NA_", "roleTypeIdTo", role.name(), "partyRelationshipTypeId", role.getPartyRelationshipTypeId(), "userLogin", system));
            if (ServiceUtil.isError(serviceResult))
            {
                return serviceResult;
            }
        }
        catch (GenericServiceException e)
        {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotCopyPartyContactMech",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        return serviceResult;
    }

    // Service
    public static Map<String, Object> getAccountLeadPartyRelationshipsForPartyId(DispatchContext dctx, Map<String, Object> context)
    {
        final String module = "getAccountLeadPartyRelationshipsForPartyId";
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String partyIdFrom = (String) context.get("partyId");
        String roleTypeIdFrom = "ACCOUNT_LEAD";
        String partyRelationshipTypeId = "ACCOUNT";

        List<EntityCondition> condList = new ArrayList<>();
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        condList.add(EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom));
        condList.add(EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId));
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try
        {
            partyRelationships = delegator.findList("PartyRelationship", condition, null, null, null, false);
            partyRelationships = EntityUtil.filterByDate(partyRelationships);
        }
        catch (GenericEntityException e)
        {
            Debug.logError(e, "Problem finding PartyRelationships.", module);
            return ServiceUtil.returnError("Problem finding PartyRelationships.");
        }

        result.put("partyRelationships", partyRelationships);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> getGovOrgPartyRelationshipsForPartyGroupId(DispatchContext dctx, Map<String, Object> context)
    {
        final String module = "getGovOrgPartyRelationshipsForPartyGroupId";
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String partyIdFrom = (String) context.get("partyGroupPartyId");
        String roleTypeIdFrom = "GOVERNMENT_ORG";
        String partyRelationshipTypeId = "ORG_ROLLUP";

        List<EntityCondition> condList = new ArrayList<>();
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        condList.add(EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom));
        condList.add(EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId));
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try
        {
            partyRelationships = delegator.findList("PartyRelationship", condition, null, null, null, false);
            partyRelationships = EntityUtil.filterByDate(partyRelationships);
        }
        catch (GenericEntityException e)
        {
            Debug.logError(e, "Problem finding PartyRelationships.", module);
            return ServiceUtil.returnError("Problem finding PartyRelationships.");
        }

        result.put("partyRelationships", partyRelationships);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> getPartyRelationshipsForPartyGroupId(DispatchContext dctx, Map<String, Object> context)
    {
        final String module = "getPartyRelationshipsForPartyGroupId";
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String partyIdFrom = (String) context.get("partyGroupPartyId");
        String partyIdTo = (String) context.get("partyId");

        List<EntityCondition> condList = new ArrayList<>();
        condList.add(EntityCondition.makeCondition("partyIdTo", partyIdTo));
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try
        {
            partyRelationships = delegator.findList("PartyRelationship", condition, null, null, null, false);
            partyRelationships = EntityUtil.filterByDate(partyRelationships);
        }
        catch (GenericEntityException e)
        {
            Debug.logError(e, "Problem finding PartyRelationships.", module);
            return ServiceUtil.returnError("Problem finding PartyRelationships.");
        }

        result.put("partyRelationships", partyRelationships);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> getSupplierAccountLeadPartyRelationshipsForPartyId(DispatchContext dctx, Map<String, Object> context)
    {
        final String module = "getSupplierAccountLeadPartyRelationshipsForPartyId";
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String partyIdFrom = (String) context.get("partyId");
        String roleTypeIdFrom = "ACCOUNT_LEAD";
        String roleTypeIdTo = "SUPPLIER";
        String partyRelationshipTypeId = "ACCOUNT";

        List<EntityCondition> condList = new ArrayList<>();
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        condList.add(EntityCondition.makeCondition("roleTypeIdTo", roleTypeIdTo));
        condList.add(EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom));
        condList.add(EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId));
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try
        {
            partyRelationships = delegator.findList("PartyRelationship", condition, null, null, null, false);
            partyRelationships = EntityUtil.filterByDate(partyRelationships);
        }
        catch (GenericEntityException e)
        {
            Debug.logError(e, "Problem finding PartyRelationships.", module);
            return ServiceUtil.returnError("Problem finding PartyRelationships.");
        }

        result.put("partyRelationships", partyRelationships);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> getPartnerAccountLeadPartyRelationshipsForPartyId(DispatchContext dctx, Map<String, Object> context)
    {
        final String module = "getPartnerAccountLeadPartyRelationshipsForPartyId";
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String partyIdFrom = (String) context.get("partyId");
        String roleTypeIdFrom = "ACCOUNT_LEAD";
        String roleTypeIdTo = "PARTNER";
        String partyRelationshipTypeId = "ACCOUNT";

        List<EntityCondition> condList = new ArrayList<>();
        condList.add(EntityCondition.makeCondition("partyIdFrom", partyIdFrom));
        condList.add(EntityCondition.makeCondition("roleTypeIdTo", roleTypeIdTo));
        condList.add(EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom));
        condList.add(EntityCondition.makeCondition("partyRelationshipTypeId", partyRelationshipTypeId));
        EntityCondition condition = EntityCondition.makeCondition(condList);

        List<GenericValue> partyRelationships = null;
        try
        {
            partyRelationships = delegator.findList("PartyRelationship", condition, null, null, null, false);
            partyRelationships = EntityUtil.filterByDate(partyRelationships);
        }
        catch (GenericEntityException e)
        {
            Debug.logError(e, "Problem finding PartyRelationships.", module);
            return ServiceUtil.returnError("Problem finding PartyRelationships.");
        }

        result.put("partyRelationships", partyRelationships);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

}
