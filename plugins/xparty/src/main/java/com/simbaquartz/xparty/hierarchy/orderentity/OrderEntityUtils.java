

package com.simbaquartz.xparty.hierarchy.orderentity;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;
import com.simbaquartz.xparty.hierarchy.role.HierarchyRoleUtils;

import org.apache.commons.lang.NullArgumentException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderEntityUtils
{
    public static final String resourceError = "HierarchyErrorUiLabels";

    public static <T extends Enum<T> & HierarchyRolesEnum> List<GenericValue> getOrderEntityRoleParties(Delegator delegator, OrderEntityType orderEntityType, Class<T> personRoleEnum, String primaryKey) throws GenericEntityException
    {
        List<String> roleTypeIds = HierarchyRoleUtils.roleTypeIds(personRoleEnum);

        //get partyid for all collaborators for this request
        EntityConditionList<EntityExpr> mainCond = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(orderEntityType.getPrimaryKeyName(), EntityOperator.EQUALS, primaryKey),
                EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roleTypeIds)),
                EntityOperator.AND);
        List<GenericValue> custRequestParties = delegator.findList(orderEntityType.getEntityRoleName(), mainCond, null, null, null, false);

        List<GenericValue> collaboratorParties = new ArrayList<>();
        if(custRequestParties != null && custRequestParties.size() > 0)
        {
            for (GenericValue custRequestParty : custRequestParties)
            {
                collaboratorParties.add(HierarchyUtils.getPartyByPartyId(delegator, custRequestParty.getString("partyId")));
            }
        }
        else
        {
            return null;
        }
        return collaboratorParties;
    }

    public static <T extends Enum<T> & HierarchyRolesEnum> List<String> getOrderEntityRolePartyIds(Delegator delegator, OrderEntityType orderEntityType, Class<T> personRoleEnum, String primaryKey) throws GenericEntityException
    {
        List<String> roleTypeIds = HierarchyRoleUtils.roleTypeIds(personRoleEnum);

        //get partyid for all collaborators for this request
        EntityConditionList<EntityExpr> mainCond = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(orderEntityType.getPrimaryKeyName(), EntityOperator.EQUALS, primaryKey),
                EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roleTypeIds)),
                EntityOperator.AND);
        List<GenericValue> custRequestParties = delegator.findList(orderEntityType.getEntityRoleName(), mainCond, null, null, null, false);

        List<String> collaboratorPartyIds = new ArrayList<>();
        if(custRequestParties != null && custRequestParties.size() > 0)
        {
            for (GenericValue custRequestParty : custRequestParties)
            {
                collaboratorPartyIds.add(custRequestParty.getString("partyId"));
            }
        }
        else
        {
            return null;
        }

        return collaboratorPartyIds;
    }

    /**
     * Get the OrderEntity
     *@param delegator needed Delegator
     *@param entityType Type of OrderEntity we're looking for
     *@param primaryKey Type of the id of the orderEntity we're looking for
     *@return GenericValue representing the OrderEntity record
     */
    public static GenericValue getOrderEntity(Delegator delegator, OrderEntityType entityType, String primaryKey, Locale locale)throws GenericEntityException
    {
        GenericValue orderEntity = null;
        orderEntity = delegator.findOne(entityType.getEntityName(), UtilMisc.toMap(entityType.getPrimaryKeyName(), primaryKey), false);

        if (orderEntity == null)
        {
            throw new NullArgumentException(UtilProperties.getMessage(resourceError,
                    "OrderEntityFindError", UtilMisc.toMap("entityName", entityType.getEntityName(), "primaryKey", entityType.getPrimaryKeyName()), locale));
        }
        return orderEntity;
    }
}
