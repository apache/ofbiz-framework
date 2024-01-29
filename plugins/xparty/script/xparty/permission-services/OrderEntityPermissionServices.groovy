

import com.fidelissd.fsdOrderManager.orderentity.OrderEntityUtils
import com.fidelissd.hierarchy.role.HierarchyRoleUtils
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.commons.lang.NotImplementedException;
import com.fidelissd.hierarchy.HierarchyUtils;
import com.fidelissd.hierarchy.orderentity.OrderEntityType;
import com.fidelissd.hierarchy.orderentity.role.OrderEntityViewerRoles;
import com.fidelissd.hierarchy.orderentity.role.OrderEntityUpdaterRoles;
import com.fidelissd.hierarchy.orderentity.role.OrderEntityCreatorRoles;

// Gets a list of Order Entity Viewers
private List<GenericValue> getOrderEntityViewers(OrderEntityType orderEntityType, GenericValue orderEntity)
{
    // Add a condition for the primaryKeyName, example: CustRequestId for CustRequest
    EntityExpr condnPrimaryKey = EntityCondition.makeCondition(orderEntityType.primaryKeyName, EntityOperator.EQUALS, orderEntity.get(orderEntityType.primaryKeyName));
    // Add a condition for the  list of viewers as defined by the OrderEntityViewerRoles enumeration
    List<String> viewerRoleTypes = HierarchyRoleUtils.roleTypeIds(OrderEntityViewerRoles.class);
    EntityExpr exprListRoleTypes = EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, viewerRoleTypes);
    EntityConditionList mainCond = EntityCondition.makeCondition([condnPrimaryKey, exprListRoleTypes], EntityOperator.AND);
    List<GenericValue> viewers = delegator.findList(orderEntityType.EntityRoleName, mainCond, null, null, null, false)
    return viewers;
}

// Gets a list of Order Entity Updaters
private List<GenericValue> getOrderEntityUpdaters(OrderEntityType orderEntityType, GenericValue orderEntity)
{
    // Add a condition for the primaryKeyName, example: CustRequestId for CustRequest
    EntityExpr condnPrimaryKey = EntityCondition.makeCondition(orderEntityType.primaryKeyName, EntityOperator.EQUALS, orderEntity.get(orderEntityType.primaryKeyName));
    // Add a condition for the  list of viewers as defined by the OrderEntityViewerRoles enumeration
    List<String> viewerRoleTypes = HierarchyRoleUtils.roleTypeIds(OrderEntityUpdaterRoles.class);
    EntityExpr exprListRoleTypes = EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, viewerRoleTypes);
    EntityConditionList mainCond = EntityCondition.makeCondition([condnPrimaryKey, exprListRoleTypes], EntityOperator.AND);
    List<GenericValue> viewers = delegator.findList(orderEntityType.EntityRoleName, mainCond, null, null, null, false)
    return viewers;
}

private Boolean iterateOrderEntityPeople(List<GenericValue> orderEntityPersons)
{
    Boolean hasPermission = false;

    // loop through and see if any of the orderEntityPersons match the userLogin
    for (it in orderEntityPersons)
    {
        Map doesPartyIdEqualUserLoginPartyIdPermissionCheckResult = runService('doesPartyIdEqualUserLoginPartyIdPermissionCheck', [partyId: it.partyId])
        if (doesPartyIdEqualUserLoginPartyIdPermissionCheckResult.hasPermission)
        {
            hasPermission = true;
            break;
        }
        else
        {
            // hasPermission isn't true, so we check and see if each of the orderEntityPersons is in userLogin's Org.
            Map isSubordinatePartyIdInManagerOrgPermissionCheckResult = runService('isSubordinatePartyIdInManagerOrgPermissionCheck', [subordinatePartyId: it.partyId, managerPartyId: userLogin.partyId])
            if (isSubordinatePartyIdInManagerOrgPermissionCheckResult.hasPermission)
            {
                hasPermission = true;
                break;
            }
        }
    }

    return hasPermission;
}

private Boolean checkOrderEntityPermissionView(OrderEntityType orderEntityType, GenericValue orderEntity)
{
    Boolean hasPermission = false;

    switch(orderEntityType) {
        case orderEntityType.CUST_REQUEST:
        case orderEntityType.QUOTE:
            // First check and see if orderEntity is empty
            if (UtilValidate.isEmpty(orderEntity))
                return ServiceUtil.returnError("Error : orderEntity doesn't exist.");

            // Get a list of viewers
            List<GenericValue> viewers = getOrderEntityViewers(orderEntityType, orderEntity);

            // loop through and see if any of the orderEntityPersons match the userLogin
            hasPermission = iterateOrderEntityPeople(viewers);
            break;
        case orderEntityType.INVOICE:
            // TODO Not implemented yet
            throw new NotImplementedException();
        case orderEntityType.ORDER:
            return true; // for now!
        case orderEntityType.SHIPMENT:
            // TODO Not implemented yet
            throw new NotImplementedException();
        default:
            return false;
    }

            return hasPermission;
}

private Boolean checkOrderEntityPermissionCreate(OrderEntityType orderEntityType)
{
    Boolean hasPermission = false;

    switch(orderEntityType)
    {
        case orderEntityType.CUST_REQUEST:
        case orderEntityType.QUOTE:
            // Add a condition for the list of creators as defined by the OrderEntityCreatorRoles enumeration
            List<String> creatorRoleTypes = HierarchyRoleUtils.roleTypeIds(OrderEntityCreatorRoles.class);
            hasPermission = HierarchyUtils.checkPartyRolesAnd(userLogin, creatorRoleTypes);
            break;
        case orderEntityType.INVOICE:
            // TODO Not implemented yet
            throw new NotImplementedException();
        case orderEntityType.ORDER:
            return true; // for now!
        case orderEntityType.SHIPMENT:
            // TODO Not implemented yet
            throw new NotImplementedException();
        default:
            return false;
    }

    return hasPermission;
}


private Boolean checkOrderEntityPermissionUpdate(OrderEntityType orderEntityType, GenericValue orderEntity, Boolean checkEditableStatuses)
{
    Boolean hasPermission = false;

    switch(orderEntityType)
    {
        case orderEntityType.CUST_REQUEST:
        case orderEntityType.QUOTE:
            // First check and see if orderEntity is empty
            if (UtilValidate.isEmpty(orderEntity))
                return ServiceUtil.returnError("Error : orderEntity doesn't exist.");

            // If this is just a status update, don't check editable statuses.
            if (checkEditableStatuses)
            {
                if(!OrderEntityUtils.isEditableStatus(orderEntity, orderEntityType))
                    return false;
            }

            // Get a list of updaters
            List<GenericValue> updaters = getOrderEntityUpdaters(orderEntityType, orderEntity);
            // loop through and see if any of the updaters match the userLogin
            hasPermission = iterateOrderEntityPeople(updaters);
            break;
        case orderEntityType.INVOICE:
            // TODO Not implemented yet
            throw new NotImplementedException();
        case orderEntityType.ORDER:
            return true; // for now!
        case orderEntityType.SHIPMENT:
            // TODO Not implemented yet
            throw new NotImplementedException();
        default:
            return false;
    }

    return hasPermission;
}

private GenericValue getOrderEntityById(OrderEntityType orderEntityType, String primaryKey)
{
    Map<String, String> cond = [:];
    cond.put(orderEntityType.primaryKeyName, primaryKey);

    return delegator.findOne(orderEntityType.EntityName, cond, false);
}

public Map checkOrderEntityPermission(OrderEntityType orderEntityType, Map parameters, Boolean checkEditableStatuses)
{
    final String module = "checkOrderEntityPermission";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    //uiLabelMap.addBottomResourceBundle("OrderErrorUiLabels");
    final String primaryKey = parameters.get(orderEntityType.primaryKeyName);    // get the primaryKey of this Order Entity
    GenericValue orderEntity = null;

    // First check special permission.
    if(HierarchyUtils.checkSpecialConditionPermission(context))
    {
        result.put("hasPermission", true);
        return result;
    }

    switch(parameters.mainAction)
    {
        case 'VIEW':
            orderEntity = getOrderEntityById(orderEntityType, primaryKey);
            hasPermission = checkOrderEntityPermissionView(orderEntityType, orderEntity);
            if (!hasPermission)
                result.put("failMessage", uiLabelMap.HierarchyViewPermissionError);
            break;
        case 'CREATE':
            hasPermission = checkOrderEntityPermissionCreate(orderEntityType);
            if (!hasPermission)
                result.put("failMessage", uiLabelMap.HierarchyCreatePermissionError);
            break;
        case 'UPDATE':
        case 'DELETE':
            orderEntity = getOrderEntityById(orderEntityType, primaryKey);
            hasPermission = checkOrderEntityPermissionUpdate(orderEntityType, orderEntity, checkEditableStatuses);
            if (!hasPermission)
                result.put("failMessage", uiLabelMap.HierarchyUpdateDeletePermissionError);
            break;
        default:
            result.put("failMessage", uiLabelMap.HierarchyPermissionError);
            break;

    }

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}

public Map checkCustRequestPermission()
{
    return checkOrderEntityPermission(OrderEntityType.CUST_REQUEST, parameters, true);
}

public Map checkCustRequestStatusUpdatePermission()
{
    // TODO Not Tested Yet
    throw new NotImplementedException();
    //return checkOrderEntityPermission(OrderEntityType.CUST_REQUEST, parameters, false);
}

public Map checkQuotePermission()
{
    return checkOrderEntityPermission(OrderEntityType.QUOTE, parameters, true);
}

public Map checkQuoteStatusUpdatePermission()
{
    return checkOrderEntityPermission(OrderEntityType.QUOTE, parameters, false);
}

public Map checkOrderPermission()
{
    return checkOrderEntityPermission(OrderEntityType.ORDER, parameters, true);
}

public Map checkOrderStatusUpdatePermission()
{
    //return checkOrderEntityPermission(OrderEntityType.ORDER, parameters, false);
    // TODO Not Tested Yet
    throw new NotImplementedException();
}

public Map checkInvoicePermission()
{
    //return checkOrderEntityPermission(OrderEntityType.INVOICE, parameters, true);
    // TODO Not Tested Yet
    throw new NotImplementedException();
}

public Map checkInvoiceStatusUpdatePermission()
{
    //return checkInvoiceEntityPermission(InvoiceEntityType.Invoice, parameters, false);
    // TODO Not Tested Yet
    throw new NotImplementedException();
}

public Map checkShipmentPermission()
{
    //return checkOrderEntityPermission(OrderEntityType.SHIPMENT, parameters, true);
    // TODO Not Tested Yet
    throw new NotImplementedException();
}

public Map checkShipmentStatusUpdatePermission()
{
    //return checkShipmentEntityPermission(ShipmentEntityType.Shipment, parameters, false);
    // TODO Not Tested Yet
    throw new NotImplementedException();
}
