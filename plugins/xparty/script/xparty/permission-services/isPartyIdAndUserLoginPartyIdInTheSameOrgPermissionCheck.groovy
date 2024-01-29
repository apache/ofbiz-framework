

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

public Map isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck()
{
    final String module = "isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();

    ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("HierarchyErrorUiLabels", locale);
    //uiLabelMap.addBottomResourceBundle("CommonUiLabels");

    if(userLogin.partyId.equals("system")) {
        // Always allow system user
        result.put("hasPermission", true)
        return result
    }
    String callerPartyId = userLogin.partyId;
    String partyId = parameters.partyId;

    if (UtilValidate.isNotEmpty(partyId) && (callerPartyId == partyId))
    {
        hasPermission = true;
        Debug.logInfo("partyId and callerPartyId are equal", module);
    }
    else
    {
        //Get the PartyGroup of both the userLogin.partyId and parameter.partyId
        GenericValue partyIdPartyGroup, callerPartyIdPartyGroup;

        Map serviceResult;
        try
        {
            serviceResult = runService("getPartyGroupForPartyId", [partyId: partyId]);
            if (ServiceUtil.isError(serviceResult))
            {
                Debug.logError("An error occurred while trying to fetch party group for party id : " + partyId +
                        " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

                result.put("hasPermission", hasPermission);
                return result;
            }
        } catch (Exception e)
        {
            Debug.logError("An error occurred while trying to fetch party group for party id : " + callerPartyId +
                    " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

            result.put("hasPermission", hasPermission);
            return result;
        }

        partyIdPartyGroup = serviceResult.organizationPartyGroup;

        try
        {
            serviceResult = runService("getPartyGroupForPartyId", [partyId: callerPartyId]);
            if (ServiceUtil.isError(serviceResult))
            {
                Debug.logError("An error occurred while trying to fetch party group for party id : " + callerPartyId +
                        " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

                result.put("hasPermission", hasPermission);
                return result;
            }
        } catch (Exception e)
        {
            Debug.logError("An error occurred while trying to fetch party group for party id : " + callerPartyId +
                    " details : " + e.getMessage(), module);

            result.put("hasPermission", hasPermission);
            return result;
        }

        callerPartyIdPartyGroup = serviceResult.organizationPartyGroup;

        if (UtilValidate.isNotEmpty(partyIdPartyGroup) && UtilValidate.isNotEmpty(callerPartyIdPartyGroup)
                && (partyIdPartyGroup.partyId == callerPartyIdPartyGroup.partyId))
        {
            hasPermission = true;
        }
        else
        {
            result.put("failMessage", uiLabelMap.HierarchyPermissionError);
        }
    }

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
