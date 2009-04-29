package org.ofbiz.security.authz.da;

import java.util.Map;

import org.ofbiz.entity.GenericDelegator;

public interface DynamicAccessHandler {
    
    /**
     * Method invoked to call the DynamicAccess implementation
     * 
     * @param accessString Access string for the permission
     * @param userId the user's userId
     * @param permission the raw permission string
     * @param context name/value pairs needed for permission lookup
     * @return the value returned from the DynamicAccess implementation
     */
    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context);
    
    /**
     * Returns the handlers matching pattern. 
     * Example: ^service:(.*)$ 
     * Example: (^.*\.groovy$)
     * 
     * @return String containing the pattern this handler will control
     */
    public String getPattern();
    
    /**
     * Method for injecting the delegator object
     * 
     * @param delegator the GenericDelegator object to use for the Authorization implementation
     */
    public void setDelegator(GenericDelegator delegator);
}
