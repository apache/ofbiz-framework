/*
 * $Id: ModelPermission.java 5462 2005-08-05 18:35:48Z jonesde $
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
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;

import java.util.List;
import java.io.Serializable;

/**
 * Service Permission Model Class
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class ModelPermission implements Serializable {

    public static final String module = ModelPermission.class.getName();

    public static final int PERMISSION = 1;
    public static final int ENTITY_PERMISSION = 2;
    public static final int ROLE_MEMBER = 3;

    public ModelService serviceModel = null;
    public int permissionType = 0;
    public String nameOrRole = null;
    public String action = null;

    public boolean evalPermission(Security security, GenericValue userLogin) {
        if (userLogin == null) {
            Debug.logInfo("Secure service requested with no userLogin object", module);
            return false;
        }
        switch (permissionType) {
            case 1:
                return evalSimplePermission(security, userLogin);
            case 2:
                return evalEntityPermission(security, userLogin);
            case 3:
                return evalRoleMember(userLogin);
            default:
                Debug.logWarning("Invalid permission type [" + permissionType + "] for permission named : " + nameOrRole + " on service : " + serviceModel.name, module);
                return false;
        }
    }

    private boolean evalSimplePermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null permission name passed for evaluation", module);
            return false;
        }
        return security.hasPermission(nameOrRole, userLogin);
    }

    private boolean evalEntityPermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null permission name passed for evaluation", module);
            return false;
        }
        if (action == null) {
            Debug.logWarning("Null action passed for evaluation",  module);
        }
        return security.hasEntityPermission(nameOrRole, action, userLogin);
    }

    private boolean evalRoleMember(GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null role type name passed for evaluation", module);
            return false;
        }
        GenericDelegator delegator = userLogin.getDelegator();
        List partyRoles = null;
        try {
            partyRoles = delegator.findByAnd("PartyRole", UtilMisc.toMap("roleTypeId", nameOrRole, "partyId", userLogin.get("partyId")));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to lookup PartyRole records", module);
        }

        if (partyRoles != null && partyRoles.size() > 0) {
            partyRoles = EntityUtil.filterByDate(partyRoles);
            if (partyRoles != null && partyRoles.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
