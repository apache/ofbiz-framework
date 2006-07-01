/*
 * $Id: IfNotEmpty.java 5462 2005-08-05 18:35:48Z jonesde $
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

/**
 * Iff the specified field is not empty process sub-operations
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class IfNotEmpty extends MethodOperation {
    
    public static final String module = IfNotEmpty.class.getName();

    List subOps = new LinkedList();
    List elseSubOps = null;

    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;

    public IfNotEmpty(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));

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
        // return true;

        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", not running operations", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal == null) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr + ", not running operations", module);
        }

        // only run subOps if element is not empty/null
        boolean runSubOps = !ObjectType.isEmpty(fieldVal);

        if (runSubOps) {
            // if (Debug.verboseOn()) Debug.logVerbose("IfNotEmpty: Running if operations mapAcsr=" + mapAcsr + " fieldAcsr=" + fieldAcsr, module);
            return SimpleMethod.runSubOps(subOps, methodContext);
        } else {
            if (elseSubOps != null) {
                // if (Debug.verboseOn()) Debug.logVerbose("IfNotEmpty: Running else operations mapAcsr=" + mapAcsr + " fieldAcsr=" + fieldAcsr, module);
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                // if (Debug.verboseOn()) Debug.logVerbose("IfNotEmpty: Not Running any operations mapAcsr=" + mapAcsr + " fieldAcsr=" + fieldAcsr, module);
                return true;
            }
        }
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<if-not-empty field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
