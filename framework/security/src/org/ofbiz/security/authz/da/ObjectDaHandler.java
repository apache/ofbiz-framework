package org.ofbiz.security.authz.da;

import java.util.Map;

import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;

public class ObjectDaHandler implements DynamicAccessHandler {

    private static UtilCache<String,DynamicAccess> dynamicAccessCache = new UtilCache<String,DynamicAccess>("security.DynamicAccessCache");
    
    protected GenericDelegator delegator;
    
    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;   
    }
    
    public String getPattern() {
        // returns "*" as a fall back pattern (catch all)
        // if no other handler comes back this handler will catch
        return "*";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        DynamicAccess da = getDynamicAccessObject(accessString);
        if (da != null) {
            return da.hasPermission(userId, permission, context);
        }
        return false;
    }
    
    private DynamicAccess getDynamicAccessObject(String name) {
        DynamicAccess da = dynamicAccessCache.get(name);
        
        if (da == null) {
            da = DynamicAccessFactory.loadDynamicAccessObject(delegator, name);
            if (da != null) {
                dynamicAccessCache.put(name, da);
            }
        }
        
        return da;
    }   
}
