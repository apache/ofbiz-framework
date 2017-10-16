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
import org.apache.ofbiz.security.Security;

/**
 * Web Event for doing updates on Generic Entities
 */
public class GenericWebEvent {

    public static final String module = GenericWebEvent.class.getName();
    public static final String err_resource = "WebtoolsErrorUiLabels";

    /** An HTTP WebEvent handler that updates a Generic entity
     *
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
            String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.entity_name_not_specified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[GenericWebEvent.updateGeneric] The entityName was not specified, but is required.", module);
            return "error";
        }

        Security security = (Security) request.getAttribute("security");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if (security == null) {
            String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource,"genericWebEvent.security_object_not_found", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] The security object was not found in the request, please check the control servlet init.", module);
            return "error";
        }
        if (delegator == null) {
            String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.delegator_object_not_found", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] The delegator object was not found in the request, please check the control servlet init.", module);
            return "error";
        }

        ModelReader reader = delegator.getModelReader();
        ModelEntity entity = null;

        try {
            entity = reader.getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        String updateMode = request.getParameter("UPDATE_MODE");

        if (UtilValidate.isEmpty(updateMode)) {
            String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.update_mode_not_specified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[updateGeneric] Update Mode was not specified, but is required; entityName: " + entityName, module);
            return "error";
        }

        // check permissions before moving on...
        if (!security.hasEntityPermission("ENTITY_DATA", "_" + updateMode, request.getSession()) &&
            !security.hasEntityPermission(entity.getPlainTableName(), "_" + updateMode, request.getSession())) {
                Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode, "entityName", entity.getEntityName(), "entityPlainTableName", entity.getPlainTableName());
                String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.not_sufficient_permissions_01", messageMap, locale);
                errMsg += UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.not_sufficient_permissions_02", messageMap, locale) + ".";

                request.setAttribute("_ERROR_MESSAGE_", errMsg);
            // not really successful, but error return through ERROR_MESSAGE, so quietly fail
            return "error";
        }

        GenericValue findByEntity = delegator.makeValue(entityName);

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
                Debug.logWarning(e, module);
                Map<String, String> messageMap = UtilMisc.toMap("fieldType", field.getType());
                errMsg += UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.fatal_error_param", messageMap, locale) + ".";
            }

            String fval = request.getParameter(field.getName());
            if (UtilValidate.isNotEmpty(fval)) {
                try {
                    findByEntity.setString(field.getName(), fval);
                } catch (Exception e) {
                    Map<String, String> messageMap = UtilMisc.toMap("fval", fval);
                    errMsg = errMsg + "<li>" + field.getColName() + UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.conversion_failed", messageMap, locale) + type.getJavaType() + ".";
                    Debug.logWarning("[updateGeneric] " + field.getColName() + " conversion failed: \"" + fval + "\" is not a valid " + type.getJavaType() + "; entityName: " + entityName, module);
                }
            }
        }

        if (errMsgPk.length() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errMsgPk);
            return "error";
        }

        // if this is a delete, do that before getting all of the non-pk parameters and validating them
        if ("DELETE".equals(updateMode)) {
            // Remove associated/dependent entries from other tables here
            // Delete actual main entity last, just in case database is set up to do a cascading delete, caches won't get cleared
            try {
                delegator.removeByPrimaryKey(findByEntity.getPrimaryKey());
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.delete_failed", locale) + ": " + e.toString();
                Debug.logWarning(e, errMsg, module);
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
                Debug.logWarning(e, module);
                Map<String, String> messageMap = UtilMisc.toMap("fieldType", field.getType());
                errMsgNonPk += UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.fatal_error_param", messageMap, locale) + ".";
            }

            String fval = request.getParameter(field.getName());
            if (UtilValidate.isNotEmpty(fval)) {
                try {
                    findByEntity.setString(field.getName(), fval);
                } catch (Exception e) {
                    Map<String, String> messageMap = UtilMisc.toMap("fval", fval);
                    errMsgNonPk += field.getColName() + UtilProperties.getMessage(GenericWebEvent.err_resource,
                            "genericWebEvent.conversion_failed", messageMap, locale) + type.getJavaType() + ".";
                    Debug.logWarning("[updateGeneric] " + field.getColName() + " conversion failed: \"" + fval + "\" is not a valid " + type.getJavaType() + "; entityName: " + entityName, module);
                }
            } else {
                findByEntity.set(field.getName(), null);
            }
        }

        if (errMsgNonPk.length() > 0) {
            request.setAttribute("_ERROR_MESSAGE_", errMsgNonPk);
            return "error";
        }


        // if the updateMode is CREATE, check to see if an entity with the specified primary key already exists
        if ("CREATE".equals(updateMode)) {
            GenericValue tempEntity = null;

            try {
                tempEntity = EntityQuery.use(delegator).from(findByEntity.getEntityName()).where(findByEntity.getPrimaryKey()).queryOne();
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.create_failed_by_check", locale) + ": " + e.toString();
                Debug.logWarning(e, errMsg, module);

                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            if (tempEntity != null) {
                Map<String, String> messageMap = UtilMisc.toMap("primaryKey", findByEntity.getPrimaryKey().toString());
                String errMsg = "[updateGeneric] " + entity.getEntityName() + UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.already_exists_pk", messageMap, locale)+ ".";
                Debug.logWarning(errMsg, module);
            }
        }

        // Validate parameters...
        String errMsgParam = "";
        Iterator<ModelField> fieldIter = entity.getFieldsIterator();
        while (fieldIter.hasNext()) {
            ModelField field = fieldIter.next();

            for (String curValidate : field.getValidators()) {
                Class<?>[] paramTypes = new Class[] {String.class};
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
                    Debug.logError("[updateGeneric] Could not find validation class: " + className + "; ignoring.", module);
                    continue;
                }
                Method valMethod;

                try {
                    valMethod = valClass.getMethod(methodName, paramTypes);
                } catch (NoSuchMethodException cnfe) {
                    Debug.logError("[updateGeneric] Could not find validation method: " + methodName + " of class " + className + "; ignoring.", module);
                    continue;
                }

                Boolean resultBool;

                try {
                    resultBool = (Boolean) valMethod.invoke(null, params);
                } catch (Exception e) {
                    Debug.logError("[updateGeneric] Could not access validation method: " + methodName + " of class " + className + "; returning true.", module);
                    resultBool = Boolean.TRUE;
                }

                if (!resultBool.booleanValue()) {
                    Field msgField;
                    String message;

                    try {
                        msgField = valClass.getField(curValidate + "Msg");
                        message = (String) msgField.get(null);
                    } catch (Exception e) {
                        Debug.logError("[updateGeneric] Could not find validation message field: " + curValidate + "Msg of class " + className + "; returning generic validation failure message.", module);
                        message = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.validation_failed", locale) + ".";
                    }
                    errMsgParam += field.getColName() + " " + curValidate + " " + UtilProperties.getMessage(GenericWebEvent.err_resource,
                            "genericWebEvent.failed", locale) + ": " + message;

                    Debug.logWarning("[updateGeneric] " + field.getColName() + " " + curValidate + " failed: " + message, module);
                }
            }
        }

        if (errMsgParam.length() > 0) {
            errMsgParam = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.following_error_occurred", locale) + errMsgParam;
            request.setAttribute("_ERROR_MESSAGE_", errMsgParam);
            return "error";
        }

        if ("CREATE".equals(updateMode)) {
            try {
                delegator.create(findByEntity.getEntityName(), findByEntity.getAllFields());
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("entityName", entity.getEntityName());
                String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.creation_param_failed", messageMap, locale)+ ": " + findByEntity.toString() + ": " + e.toString();
                Debug.logWarning(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else if ("UPDATE".equals(updateMode)) {
            GenericValue value = delegator.makeValue(findByEntity.getEntityName(), findByEntity.getAllFields());

            try {
                value.store();
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("entityName", entity.getEntityName());
                String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.update_of_param_failed", messageMap, locale)+ ": " + value.toString() + ": " + e.toString();
                Debug.logWarning(e, errMsg, module);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else {
            Map<String, String> messageMap = UtilMisc.toMap("updateMode", updateMode);
            String errMsg = UtilProperties.getMessage(GenericWebEvent.err_resource, "genericWebEvent.update_of_param_failed", messageMap, locale)+ ".";

            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("updateGeneric: Update Mode specified (" + updateMode + ") was not valid for entity: " + findByEntity.toString(), module);
            return "error";
        }

        return "success";
    }
}
