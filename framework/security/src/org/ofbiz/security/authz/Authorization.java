package org.ofbiz.security.authz;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.ofbiz.entity.GenericDelegator;

public interface Authorization {

	/**
	 * Test to see if the specified user has permission
	 * 
	 * @param userId the user's userId
	 * @param permission the raw permission string
	 * @param context name/value pairs used for permission lookup
	 * @param expanded true if the permission string is already expanded, false if it will contain ${} context values
	 * @return true if the user has permission
	 */
	public boolean hasPermission(String userId, String permission, Map<String, ? extends Object> context, boolean expanded);
	
	/**
     * Test to see if the specified user has permission
     * 
     * @param session HttpSession used to obtain the userId
     * @param permission the raw permission string
     * @param context name/value pairs used for permission lookup
     * @param expanded true if the permission string is already expanded, false if it will contain ${} context values
     * @return true if the user has permission
     */
    public boolean hasPermission(HttpSession session, String permission, Map<String, ? extends Object> context, boolean expanded);
	
    /**
     * Method for injecting the delegator object
     * 
     * @param delegator the GenericDelegator object to use for the Authorization implementation
     */
    public void setDelegator(GenericDelegator delegator);
}
