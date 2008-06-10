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

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.common.login.LoginServices;

import org.enhydra.shark.api.internal.authentication.AuthenticationManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.UserTransaction;

/**
 * Shark Generic Authentication Manager - Uses the Generic Entities
 */
public class GenericAuthenticationMgr implements AuthenticationManager {

    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callBack) throws RootException {
        this.callBack = callBack;
    }

    public boolean validateUser(UserTransaction userTransaction, String userName, String password) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue sharkUser = null;
        try {
            sharkUser = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.SharkUser, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.userName, userName));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        
        if (sharkUser != null) {
            String registeredPwd = sharkUser.getString(org.ofbiz.shark.SharkConstants.passwd);
            if (password.equals(registeredPwd)) {
                return true;
            } else if (LoginServices.getPasswordHash(password).equals(registeredPwd)){
                return true;
            } else if (LoginServices.getPasswordHash(registeredPwd).equals(password)){
                return true;
            } else {
                return false;
            }
        }
 
        return false;
    }
}
