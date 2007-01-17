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
package org.ofbiz.minilang.method.envops;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * A general set operation to set a field from another field or from a value. Also supports a default-value, and type conversion.
 */
public class SetOperation extends MethodOperation {
    public static final String module = SetOperation.class.getName();
    
    protected ContextAccessor field;
    protected ContextAccessor fromField;
    protected FlexibleStringExpander valueExdr;
    protected FlexibleStringExpander defaultExdr;
    protected String type;
    protected boolean setIfNull; // default to false
    protected boolean setIfEmpty; // default to true

    public SetOperation(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.field = new ContextAccessor(element.getAttribute("field"));
        this.fromField = new ContextAccessor(element.getAttribute("from-field"));
        this.valueExdr = new FlexibleStringExpander(element.getAttribute("value"));
        this.defaultExdr = new FlexibleStringExpander(element.getAttribute("default-value"));
        this.type = element.getAttribute("type");
        // default to false, anything but true is false
        this.setIfNull = "true".equals(element.getAttribute("set-if-null"));
        // default to true, anything but false is true
        this.setIfEmpty = !"false".equals(element.getAttribute("set-if-empty"));

        if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify a from-field [" + element.getAttribute("from-field") + "] and a value [" + element.getAttribute("value") + "] on the set action in a screen widget");
        }
    }

    public boolean exec(MethodContext methodContext) {
        Object newValue = null;
        if (!this.fromField.isEmpty()) {
            newValue = this.fromField.get(methodContext);
            if (Debug.verboseOn()) Debug.logVerbose("In screen getting value for field from [" + this.fromField.toString() + "]: " + newValue, module);
        } else if (!this.valueExdr.isEmpty()) {
            newValue = methodContext.expandString(this.valueExdr);
        }

        // If newValue is still empty, use the default value
        if (ObjectType.isEmpty(newValue) && !this.defaultExdr.isEmpty()) {
            newValue = methodContext.expandString(this.defaultExdr);
        }

        if (!setIfNull && newValue == null) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found (null) with name [" + fromField + "] and value [" + valueExdr + "], and there was not default value, not setting field", module);
            return true;
        }
        if (!setIfEmpty && ObjectType.isEmpty(newValue)) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found (empty) with name [" + fromField + "] and value [" + valueExdr + "], and there was not default value, not setting field", module);
            return true;
        }

        if (UtilValidate.isNotEmpty(this.type)) {
            try {
                newValue = ObjectType.simpleTypeConvert(newValue, this.type, null, null);
            } catch (GeneralException e) {
                String errMsg = "Could not convert field value for the field: [" + this.field.toString() + "] to the [" + this.type + "] type for the value [" + newValue + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            }
        }
        
        if (Debug.verboseOn()) Debug.logVerbose("In screen setting field [" + this.field.toString() + "] to value: " + newValue, module);
        this.field.put(methodContext, newValue);
        return true;
    }

    public String rawString() {
        return "<set field=\"" + this.field 
                + (this.valueExdr.isEmpty() ? "" : "\" value=\"" + this.valueExdr.getOriginal()) 
                + (this.fromField.isEmpty() ? "" : "\" from-field=\"" + this.fromField) 
                + (this.defaultExdr.isEmpty() ? "" : "\" default-value=\"" + this.defaultExdr.getOriginal()) 
                + (this.type == null || this.type.length() == 0 ? "" : "\" type=\"" + this.type) 
                + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
