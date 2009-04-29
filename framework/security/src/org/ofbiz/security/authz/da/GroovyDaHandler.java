package org.ofbiz.security.authz.da;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.entity.GenericDelegator;

public class GroovyDaHandler implements DynamicAccessHandler {

    private static final String module = GroovyDaHandler.class.getName();
    protected GenericDelegator delegator;
    
    public String getPattern() {
        return "(^.*\\.groovy$)";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        Map<String,Object> bindings = FastMap.newInstance();
        bindings.put("delegator", delegator);
        bindings.put("accessString", accessString);
        bindings.put("permission", permission);
        bindings.put("userId", userId);
        bindings.put("context", context);
        
        Debug.log("Attempting to call groovy script : " + accessString, module);
        Object result = null;
        
        if (accessString.startsWith("component://")) {
            // loaded using the OFBiz location API            
            try {
                result = GroovyUtil.runScriptAtLocation(accessString, bindings);
            } catch (GeneralException e) {
                Debug.logWarning(e, module);
            }
            
        } else {
            // try the standard class path
            String classpathString = accessString.substring(0, accessString.lastIndexOf("."));
            try {
                result = GroovyUtil.runScriptFromClasspath(classpathString, bindings);
            } catch (GeneralException e) {
                Debug.logWarning(e, module);
            }
        }
       
        // parse the result
        if (result != null && (result instanceof Boolean)) {
            return (Boolean) result;
        } else {
            Debug.logWarning("Groovy DynamicAccess implementation did not return a boolean [" + accessString + "]", module);
        }
        
        return false;
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;        
    }       
}
