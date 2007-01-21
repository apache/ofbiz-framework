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
package org.ofbiz.shark.auth;

import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityCrypto;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.common.login.LoginServices;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.config.ServiceConfigUtil;

import org.enhydra.shark.api.internal.authentication.AuthenticationManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.UserTransaction;

/**
 * Shark OFBiz Authentication Manager - Uses the OFBiz Entities
 */
public class OfbizAuthenticationMgr implements AuthenticationManager {

    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callBack) throws RootException {
        this.callBack = callBack;
    }

    public boolean validateUser(UserTransaction userTransaction, String userName, String password) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        String p = null;
        GenericValue adminUser = null;
        String pass_hash = LoginServices.getPasswordHash(password);
        try {
            adminUser = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userName));
            String a = adminUser.getString("userLoginId");
            p = adminUser.getString("currentPassword");
        } catch (GenericEntityException e) {}
        if (adminUser != null) {
            if (password.equals(p)) {
                return true;
            } else if (LoginServices.getPasswordHash(password).equals(p)){
                return true;
            } else if (LoginServices.getPasswordHash(p).equals(password)){
                return true;
            } else {
                return false;
            }
                
        }
        return false;
    }
}

