
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import com.fidelissd.hierarchy.HierarchyUtils;

public Map getEmployeePersons() {
    final String module = "getEmployeePersons";
    Map serviceResult = ServiceUtil.returnSuccess();
    GenericValue system = HierarchyUtils.getSysUserLogin(delegator);

    if(UtilValidate.isEmpty(parameters.partyGroupPartyId)){
        Debug.logError("partyGroupPartyId parameter cannot be null.", module);
        return ServiceUtil.returnError("partyGroupPartyId parameter cannot be null.");
    }

    // TODO this should be updated to detect the partyGroupPartyId Org type (isPartyGroupPartyIdXxxxxPermissionCheck), and use the applicable XxxxxxPersonRoles Enum to create a series of Entity Conditions.
    Map getRelatedPartiesResult = dispatcher.runSync("getRelatedParties", [userLogin : system, "partyIdFrom": parameters.partyGroupPartyId, "recurse": "Y", "roleTypeIdFrom": "_NA_", "partyRelationshipTypeId": "EMPLOYMENT", "includeFromToSwitched" : "N"]);
    if (ServiceUtil.isError(getRelatedPartiesResult))
    {
        Debug.logError(ServiceUtil.getErrorMessage(getRelatedPartiesResult), module);
        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(getRelatedPartiesResult));
    }
    
    // Get a unique list of the relatedParties.
    List<String> relatedParties = getRelatedPartiesResult.relatedPartyIdList.unique();
    
    List<GenericValue> employeePersons = [];
    relatedParties.each { String relatedPartyId ->
        if(!relatedPartyId.equals(parameters.partyGroupPartyId)){//ignore party entries with parameters.partyGroupPartyId as the response includes the partyIdFrom as well
            GenericValue employeePerson = delegator.findOne("Person", [partyId : relatedPartyId], false);
            
            if ( UtilValidate.isNotEmpty(employeePerson) ) {
                employeePersons.add(employeePerson);
            }
        }
    }

    serviceResult.put("employeePersonsList", employeePersons);
    return serviceResult;
}
