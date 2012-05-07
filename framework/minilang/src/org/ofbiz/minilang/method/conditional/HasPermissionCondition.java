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
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangElement;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.security.Security;
import org.ofbiz.security.authz.Authorization;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public final class HasPermissionCondition extends MiniLangElement implements Conditional {

    private final FlexibleStringExpander actionFse;
    private final FlexibleStringExpander permissionFse;

    public HasPermissionCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "permission", "action");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "permission");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "permission", "action");
        }
        this.permissionFse = FlexibleStringExpander.getInstance(element.getAttribute("permission"));
        this.actionFse = FlexibleStringExpander.getInstance(element.getAttribute("action"));
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        GenericValue userLogin = methodContext.getUserLogin();
        if (userLogin != null) {
            String permission = permissionFse.expandString(methodContext.getEnvMap());
            String action = actionFse.expandString(methodContext.getEnvMap());
            if (!action.isEmpty()) {
                Security security = methodContext.getSecurity();
                if (security.hasEntityPermission(permission, action, userLogin)) {
                    return true;
                }
            } else {
                Authorization authz = methodContext.getAuthz();
                if (authz.hasPermission(userLogin.getString("userLoginId"), permission, methodContext.getEnvMap())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("has-permission[");
        messageBuffer.append(this.permissionFse);
        if (UtilValidate.isNotEmpty(this.actionFse)) {
            messageBuffer.append(":");
            messageBuffer.append(this.actionFse);
        }
        messageBuffer.append("]");
    }

    public static final class HasPermissionConditionFactory extends ConditionalFactory<HasPermissionCondition> {
        @Override
        public HasPermissionCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new HasPermissionCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-has-permission";
        }
    }
}
