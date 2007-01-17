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
package org.ofbiz.minilang.method.conditional;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.security.Security;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public class HasPermissionCondition implements Conditional {
    
    SimpleMethod simpleMethod;
    
    String permission;
    String action;
    
    public HasPermissionCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        
        this.permission = element.getAttribute("permission");
        this.action = element.getAttribute("action");
    }

    public boolean checkCondition(MethodContext methodContext) {
        // only run subOps if element is empty/null
        boolean runSubOps = false;

        // if no user is logged in, treat as if the user does not have permission: do not run subops
        GenericValue userLogin = methodContext.getUserLogin();
        if (userLogin != null) {
            String permission = methodContext.expandString(this.permission);
            String action = methodContext.expandString(this.action);
            
            Security security = methodContext.getSecurity();
            if (action != null && action.length() > 0) {
                // run hasEntityPermission
                if (security.hasEntityPermission(permission, action, userLogin)) {
                    runSubOps = true;
                }
            } else {
                // run hasPermission
                if (security.hasPermission(permission, userLogin)) {
                    runSubOps = true;
                }
            }
        }
        
        return runSubOps;
    }

    public void prettyPrint(StringBuffer messageBuffer, MethodContext methodContext) {
        messageBuffer.append("has-permission[");
        messageBuffer.append(this.permission);
        if (UtilValidate.isNotEmpty(this.action)) {
            messageBuffer.append(":");
            messageBuffer.append(this.action);
        }
        messageBuffer.append("]");
    }
}
