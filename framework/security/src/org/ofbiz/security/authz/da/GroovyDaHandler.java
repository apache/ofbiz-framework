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
package org.ofbiz.security.authz.da;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.entity.Delegator;

public class GroovyDaHandler implements DynamicAccessHandler {

    private static final String module = GroovyDaHandler.class.getName();
    protected Delegator delegator;
    
    public String getPattern() {
        return "(^.*\\.groovy$)";
    }

    public boolean handleDynamicAccess(String accessString, String userId, String permission, Map<String, ? extends Object> context) {
        Map<String,Object> bindings = FastMap.newInstance();
        bindings.put("delegator", delegator);
        bindings.put("accessString", accessString);
        bindings.put("permission", permission);
        bindings.put("userId", userId);
        bindings.put("permissionContext", context);
        
        Debug.log("Attempting to call groovy script : " + accessString, module);
        Object result = null;
        
        if (accessString.startsWith("component://")) {
            // loaded using the OFBiz location API            
            try {
                result = GroovyUtil.runScriptAtLocation(accessString, bindings);
            } catch (GeneralException e) {
                Debug.logWarning(e, module);
            }
            
        } else {
            // try the standard class path
            String classpathString = accessString.substring(0, accessString.lastIndexOf("."));
            try {
                result = GroovyUtil.runScriptFromClasspath(classpathString, bindings);
            } catch (GeneralException e) {
                Debug.logWarning(e, module);
            }
        }
       
        // parse the result
        if (result != null && (result instanceof Boolean)) {
            return (Boolean) result;
        } else {
            Debug.logWarning("Groovy DynamicAccess implementation did not return a boolean [" + accessString + "]", module);
        }
        
        return false;
    }

    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;        
    }       
}
