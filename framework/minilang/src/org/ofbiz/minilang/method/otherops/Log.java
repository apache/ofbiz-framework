/*
 * $Id: Log.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Calculates a result based on nested calcops.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class Log extends MethodOperation {
    
    public static final String module = Log.class.getName();

    String levelStr;
    String message;
    List methodStrings = null;
    
    public Log(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.message = element.getAttribute("message");
        this.levelStr = element.getAttribute("level");

        List methodStringElements = UtilXml.childElementList(element);
        if (methodStringElements.size() > 0) {
            methodStrings = new LinkedList();
            
            Iterator methodStringIter = methodStringElements.iterator();
            while (methodStringIter.hasNext()) {
                Element methodStringElement = (Element) methodStringIter.next();
                if ("string".equals(methodStringElement.getNodeName())) {
                    methodStrings.add(new StringString(methodStringElement, simpleMethod)); 
                } else if ("field".equals(methodStringElement.getNodeName())) {
                    methodStrings.add(new FieldString(methodStringElement, simpleMethod)); 
                } else {
                    //whoops, invalid tag here, print warning
                    Debug.logWarning("Found an unsupported tag under the log tag: " + methodStringElement.getNodeName() + "; ignoring", module);
                }
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        String levelStr = methodContext.expandString(this.levelStr);
        String message = methodContext.expandString(this.message);
        
        int level;
        Integer levelInt = Debug.getLevelFromString(levelStr);
        if (levelInt == null) {
            Debug.logWarning("Specified level [" + levelStr + "] was not valid, using INFO", module);
            level = Debug.INFO;
        } else {
            level = levelInt.intValue();
        }

        //bail out quick if the logging level isn't on, ie don't even create string
        if (!Debug.isOn(level)) {
            return true;
        }
        
        StringBuffer buf = new StringBuffer();
        
        if (message != null) buf.append(message);
        
        if (methodStrings != null) {
            Iterator methodStringsIter = methodStrings.iterator();
            while (methodStringsIter.hasNext()) {
                MethodString methodString = (MethodString) methodStringsIter.next();
                String strValue = methodString.getString(methodContext);
                if (strValue != null) buf.append(strValue);
            }
        }        

        Debug.log(level, null, buf.toString(), module);
        
        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<log level=\"" + this.levelStr + "\" message=\"" + this.message + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
