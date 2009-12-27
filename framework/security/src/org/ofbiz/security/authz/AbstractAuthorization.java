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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;

public abstract class AbstractAuthorization implements Authorization {
    
    private static final String module = AbstractAuthorization.class.getName();
    
    /**
     * Used to manage Auto-Grant permissions for the current "request"
     */
    private static ThreadLocal<List<String>> autoGrant = new ThreadLocal<List<String>>();
    private static ThreadLocal<String> origPermission = new ThreadLocal<String>();
    private static ThreadLocal<String> uid = new ThreadLocal<String>();
    
    private static final String[] basePermissions = { "access", "create", "read", "update", "delete" };
    
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
     * Takes a regular expression (permissionRegexp) and evaluates it against base permissions and returns permission
     * values for each match.
     * Example 1: ".*:example" will return values for access:example, create:example, read:example, update:example and delete:example
     * Example 2: "(access|read):example:${exampleId} will return values for access:example:${exampleId} and read:example:${exampleId} 
     *  
     * NOTE: the regular expression can only be part of the base permission (before the first colon)
     * 
     * @param userId the user's userId
     * @param permissionRegexp permission string containing regexp in the base position    
     * @param expanded  true if the permission string is already expanded, false if it will contain ${} context values
     * @return a map of allowed or disallowed permissions
     */
    public Map<String, Boolean> findMatchingPermission(String userId, String permissionRegexp, Map<String, ? extends Object> context) {
        Map<String, Boolean> resultMap = FastMap.newInstance();
        
        String regexp = permissionRegexp.substring(0, permissionRegexp.indexOf(":"));
        String permStr = permissionRegexp.substring(permissionRegexp.indexOf(":"));
        
        Pattern p = Pattern.compile("^" + regexp + ":.*$");
        for (String base : basePermissions) {
            Matcher m = p.matcher(base + permStr);
            if (m.find()) {
                String permission = m.group();
                resultMap.put(permission, hasPermission(userId, permission, context));
            }
        }
        return resultMap;
    }
    
    /**
     * Test to see if the specified user has permission
     * 
     * @param userId the user's userId
     * @param permission the raw permission string
     * @param context name/value pairs used for permission lookup    
     * @return true if the user has permission
     */
    public boolean hasPermission(String userId, String permission, Map<String, ? extends Object> context) {
        // expand the permission string
        String expandedPermission = FlexibleStringExpander.expandString(permission, context);
        
        // verify the ThreadLocal data; make sure it isn't stale (from a thread pool)
        String threadUid = uid.get();
        if (threadUid != null && !userId.equals(threadUid)) {
            origPermission.remove();
            autoGrant.remove();
            uid.remove();
            threadUid = null;
        }
        
        // set the tracking values on thread local
        boolean initialCall = false;
        if (UtilValidate.isEmpty(threadUid)) {
            origPermission.set(permission);
            uid.set(userId);
            initialCall = true;
        }
                           
        // split the permission string; so we can walk up the levels
        String[] permSplit = expandedPermission.split(":");
        StringBuilder joined = new StringBuilder();
        int index = 1;
        
        if (permSplit != null && permSplit.length > 1) {
            if (Debug.verboseOn()) Debug.logVerbose("Security 2.0 schema found -- walking tree : " + expandedPermission, module);
            // start walking
            for (String perm : permSplit) {
                if (permSplit.length >= index) {
                    if (joined.length() > 0) {
                        joined.append(":");
                    }
                    joined.append(perm);
                    
                    // first check auto-granted permissions
                    List<String> grantedPerms = autoGrant.get();
                    if (UtilValidate.isNotEmpty(grantedPerms)) {
                        Debug.logVerbose("Auto-Grant permissions found; looking for a match", module);
                        for (String granted : grantedPerms) {
                            if (Debug.verboseOn()) Debug.logVerbose("Testing - " + granted + " - with - " + joined.toString(), module);
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
            if (initialCall || !permission.equals(threadPerm)) {
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
        if (UtilValidate.isNotEmpty(granted)) {
            List<String> alreadyGranted = autoGrant.get();
            if (alreadyGranted == null) {
                alreadyGranted = FastList.newInstance();
            }
            
            // expand the auto-grant permissions
            for (String toGrant : granted) {
                if (UtilValidate.isNotEmpty(toGrant)) {                    
                    String grantExpanded = FlexibleStringExpander.expandString(toGrant, context);
                    if (Debug.verboseOn()) Debug.logVerbose("Adding auto-grant permission -- " + grantExpanded, module);
                    alreadyGranted.add(grantExpanded); 
                }
            }
            autoGrant.set(granted);            
        }
    }
    
    /**
     * Used to clear the values set in ThreadLocal
     * -- needed when thread pools are used which do not handle clearing between requests
     */
    public static void clearThreadLocal() {
        origPermission.remove();
        autoGrant.remove();
        uid.remove();        
    }
}
