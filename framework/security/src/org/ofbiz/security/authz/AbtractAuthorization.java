/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.security.authz;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;

public abstract class AbtractAuthorization implements Authorization {
	
    private static final String module = AbtractAuthorization.class.getName();
    
	/**
	 * Used to manage Auto-Grant permissions for the current "request"
	 */
	private static ThreadLocal<List<String>> autoGrant = new ThreadLocal<List<String>>();
	private static ThreadLocal<String> origPermission = new ThreadLocal<String>();
	private static ThreadLocal<String> uid = new ThreadLocal<String>();
	
	/**
	 * Checks to see if the user has a static permission
	 * 
	 * @param userId the user's userId
	 * @param permission the expanded permission string
	 * @param context name/value pairs used for permission lookup
	 * @return true if the user has permission
	 */
	public abstract boolean hasStaticPermission(String userId, String permission, Map<String, ? extends Object> context);
	
	/**
	 * Locates the Dynamic Access implementation for the permissions and invokes it
	 * 
	 * @param userId the user's userId
	 * @param permission the expanded permission string
	 * @param context name/value pairs used for permission lookup
	 * @return true if the user has permission
	 */
	public abstract boolean hasDynamicPermission(String userId, String permission, Map<String, ? extends Object> context);
	
	/**
	 * Obtains a list of permissions auto-granted by the given permission
	 * 
	 * @param userId the user's userId
	 * @param permission the expanded permission string
	 * @param context name/value pairs used for permission lookup
	 * @return a List of permission strings to auto-grant the user
	 */
	public abstract List<String> getAutoGrantPermissions(String userId, String permission, Map<String, ? extends Object> context);
	
	/**
	 * Test to see if the specified user has permission
	 * 
	 * @param userId the user's userId
	 * @param permission the raw permission string
	 * @param context name/value pairs used for permission lookup
	 * @param expanded true if the permission string is already expanded, false if it will contain ${} context values
	 * @return true if the user has permission
	 */
	public boolean hasPermission(String userId, String permission, Map<String, ? extends Object> context, boolean expanded) {
	    // expand the permission string
		String expandedPermission;
		if (!expanded) {
			expandedPermission = FlexibleStringExpander.expandString(permission, context);
		} else {
			expandedPermission = permission;
		}
		
		// verify the ThreadLocal data; make sure it isn't stale (from a thread pool)
        String threadUid = uid.get();
        if (threadUid != null && !userId.equals(threadUid)) {
            origPermission.remove();
            autoGrant.remove();
            uid.remove();
            threadUid = null;
        }
        
        // set the tracking values on thread local
        if (UtilValidate.isEmpty(threadUid)) {
            origPermission.set(permission);
            uid.set(userId);
        }
                       	
		// split the permission string; so we can walk up the levels
		String[] permSplit = expandedPermission.split(":");
		StringBuilder joined = new StringBuilder();
		int index = 1;
		
		if (permSplit != null && permSplit.length > 1) {
		    if (Debug.infoOn()) Debug.logInfo("Security 2.0 schema found -- walking tree : " + expandedPermission, module);
    		// start walking
    		for (String perm : permSplit) {
    		    if (permSplit.length >= index) {
        			if (joined.length() > 0) {
        				joined.append(":");
        			}
        			joined.append(perm);
        			
        			// first check auto-granted permissions
        			List<String> grantedPerms = autoGrant.get();
        			if (grantedPerms != null && grantedPerms.size() > 0) {
        				for (String granted : grantedPerms) {
        					if (joined.toString().equals(granted)) {
        					    // permission granted
        					    handleAutoGrantPermissions(userId, expandedPermission, context);
        						return true;
        					}
        				}
        			}
        			
        			// next check static permission
        			if (hasStaticPermission(userId, joined.toString(), context)) {
        				// permission granted
        				handleAutoGrantPermissions(userId, expandedPermission, context);
        				return true;
        			}
    		    }
    			index++;
    		}
    		
    		// finally check dynamic permission (outside the loop)
    		String threadPerm = origPermission.get();
    		if (!permission.equals(threadPerm)) {
        		if (hasDynamicPermission(userId, expandedPermission, context)) {
        		    // permission granted
        		    handleAutoGrantPermissions(userId, expandedPermission, context);
        		    return true;
        		}
    		} else {
    		    Debug.logWarning("Recursive permission check detected; do not call hasPermission() from a dynamic access implementation!", module);
    		}
		} else {
		    // legacy mode; only call static permission check; no auto grants
		    Debug.logVerbose("Legacy permission detected; falling back to static permission check", module);
		    return hasStaticPermission(userId, expandedPermission, context);
		}
		return false;
	}
	
	protected void handleAutoGrantPermissions(String userId, String expandedPermission, Map<String, ? extends Object> context) {	    	    
	    List<String> granted = getAutoGrantPermissions(userId, expandedPermission, context);
	    if (granted != null && granted.size() > 0) {
            List<String> alreadyGranted = autoGrant.get();
            if (alreadyGranted == null) {
                alreadyGranted = FastList.newInstance();
            }
            
            // expand the auto-grant permissions
            for (String toGrant : granted) {
                if (UtilValidate.isNotEmpty(toGrant)) {
                    if (Debug.verboseOn()) Debug.logVerbose("Adding auto-grant permission -- " + toGrant, module);
                    alreadyGranted.add(FlexibleStringExpander.expandString(toGrant, context)); 
                }
            }
            autoGrant.set(granted);            
        }
	}
}
