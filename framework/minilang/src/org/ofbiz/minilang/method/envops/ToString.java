/*
 * $Id: ToString.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.*;

import org.w3c.dom.*;

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Converts the specified field to a String, using toString()
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ToString extends MethodOperation {
    
    public static final String module = ToString.class.getName();
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    String format;
    Integer numericPadding;

    public ToString(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        format = element.getAttribute("format");
        
        String npStr = element.getAttribute("numeric-padding");
        if (UtilValidate.isNotEmpty(npStr)) {
            try {
                this.numericPadding = Integer.valueOf(npStr);
            } catch (Exception e) {
                Debug.logError(e, "Error parsing numeric-padding attribute value on the to-string element", module);
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);

            if (toMap == null) {
                // it seems silly to create a new map, but necessary since whenever
                // an env field like a Map or List is referenced it should be created, even if empty
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = new HashMap();
                mapAcsr.put(methodContext, toMap);
            }

            Object obj = fieldAcsr.get(toMap, methodContext);
            if (obj != null) {
                fieldAcsr.put(toMap, doToString(obj, methodContext), methodContext);
            }
        } else {
            Object obj = fieldAcsr.get(methodContext);
            if (obj != null) {
                fieldAcsr.put(methodContext, doToString(obj, methodContext));
            }
        }

        return true;
    }
    
    public String doToString(Object obj, MethodContext methodContext) {
        String outStr = null;
        try {
            if (UtilValidate.isNotEmpty(format)) {
                outStr = (String) ObjectType.simpleTypeConvert(obj, "java.lang.String", format, methodContext.getLocale());
            } else {
                outStr = obj.toString();
            }
        } catch (GeneralException e) {
            Debug.logError(e, "", module);
            outStr = obj.toString();
        }
        
        if (this.numericPadding != null) {
            StringBuffer outStrBfr = new StringBuffer(outStr); 
            while (this.numericPadding.intValue() > outStrBfr.length()) {
                outStrBfr.insert(0, '0');
            }
            outStr = outStrBfr.toString();
        }
        
        return outStr;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<to-string field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
