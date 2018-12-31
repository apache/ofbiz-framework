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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Generic Service Model Parameter
 */
@SuppressWarnings("serial")
public class ModelParam implements Serializable {

    public static final String module = ModelParam.class.getName();

    /** Parameter name */
    public String name;

    /** The description of this parameter */
    public String description;

    /** Paramater type */
    public String type;

    /** Parameter mode (IN/OUT/INOUT) */
    public String mode;

    /** The form label */
    public String formLabel;

    /** The entity name */
    public String entityName;

    /** The entity field name */
    public String fieldName;

    /** Request attribute to look for if not defined as a parameter */
    public String requestAttributeName;

    /** Session attribute to look for if not defined as a parameter */
    public String sessionAttributeName;

    /** Parameter prefix for creating an attribute Map */
    public String stringMapPrefix;

    /** Parameter suffix for creating an attribute List */
    public String stringListSuffix;

    /** Validation methods */
    public List<ModelParamValidator> validators;

    /** Default value */
    private String defaultValue = null;

    /** Is this Parameter required or optional? Default to false, or required */
    public boolean optional = false;
    public boolean overrideOptional = false;

    /** Is this parameter to be displayed via the form tool? */
    public boolean formDisplay = true;
    public boolean overrideFormDisplay = false;

    /** Default value */
    public String allowHtml = null;

    /** Is this Parameter set internally? */
    public boolean internal = false;

    public ModelParam() {}

    public ModelParam(ModelParam param) {
        this.name = param.name;
        this.description = param.description;
        this.type = param.type;
        this.mode = param.mode;
        this.formLabel = param.formLabel;
        this.entityName = param.entityName;
        this.fieldName = param.fieldName;
        this.requestAttributeName = param.requestAttributeName;
        this.sessionAttributeName = param.sessionAttributeName;
        this.stringMapPrefix = param.stringMapPrefix;
        this.stringListSuffix = param.stringListSuffix;
        this.validators = param.validators;
        if (param.defaultValue != null) {
            this.setDefaultValue(param.defaultValue);
        }
        this.optional = param.optional;
        this.overrideOptional = param.overrideOptional;
        this.formDisplay = param.formDisplay;
        this.overrideFormDisplay = param.overrideFormDisplay;
        this.allowHtml = param.allowHtml;
        this.internal = param.internal;
    }

    public void addValidator(String className, String methodName, String failMessage) {
        validators.add(new ModelParamValidator(className, methodName, failMessage, null, null));
    }

    public void addValidator(String className, String methodName, String failResource, String failProperty) {
        validators.add(new ModelParamValidator(className, methodName, null, failResource, failProperty));
    }

    public String getPrimaryFailMessage(Locale locale) {
        if (UtilValidate.isNotEmpty(validators)) {
            return validators.get(0).getFailMessage(locale);
        }
        return null;
    }

    public String getShortDisplayDescription() {
        return this.name + "[" + this.type + "-" + this.mode + "]" + (optional ? "" : "*");
    }

    public String getName() {
        return this.name;
    }
    // Method to retrieve form-label from model parameter object in freemarker
    public String getFormLabel() {
        return this.formLabel;
    }

    public String getType() {
        return this.type;
    }

    public String getMode() {
        return this.mode;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public boolean getInternal() {
        return this.internal;
    }

    public boolean isIn() {
        return ModelService.IN_PARAM.equals(this.mode) || ModelService.IN_OUT_PARAM.equals(this.mode);
    }

    public boolean isOut() {
        return ModelService.OUT_PARAM.equals(this.mode) || ModelService.IN_OUT_PARAM.equals(this.mode);
    }

    public boolean isOptional() {
        return this.optional;
    }

    public Object getDefaultValue() {
        Object defaultValueObj = null;
        if (this.type != null) {
            try {
                defaultValueObj = ObjectType.simpleTypeOrObjectConvert(this.defaultValue, this.type, null, null, false);
            } catch (Exception e) {
                Debug.logWarning(e, "Service attribute [" + name + "] default value could not be converted to type [" + type + "]: " + e.toString(), module);
            }
            if (defaultValueObj == null) {
                // uh-oh, conversion failed, set the String and see what happens
                defaultValueObj = this.defaultValue;
            }
        } else {
            defaultValueObj = this.defaultValue;
        }
        return defaultValueObj;
    }
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        if (this.defaultValue != null) {
            this.optional = true;
        }
        if (Debug.verboseOn()) Debug.logVerbose("Default value for attribute [" + this.name + "] set to [" + this.defaultValue + "]", module);
    }
    public void copyDefaultValue(ModelParam param) {
        this.setDefaultValue(param.defaultValue);
    }

    public boolean equals(ModelParam model) {
        return model.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowHtml, defaultValue, description, entityName, fieldName, entityName,
                fieldName, formDisplay, formLabel, internal, mode, name, optional, overrideFormDisplay,
                overrideOptional, requestAttributeName, stringListSuffix, stringMapPrefix, type, validators);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModelParam)) {
            return false;
        }
        return equals((ModelParam) obj);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append("::");
        buf.append(type).append("::");
        buf.append(mode).append("::");
        buf.append(formLabel).append("::");
        buf.append(entityName).append("::");
        buf.append(fieldName).append("::");
        buf.append(stringMapPrefix).append("::");
        buf.append(stringListSuffix).append("::");
        buf.append(optional).append("::");
        buf.append(overrideOptional).append("::");
        buf.append(formDisplay).append("::");
        buf.append(overrideFormDisplay).append("::");
        buf.append(allowHtml).append("::");
        buf.append(defaultValue).append("::");
        buf.append(internal);
        if (validators != null) {
            buf.append(validators.toString()).append("::");
        }
        return buf.toString();
    }

    public Part getWSDLPart(Definition def) {
        Part part = def.createPart();
        part.setName(this.name);
        part.setTypeName(new QName(ModelService.TNS, this.java2wsdlType()));
        return part;
    }

    protected String java2wsdlType() {
        if (ObjectType.instanceOf(java.lang.Character.class, this.type)) {
            return "std-String";
        } else if (ObjectType.instanceOf(java.lang.String.class, this.type)) {
            return "std-String";
        } else if (ObjectType.instanceOf(java.lang.Byte.class, this.type)) {
            return "std-String";
        } else if (ObjectType.instanceOf(java.lang.Boolean.class, this.type)) {
            return "std-Boolean";
        } else if (ObjectType.instanceOf(java.lang.Integer.class, this.type)) {
            return "std-Integer";
        } else if (ObjectType.instanceOf(java.lang.Double.class, this.type)) {
            return "std-Double";
        } else if (ObjectType.instanceOf(java.lang.Float.class, this.type)) {
            return "std-Float";
        } else if (ObjectType.instanceOf(java.lang.Short.class, this.type)) {
            return "std-Integer";
        } else if (ObjectType.instanceOf(java.math.BigDecimal.class, this.type)) {
            return "std-Long";
        } else if (ObjectType.instanceOf(java.math.BigInteger.class, this.type)) {
            return "std-Integer";
        } else if (ObjectType.instanceOf(java.util.Calendar.class, this.type)) {
            return "sql-Timestamp";
        } else if (ObjectType.instanceOf(com.ibm.icu.util.Calendar.class, this.type)) {
            return "sql-Timestamp";
        } else if (ObjectType.instanceOf(java.sql.Date.class, this.type)) {
            return "sql-Date";
        } else if (ObjectType.instanceOf(java.util.Date.class, this.type)) {
            return "sql-Timestamp";
        } else if (ObjectType.instanceOf(java.lang.Long.class, this.type)) {
            return "std-Long";
        } else if (ObjectType.instanceOf(java.sql.Timestamp.class, this.type)) {
            return "sql-Timestamp";
        } else if (ObjectType.instanceOf(org.apache.ofbiz.entity.GenericValue.class, this.type)) {
            return "eeval-";
        } else if (ObjectType.instanceOf(org.apache.ofbiz.entity.GenericPK.class, this.type)) {
            return "eepk-";
        } else if (ObjectType.instanceOf(java.util.Map.class, this.type)) {
            return "map-Map";
        } else if (ObjectType.instanceOf(java.util.List.class, this.type)) {
            return "col-LinkedList";
        } else {
            return "cus-obj";
        }
    }

    static class ModelParamValidator implements Serializable {
        protected String className;
        protected String methodName;
        protected String failMessage;
        protected String failResource;
        protected String failProperty;

        public ModelParamValidator(String className, String methodName, String failMessage, String failResource, String failProperty) {
            this.className = className;
            this.methodName = methodName;
            this.failMessage = failMessage;
            this.failResource = failResource;
            this.failProperty = failProperty;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getFailMessage(Locale locale) {
            if (failMessage != null) {
                return this.failMessage;
            }
            if (failResource != null && failProperty != null) {
                return UtilProperties.getMessage(failResource, failProperty, locale);
            }
            return null;
        }

        @Override
        public String toString() {
            return className + "::" + methodName + "::" + failMessage + "::" + failResource + "::" + failProperty;
        }
    }
}
