package org.ofbiz.security.authz.da;

import java.util.Map;

import org.ofbiz.entity.GenericDelegator;

public interface DynamicAccess {

	/**
	 * Processes the dynamic permission check
	 * 
	 * @param userId the user's userId
	 * @param permission the raw permission string
	 * @param context name/value pairs needed for permission lookup
	 * @return true if the user has permission
	 */
	public boolean hasPermission(String userId, String permission, Map<String, ? extends Object> context);

	/**
	 * Returns the name of the permission this object handles
	 * @return permission name
	 */
	public String getPermissionName();
	
	/**
     * Method for injecting the delegator object
     * 
     * @param delegator the GenericDelegator object to use for the Authorization implementation
     */
    public void setDelegator(GenericDelegator delegator);
}
