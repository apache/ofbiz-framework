/*
 * $Id: CheckId.java 5462 2005-08-05 18:35:48Z jonesde $
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
 * Iff the given ID field is not valid the fail-message 
 * or fail-property sub-elements are used to add a message to the error-list.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CheckId extends MethodOperation {
    
    public static final String module = CheckId.class.getName();
    
    String message = null;
    String propertyResource = null;
    boolean isProperty = false;

    ContextAccessor fieldAcsr;
    ContextAccessor mapAcsr;
    ContextAccessor errorListAcsr;

    public CheckId(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");

        //note: if no fail-message or fail-property then message will be null
        Element failMessage = UtilXml.firstChildElement(element, "fail-message");
        Element failProperty = UtilXml.firstChildElement(element, "fail-property");

        if (failMessage != null) {
            this.message = failMessage.getAttribute("message");
            this.isProperty = false;
        } else if (failProperty != null) {
            this.propertyResource = failProperty.getAttribute("resource");
            this.message = failProperty.getAttribute("property");
            this.isProperty = true;
        }
    }

    public boolean exec(MethodContext methodContext) {
        boolean isValid = true;

        List messages = (List) errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = new LinkedList();
            errorListAcsr.put(methodContext, messages);
        }

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
        
        String fieldStr = fieldVal.toString();
        StringBuffer errorDetails = new StringBuffer();
        
        //check various illegal characters, etc for ids
        if (fieldStr.indexOf(' ') >= 0) {
            isValid = false;
            errorDetails.append("[space found at position " + (fieldStr.indexOf(' ') + 1) + "]");
        }
        if (fieldStr.indexOf('"') >= 0) {
            isValid = false;
            errorDetails.append("[double-quote found at position " + (fieldStr.indexOf('"') + 1) + "]");
        }
        if (fieldStr.indexOf('\'') >= 0) {
            isValid = false;
            errorDetails.append("[single-quote found at position " + (fieldStr.indexOf('\'') + 1) + "]");
        }
        if (fieldStr.indexOf('&') >= 0) {
            isValid = false;
            errorDetails.append("[ampersand found at position " + (fieldStr.indexOf('&') + 1) + "]");
        }
        if (fieldStr.indexOf('?') >= 0) {
            isValid = false;
            errorDetails.append("[question mark found at position " + (fieldStr.indexOf('?') + 1) + "]");
        }
        if (fieldStr.indexOf('<') >= 0) {
            isValid = false;
            errorDetails.append("[less-than sign found at position " + (fieldStr.indexOf('<') + 1) + "]");
        }
        if (fieldStr.indexOf('>') >= 0) {
            isValid = false;
            errorDetails.append("[greater-than sign found at position " + (fieldStr.indexOf('>') + 1) + "]");
        }
        if (fieldStr.indexOf('\\') >= 0) {
            isValid = false;
            errorDetails.append("[back-slash found at position " + (fieldStr.indexOf('\\') + 1) + "]");
        }
        if (fieldStr.indexOf('/') >= 0) {
            isValid = false;
            errorDetails.append("[forward-slash found at position " + (fieldStr.indexOf('/') + 1) + "]");
        }

        if (!isValid) {
            this.addMessage(messages, methodContext, "The ID value in the field [" + fieldAcsr + "] was not valid", ": " + errorDetails.toString());
        }

        return true;
    }

    public void addMessage(List messages, MethodContext methodContext, String defaultMessage, String errorDetails) {
        ClassLoader loader = methodContext.getLoader();
        
        String message = methodContext.expandString(this.message);
        String propertyResource = methodContext.expandString(this.propertyResource);
        
        if (!isProperty && message != null) {
            messages.add(message + errorDetails);
        } else if (isProperty && propertyResource != null && message != null) {
            //String propMsg = UtilProperties.getPropertyValue(UtilURL.fromResource(propertyResource, loader), message);
            String propMsg = UtilProperties.getMessage(propertyResource, message, methodContext.getEnvMap(), methodContext.getLocale());

            if (propMsg == null || propMsg.length() == 0) {
                messages.add(defaultMessage + errorDetails);
            } else {
                messages.add(methodContext.expandString(propMsg) + errorDetails);
            }
        } else {
            messages.add(defaultMessage + errorDetails);
        }
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<check-id field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
