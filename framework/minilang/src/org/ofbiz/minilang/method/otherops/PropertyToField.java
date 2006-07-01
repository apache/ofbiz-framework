/*
 * $Id: PropertyToField.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.otherops;

import java.text.*;
import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies an properties file property value to a field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class PropertyToField extends MethodOperation {
    
    public static final String module = PropertyToField.class.getName();
    
    String resource;
    String property;
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    String defaultVal;
    boolean noLocale;
    ContextAccessor argListAcsr;

    public PropertyToField(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        resource = element.getAttribute("resource");
        property = element.getAttribute("property");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        defaultVal = element.getAttribute("default");
        // defaults to false, ie anything but true is false
        noLocale = "true".equals(element.getAttribute("no-locale"));
        argListAcsr = new ContextAccessor(element.getAttribute("arg-list-name"));
    }

    public boolean exec(MethodContext methodContext) {
        String resource = methodContext.expandString(this.resource);
        String property = methodContext.expandString(this.property);
        
        String value = null;
        if (noLocale) {
            value = UtilProperties.getPropertyValue(resource, property);
        } else {
            value = UtilProperties.getMessage(resource, property, methodContext.getLocale());
        }
        if (value == null || value.length() == 0) {
            value = defaultVal;
        }
        
        // note that expanding the value string here will handle defaultValue and the string from 
        //  the properties file; if we decide later that we don't want the string from the properties 
        //  file to be expanded we should just expand the defaultValue at the beginning of this method.
        value = methodContext.expandString(value);

        if (!argListAcsr.isEmpty()) {
            List argList = (List) argListAcsr.get(methodContext);
            if (argList != null && argList.size() > 0) {
                value = MessageFormat.format(value, argList.toArray());
            }
        }

        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);

            if (toMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", creating new map", module);
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
        // TODO: add all attributes and other info
        return "<property-to-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
