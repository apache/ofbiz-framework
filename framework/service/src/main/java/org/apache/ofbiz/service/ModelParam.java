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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;

/**
 * Generic Service Model Parameter
 */
@SuppressWarnings("serial")
public class ModelParam implements Serializable {

    private static final String MODULE = ModelParam.class.getName();

    /** Parameter name */
    private String name;

    /** The description of this parameter */
    private String description;

    /** Paramater type */
    private String type;

    /** Parameter mode (IN/OUT/INOUT) */
    private String mode;

    /** The form label */
    private String formLabel;

    /** The entity name */
    private String entityName;

    /** The entity field name */
    private String fieldName;

    /** Request attribute to look for if not defined as a parameter */
    private String requestAttributeName;

    /** Session attribute to look for if not defined as a parameter */
    private String sessionAttributeName;

    /** Parameter prefix for creating an attribute Map */
    private String stringMapPrefix;

    /** Parameter suffix for creating an attribute List */
    private String stringListSuffix;

    /** Validation methods */
    private List<ModelParamValidator> validators;

    /** Default value */
    private FlexibleStringExpander defaultValue = null;

    /** Is this Parameter required or optional? Default to false, or required */
    private boolean optional = false;
    private boolean overrideOptional = false;

    /** Is this parameter to be displayed via the form tool? */
    private boolean formDisplay = true;
    private boolean overrideFormDisplay = false;

    /** Default value */
    private String allowHtml = null;

    /** Is this Parameter set internally? */
    private boolean internal = false;
    /** Children attributes*/
    private ArrayList<ModelParam> children = null;

    public ModelParam() { }

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
            this.setDefaultValue(param.defaultValue.getOriginal());
        }
        this.optional = param.optional;
        this.overrideOptional = param.overrideOptional;
        this.formDisplay = param.formDisplay;
        this.overrideFormDisplay = param.overrideFormDisplay;
        this.allowHtml = param.allowHtml;
        this.internal = param.internal;
    }

    /**
     * Add validator.
     * @param className   the class name
     * @param methodName  the method name
     * @param failMessage the fail message
     */
    public void addValidator(String className, String methodName, String failMessage) {
        validators.add(new ModelParamValidator(className, methodName, failMessage, null, null));
    }

    /**
     * Add validator.
     * @param className    the class name
     * @param methodName   the method name
     * @param failResource the fail resource
     * @param failProperty the fail property
     */
    public void addValidator(String className, String methodName, String failResource, String failProperty) {
        validators.add(new ModelParamValidator(className, methodName, null, failResource, failProperty));
    }

    /**
     * Gets primary fail message.
     * @param locale the locale
     * @return the primary fail message
     */
    public String getPrimaryFailMessage(Locale locale) {
        if (UtilValidate.isNotEmpty(validators)) {
            return validators.get(0).getFailMessage(locale);
        }
        return null;
    }

    /**
     * Gets short display description.
     * @return the short display description
     */
    public String getShortDisplayDescription() {
        return this.name + "[" + this.type + "-" + this.mode + "]" + (optional ? "" : "*");
    }

    /**
     * Gets name.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets mode.
     * @param mode the mode
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Gets validators.
     * @return the validators
     */
    public List<ModelParamValidator> getValidators() {
        return validators;
    }

    /**
     * Sets form display.
     * @param formDisplay the form display
     */
    public void setFormDisplay(boolean formDisplay) {
        this.formDisplay = formDisplay;
    }

    /**
     * Sets description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets override optional.
     * @param overrideOptional the override optional
     */
    public void setOverrideOptional(boolean overrideOptional) {
        this.overrideOptional = overrideOptional;
    }

    /**
     * Sets override form display.
     * @param overrideFormDisplay the override form display
     */
    public void setOverrideFormDisplay(boolean overrideFormDisplay) {
        this.overrideFormDisplay = overrideFormDisplay;
    }

    /**
     * Sets validators.
     * @param validators the validators
     */
    public void setValidators(List<ModelParamValidator> validators) {
        this.validators = validators;
    }

    /**
     * Sets internal.
     * @param internal the internal
     */
    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    /**
     * Sets request attribute name.
     * @param requestAttributeName the request attribute name
     */
    public void setRequestAttributeName(String requestAttributeName) {
        this.requestAttributeName = requestAttributeName;
    }

    /**
     * Sets session attribute name.
     * @param sessionAttributeName the session attribute name
     */
    public void setSessionAttributeName(String sessionAttributeName) {
        this.sessionAttributeName = sessionAttributeName;
    }

    /**
     * Sets string map prefix.
     * @param stringMapPrefix the string map prefix
     */
    public void setStringMapPrefix(String stringMapPrefix) {
        this.stringMapPrefix = stringMapPrefix;
    }

    /**
     * Sets string list suffix.
     * @param stringListSuffix the string list suffix
     */
    public void setStringListSuffix(String stringListSuffix) {
        this.stringListSuffix = stringListSuffix;
    }

    /**
     * Sets name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets allow html.
     * @param allowHtml the allow html
     */
    public void setAllowHtml(String allowHtml) {
        this.allowHtml = allowHtml;
    }

    /**
     * Gets allow html.
     * @return the allow html
     */
    public String getAllowHtml() {
        return allowHtml;
    }

    /**
     * Gets request attribute name.
     * @return the request attribute name
     */
    public String getRequestAttributeName() {
        return requestAttributeName;
    }

    /**
     * Gets session attribute name.
     * @return the session attribute name
     */
    public String getSessionAttributeName() {
        return sessionAttributeName;
    }

    /**
     * Is override optional boolean.
     * @return the boolean
     */
    public boolean isOverrideOptional() {
        return overrideOptional;
    }

    /**
     * Is form display boolean.
     * @return the boolean
     */
    public boolean isFormDisplay() {
        return formDisplay;
    }

    /**
     * Is override form display boolean.
     * @return the boolean
     */
    public boolean isOverrideFormDisplay() {
        return overrideFormDisplay;
    }

    /**
     * Sets type.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets form label.
     * @param formLabel the form label
     */
    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }

    /**
     * Sets entity name.
     * @param entityName the entity name
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * Sets field name.
     * @param fieldName the field name
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Is internal boolean.
     * @return the boolean
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * Sets optional.
     * @param optional the optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Gets string map prefix.
     * @return the string map prefix
     */
    public String getStringMapPrefix() {
        return stringMapPrefix;
    }

    /**
     * Gets string list suffix.
     * @return the string list suffix
     */
    public String getStringListSuffix() {
        return stringListSuffix;
    }

    /**
     * Gets form label.
     * @return the form label
     */
// Method to retrieve form-label from model parameter object in freemarker
    public String getFormLabel() {
        return this.formLabel;
    }

    /**
     * Gets type.
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Gets mode.
     * @return the mode
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * Gets entity name.
     * @return the entity name
     */
    public String getEntityName() {
        return this.entityName;
    }

    /**
     * Gets field name.
     * @return the field name
     */
    public String getFieldName() {
        return this.fieldName;
    }

    /**
     * Gets internal.
     * @return the internal
     */
    public boolean getInternal() {
        return this.internal;
    }

    /**
     * Is in boolean.
     * @return the boolean
     */
    public boolean isIn() {
        return ModelService.IN_PARAM.equals(this.mode) || ModelService.IN_OUT_PARAM.equals(this.mode);
    }

    /**
     * Is out boolean.
     * @return the boolean
     */
    public boolean isOut() {
        return ModelService.OUT_PARAM.equals(this.mode) || ModelService.IN_OUT_PARAM.equals(this.mode);
    }

    /**
     * Is optional boolean.
     * @return the boolean
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * Gets default value.
     * @return the default value
     */
    public FlexibleStringExpander getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Gets default value.
     * @param context the context
     * @return the default value
     */
    public Object getDefaultValue(Map<String, Object> context) {
        Object defaultValueObj = null;
        if (this.type != null) {
            try {
                defaultValueObj = ObjectType.simpleTypeOrObjectConvert(this.defaultValue.expandString(context), this.type, null, null, false);
            } catch (Exception e) {
                Debug.logWarning(e, "Service attribute [" + name + "] default value could not be converted to type [" + type + "]: "
                        + e.toString(), MODULE);
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

    /**
     * Sets default value.
     * @param defaultValue the default value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = FlexibleStringExpander.getInstance(defaultValue);
        if (this.defaultValue != null) {
            this.optional = true;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Default value for attribute [" + this.name + "] set to [" + this.defaultValue + "]", MODULE);
        }
    }

    /**
     * @return the children of the attribute
     */
    public ArrayList<ModelParam> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    /**
     * Copy default value.
     * @param param the param
     */
    public void copyDefaultValue(ModelParam param) {
        this.setDefaultValue(param.defaultValue.getOriginal());
    }

    /**
     * Equals boolean.
     * @param model the model
     * @return the boolean
     */
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

    /**
     * Gets wsdl part.
     * @param def the def
     * @return the wsdl part
     */
    public Part getWSDLPart(Definition def) {
        Part part = def.createPart();
        part.setName(this.name);
        part.setTypeName(new QName(ModelService.TNS, this.java2wsdlType()));
        return part;
    }

    /**
     * Java 2 wsdl type string.
     * @return the string
     */
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
        private String className;
        private String methodName;
        private String failMessage;
        private String failResource;
        private String failProperty;

        ModelParamValidator(String className, String methodName, String failMessage, String failResource, String failProperty) {
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
