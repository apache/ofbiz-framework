/******************************************************************************************
 * Copyright (c) Fidelis Sustainability Distribution, LLC 2016. - All Rights Reserved     *
 * Unauthorized copying of this file, via any medium is strictly prohibited               *
 * Proprietary and confidential                                                           *
 * Written by Forrest Rae <forrest.rae@fidelissd.com>, January 2016                       *
 ******************************************************************************************/

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

public Map isPartyGroupPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck()
{
    final String module = "isPartyGroupPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck";
    Boolean hasPermission = false;
    Map result = ServiceUtil.returnSuccess();
    if(userLogin.partyId.equals("system")) {
        // Always allow system user
        result.put("hasPermission", true)
        return result
    }
    Map serviceResult;
    try
    {
        serviceResult = runService("isPartyIdAndUserLoginPartyIdInTheSameOrgPermissionCheck", [partyId: parameters.partyGroupPartyId]);
        if (ServiceUtil.isError(serviceResult))
        {
            Debug.logError("An error occurred while trying to fetch party group for party id : " + partyId +
                    " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

            result.put("hasPermission", hasPermission);
            return result;
        }

        hasPermission = serviceResult.get("hasPermission");
    }
    catch (Exception e)
    {
        Debug.logError("An error occurred while trying to fetch party group for party id : " + callerPartyId +
                " details : " + ServiceUtil.getErrorMessage(serviceResult), module);

        result.put("hasPermission", hasPermission);
        return result;
    }

    Debug.logInfo("hasPermission: " + hasPermission, module);
    result.put("hasPermission", hasPermission);
    return result;
}
