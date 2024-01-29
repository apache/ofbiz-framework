
package utility

import com.fidelissd.hierarchy.role.HierarchyRoleUtils
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.role.CustomerPersonRoles;

public Map getCustomerPersonRoleTypeIdNameMap() {
    Map result = ServiceUtil.returnSuccess();
    result.put("roleTypeIdNameMap", HierarchyRoleUtils.getRoleTypeIdNameMap(CustomerPersonRoles.class));
    return result;
}
