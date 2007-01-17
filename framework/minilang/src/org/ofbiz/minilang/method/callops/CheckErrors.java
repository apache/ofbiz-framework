/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
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
