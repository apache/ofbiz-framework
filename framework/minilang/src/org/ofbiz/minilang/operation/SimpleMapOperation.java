/*
 * $Id: SimpleMapOperation.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * A single operation, does the specified operation on the given field
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class SimpleMapOperation {
    
    String message = null;
    String propertyResource = null;
    boolean isProperty = false;
    SimpleMapProcess simpleMapProcess;
    String fieldName;

    public SimpleMapOperation(Element element, SimpleMapProcess simpleMapProcess) {
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

        this.simpleMapProcess = simpleMapProcess;
        this.fieldName = simpleMapProcess.getFieldName();
    }

    public abstract void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader);

    public void addMessage(List messages, ClassLoader loader, Locale locale) {
        if (!isProperty && message != null) {
            messages.add(new MessageString(message, fieldName, true));
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] Adding message: " + message, module);
        } else if (isProperty && propertyResource != null && message != null) {
            // this one doesn't do the proper i18n: String propMsg = UtilProperties.getPropertyValue(UtilURL.fromResource(propertyResource, loader), message);
            String propMsg = UtilProperties.getMessage(propertyResource, message, locale);
            if (propMsg == null || propMsg.length() == 0) {
                messages.add(new MessageString("Simple Map Processing error occurred, but no message was found, sorry.", fieldName, propertyResource, message, locale, true));
            } else {
                messages.add(new MessageString(propMsg, fieldName, propertyResource, message, locale, true));
            }
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] Adding property message: " + propMsg, module);
        } else {
            messages.add(new MessageString("Simple Map Processing error occurred, but no message was found, sorry.", fieldName, true));
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] ERROR: No message found", module);
        }
    }
}
