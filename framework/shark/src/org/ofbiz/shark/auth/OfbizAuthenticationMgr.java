/*
 * $Id: OfbizAuthenticationMgr.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
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
package org.ofbiz.shark.auth;

import java.util.Map;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.base.util.UtilMisc;
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class OfbizAuthenticationMgr implements AuthenticationManager {

    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callBack) throws RootException {
        this.callBack = callBack;
    }

    public boolean validateUser(UserTransaction userTransaction, String userName, String password) throws RootException {
        String service = ServiceConfigUtil.getElementAttr("authorization", "service-name");
        if (service == null) {
            throw new RootException("No Authentication Service Defined");
        }

        LocalDispatcher dispatcher = SharkContainer.getDispatcher();
        Map context = UtilMisc.toMap("login.username", userName, "login.password", password, "isServiceAuth", new Boolean(true));
        Map serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(service, context);
        } catch (GenericServiceException e) {
            throw new RootException(e);
        }

        if (!ServiceUtil.isError(serviceResult)) {
            GenericValue userLogin = (GenericValue) serviceResult.get("userLogin");
            if (userLogin != null) {
                return true;
            }
        }

        return false;
    }
}

