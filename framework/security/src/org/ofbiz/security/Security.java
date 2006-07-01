/*
 * $Id: Security.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.security;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

/**
 * Security handler: This class is an abstract implementation for all commononly used security aspects.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:hermanns@aixcept.de">Rainer Hermanns</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class Security {

    /**
     * UtilCache to cache a Collection of UserLoginSecurityGroup entities for each UserLogin, by userLoginId.
     */
    public static UtilCache userLoginSecurityGroupByUserLoginId = new UtilCache("security.UserLoginSecurityGroupByUserLoginId");

    /**
     * UtilCache to cache whether or not a certain SecurityGroupPermission row exists or not.
     * For each SecurityGroupPermissionPK there is a Boolean in the cache specifying whether or not it exists.
     * In this way the cache speeds things up whether or not the user has a permission.
     */
    public static UtilCache securityGroupPermissionCache = new UtilCache("security.SecurityGroupPermissionCache");

    GenericDelegator delegator = null;

    public GenericDelegator getDelegator() {
        return delegator;
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Uses userLoginSecurityGroupByUserLoginId cache to speed up the finding of the userLogin's security group list.
     *
     * @param userLoginId The userLoginId to find security groups by
     * @return An iterator made from the Collection either cached or retrieved from the database through the
     * 		   UserLoginSecurityGroup Delegator.
     */
    public abstract Iterator findUserLoginSecurityGroupByUserLoginId(String userLoginId);

    /**
     * Finds whether or not a SecurityGroupPermission row exists given a groupId and permission.
     * Uses the securityGroupPermissionCache to speed this up.
     * The groupId,permission pair is cached instead of the userLoginId,permission pair to keep the cache small and to
     * make it more changeable.
     *
     * @param groupId The ID of the group
     * @param permission The name of the permission
     * @return boolean specifying whether or not a SecurityGroupPermission row exists
     */
    public abstract boolean securityGroupPermissionExists(String groupId, String permission);

    /**
     * Checks to see if the currently logged in userLogin has the passed permission.
     *
     * @param permission Name of the permission to check.
     * @param session The current HTTP session, contains the logged in userLogin as an attribute.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasPermission(String permission, HttpSession session);

    /**
     * Checks to see if the userLogin has the passed permission.
     *
     * @param permission Name of the permission to check.
     * @param userLogin The userLogin object for user to check against.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasPermission(String permission, GenericValue userLogin);

    /**
     * Like hasPermission above, except it has functionality specific to Entity permissions. Checks the entity for the
     * specified action, as well as for "_ADMIN" to allow for simplified general administration permission.
     *
     * @param entity The name of the Entity corresponding to the desired permission.
     * @param action The action on the Entity corresponding to the desired permission.
     * @param session The current HTTP session, contains the logged in userLogin as an attribute.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasEntityPermission(String entity, String action, HttpSession session);

    /**
     * Like hasPermission above, except it has functionality specific to Entity permissions. Checks the entity for the
     * specified action, as well as for "_ADMIN" to allow for simplified general administration permission.
     *
     * @param entity The name of the Entity corresponding to the desired permission.
     * @param action The action on the Entity corresponding to the desired permission.
     * @param userLogin The userLogin object for user to check against.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasEntityPermission(String entity, String action, GenericValue userLogin);
    
    /**
     * Like hasEntityPermission above, this checks the specified action, as well as for "_ADMIN" to allow for simplified
     * general administration permission, but also checks action_ROLE and validates the user is a member for the
     * application.
     *
     * @param application The name of the application corresponding to the desired permission.
     * @param action The action on the application corresponding to the desired permission.
     * @param primaryKey The primary key for the role check.
     * @param role The roleTypeId which the user must validate with. 
     * @param session The current HTTP session, contains the logged in userLogin as an attribute.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */    
    public abstract boolean hasRolePermission(String application, String action, String primaryKey, String role, HttpSession session);

    /**
     * Like hasEntityPermission above, this checks the specified action, as well as for "_ADMIN" to allow for simplified
     * general administration permission, but also checks action_ROLE and validates the user is a member for the
     * application.
     *
     * @param application The name of the application corresponding to the desired permission.
     * @param action The action on the application corresponding to the desired permission.
     * @param primaryKey The primary key for the role check.
     * @param role The roleTypeId which the user must validate with.
     * @param userLogin The userLogin object for user to check against.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasRolePermission(String application, String action, String primaryKey, String role, GenericValue userLogin);
        
    /**
     * Like hasEntityPermission above, this checks the specified action, as well as for "_ADMIN" to allow for simplified
     * general administration permission, but also checks action_ROLE and validates the user is a member for the
     * application.
     *
     * @param application The name of the application corresponding to the desired permission.
     * @param action The action on the application corresponding to the desired permission.
     * @param primaryKey The primary key for the role check.
     * @param roles List of roleTypeId of which the user must validate with (ORed).
     * @param userLogin The userLogin object for user to check against.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public abstract boolean hasRolePermission(String application, String action, String primaryKey, List roles, GenericValue userLogin);
    
    /**
     * Like hasEntityPermission above, this checks the specified action, as well as for "_ADMIN" to allow for simplified
     * general administration permission, but also checks action_ROLE and validates the user is a member for the
     * application.
     *
     * @param application The name of the application corresponding to the desired permission.
     * @param action The action on the application corresponding to the desired permission.
     * @param primaryKey The primary key for the role check.
     * @param roles List of roleTypeId of which the user must validate with (ORed). 
     * @param session The current HTTP session, contains the logged in userLogin as an attribute.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */    
    public abstract boolean hasRolePermission(String application, String action, String primaryKey, List roles, HttpSession session);
    
}
