package org.ofbiz.security.authz.da;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastList;

import org.ofbiz.base.util.AbstractResolver;
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
    private static AccessHandlerResolver resolver = new AccessHandlerResolver();
    
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
        List<DynamicAccessHandler> handlers = resolver.getHandlers();
        for (DynamicAccessHandler handler : handlers) {
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
    
    static class AccessHandlerResolver extends AbstractResolver {
        
        protected List<DynamicAccessHandler> handlers;
                
        protected List<DynamicAccessHandler> getHandlers() {
            handlers = FastList.newInstance();
            find("org.ofbiz");
            return handlers;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void resolveClass(Class clazz) {
            Class theClass = clazz;
            boolean checking = true;
            boolean found = false;
            
            while (checking) {
                Class[] ifaces = theClass.getInterfaces();
                for (Class iface : ifaces) {
                    if (DynamicAccessHandler.class.equals(iface)) {
                        loadHandler(theClass);
                        found = true;
                    }
                }
                
                if (!found) {
                    theClass = theClass.getSuperclass();
                    if (theClass == null) {
                        checking = false;
                    }
                } else {
                    checking = false;
                }
            }   
        }
            
        private void loadHandler(Class<DynamicAccessHandler> clazz) {
            DynamicAccessHandler handler = null;
            try {
                handler = clazz.newInstance();
                handlers.add(handler);
            } catch (InstantiationException e) {
                Debug.logError(e, module);       
            } catch (IllegalAccessException e) {
                Debug.logError(e, module);
            }
        }
    }
}
