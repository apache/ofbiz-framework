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
package org.ofbiz.jcr.loader;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class OFBizLoginModule implements LoginModule {

    public static final String module = OFBizLoginModule.class.getName();

    protected Subject subject;
    protected CallbackHandler callbackHandler;
    protected Map<String, ?> sharedState;
    protected Map<String, ?> options;

    private Delegator delegator;
    private LocalDispatcher dispatcher;

    private GenericValue userLogin;

    @Override
    public boolean abort() throws LoginException {
        return logout();
    }

    @Override
    public boolean commit() throws LoginException {
        if (userLogin != null) {
            return true;
        }
        return false;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        // get the delegator
        delegator = DelegatorFactory.getDelegator("default");

        // get the dispatcher
        dispatcher = GenericDispatcher.getLocalDispatcher("auth-dispatcher", delegator);

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        if (options != null) {
            for (Map.Entry<String, ?> option : options.entrySet()) {
                Debug.logWarning("OFBizLoginModule does not support provided option [" + option.getKey() + "] with value [" + option.getValue() + "], ignoring", module);
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("userLoginId");
        PasswordCallback passwordCallback = new PasswordCallback("currentPassword", false);
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException e) {
            Debug.logError(e, module);
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            Debug.logError(e, module);
            throw new LoginException(e.getMessage());
        }

        String userLoginId = nameCallback.getName();
        String password = String.valueOf(passwordCallback.getPassword());
        passwordCallback.clearPassword();
        // try matching against the encrypted password
        try {
            GenericValue newUserLogin = delegator.findOne("UserLogin", false, "userLoginId", userLoginId);
            if (newUserLogin.getString("currentPassword") == null || newUserLogin.getString("currentPassword").equals(password)) {
                userLogin = newUserLogin;
                return true;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        // plain text password
        if (UtilValidate.isNotEmpty(password)) {
            Map<String, Object> loginCtx = FastMap.newInstance();
            loginCtx.put("login.username", userLoginId);
            loginCtx.put("login.password", password);
            try {
                Map<String, ? extends Object> result = dispatcher.runSync("userLogin", loginCtx);
                if (ServiceUtil.isSuccess(result)) {
                    userLogin = (GenericValue) result.get("userLogin");
                    return true;
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                throw new LoginException(e.getMessage());
            }
        }
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        userLogin = null;
        return true;
    }

}
