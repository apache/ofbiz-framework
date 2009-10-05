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
package org.ofbiz.securityext.da;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.security.authz.da.DynamicAccessHandler;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class ServiceDaHandler implements DynamicAccessHandler {

    private static final String module = ServiceDaHandler.class.getName();
    protected LocalDispatcher dispatcher;
    protected Delegator delegator;
    
    public String getPattern() {        
        return "^service:(.*)$";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        Map<String,Object> serviceContext = FastMap.newInstance();
        serviceContext.put("userId", userId);
        serviceContext.put("permission", permission);
        serviceContext.put("accessString", accessString);
        serviceContext.put("permissionContext", context);
        
        String serviceName = accessString.substring(8);
        Map<String, Object> result;
        try {
            result = dispatcher.runSync(serviceName, serviceContext, 60, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return false;
        }
        
        if (result != null && !ServiceUtil.isError(result)) {
            Boolean reply = (Boolean) result.get("permissionGranted");
            if (reply == null) {
                reply = Boolean.FALSE;
            }
            return reply;
        } else {
            return false;
        }
    }

    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
        this.dispatcher = GenericDispatcher.getLocalDispatcher("SecurityDA", delegator);
    }
}
