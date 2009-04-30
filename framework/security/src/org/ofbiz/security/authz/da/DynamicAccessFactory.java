package org.ofbiz.security.authz.da;

import java.util.Set;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.spi.ServiceRegistry;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;

public class DynamicAccessFactory {
    
    /**
     * Cache to store the DynamicAccess implementations
     */
    private static UtilCache<String,DynamicAccessHandler> dynamicAccessHandlerCache = new UtilCache<String,DynamicAccessHandler>("security.DynamicAccessHandlerCache");
    private static final String module = DynamicAccessFactory.class.getName();
    
    public static DynamicAccessHandler getDynamicAccessHandler(GenericDelegator delegator, String accessString) {
        if (dynamicAccessHandlerCache.size() == 0) { // should always be at least 1
            loadAccessHandlers(delegator);
        }
        
        Set<? extends String> patterns = dynamicAccessHandlerCache.getCacheLineKeys();
        for (String pattern : patterns) {
            if (!pattern.equals("*")) { // ignore the default pattern for now
                Debug.logInfo("Checking DOH pattern : " + pattern, module);
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(accessString);
                if (m.find()) {
                    Debug.logInfo("Pattern [" + pattern + "] matched -- " + accessString, module);
                    return dynamicAccessHandlerCache.get(pattern); 
                }
            }
        }
        
        return dynamicAccessHandlerCache.get("*");
    }
    
    private static void loadAccessHandlers(GenericDelegator delegator) {
        Iterator<DynamicAccessHandler> it = ServiceRegistry.lookupProviders(DynamicAccessHandler.class, DynamicAccessFactory.class.getClassLoader());
        while (it.hasNext()) {
            DynamicAccessHandler handler = it.next();
            handler.setDelegator(delegator);
            dynamicAccessHandlerCache.put(handler.getPattern(), handler);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static DynamicAccess loadDynamicAccessObject(GenericDelegator delegator, String accessString) {
        DynamicAccess da = null;
        Class<DynamicAccess> clazz;
        try {
            clazz = (Class<DynamicAccess>) ObjectType.loadClass(accessString);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, module);
            return null;
        } catch (ClassCastException e) {
            Debug.logError(e, module);
            return null;
        }
        
        if (clazz != null) {
            try {
                da = clazz.newInstance();
                da.setDelegator(delegator);
            } catch (InstantiationException e) {
                Debug.logError(e, module);
                return null;             
            } catch (IllegalAccessException e) {
                Debug.logError(e, module);
                return null; 
            }
        }
        
        return da;
    }
}
