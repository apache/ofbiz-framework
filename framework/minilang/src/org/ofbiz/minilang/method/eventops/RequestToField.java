/*
 * $Id: RequestToField.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.eventops;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleServletAccessor;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies a Servlet request attribute to a map field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class RequestToField extends MethodOperation {
    
    public static final String module = RequestToField.class.getName();
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    FlexibleServletAccessor requestAcsr;
    String defaultVal;

    public RequestToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        requestAcsr = new FlexibleServletAccessor(element.getAttribute("request-name"), element.getAttribute("field-name"));
        defaultVal = element.getAttribute("default");
    }

    public boolean exec(MethodContext methodContext) {
        String defaultVal = methodContext.expandString(this.defaultVal);

        Object fieldVal = null;
        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            fieldVal = requestAcsr.get(methodContext.getRequest(), methodContext.getEnvMap());
            if (fieldVal == null) {
                Debug.logWarning("Request attribute value not found with name " + requestAcsr, module);
            }
        }

        // if fieldVal is null, or is a String and has zero length, use defaultVal
        if (fieldVal == null) {
            fieldVal = defaultVal;
        } else if (fieldVal instanceof String) {
            String strVal = (String) fieldVal;

            if (strVal.length() == 0) {
                fieldVal = defaultVal;
            }
        }

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);

            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + " creating a new map", module);
                fromMap = new HashMap();
                mapAcsr.put(methodContext, fromMap);
            }

            fieldAcsr.put(fromMap, fieldVal, methodContext);
        } else {
            fieldAcsr.put(methodContext, fieldVal);
        }
        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<request-to-field request-name=\"" + this.requestAcsr + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
