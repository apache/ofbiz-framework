/*
 * $Id: StringToField.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.envops;

import java.text.*;
import java.util.*;

import org.w3c.dom.*;

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies the specified String to a field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class StringToField extends MethodOperation {
    
    public static final String module = StringToField.class.getName();
    
    String string;
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    ContextAccessor argListAcsr;
    String messageFieldName;

    public StringToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        argListAcsr = new ContextAccessor(element.getAttribute("arg-list-name"));
        messageFieldName = element.getAttribute("message-field-name");
    }

    public boolean exec(MethodContext methodContext) {
        String valueStr = methodContext.expandString(string);
        
        if (!argListAcsr.isEmpty()) {
            List argList = (List) argListAcsr.get(methodContext);
            if (argList != null && argList.size() > 0) {
                valueStr = MessageFormat.format(valueStr, argList.toArray());
            }
        }

        Object value;
        if (this.messageFieldName != null && this.messageFieldName.length() > 0) {
            value = new MessageString(valueStr, this.messageFieldName, true);
        } else {
            value = valueStr;
        }
        
        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = new HashMap();
                mapAcsr.put(methodContext, toMap);
            }
            fieldAcsr.put(toMap, value, methodContext);
        } else {
            fieldAcsr.put(methodContext, value);
        }

        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-to-field string=\"" + this.string + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
