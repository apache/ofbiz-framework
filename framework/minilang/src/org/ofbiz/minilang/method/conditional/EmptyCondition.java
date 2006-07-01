/*
 * $Id: EmptyCondition.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.conditional;

import java.util.*;
import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Implements compare to a constant condition.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class EmptyCondition implements Conditional {
    
    public static final String module = EmptyCondition.class.getName();
    
    SimpleMethod simpleMethod;
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    
    public EmptyCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
    }

    public boolean checkCondition(MethodContext methodContext) {
        // only run subOps if element is empty/null
        boolean runSubOps = false;
        Object fieldVal = getFieldVal(methodContext);

        if (fieldVal == null) {
            runSubOps = true;
        } else {
            if (fieldVal instanceof String) {
                String fieldStr = (String) fieldVal;

                if (fieldStr.length() == 0) {
                    runSubOps = true;
                }
            } else if (fieldVal instanceof Collection) {
                Collection fieldCol = (Collection) fieldVal;

                if (fieldCol.size() == 0) {
                    runSubOps = true;
                }
            } else if (fieldVal instanceof Map) {
                Map fieldMap = (Map) fieldVal;

                if (fieldMap.size() == 0) {
                    runSubOps = true;
                }
            }
        }
        
        return runSubOps;
    }
    
    protected Object getFieldVal(MethodContext methodContext) {
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", running operations", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }
        return fieldVal;
    }

    public void prettyPrint(StringBuffer messageBuffer, MethodContext methodContext) {
        messageBuffer.append("empty[");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        if (methodContext != null) {
            messageBuffer.append("=");
            messageBuffer.append(getFieldVal(methodContext));
        }
        messageBuffer.append("]");
    }
}
