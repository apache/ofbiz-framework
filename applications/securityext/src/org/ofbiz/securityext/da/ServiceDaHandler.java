package org.ofbiz.securityext.da;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.security.authz.da.DynamicAccessHandler;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class ServiceDaHandler implements DynamicAccessHandler {

    private static final String module = ServiceDaHandler.class.getName();
    protected LocalDispatcher dispatcher;
    protected GenericDelegator delegator;
    
    public String getPattern() {        
        return "^service:(.*)$";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        Map<String,Object> serviceContext = FastMap.newInstance();
        serviceContext.put("userId", userId);
        serviceContext.put("permission", permission);
        serviceContext.put("accessString", accessString);
        serviceContext.put("permissionContext", context);
        
        String serviceName = accessString.substring(8);
        Map<String, Object> result;
        try {
            result = dispatcher.runSync(serviceName, serviceContext, 60, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return false;
        }
        
        if (result != null && !ServiceUtil.isError(result)) {
            Boolean reply = (Boolean) result.get("permissionGranted");
            if (reply == null) {
                reply = Boolean.FALSE;
            }
            return reply;
        } else {
            return false;
        }
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
        this.dispatcher = GenericDispatcher.getLocalDispatcher("SecurityDA", delegator);
    }
}
