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
package org.ofbiz.service;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilProperties;

/**
 * Generic Service Model Parameter
 */
public class ModelParam implements Serializable {

    public static final String module = ModelParam.class.getName();

    /** Parameter name */
    public String name;

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

    /** Parameter prefix for creating an attribute Map */
    public String stringMapPrefix;

    /** Parameter suffix for creating an attribute List */
    public String stringListSuffix;

    /** Validation methods */
    public List validators;

    /** Default value */
    private String defaultValue = null;    

    /** Is this Parameter required or optional? Default to false, or required */
    public boolean optional = false;
    public boolean overrideOptional = false;

    /** Is this parameter to be displayed via the form tool? */
    public boolean formDisplay = true;
    public boolean overrideFormDisplay = false;
    
    /** Is this Parameter set internally? */
    public boolean internal = false;
    
    public ModelParam() {}
    
    public ModelParam(ModelParam param) {
        this.name = param.name;
        this.type = param.type;
        this.mode = param.mode;
        this.formLabel = param.formLabel;
        this.entityName = param.entityName;
        this.fieldName = param.fieldName;
        this.stringMapPrefix = param.stringMapPrefix;
        this.stringListSuffix = param.stringListSuffix;
        this.validators = param.validators;
        if (param.defaultValue != null) this.setDefaultValue(param.defaultValue);
        this.optional = param.optional;
        this.overrideOptional = param.overrideOptional;
        this.formDisplay = param.formDisplay;
        this.overrideFormDisplay = param.overrideFormDisplay;
        this.internal = param.internal;
    }

    public void addValidator(String className, String methodName, String failMessage) {
        validators.add(new ModelParamValidator(className, methodName, failMessage, null, null));
    }

    public void addValidator(String className, String methodName, String failResource, String failProperty) {
        validators.add(new ModelParamValidator(className, methodName, null, failResource, failProperty));
    }

    public String getPrimaryFailMessage(Locale locale) {
        if (validators != null && validators.size() > 0) {
            return ((ModelParamValidator) validators.get(0)).getFailMessage(locale);
        } else {
            return null;
        }
    }
    
    public Object getDefaultValue() {
        Object defaultValueObj = null;
        if (this.type != null) {
            try {
                defaultValueObj = ObjectType.simpleTypeConvert(this.defaultValue, this.type, null, null, false);
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
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
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
        buf.append(defaultValue).append("::");
        buf.append(internal);
        if (validators != null)
            buf.append(validators.toString()).append("::");
        return buf.toString();
    }

    public Part getWSDLPart(Definition def) throws WSDLException {
        Part part = def.createPart();
        part.setName(this.name);
        part.setTypeName(new QName(ModelService.XSD, this.java2wsdlType()));
        return part;
    }

    protected String java2wsdlType() throws WSDLException {
        if (ObjectType.instanceOf(java.lang.Character.class, this.type)) {
            return "string";
        } else if (ObjectType.instanceOf(java.lang.String.class, this.type)) {
            return "string";
        } else if (ObjectType.instanceOf(java.lang.Byte.class, this.type)) {
            return "byte";
        } else if (ObjectType.instanceOf(java.lang.Boolean.class, this.type)) {
            return "boolean";
        } else if (ObjectType.instanceOf(java.lang.Integer.class, this.type)) {
            return "int";
        } else if (ObjectType.instanceOf(java.lang.Double.class, this.type)) {
            return "double";
        } else if (ObjectType.instanceOf(java.lang.Float.class, this.type)) {
            return "float";
        } else if (ObjectType.instanceOf(java.lang.Short.class, this.type)) {
            return "short";
        } else if (ObjectType.instanceOf(java.math.BigDecimal.class, this.type)) {
            return "decimal";
        } else if (ObjectType.instanceOf(java.math.BigInteger.class, this.type)) {
            return "integer";
        } else if (ObjectType.instanceOf(java.util.Calendar.class, this.type)) {
            return "dateTime";
        } else if (ObjectType.instanceOf(java.util.Date.class, this.type)) {
            return "dateTime";
        }
        // TODO add array support (maybe even convert List objects); add GenericValue/Map support
        throw new WSDLException(WSDLException.OTHER_ERROR, "Service cannot be described with WSDL (" + this.name + " / " + this.type + ")");
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
            } else {
                if (failResource != null && failProperty != null) {
                    return UtilProperties.getMessage(failResource, failProperty, locale);
                }
            }
            return null;
        }

        public String toString() {
            return className + "::" + methodName + "::" + failMessage + "::" + failResource + "::" + failProperty;
        }
    }
}
