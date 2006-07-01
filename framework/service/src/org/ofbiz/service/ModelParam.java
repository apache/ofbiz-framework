/*
 * $Id: ModelParam.java 5720 2005-09-13 03:10:59Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 */
package org.ofbiz.service;

import java.util.List;
import java.util.Locale;
import java.io.Serializable;

import javax.wsdl.WSDLException;
import javax.wsdl.Part;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.ObjectType;

/**
 * Generic Service Model Parameter
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jon</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ModelParam implements Serializable {

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

    public boolean equals(ModelParam model) {
        if (model.name.equals(this.name))
            return true;
        return false;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(name + "::");
        buf.append(type + "::");
        buf.append(mode + "::");
        buf.append(formLabel + "::");
        buf.append(entityName + "::");
        buf.append(fieldName + "::");
        buf.append(stringMapPrefix + "::");
        buf.append(stringListSuffix + "::");
        buf.append(validators.toString() + "::");
        buf.append(optional + "::");
        buf.append(overrideOptional + "::");
        buf.append(formDisplay + "::");
        buf.append(overrideFormDisplay + "::");
        buf.append(internal);
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
