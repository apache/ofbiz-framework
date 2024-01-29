
package utility

import com.fidelissd.hierarchy.role.HierarchyRoleUtils;
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.role.PartnerPersonRoles;

public Map getPartnerPersonRoles() {
    Map result = ServiceUtil.returnSuccess();
    result.put("roleTypeIds", HierarchyRoleUtils.roleTypeIds(PartnerPersonRoles.class));
    return result;
}
