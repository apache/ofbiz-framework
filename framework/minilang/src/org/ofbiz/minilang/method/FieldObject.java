/*
 * $Id: FieldObject.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * A type of MethodObject that represents an Object value in a certain location
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FieldObject extends MethodObject {
    
    public static final String module = FieldObject.class.getName();
    
    ContextAccessor fieldAcsr;
    ContextAccessor mapAcsr;
    String type;

    public FieldObject(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        type = element.getAttribute("type");
        if (UtilValidate.isEmpty(type)) {
            type = "String";
        }
    }

    /** Get the name for the type of the object */
    public String getTypeName() {
        return type;
    }
    
    public Class getTypeClass(ClassLoader loader) {
        try {
            return ObjectType.loadClass(type, loader);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not find class for type: " + type, module);
            return null;
        }
    }
    
    public Object getObject(MethodContext methodContext) {
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
           if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + ", not getting Object value, returning null.", module);
                return null;
            }
            fieldVal = fieldAcsr.get(fromMap, methodContext);
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal == null) {
            if (Debug.infoOn()) Debug.logInfo("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr + ", not getting Object value, returning null.", module);
            return null;
        }
        
        return fieldVal;
    }
}
