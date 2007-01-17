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
package org.ofbiz.guiapp.xui;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.LoginWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class XuiSession {

    public static final String module = XuiSession.class.getName();

    protected GenericDelegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue userLogin = null;
    protected XuiContainer container = null;
    protected Map attributes = new HashMap();
    protected String id = null;


    public XuiSession(String id, GenericDelegator delegator, LocalDispatcher dispatcher, XuiContainer container) {
        this.id = id;
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.container = container;
        Debug.logInfo("Created XuiSession [" + id + "]", module);
    }

    public XuiContainer getContainer() {
        return this.container;
    }
    
    public GenericDelegator getDelegator() {
        return this.delegator;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String getId() {
        return this.id;
    }

    public String getUserId() {
        if (this.userLogin == null) {
            return null;
        } else {
            return this.userLogin.getString("userLoginId");
        }
    }

    public String getUserPartyId() {
        if (this.userLogin == null) {
            return null;
        } else {
            return this.userLogin.getString("partyId");
        }
    }

    public void logout() {
        if (this.userLogin != null) {
            LoginWorker.setLoggedOut(this.userLogin.getString("userLoginId"), this.getDelegator());
            this.userLogin = null;
        }
    }

    public void login(String username, String password) throws UserLoginFailure {
        // if already logged in; verify for lock
        if (this.userLogin != null) {
            if (!userLogin.getString("userLoginId").equals(username)) {
                throw new UserLoginFailure("Username does not match already logged in user!");
            }
        }
        this.userLogin = this.checkLogin(username, password);
    }

    public GenericValue checkLogin(String username, String password) throws UserLoginFailure {
        // check the required parameters and objects
        if (dispatcher == null) {
            throw new UserLoginFailure("Unable to log in; XUI not configured propertly");
        }
        if (UtilValidate.isEmpty(username)) {
            throw new UserLoginFailure("Username is missing");
        }
        if (UtilValidate.isEmpty(password)) {
            throw new UserLoginFailure("Password is missing");
        }

        // call the login service
        Map result = null;
        try {
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new UserLoginFailure(e);
        } catch (Throwable t) {
            Debug.logError(t, "Thowable caught!", module);
        }

        // check for errors
        if (ServiceUtil.isError(result)) {
            throw new UserLoginFailure(ServiceUtil.getErrorMessage(result));
        } else {
            GenericValue ul = (GenericValue) result.get("userLogin");
            if (ul == null) {
                throw new UserLoginFailure("UserLogin return was not valid (null)");
            }
            return ul;
        }
    }

    public boolean hasRole(GenericValue userLogin, String roleTypeId) {
        if (userLogin == null || roleTypeId == null) {
            return false;
        }
        String partyId = userLogin.getString("partyId");
        GenericValue partyRole = null;
        try {
            partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return false;
        }

        if (partyRole == null) {
            return false;
        }

        return true;
    }

    public class UserLoginFailure extends GeneralException {
        public UserLoginFailure() {
            super();
        }

        public UserLoginFailure(String str) {
            super(str);
        }

        public UserLoginFailure(String str, Throwable nested) {
            super(str, nested);
        }

        public UserLoginFailure(Throwable nested) {
            super(nested);
        }
    }
}
