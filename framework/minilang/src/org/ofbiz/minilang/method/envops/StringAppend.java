/*
 * $Id: StringAppend.java 5462 2005-08-05 18:35:48Z jonesde $
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
 * Appends the specified String to a field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.2
 */
public class StringAppend extends MethodOperation {
    
    public static final String module = StringAppend.class.getName();
    
    String string;
    String prefix;
    String suffix;
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    ContextAccessor argListAcsr;

    public StringAppend(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        prefix = element.getAttribute("prefix");
        suffix = element.getAttribute("suffix");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        argListAcsr = new ContextAccessor(element.getAttribute("arg-list-name"));
    }

    public boolean exec(MethodContext methodContext) {
        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = new HashMap();
                mapAcsr.put(methodContext, toMap);
            }
            
            String oldValue = (String) fieldAcsr.get(toMap, methodContext);
            fieldAcsr.put(toMap, this.appendString(oldValue, methodContext), methodContext);
        } else {
            String oldValue = (String) fieldAcsr.get(methodContext);
            fieldAcsr.put(methodContext, this.appendString(oldValue, methodContext));
        }

        return true;
    }
    
    public String appendString(String oldValue, MethodContext methodContext) {
        String value = methodContext.expandString(string);
        String prefixValue = methodContext.expandString(prefix);
        String suffixValue = methodContext.expandString(suffix);
        
        if (!argListAcsr.isEmpty()) {
            List argList = (List) argListAcsr.get(methodContext);
            if (argList != null && argList.size() > 0) {
                value = MessageFormat.format(value, argList.toArray());
            }
        }

        StringBuffer newValue = new StringBuffer();
        if (value != null && value.length() > 0) {
            if (oldValue == null || oldValue.length() == 0) {
                newValue.append(value);
            } else {
                newValue.append(oldValue);
                if (prefixValue != null) newValue.append(prefixValue);
                newValue.append(value);
                if (suffixValue != null) newValue.append(suffixValue);
            }
        } else {
            if (oldValue == null || oldValue.length() == 0) {
                newValue.append(oldValue);
            }
        }
        
        return newValue.toString();
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-append string=\"" + this.string + "\" prefix=\"" + this.prefix + "\" suffix=\"" + this.suffix + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
