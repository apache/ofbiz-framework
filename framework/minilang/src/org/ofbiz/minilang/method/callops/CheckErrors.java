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

import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Halts script execution if the error message list contains any messages.
 */
public final class CheckErrors extends MethodOperation {

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        String errorListAttr = element.getAttribute("error-list-name");
        if (errorListAttr.length() > 0) {
            element.removeAttribute("error-list-name");
            return true;
        }
        return false;
    }
    
    private final FlexibleStringExpander errorCodeFse;

    public CheckErrors(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "error-code");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "error-code");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        this.errorCodeFse = FlexibleStringExpander.getInstance(element.getAttribute("error-code"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            List<Object> messages = methodContext.getEnv(this.simpleMethod.getEventErrorMessageListName());
            if (messages != null && messages.size() > 0) {
                methodContext.putEnv(this.simpleMethod.getEventResponseCodeName(), getErrorCode(methodContext));
                return false;
            }
        } else {
            List<Object> messages = methodContext.getEnv(this.simpleMethod.getServiceErrorMessageListName());
            if (messages != null && messages.size() > 0) {
                methodContext.putEnv(this.simpleMethod.getServiceResponseMessageName(), getErrorCode(methodContext));
                return false;
            }
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    private String getErrorCode(MethodContext methodContext) {
        String errorCode = this.errorCodeFse.expandString(methodContext.getEnvMap());
        if (errorCode.length() == 0) {
            errorCode = this.simpleMethod.getDefaultErrorCode();
        }
        return errorCode;
    }

    @Override
    public String rawString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<check-errors ");
        if (!this.errorCodeFse.isEmpty()) {
            sb.append("error-code=\"").append(this.errorCodeFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    public static final class CheckErrorsFactory implements Factory<CheckErrors> {
        public CheckErrors createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CheckErrors(element, simpleMethod);
        }

        public String getName() {
            return "check-errors";
        }
    }
}
