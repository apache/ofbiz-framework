/*
 * $Id: CallClassMethod.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Calls a Java class method using the given fields as parameters
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CallClassMethod extends MethodOperation {
    
    public static final String module = CallClassMethod.class.getName();

    String className;
    String methodName;
    ContextAccessor retFieldAcsr;
    ContextAccessor retMapAcsr;

    /** A list of MethodObject objects to use as the method call parameters */
    List parameters;

    public CallClassMethod(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        className = element.getAttribute("class-name");
        methodName = element.getAttribute("method-name");
        retFieldAcsr = new ContextAccessor(element.getAttribute("ret-field-name"));
        retMapAcsr = new ContextAccessor(element.getAttribute("ret-map-name"));
        
        List parameterElements = UtilXml.childElementList(element);
        if (parameterElements.size() > 0) {
            parameters = new ArrayList(parameterElements.size());
            
            Iterator parameterIter = parameterElements.iterator();
            while (parameterIter.hasNext()) {
                Element parameterElement = (Element) parameterIter.next();
                MethodObject methodObject = null;
                if ("string".equals(parameterElement.getNodeName())) {
                    methodObject = new StringObject(parameterElement, simpleMethod); 
                } else if ("field".equals(parameterElement.getNodeName())) {
                    methodObject = new FieldObject(parameterElement, simpleMethod);
                } else {
                    //whoops, invalid tag here, print warning
                    Debug.logWarning("Found an unsupported tag under the call-object-method tag: " + parameterElement.getNodeName() + "; ignoring", module);
                }
                if (methodObject != null) {
                    parameters.add(methodObject);
                }
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        String className = methodContext.expandString(this.className);
        String methodName = methodContext.expandString(this.methodName);
        
        Class methodClass = null;
        try {
            methodClass = ObjectType.loadClass(className, methodContext.getLoader());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Class to create not found with name " + className + " in create-object operation", module);

            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Class to create not found with name " + className + ": " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        
        return CallObjectMethod.callMethod(simpleMethod, methodContext, parameters, methodClass, null, methodName, retFieldAcsr, retMapAcsr);
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-class-method/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
