/*
 * $Id: CheckErrors.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.List;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * An event operation that checks a message list and may introduce a return code and stop the event
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class CheckErrors extends MethodOperation {
    
    ContextAccessor errorListAcsr;
    String errorCode;

    FlexibleMessage errorPrefix;
    FlexibleMessage errorSuffix;
    FlexibleMessage messagePrefix;
    FlexibleMessage messageSuffix;

    public CheckErrors(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        errorCode = element.getAttribute("error-code");
        if (errorCode == null || errorCode.length() == 0) errorCode = "error";
        
        errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");

        errorPrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-prefix"), "check.error.prefix");
        errorSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-suffix"), "check.error.suffix");
        messagePrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-prefix"), "check.message.prefix");
        messageSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-suffix"), "check.message.suffix");
    }

    public boolean exec(MethodContext methodContext) {
        List messages = (List) errorListAcsr.get(methodContext);

        if (messages != null && messages.size() > 0) {
            String errorCode = methodContext.expandString(this.errorCode);
            
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                /* The OLD way, now puts formatting control in the template...
                String errMsg = errorPrefix.getMessage(methodContext.getLoader(), methodContext) +
                    ServiceUtil.makeMessageList(messages, messagePrefix.getMessage(methodContext.getLoader(), methodContext), 
                            messageSuffix.getMessage(methodContext.getLoader(), methodContext)) +
                            errorSuffix.getMessage(methodContext.getLoader(), methodContext);
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                 */
                methodContext.putEnv(simpleMethod.getEventErrorMessageListName(), messages);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), errorCode);
                return false;
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), errorCode);
                return false;
            } else {
                return false;
            }
        }

        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<check-errors/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
