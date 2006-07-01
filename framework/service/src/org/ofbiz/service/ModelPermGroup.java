/*
 * $Id: ModelPermGroup.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.service;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * Service Permission Group Model Class
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class ModelPermGroup implements Serializable {

    public static final String module = ModelPermGroup.class.getName();

    public static final String PERM_JOIN_AND = "AND";
    public static final String PERM_JOIN_OR = "OR";

    public List permissions = new LinkedList();
    public String joinType;

    public boolean evalPermissions(Security security, GenericValue userLogin) {
        if (permissions != null && permissions.size() > 0)  {
            boolean foundOne = false;
            Iterator i = permissions.iterator();
            while (i.hasNext()) {
                ModelPermission perm = (ModelPermission) i.next();
                if (perm.evalPermission(security, userLogin)) {
                    foundOne = true;
                } else {
                    if (joinType.equals(PERM_JOIN_AND)) {
                        return false;
                    }
                }
            }
            return foundOne;
        } else {
            return true;
        }
    }
}
