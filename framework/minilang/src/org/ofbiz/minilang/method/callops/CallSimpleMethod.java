/*
 * $Id: CallSimpleMethod.java 5462 2005-08-05 18:35:48Z jonesde $
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
 * An operation that calls a simple method in the same, or from another, file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CallSimpleMethod extends MethodOperation {
    
    public static final String module = CallSimpleMethod.class.getName();
    
    String xmlResource;
    String methodName;

    public CallSimpleMethod(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.methodName = element.getAttribute("method-name");
        this.xmlResource = element.getAttribute("xml-resource");
    }

    public boolean exec(MethodContext methodContext) {
        if (this.methodName != null && this.methodName.length() > 0) {
            String methodName = methodContext.expandString(this.methodName);
            String xmlResource = methodContext.expandString(this.xmlResource);

            SimpleMethod simpleMethodToCall = null;
            if (xmlResource == null || xmlResource.length() == 0) {
                simpleMethodToCall = this.simpleMethod.getSimpleMethodInSameFile(methodName);
            } else {
                Map simpleMethods = null;
                try {
                    simpleMethods = SimpleMethod.getSimpleMethods(xmlResource, methodName, methodContext.getLoader());
                } catch (MiniLangException e) {
                    String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [error getting methods from resource: " + e.getMessage() + "]";
                    Debug.logError(e, errMsg, module);
                    methodContext.setErrorReturn(errMsg, simpleMethod);
                    return false;
                }

                simpleMethodToCall = (SimpleMethod) simpleMethods.get(methodName);
            }

            if (simpleMethodToCall == null) {
                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process, could not find SimpleMethod " + methodName + " in XML document in resource: " + xmlResource;
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            }
            
            String returnVal = simpleMethodToCall.exec(methodContext);
            if (Debug.verboseOn()) Debug.logVerbose("Called inline simple-method named [" + methodName + "] in resource [" + xmlResource + "], returnVal is [" + returnVal + "]", module);
            
            if (returnVal != null && returnVal.equals(simpleMethodToCall.getDefaultErrorCode())) {
                // in this case just set the error code just in case it hasn't already been set, the error messages will already be in place...
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
                }
                return false;
            }
            
            // if the response code/meassge is error, if so show the error and return false
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                String responseCode = (String) methodContext.getEnv(simpleMethod.getEventResponseCodeName());
                if (responseCode != null && responseCode.equals(simpleMethod.getDefaultErrorCode())) {
                    Debug.logWarning("Got error [" + responseCode + "] calling inline simple-method named [" + methodName + "] in resource [" + xmlResource + "], message is " + methodContext.getEnv(simpleMethod.getEventErrorMessageName()) , module);
                    return false;
                }
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                String resonseMessage = (String) methodContext.getEnv(simpleMethod.getServiceResponseMessageName());
                if (resonseMessage != null && resonseMessage.equals(simpleMethod.getDefaultErrorCode())) {
                    Debug.logWarning("Got error [" + resonseMessage + "] calling inline simple-method named [" + methodName + "] in resource [" + xmlResource + "], message is " + methodContext.getEnv(simpleMethod.getServiceErrorMessageName()) + ", and the error message list is: " + methodContext.getEnv(simpleMethod.getServiceErrorMessageListName()), module);
                    return false;
                }
            }
        } else {
            String errMsg = "ERROR in call-simple-method: methodName was missing; not running simpleMethod";
            Debug.logError(errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }

        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-simple-method/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
