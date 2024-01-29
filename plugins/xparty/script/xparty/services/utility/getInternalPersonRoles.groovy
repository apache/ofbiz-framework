
package utility

import com.fidelissd.hierarchy.role.HierarchyRoleUtils;
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.role.InternalPersonRoles;

public Map getInternalPersonRoles() {
    Map result = ServiceUtil.returnSuccess();
    result.put("roleTypeIds", HierarchyRoleUtils.roleTypeIds(InternalPersonRoles.class));
    return result;
}
