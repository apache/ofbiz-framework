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
package org.apache.ofbiz.service;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;

/**
 * Service Permission Model Class
 */
@SuppressWarnings("serial")
public class ModelPermission implements Serializable {

    public static final String module = ModelPermission.class.getName();

    public static final int PERMISSION = 1;
    public static final int ENTITY_PERMISSION = 2;
    public static final int PERMISSION_SERVICE = 4;

    public ModelService serviceModel = null;
    public int permissionType = 0;
    public String nameOrRole = null;
    public String action = null;
    public String permissionServiceName = null;
    public String permissionMainAction = null;
    public String permissionResourceDesc = null;
    public boolean permissionRequireNewTransaction = false;
    public boolean permissionReturnErrorOnFailure = true;
    public Boolean auth;

    public static final String resource = "ServiceErrorUiLabels";

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(serviceModel.name).append("::");
        buf.append(permissionType).append("::");
        buf.append(nameOrRole).append("::");
        buf.append(action).append("::");
        buf.append(permissionServiceName).append("::");
        buf.append(permissionMainAction).append("::");
        buf.append(permissionResourceDesc).append("::");
        buf.append(permissionRequireNewTransaction).append("::");
        buf.append(permissionReturnErrorOnFailure).append("::");
        return buf.toString();
    }

    public Map<String, Object> evalPermission(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Security security = dctx.getSecurity();
        if (userLogin == null) {
            Debug.logInfo("Secure service requested with no userLogin object", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorUserLoginMissing", locale));
        }
        boolean hasPermission = false;
        if (Debug.verboseOn()) Debug.logVerbose(" Permission : Analyse " + this.toString(), module);
        switch (permissionType) {
            case PERMISSION:
                hasPermission = evalSimplePermission(security, userLogin);
                break;
            case ENTITY_PERMISSION:
                hasPermission = evalEntityPermission(security, userLogin);
                break;
            case PERMISSION_SERVICE:
                return evalPermissionService(serviceModel, dctx, context);
            default:
                Debug.logWarning("Invalid permission type [" + permissionType + "] for permission named : " + nameOrRole + " on service : " + serviceModel.name, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorInvalidPermissionType", locale));
        }
        if (! hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorRefused", locale));
        }
        return ServiceUtil.returnSuccess();
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
            Debug.logError("Null permission name passed for evaluation", module);
            return false;
        }
        if (action == null) {
            Debug.logWarning("Null action passed for evaluation",  module);
        }
        return security.hasEntityPermission(nameOrRole, action, userLogin);
    }

    private Map<String, Object> evalPermissionService(ModelService origService, DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        ModelService permission;
        Locale locale = (Locale) context.get("locale");
        if (permissionServiceName == null) {
            Debug.logWarning("No ModelService found; no service name specified!", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorDefinitionProblem", locale));
        }
        try {
            permission = dctx.getModelService(permissionServiceName);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to get ModelService: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorDefinitionProblem", locale));
        }

        permission.auth = true;
        Map<String, Object> ctx = permission.makeValid(context, ModelService.IN_PARAM);
        if (UtilValidate.isNotEmpty(action)) {
            ctx.put("mainAction", action);
        }
        if (UtilValidate.isNotEmpty(permissionResourceDesc)) {
            ctx.put("resourceDescription", permissionResourceDesc);
        } else if (origService != null) {
            ctx.put("resourceDescription", origService.name);
        }
        Map<String, Object> resp;
        String failMessage = null;
        try {
            if (permissionRequireNewTransaction) {
                resp = dispatcher.runSync(permission.name, ctx, 300, true);
            } else {
                resp = dispatcher.runSync(permission.name, ctx);
            }
            failMessage = (String) resp.get("failMessage");
        } catch (GenericServiceException e) {
            Debug.logError(failMessage + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServicePermissionErrorDefinitionProblem", locale));
        }
        if (Debug.verboseOn()) Debug.logVerbose("Service permision result : hasPermission " + resp.get("hasPermission") + ", failMessage " + failMessage , module);
        if (permissionReturnErrorOnFailure &&
                (UtilValidate.isNotEmpty(failMessage) || ! ((Boolean) resp.get("hasPermission")).booleanValue())) {
            if (UtilValidate.isEmpty(failMessage)) failMessage = UtilProperties.getMessage(resource, "ServicePermissionErrorRefused", locale);
            return ServiceUtil.returnError(failMessage);
        }
        return resp;
    }
}
