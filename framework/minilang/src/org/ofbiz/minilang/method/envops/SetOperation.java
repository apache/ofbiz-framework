/*
 * $Id: EnvToEnv.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2005-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.5
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
