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
package org.apache.ofbiz.webtools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;

/**
 * Web Event for doing updates on Generic Entities
 */
public class GenericWebEvent {

    private static final String MODULE = GenericWebEvent.class.getName();
    private static final String ERR_RESOURCE = "WebtoolsErrorUiLabels";

    /** An HTTP WebEvent handler that updates a Generic entity
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return Returns a String specifying the outcome state of the event. This is used to decide which event
     * to run next or which view to display. If null no event is run nor view displayed, allowing the event to
     * call a forward on a RequestDispatcher.
     */
    public static String updateGeneric(HttpServletRequest request, HttpServletResponse response) {
        String entityName = request.getParameter("entityName");
        Locale locale = UtilHttp.getLocale(request);
        if (UtilValidate.isEmpty(entityName)) {
            entityName = (String) request.getAttribute("entityName");
        }
        if (UtilValidate.isEmpty(entityName)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.entity_name_not_specified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[GenericWebEvent.updateGeneric] The entityName was not specified,"
                    + " but is required.", MODULE);
            return "error";
        }

        Security security = (Security) request.getAttribute("security");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if (security == null) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.security_object_not_found", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] The security object was not found in the request,"
                    + " please check the control servlet init.", MODULE);
            return "error";
        }
        if (delegator == null) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.delegator_object_not_found", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] The delegator object was not found in the request,"
                    + " please check the control servlet init.", MODULE);
            return "error";
        }

        ModelReader reader = delegator.getModelReader();
        ModelEntity entity = null;

        try {
            entity = reader.getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        //Check if the update came from rest call
        String updateMode = request.getParameter("UPDATE_MODE");
        Map<String, Object> pkFields = null;
        if (updateMode == null) {
            switch (UtilHttp.getRequestMethod(request)) {
            case "PUT": updateMode = "UPDATE"; break;
            case "DELETE": updateMode = "DELETE"; break;
            default: updateMode = "CREATE"; break;
            }
            try {
                pkFields = EntityUtil.getPkValuesMapFromPath(delegator.getModelEntity(entityName),
                        (String) request.getAttribute("pkValues"));
            } catch (Exception e) {

                request.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.entity_path_not_valid", locale));
                return "error";
            }
        }

        if (UtilValidate.isEmpty(updateMode)) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.update_mode_not_specified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] Update Mode was not specified, but is required;"
                    + "entityName: " + entityName, MODULE);
            return "error";
        }

        // check permissions before moving on...
        String plainTableName = entity.getPlainTableName();
        if (!security.hasEntityPermission("ENTITY_DATA", "_" + updateMode, request.getSession())
                && !security.hasEntityPermission(plainTableName, "_" + updateMode, request.getSession())) {
            Map<String, String> messageMap = UtilMisc.toMap(
                    "updateMode", updateMode,
                    "entityName", entity.getEntityName(),
                    "entityPlainTableName", plainTableName);
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.not_sufficient_permissions_01", messageMap, locale);
            errMsg += UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.not_sufficient_permissions_02", messageMap, locale) + ".";

            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            // not really successful, but error return through ERROR_MESSAGE, so quietly fail
            return "error";
        }

        GenericValue findByEntity = delegator.makeValue(entityName, pkFields);

        // get the primary key parameters...
        String errMsgPk = "";
        Iterator<ModelField> pksIter = entity.getPksIterator();
        while (pksIter.hasNext()) {
            String errMsg = "";
            ModelField field = pksIter.next();

            ModelFieldType type = null;
            try {
                type = delegator.getEntityFieldType(entity, field.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("fieldType", field.getType());
                errMsg += UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.fatal_error_param", messageMap, locale) + ".";
            }

            String fval = request.getParameter(field.getName());
            if (UtilValidate.isNotEmpty(fval)) {
                try {
                    findByEntity.setString(field.getName(), fval);
                } catch (Exception e) {
                    Map<String, String> messageMap = UtilMisc.toMap("fval", fval);
                    errMsg = errMsg + "<li>" + field.getColName() + UtilProperties.getMessage(ERR_RESOURCE, "genericWebEvent.conversion_failed",
                            messageMap, locale) + type.getJavaType() + ".";
                    Debug.logWarning("[updateGeneric] " + field.getColName() + " conversion failed: \"" + fval + "\" is not a valid "
                            + type.getJavaType() + "; entityName: " + entityName, MODULE);
                }
            }
        }

        if (!errMsgPk.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_", errMsgPk);
            return "error";
        }

        // if this is a delete, do that before getting all of the non-pk parameters and validating them
        if ("DELETE".equals(updateMode)) {
            // Remove associated/dependent entries from other tables here
            // Delete actual main entity last, just in case database is set up to do a cascading delete, caches won't get cleared
            try {
                delegator.removeByPrimaryKey(findByEntity.getPrimaryKey());
                String confirmMsg = UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.delete_succeeded", locale);
                request.setAttribute("_EVENT_MESSAGE_", confirmMsg);
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.delete_failed", locale) + ": " + e.toString();
                Debug.logWarning(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }

            return "success";
        }

        // get the non-primary key parameters
        String errMsgNonPk = "";
        Iterator<ModelField> nopksIter = entity.getNopksIterator();
        while (nopksIter.hasNext()) {
            ModelField field = nopksIter.next();

            ModelFieldType type = null;
            try {
                type = delegator.getEntityFieldType(entity, field.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                Map<String, String> messageMap = UtilMisc.toMap("fieldType", field.getType());
                errMsgNonPk += UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.fatal_error_param", messageMap, locale) + ".";
            }

            String fval = request.getParameter(field.getName());
            if (UtilValidate.isNotEmpty(fval)) {
                try {
                    findByEntity.setString(field.getName(), fval);
                } catch (Exception e) {
                    Map<String, String> messageMap = UtilMisc.toMap("fval", fval);
                    errMsgNonPk += field.getColName() + UtilProperties.getMessage(ERR_RESOURCE,
                            "genericWebEvent.conversion_failed", messageMap, locale) + type.getJavaType() + ".";
                    Debug.logWarning("[updateGeneric] " + field.getColName()
                            + " conversion failed: \"" + fval + "\" is not a valid "
                            + type.getJavaType() + "; entityName: " + entityName, MODULE);
                }
            } else {
                findByEntity.set(field.getName(), null);
            }
        }

        if (!errMsgNonPk.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_", errMsgNonPk);
            return "error";
        }


        // if the updateMode is CREATE, check to see if an entity with the specified primary key already exists
        if ("CREATE".equals(updateMode)) {
            GenericValue tempEntity = null;

            try {
                tempEntity = EntityQuery.use(delegator)
                        .from(findByEntity.getEntityName())
                        .where(findByEntity.getPrimaryKey())
                        .queryOne();
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.create_failed_by_check", locale) + ": " + e.toString();
                Debug.logWarning(e, errMsg, MODULE);

                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            if (tempEntity != null) {
                Map<String, String> messageMap = UtilMisc.toMap("primaryKey", findByEntity.getPrimaryKey().toString());
                String errMsg = "[updateGeneric] " + entity.getEntityName() + UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.already_exists_pk", messageMap, locale) + ".";
                Debug.logWarning(errMsg, MODULE);
            }
        }

        // Validate parameters...
        String errMsgParam = "";
        Iterator<ModelField> fieldIter = entity.getFieldsIterator();
        while (fieldIter.hasNext()) {
            ModelField field = fieldIter.next();

            for (String curValidate : field.getValidators()) {
                Class<?>[] paramTypes = {String.class};
                Object[] params = new Object[] {findByEntity.get(field.getName()).toString()};

                String className = "org.apache.ofbiz.base.util.UtilValidate";
                String methodName = curValidate;

                if (curValidate.indexOf('.') > 0) {
                    className = curValidate.substring(0, curValidate.lastIndexOf('.'));
                    methodName = curValidate.substring(curValidate.lastIndexOf('.') + 1);
                }
                Class<?> valClass;

                try {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    valClass = loader.loadClass(className);
                } catch (ClassNotFoundException cnfe) {
                    Debug.logError("[updateGeneric] Could not find validation class: " + className
                            + "; ignoring.", MODULE);
                    continue;
                }
                Method valMethod;

                try {
                    valMethod = valClass.getMethod(methodName, paramTypes);
                } catch (NoSuchMethodException cnfe) {
                    Debug.logError("[updateGeneric] Could not find validation method: " + methodName
                            + " of class " + className + "; ignoring.", MODULE);
                    continue;
                }

                Boolean resultBool;

                try {
                    resultBool = (Boolean) valMethod.invoke(null, params);
                } catch (Exception e) {
                    Debug.logError("[updateGeneric] Could not access validation method: " + methodName
                            + " of class " + className + "; returning true.", MODULE);
                    resultBool = Boolean.TRUE;
                }

                if (!resultBool) {
                    Field msgField;
                    String message;

                    try {
                        msgField = valClass.getField(curValidate + "Msg");
                        message = (String) msgField.get(null);
                    } catch (Exception e) {
                        Debug.logError("[updateGeneric] Could not find validation message field: " + curValidate
                                + "Msg of class " + className + "; returning generic validation failure message.", MODULE);
                        message = UtilProperties.getMessage(ERR_RESOURCE, "genericWebEvent.validation_failed", locale) + ".";
                    }
                    errMsgParam += field.getColName() + " " + curValidate + " " + UtilProperties.getMessage(ERR_RESOURCE,
                            "genericWebEvent.failed", locale) + ": " + message;

                    Debug.logWarning("[updateGeneric] " + field.getColName() + " " + curValidate + " failed: " + message, MODULE);
                }
            }
        }

        if (!errMsgParam.isEmpty()) {
            errMsgParam = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.following_error_occurred", locale) + errMsgParam;
            request.setAttribute("_ERROR_MESSAGE_", errMsgParam);
            return "error";
        }

        if ("CREATE".equals(updateMode)) {
            try {
                delegator.create(findByEntity.getEntityName(), findByEntity.getAllFields());
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("entityName", entity.getEntityName());
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.creation_param_failed", messageMap, locale)
                        + ": " + findByEntity.toString() + ": " + e.toString();
                Debug.logWarning(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else if ("UPDATE".equals(updateMode)) {
            GenericValue value = delegator.makeValue(findByEntity.getEntityName(), findByEntity.getAllFields());

            try {
                value.store();
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("entityName", entity.getEntityName());
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                        "genericWebEvent.update_of_param_failed", messageMap, locale)
                        + ": " + value.toString() + ": " + e.toString();
                Debug.logWarning(e, errMsg, MODULE);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "genericWebEvent.update_of_param_failed", messageMap, locale) + ".";

            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("updateGeneric: Update Mode specified (" + updateMode + ") was not valid for entity: "
                    + findByEntity.toString(), MODULE);
            return "error";
        }

        return "success";
    }
}
