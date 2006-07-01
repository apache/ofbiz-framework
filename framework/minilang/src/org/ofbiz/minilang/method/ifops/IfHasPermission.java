/*
 * $Id: IfHasPermission.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.ifops;

import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.security.Security;
import org.w3c.dom.Element;

/**
 * Iff the user has the specified permission, process the sub-operations. Otherwise
 * process else operations if specified.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class IfHasPermission extends MethodOperation {

    protected List subOps = new LinkedList();
    protected List elseSubOps = null;

    protected FlexibleStringExpander permissionExdr;
    protected FlexibleStringExpander actionExdr;

    public IfHasPermission(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.permissionExdr = new FlexibleStringExpander(element.getAttribute("permission"));
        this.actionExdr = new FlexibleStringExpander(element.getAttribute("action"));

        SimpleMethod.readOperations(element, subOps, simpleMethod);

        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = new LinkedList();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    public boolean exec(MethodContext methodContext) {
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

    public String rawString() {
        return "<if-has-permission permission=\"" + this.permissionExdr + "\" action=\"" + this.actionExdr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
