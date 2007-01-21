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
package org.ofbiz.shark.user;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;

import org.enhydra.shark.api.internal.usergroup.UserGroupManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.UserTransaction;
public class PartyUserGroupMgr implements UserGroupManager {

    public static final String module = PartyUserGroupMgr.class.getName();
    protected CallbackUtilities callBack = null;
    protected GenericDelegator delegator = null;

    public void configure(CallbackUtilities cb) throws RootException {
        this.delegator = SharkContainer.getDelegator();
        this.callBack = cb;
    }

    public List getAllGroupnames(UserTransaction trans) throws RootException {
        Debug.logInfo("Call : List getAllGroupnames(UserTransaction trans)",module);
        return null;
    }

    public List getAllUsers(UserTransaction trans) throws RootException {
        List userLogins = null;
        List allUsers = null;
        try {
            userLogins = delegator.findAll("UserLogin", UtilMisc.toList("userLoginId"));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }

        if (userLogins != null) {
            allUsers = new ArrayList();
            Iterator i = userLogins.iterator();
            while (i.hasNext()) {
                GenericValue userLogin = (GenericValue) i.next();
                allUsers.add(userLogin.getString("userLoginId"));
            }
        }
        return allUsers;
    }

    public List getAllUsers(UserTransaction trans, String groupName) throws RootException {
        Debug.logInfo("Call : List getAllUsers(UserTransaction trans, String groupName)",module);
        return this.getAllUsers(trans);
    }

    public List getAllUsers(UserTransaction trans, List groupNames) throws RootException {
        Debug.logInfo("Call : List getAllUsers(UserTransaction trans, List groupNames)",module);
        return null;  // TODO: Implement Me!
    }

    public List getAllImmediateUsers(UserTransaction trans, String groupName) throws RootException {
        Debug.logInfo("Call : List getAllImmediateUsers(UserTransaction trans, String groupName)",module);
        return this.getAllUsers(trans);
    }

    public List getAllSubgroups(UserTransaction trans, String groupName) throws RootException {
        Debug.logInfo("Call : List getAllSubgroups(UserTransaction trans, String groupName)",module);
        return null;
    }

    public List getAllSubgroups(UserTransaction trans, List groupNames) throws RootException {
        Debug.logInfo("Call : List getAllSubgroups(UserTransaction trans, List groupNames)",module);
        return null;
    }

    public List getAllImmediateSubgroups(UserTransaction trans, String groupName) throws RootException {
        Debug.logInfo("Call : List getAllImmediateSubgroups(UserTransaction trans, String groupName)",module);
        return null;
    }

    public void createGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeGroup(UserTransaction trans, String groupName) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public boolean doesGroupExist(UserTransaction trans, String groupName) throws RootException {
        Debug.logInfo("Call : doesGroupExist(UserTransaction trans, String groupName)",module);
        return false;  // TODO: Implement Me!
    }

    public boolean doesGroupBelongToGroup(UserTransaction trans, String groupName, String subGroupName) throws RootException {
        Debug.logInfo("Call : boolean doesGroupBelongToGroup(UserTransaction trans, String groupName, String subGroupName)",module);
        return false;  // TODO: Implement Me!
    }

    public void updateGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void addGroupToGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeGroupFromGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeGroupTree(UserTransaction trans, String s) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeUsersFromGroupTree(UserTransaction trans, String s) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void moveGroup(UserTransaction trans, String s, String s1, String s2) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public String getGroupDescription(UserTransaction trans, String s) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void addUserToGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeUserFromGroup(UserTransaction trans, String s, String s1) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void moveUser(UserTransaction trans, String s, String s1, String s2) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public boolean doesUserBelongToGroup(UserTransaction trans, String groupName, String username) throws RootException {
        Debug.logInfo("Call : doesUserBelongToGroup(UserTransaction trans, String groupName, String username)",module);
        return false;  // TODO: Implement Me!
    }

    public void createUser(UserTransaction trans, String groupName, String username, String password, String firstname, String lastname, String email) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void updateUser(UserTransaction trans, String username, String firstname, String lastname, String email) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public void removeUser(UserTransaction trans, String username) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public boolean doesUserExist(UserTransaction trans, String username) throws RootException {
        GenericValue userLogin = this.getUserLogin(username);
        return (userLogin != null);
    }

    public void setPassword(UserTransaction trans, String username, String password) throws RootException {
        throw new RootException("PartyUserGroupMgr does not implement create/update/remove methods. Use the party manager instead!");
    }

    public String getUserRealName(UserTransaction trans, String username) throws RootException {
        return username;
    }

    public String getUserFirstName(UserTransaction trans, String username) throws RootException {
        return username;
    }

    public String getUserLastName(UserTransaction trans, String username) throws RootException {
        return username;
    }

    public String getUserEMailAddress(UserTransaction trans, String username) throws RootException {
        return username;
    }

    protected GenericValue getUserLogin(String username) throws RootException {
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", username));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        return userLogin;
    }
}
