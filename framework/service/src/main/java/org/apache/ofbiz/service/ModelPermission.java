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

    private static final String MODULE = ModelPermission.class.getName();

    private static final int PERMISSION = 1;
    private static final int ENTITY_PERMISSION = 2;
    private static final int PERMISSION_SERVICE = 4;

    private ModelService serviceModel = null;
    private int permissionType = 0;
    private String nameOrRole = null;
    private String action = null;
    private String permissionServiceName = null;
    private String permissionMainAction = null;
    private String permissionResourceDesc = null;
    private boolean permissionRequireNewTransaction = false;
    private boolean permissionReturnErrorOnFailure = true;
    private Boolean auth;

    private static final String RESOURCE = "ServiceErrorUiLabels";

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(serviceModel.getName()).append("::");
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

    /**
     * Eval permission map.
     * @param dctx    the dctx
     * @param context the context
     * @return the map
     */
    public Map<String, Object> evalPermission(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Security security = dctx.getSecurity();
        if (userLogin == null) {
            Debug.logInfo("Secure service requested with no userLogin object", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorUserLoginMissing", locale));
        }
        boolean hasPermission = false;
        if (Debug.verboseOn()) {
            Debug.logVerbose(" Permission : Analyse " + this.toString(), MODULE);
        }
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
            Debug.logWarning("Invalid permission type [" + permissionType + "] for permission named : " + nameOrRole
                    + " on service : " + serviceModel.getName(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorInvalidPermissionType", locale));
        }
        if (!hasPermission) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorRefused", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private boolean evalSimplePermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null permission name passed for evaluation", MODULE);
            return false;
        }
        return security.hasPermission(nameOrRole, userLogin);
    }

    private boolean evalEntityPermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logError("Null permission name passed for evaluation", MODULE);
            return false;
        }
        if (action == null) {
            Debug.logWarning("Null action passed for evaluation", MODULE);
        }
        return security.hasEntityPermission(nameOrRole, action, userLogin);
    }

    /**
     * Sets permission service name.
     * @param permissionServiceName the permission service name
     */
    public void setPermissionServiceName(String permissionServiceName) {
        this.permissionServiceName = permissionServiceName;
    }

    /**
     * Sets permission type.
     * @param permissionType the permission type
     */
    public void setPermissionType(int permissionType) {
        this.permissionType = permissionType;
    }

    /**
     * Sets permission main action.
     * @param permissionMainAction the permission main action
     */
    public void setPermissionMainAction(String permissionMainAction) {
        this.permissionMainAction = permissionMainAction;
    }

    /**
     * Sets permission resource desc.
     * @param permissionResourceDesc the permission resource desc
     */
    public void setPermissionResourceDesc(String permissionResourceDesc) {
        this.permissionResourceDesc = permissionResourceDesc;
    }

    /**
     * Sets permission require new transaction.
     * @param permissionRequireNewTransaction the permission require new transaction
     */
    public void setPermissionRequireNewTransaction(boolean permissionRequireNewTransaction) {
        this.permissionRequireNewTransaction = permissionRequireNewTransaction;
    }

    /**
     * Sets permission return error on failure.
     * @param permissionReturnErrorOnFailure the permission return error on failure
     */
    public void setPermissionReturnErrorOnFailure(boolean permissionReturnErrorOnFailure) {
        this.permissionReturnErrorOnFailure = permissionReturnErrorOnFailure;
    }

    /**
     * Sets name or role.
     * @param nameOrRole the name or role
     */
    public void setNameOrRole(String nameOrRole) {
        this.nameOrRole = nameOrRole;
    }

    /**
     * Sets action.
     * @param action the action
     */
    public void setAction(String action) {
        this.action = action;
    }

    public static int getPERMISSION() {
        return PERMISSION;
    }

    public static int getEntityPermission() {
        return ENTITY_PERMISSION;
    }

    /**
     * Gets action.
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets auth.
     * @param auth the auth
     */
    public void setAuth(Boolean auth) {
        this.auth = auth;
    }

    /**
     * Sets service model.
     * @param serviceModel the service model
     */
    public void setServiceModel(ModelService serviceModel) {
        this.serviceModel = serviceModel;
    }

    /**
     * Gets permission service.
     * @return the permission service
     */
    public static int getPermissionService() {
        return PERMISSION_SERVICE;
    }

    private Map<String, Object> evalPermissionService(ModelService origService, DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        ModelService permission;
        Locale locale = (Locale) context.get("locale");
        if (permissionServiceName == null) {
            Debug.logWarning("No ModelService found; no service name specified!", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorDefinitionProblem", locale));
        }
        try {
            permission = dctx.getModelService(permissionServiceName);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to get ModelService: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorDefinitionProblem", locale));
        }

        permission.setAuth(true);
        Map<String, Object> ctx = permission.makeValid(context, ModelService.IN_PARAM);
        if (UtilValidate.isNotEmpty(permissionMainAction)) {
            ctx.put("mainAction", permissionMainAction);
        }
        if (UtilValidate.isNotEmpty(permissionResourceDesc)) {
            ctx.put("resourceDescription", permissionResourceDesc);
        } else if (origService != null) {
            ctx.put("resourceDescription", origService.getName());
        }
        Map<String, Object> resp;
        String failMessage = null;
        try {
            if (permissionRequireNewTransaction) {
                resp = dispatcher.runSync(permission.getName(), ctx, 300, true);
            } else {
                resp = dispatcher.runSync(permission.getName(), ctx);
            }
            failMessage = (String) resp.get("failMessage");
        } catch (GenericServiceException e) {
            Debug.logError(failMessage + e.getMessage(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorDefinitionProblem", locale));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Service permission result : hasPermission " + resp.get("hasPermission") + ", failMessage " + failMessage, MODULE);
        }
        if (permissionReturnErrorOnFailure
                && (UtilValidate.isNotEmpty(failMessage) || !((Boolean) resp.get("hasPermission")).booleanValue())) {
            if (UtilValidate.isEmpty(failMessage)) {
                failMessage = UtilProperties.getMessage(RESOURCE, "ServicePermissionErrorRefused", locale);
            }
            return ServiceUtil.returnError(failMessage);
        }
        return resp;
    }
}
