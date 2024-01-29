


import com.fidelissd.hierarchy.role.CustomerRoles;
import com.fidelissd.hierarchy.role.HierarchyRoleUtils;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.base.util.Debug;

public Map isPartyGroupPartyIdCustomerPermissionCheck()
{
    final String module = "isPartyGroupPartyIdCustomerPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    List<String> roles = HierarchyRoleUtils.roleTypeIds(CustomerRoles.class);

    condList = [
            EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyGroupPartyId),
            EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roles)
    ];
    EntityCondition cond = EntityCondition.makeCondition(condList);
    List<GenericValue> partyRoles = delegator.findList("PartyRole", cond, null, null, null, true);

    if(partyRoles.size() > 0)
        hasPermission = true;

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}