

import com.fidelissd.hierarchy.role.CustomerPersonRoles;
import com.fidelissd.hierarchy.HierarchyRelationshipUtils;

public Map createGovCustPersonRelationship() {
    final String module = "createGovCustEmployeeRelationship";
    CustomerPersonRoles role = CustomerPersonRoles.valueOf(parameters.roleTypeId);
    Map serviceResult = HierarchyRelationshipUtils.createGovCustPersonRelationship(context, parameters.partyGroupPartyId, parameters.partyId, role);
    return serviceResult;
}
