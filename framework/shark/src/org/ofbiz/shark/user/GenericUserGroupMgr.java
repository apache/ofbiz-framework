/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.user;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import org.enhydra.shark.api.internal.usergroup.UserGroupManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.UserTransaction;

/**
 * Shark User/Group Manager
 */
public class GenericUserGroupMgr implements UserGroupManager {

    public static final String module = GenericUserGroupMgr.class.getName();
    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callbackUtilities) throws RootException {
        this.callBack = callbackUtilities;
    }

    public List getAllGroupnames(UserTransaction trans) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List groupNames = new ArrayList();
        List groups = null;
        try {
            groups = delegator.findAll("SharkGroup");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        if (groups != null && groups.size() > 0) {
            Iterator i = groups.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                groupNames.add(v.getString("groupName"));
            }
        }
        return groupNames;
    }

    public List getAllUsers(UserTransaction trans) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List userNames = new ArrayList();
        List users = null;
        try {
            users = delegator.findAll("SharkUser");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        if (users != null && users.size() > 0) {
            Iterator i = users.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                userNames.add(v.getString("userName"));
            }
        }
        return userNames;
    }

    public List getAllUsers(UserTransaction trans, String groupName) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List userNames = new ArrayList();
        List members = null;
        try {
            members = delegator.findByAnd("SharkGroupMember", UtilMisc.toMap("groupName", groupName));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        if (members != null && members.size() > 0) {
            Iterator i = members.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                userNames.add(v.getString("userName"));
            }
        }
        return userNames;
    }

    public List getAllUsers(UserTransaction trans, List groupNames) throws RootException {
        List userNames = new ArrayList();
        if (groupNames != null && groupNames.size() > 0) {
            Iterator i = groupNames.iterator();
            while (i.hasNext()) {
                String groupName = (String) i.next();
                userNames.addAll(getAllUsers(trans, groupName));
            }
        }
        return userNames;
    }

    public List getAllImmediateUsers(UserTransaction trans, String groupName) throws RootException {
        return this.getAllUsers(trans, groupName);
    }

    public List getAllSubgroups(UserTransaction trans, String groupName) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List subGroups = new ArrayList();
        List rollups = null;
        try {
            rollups = delegator.findByAnd("SharkGroupRollup", UtilMisc.toMap("groupName", groupName));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        if (rollups != null && rollups.size() > 0) {
            Iterator i = rollups.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                subGroups.add(v.getString("subGroupName"));
            }
        }
        return subGroups;
    }

    public List getAllSubgroups(UserTransaction trans, List groupNames) throws RootException {
        List subGroups = new ArrayList();
        if (groupNames != null && groupNames.size() > 0) {
            Iterator i = groupNames.iterator();
            while (i.hasNext()) {
                String groupName = (String) i.next();
                subGroups.addAll(getAllSubgroups(trans, groupName));
            }
        }
        return subGroups;
    }

    public List getAllImmediateSubgroups(UserTransaction trans, String groupName) throws RootException {
        return this.getAllSubgroups(trans, groupName);
    }

    public void createGroup(UserTransaction trans, String groupName, String description) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue group = delegator.makeValue("SharkGroup", null);
        group.set("groupName", groupName);
        group.set("description", description);
        try {
            delegator.create(group);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
    }

    public void removeGroup(UserTransaction trans, String groupName) throws RootException {
        GenericValue group = getGroup(groupName);
        if (group != null) {
            try {
                group.remove();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public boolean doesGroupExist(UserTransaction trans, String groupName) throws RootException {
        GenericValue group = getGroup(groupName);
        if (group != null) {
            return true;
        }
        return false;
    }

    public boolean doesGroupBelongToGroup(UserTransaction trans, String groupName, String subGroupName) throws RootException {
        GenericValue rollup = getGroupRollup(groupName, subGroupName);
        if (rollup != null) {
            return true;
        }
        return false;
    }

    public void updateGroup(UserTransaction trans, String groupName, String description) throws RootException {
        GenericValue group = getGroup(groupName);
        if (group != null) {
            group.set("description", description);
            try {
                group.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public void addGroupToGroup(UserTransaction trans, String parentGroupName, String groupName) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue rollup = delegator.makeValue("SharkGroupRollup", null);
        rollup.set("parentGroupName", parentGroupName);
        rollup.set("groupName", groupName);
        try {
            delegator.create(rollup);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
    }

    public void removeGroupFromGroup(UserTransaction trans, String parentGroup, String group) throws RootException {
        GenericValue rollup = getGroupRollup(parentGroup, group);
        if (rollup != null) {
            try {
                rollup.remove();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public void removeGroupTree(UserTransaction trans, String s) throws RootException {
        // TODO: Implement Me!
    }

    public void removeUsersFromGroupTree(UserTransaction trans, String s) throws RootException {
        // TODO: Implement Me!
    }

    public void moveGroup(UserTransaction trans, String currentParentGroup, String newParentGroup, String groupName) throws RootException {
        this.removeGroupFromGroup(trans, currentParentGroup, groupName);
        this.addGroupToGroup(trans, newParentGroup, groupName);
    }

    public String getGroupDescription(UserTransaction trans, String groupName) throws RootException {
        GenericValue group = getGroup(groupName);
        if (group != null) {
            return group.getString("description");
        }
        return null;
    }

    public void addUserToGroup(UserTransaction trans, String groupName, String username) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue member = delegator.makeValue("SharkGroupMember", null);
        member.set("groupName", groupName);
        member.set("userName", username);
        try {
            delegator.create(member);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
    }

    public void removeUserFromGroup(UserTransaction trans, String groupName, String username) throws RootException {
        GenericValue member = getGroupMember(groupName, username);
        if (member != null) {
            try {
                member.remove();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public void moveUser(UserTransaction trans, String currentGroup, String newGroup, String username) throws RootException {
        this.removeUserFromGroup(trans, currentGroup, username);
        this.addUserToGroup(trans, newGroup, username);
    }

    public boolean doesUserBelongToGroup(UserTransaction trans, String groupName, String username) throws RootException {
        GenericValue member = getGroupMember(groupName, username);
        if (member != null) {
            return true;
        }
        return false;
    }

    public void createUser(UserTransaction trans, String groupName, String username, String password, String firstName, String lastName, String email) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue user = delegator.makeValue("SharkUser", null);
        user.set("userName", username);
        user.set("firstName", firstName);
        user.set("lastName", lastName);
        user.set("passwd", password);
        user.set("emailAddress", email);
        try {
            delegator.create(user);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        if (groupName != null) {
            this.addUserToGroup(trans, groupName, username);
        }
    }

    public void updateUser(UserTransaction trans, String username, String firstName, String lastName, String email) throws RootException {
        GenericValue user = getUser(username);
        if (user != null) {
            user.set("firstName", firstName);
            user.set("lastName", firstName);
            user.set("emailAddress", email);
            try {
                user.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public void removeUser(UserTransaction trans, String username) throws RootException {
        GenericValue user = getUser(username);
        if (user != null) {
            try {
                user.remove();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public boolean doesUserExist(UserTransaction trans, String username) throws RootException {
        GenericValue user = getUser(username);
        if (user == null) {
            return false;
        }
        return true;
    }

    public void setPassword(UserTransaction trans, String username, String password) throws RootException {
        GenericValue user = getUser(username);
        if (user != null) {
            user.set("passwd", password);
            try {
                user.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new RootException(e);
            }
        }
    }

    public String getUserRealName(UserTransaction trans, String username) throws RootException {
        StringBuffer buf = new StringBuffer();
        GenericValue user = getUser(username);
        if (!UtilValidate.isEmpty(user.getString("firstName"))) {
            buf.append(user.getString("firstName"));

        }
        if (!UtilValidate.isEmpty(user.getString("lastName"))) {
            if (buf.length() > 0) {
                buf.append(" ");
            }
            buf.append(user.getString("lastName"));
        }
        return buf.toString();
    }

    public String getUserFirstName(UserTransaction trans, String username) throws RootException {
        GenericValue user = getUser(username);
        return user.getString("firstName");
    }

    public String getUserLastName(UserTransaction trans, String username) throws RootException {
        GenericValue user = getUser(username);
        return user.getString("lastName");
    }

    public String getUserEMailAddress(UserTransaction trans, String username) throws RootException {
        GenericValue user = getUser(username);
        if (user != null) {
            return user.getString("emailAddress");
        }
        return null;
    }

    private GenericValue getUser(String username) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue value = null;
        try {
            value = delegator.findByPrimaryKey("SharkUser", UtilMisc.toMap("userName", username));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        return value;
    }

    private GenericValue getGroup(String groupName) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue value = null;
        try {
            value = delegator.findByPrimaryKey("SharkGroup", UtilMisc.toMap("groupName", groupName));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        return value;
    }

    private GenericValue getGroupMember(String groupName, String username) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue member = null;
        try {
            member = delegator.findByPrimaryKey("SharkGroupMember", UtilMisc.toMap("groupName", groupName, "userName", username));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        return member;
    }

    private GenericValue getGroupRollup(String parentGroup, String group) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue rollup = null;
        try {
            rollup = delegator.findByPrimaryKey("SharkGroupRollup", UtilMisc.toMap("parentGroupName", parentGroup, "groupName", group));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new RootException(e);
        }
        return rollup;
    }
}
