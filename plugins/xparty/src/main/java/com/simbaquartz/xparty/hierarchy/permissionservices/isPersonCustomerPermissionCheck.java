

package com.simbaquartz.xparty.hierarchy.permissionservices;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Locale;
import java.util.Map;

public class isPersonCustomerPermissionCheck
{

    public static final String module = isPersonCustomerPermissionCheck.class.getName();
    public static final String resource = "HierarchyUiLabels";
    public static final String resourceError = "HierarchyErrorUiLabels";

    public static Map<String, Object> isPersonCustomerPermissionCheckService(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String partyId = (String) context.get("partyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Boolean hasPermission = false;
        Map<String, Object> result = ServiceUtil.returnSuccess();

        // Check if party is part of the Partner Organization PartyGroup
        Map<String, Object> serviceResult;
        try {
            serviceResult = dispatcher.runSync("getPartyGroupForPartyId", UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "HierarchyPartnerLoginError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        // Check that we got success and not failure.
        if(ServiceUtil.isFailure(serviceResult))
            return serviceResult;

        if (!HierarchyUtils.checkPartyRole((GenericValue) serviceResult.get("organizationPartyGroup"), "CUSTOMER"))
        {
            result.put("failMessage", UtilProperties.getMessage(resource, "PartyNotPartnerOrg", locale));
            result.put("hasPermission", hasPermission);
            return result;
        }
        else
            hasPermission = true;

        Debug.logInfo("hasPermission: " + hasPermission, module);
        result.put("hasPermission", hasPermission);
        return result;
    }
}
