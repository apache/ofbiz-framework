/*
 * $Id: SetNonpkFields.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.entityops;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Looks for each non-PK field in the named map and if it exists there it will copy it into the named value object.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class SetNonpkFields extends MethodOperation {
    
    public static final String module = SetNonpkFields.class.getName();
    
    ContextAccessor valueAcsr;
    ContextAccessor mapAcsr;
    String setIfNullStr;

    public SetNonpkFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        setIfNullStr = element.getAttribute("set-if-null");
    }

    public boolean exec(MethodContext methodContext) {
        // if anything but false it will be true
        boolean setIfNull = !"false".equals(methodContext.expandString(setIfNullStr));
        
        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        if (value == null) {
            String errMsg = "In set-nonpk-fields a value was not found with the specified valueAcsr: " + valueAcsr + ", not setting fields";
            Debug.logWarning(errMsg, module);
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }

        Map theMap = (Map) mapAcsr.get(methodContext);
        if (theMap == null) {
            Debug.logWarning("In set-nonpk-fields could not find map with name " + mapAcsr + ", not setting any fields", module);
        } else {
            value.setNonPKFields(theMap, setIfNull);
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-nonpk-fields/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
