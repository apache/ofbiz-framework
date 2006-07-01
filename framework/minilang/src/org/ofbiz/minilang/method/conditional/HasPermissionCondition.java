/*
 * $Id: HasPermissionCondition.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.method.conditional;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.security.Security;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
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
