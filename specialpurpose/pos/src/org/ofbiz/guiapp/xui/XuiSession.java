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
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.LoginWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class XuiSession {

    public static final String module = XuiSession.class.getName();

    protected Delegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue userLogin = null;
    protected XuiContainer container = null;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected String id = null;
    protected final boolean IS_SAME_LOGIN = UtilProperties.propertyValueEqualsIgnoreCase("xui", "isSameLogin", "true");
    private Locale locale = Locale.getDefault();

    public XuiSession(String id, Delegator delegator, LocalDispatcher dispatcher, XuiContainer container) {
        this.id = id;
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.container = container;
        Debug.logInfo("Created XuiSession [" + id + "]", module);
    }

    public XuiContainer getContainer() {
        return this.container;
    }

    public Delegator getDelegator() {
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
        // if already logged in; verify for lock. Depends on SAME_LOGIN, false by default
        if (this.userLogin != null) {
            if (IS_SAME_LOGIN == true && !userLogin.getString("userLoginId").equals(username)) {
                throw new UserLoginFailure(UtilProperties.getMessage("XuiUiLabels", "XuiUsernameDoesNotMatchLoggedUser", locale));
            }
        }
        this.userLogin = this.checkLogin(username, password);
    }

    public GenericValue checkLogin(String username, String password) throws UserLoginFailure {
        // check the required parameters and objects
        if (dispatcher == null) {
            throw new UserLoginFailure(UtilProperties.getMessage("XuiUiLabels", "XuiUnableToLogIn", locale));
        }
        if (UtilValidate.isEmpty(username)) {
            throw new UserLoginFailure(UtilProperties.getMessage("PartyUiLabels", "PartyUserNameMissing", locale));
        }
        if (UtilValidate.isEmpty(password)) {
            throw new UserLoginFailure(UtilProperties.getMessage("PartyUiLabels", "PartyPasswordMissing", locale));
        }

        // call the login service
        Map<String, Object> result = null;
        try {
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new UserLoginFailure(e);
        } catch (Throwable t) {
            Debug.logError(t, "Throwable caught!", module);
        }

        // check for errors
        if (ServiceUtil.isError(result)) {
            throw new UserLoginFailure(ServiceUtil.getErrorMessage(result));
        } else {
            GenericValue ul = (GenericValue) result.get("userLogin");
            if (ul == null) {
                throw new UserLoginFailure(UtilProperties.getMessage("XuiUiLabels", "XuiUserLoginNotValid", locale));
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
            partyRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId, "roleTypeId", roleTypeId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return false;
        }

        if (partyRole == null) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("serial")
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
