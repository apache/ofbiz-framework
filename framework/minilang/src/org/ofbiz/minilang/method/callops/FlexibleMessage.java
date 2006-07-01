/*
 * $Id: FlexibleMessage.java 5720 2005-09-13 03:10:59Z jonesde $
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
package org.ofbiz.minilang.method.callops;

import java.io.Serializable;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

import org.ofbiz.minilang.method.*;

/**
 * Simple class to wrap messages that come either from a straight string or a properties file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FlexibleMessage implements Serializable {
    
    public static final String module = FlexibleMessage.class.getName();
    
    String message = null;
    String propertyResource = null;
    boolean isProperty = false;

    public FlexibleMessage(Element element, String defaultProperty) {
        String resAttr = null;
        String propAttr = null;
        String elVal = null;

        if (element != null) {
            resAttr = element.getAttribute("resource");
            propAttr = element.getAttribute("property");
            elVal = UtilXml.elementValue(element);
        }

        if (resAttr != null && resAttr.length() > 0) {
            propertyResource = resAttr;
            message = propAttr;
            isProperty = true;
        } else if (elVal != null && elVal.length() > 0) {
            message = elVal;
            isProperty = false;
        } else {
            // put in default property
            propertyResource = "DefaultMessages";
            message = defaultProperty;
            isProperty = true;
        }
    }

    public String getMessage(ClassLoader loader, MethodContext methodContext) {
        String message = methodContext.expandString(this.message);
        String propertyResource = methodContext.expandString(this.propertyResource);
        
        // if (Debug.infoOn()) Debug.logInfo("[FlexibleMessage.getMessage] isProperty: " + isProperty + ", message: " + message + ", propertyResource: " + propertyResource, module);
        if (!isProperty && message != null) {
            // if (Debug.infoOn()) Debug.logInfo("[FlexibleMessage.getMessage] Adding message: " + message, module);
            return message;
        } else if (isProperty && propertyResource != null && message != null) {
            // URL propertyURL = UtilURL.fromResource(propertyResource, loader);
            //String propMsg = UtilProperties.getPropertyValue(propertyResource, message);
            String propMsg = UtilProperties.getMessage(propertyResource, message, methodContext.getEnvMap(), methodContext.getLocale());

            // if (Debug.infoOn()) Debug.logInfo("[FlexibleMessage.getMessage] Got property message: " + propMsg, module);
            if (propMsg == null) {
                return "In Simple Map Processing property message could not be found in resource [" + propertyResource + "] with name [" + message + "]. ";
            } else {
                return propMsg;
            }
        } else {
            Debug.logInfo("[FlexibleMessage.getMessage] No message found, returning empty string", module);
            return "";
        }
    }
}
