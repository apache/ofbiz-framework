


import com.fidelissd.hierarchy.HierarchyUtils
import com.fidelissd.hierarchy.employee.EmployeeUtils
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

public Map getManagerForPartyId() {
    final String module = "getManagerForPartyId"
    Map result = ServiceUtil.returnSuccess()
    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale)
    uiLabelMap.addBottomResourceBundle("CommonUiLabels")

    // First check and see if partyId exists
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, parameters.partyId)
    if(UtilValidate.isEmpty(party))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist)

    // Check and make sure this is a PERSON.
    if(HierarchyUtils.getPartyType(party) != "PERSON")
        return ServiceUtil.returnError(uiLabelMap.WrongPartyTypePerson)

    // Search for relationships
    GenericValue partyManagerRelationship = EmployeeUtils.getEmployeeManager(delegator, parameters.partyId)
    if(UtilValidate.isEmpty(partyManagerRelationship))
        return ServiceUtil.returnFailure(uiLabelMap.PartyHasNoManager)

    // Prefer Manager rather than Account Lead:
    /* This code works, but commenting out for now to avoid destablizing the system.  Need to be able to run all the test cases before committing this code to production.
    for (GenericValue partyRelationship : partyRelationships)
    {
        if(partyRelationship.get("roleTypeIdFrom") == "MANAGER")
        {
            GenericValue manager = delegator.findOne("Party", [partyId : partyRelationship.get("partyIdFrom")], true )
            if(UtilValidate.isEmpty(manager))
                return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist)
            result.put("manager", manager)
            return result
        }
    }*/

    GenericValue manager = delegator.findOne("Party", [partyId : partyManagerRelationship.partyIdFrom], true )
    if(UtilValidate.isEmpty(manager))
        return ServiceUtil.returnError(uiLabelMap.PartyDoesNotExist)
    result.put("manager", manager)
    return result
}