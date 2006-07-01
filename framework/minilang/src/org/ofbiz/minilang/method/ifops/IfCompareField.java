/*
 * $Id: IfCompareField.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.ifops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

import org.ofbiz.minilang.operation.*;

/**
 * Iff the comparison between the specified field and the other field is true process sub-operations
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class IfCompareField extends MethodOperation {
    
    public static final String module = IfCompareField.class.getName();

    List subOps = new LinkedList();
    List elseSubOps = null;

    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    ContextAccessor toMapAcsr;
    ContextAccessor toFieldAcsr;

    String operator;
    String type;
    String format;

    public IfCompareField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        
        this.toMapAcsr = new ContextAccessor(element.getAttribute("to-map-name"));
        // set fieldAcsr to their defualt value of fieldAcsr if empty
        this.toFieldAcsr = new ContextAccessor(element.getAttribute("to-field-name"), element.getAttribute("field-name"));

        // do NOT default the to-map-name to the map-name because that
        //would make it impossible to compare from a map field to an 
        //environment field

        this.operator = element.getAttribute("operator");
        this.type = element.getAttribute("type");
        this.format = element.getAttribute("format");

        SimpleMethod.readOperations(element, subOps, simpleMethod);
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = new LinkedList();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    public boolean exec(MethodContext methodContext) {
        // if conditions fails, always return true; if a sub-op returns false 
        // return false and stop, otherwise return true

        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);

        Object fieldVal1 = null;
        Object fieldVal2 = null;

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", using null for comparison", module);
            } else {
                fieldVal1 = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal1 = fieldAcsr.get(methodContext);
        }

        if (!toMapAcsr.isEmpty()) {
            Map toMap = (Map) toMapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.infoOn()) Debug.logInfo("To Map not found with name " + toMapAcsr + ", using null for comparison", module);
            } else {
                fieldVal2 = toFieldAcsr.get(toMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal2 = toFieldAcsr.get(methodContext);
        }

        List messages = new LinkedList();
        Boolean resultBool = BaseCompare.doRealCompare(fieldVal1, fieldVal2, operator, type, format, messages, null, methodContext.getLoader());

        if (messages.size() > 0) {
            messages.add(0, "Error with comparison in if-compare-field between fields [" + mapAcsr.toString() + "." + fieldAcsr.toString() + "] with value [" + fieldVal1 + "] and [" + toMapAcsr.toString() + "." + toFieldAcsr.toString() + "] with value [" + fieldVal2 + "] with operator [" + operator + "] and type [" + type + "]: ");
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                StringBuffer fullString = new StringBuffer();

                Iterator miter = messages.iterator();
                while (miter.hasNext()) {
                    fullString.append((String) miter.next());
                }
                Debug.logWarning(fullString.toString(), module);

                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), fullString.toString());
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }

        if (resultBool != null && resultBool.booleanValue()) {
            return SimpleMethod.runSubOps(subOps, methodContext);
        } else {
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<if-compare-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
