
package utility

import com.fidelissd.hierarchy.role.HierarchyRoleUtils
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.role.PartnerPersonRoles;

public Map getPartnerPersonRoleTypeIdNameMap() {
    Map result = ServiceUtil.returnSuccess();
    result.put("roleTypeIdNameMap", HierarchyRoleUtils.getRoleTypeIdNameMap(PartnerPersonRoles.class));
    return result;
}
