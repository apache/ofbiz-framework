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
package org.ofbiz.minilang.method.ifops;

import java.util.Collections;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.security.Security;
import org.ofbiz.security.authz.Authorization;
import org.w3c.dom.Element;

/**
 * If the user has the specified permission, process the sub-operations. Otherwise process else operations if specified.
 */
public class IfHasPermission extends MethodOperation {

    protected FlexibleStringExpander actionExdr;
    protected List<MethodOperation> elseSubOps = null;
    protected FlexibleStringExpander permissionExdr;
    protected List<MethodOperation> subOps;

    public IfHasPermission(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        this.permissionExdr = FlexibleStringExpander.getInstance(element.getAttribute("permission"));
        this.actionExdr = FlexibleStringExpander.getInstance(element.getAttribute("action"));
        this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            this.elseSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(elseElement, simpleMethod));
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        // if conditions fails, always return true; if a sub-op returns false
        // return false and stop, otherwise return true
        // return true;
        // only run subOps if element is empty/null
        boolean runSubOps = false;
        // if no user is logged in, treat as if the user does not have permission: do not run subops
        GenericValue userLogin = methodContext.getUserLogin();
        if (userLogin != null) {
            String permission = methodContext.expandString(permissionExdr);
            String action = methodContext.expandString(actionExdr);
            Authorization authz = methodContext.getAuthz();
            Security security = methodContext.getSecurity();
            if (UtilValidate.isNotEmpty(action)) {
                // run hasEntityPermission
                if (security.hasEntityPermission(permission, action, userLogin)) {
                    runSubOps = true;
                }
            } else {
                // run hasPermission
                if (authz.hasPermission(userLogin.getString("userLoginId"), permission, methodContext.getEnvMap())) {
                    runSubOps = true;
                }
            }
        }
        if (runSubOps) {
            return SimpleMethod.runSubOps(subOps, methodContext);
        } else {
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.subOps);
        if (this.elseSubOps != null)
            allSubOps.addAll(this.elseSubOps);
        return allSubOps;
    }

    @Override
    public String rawString() {
        return "<if-has-permission permission=\"" + this.permissionExdr + "\" action=\"" + this.actionExdr + "\"/>";
    }

    public static final class IfHasPermissionFactory implements Factory<IfHasPermission> {
        public IfHasPermission createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new IfHasPermission(element, simpleMethod);
        }

        public String getName() {
            return "if-has-permission";
        }
    }
}
