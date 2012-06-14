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
package org.ofbiz.minilang.method.envops;

import java.util.List;

import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Halts script execution if the error message list contains any messages.
 */
public final class CheckErrors extends MethodOperation {

    private final FlexibleStringExpander errorCodeFse;
    private final FlexibleStringExpander errorListNameFse;

    public CheckErrors(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "error-code", "error-list-name");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "error-code");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.errorCodeFse = FlexibleStringExpander.getInstance(element.getAttribute("error-code"));
        this.errorListNameFse = FlexibleStringExpander.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> messages = methodContext.getEnv(this.errorListNameFse.expandString(methodContext.getEnvMap()));
        if (messages != null && messages.size() > 0) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageListName(), messages);
                methodContext.putEnv(this.simpleMethod.getEventResponseCodeName(), getErrorCode(methodContext));
            } else {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(this.simpleMethod.getServiceResponseMessageName(), getErrorCode(methodContext));
            }
            return false;
        }
        return true;
    }

    private String getErrorCode(MethodContext methodContext) {
        String errorCode = this.errorCodeFse.expandString(methodContext.getEnvMap());
        if (errorCode.length() == 0) {
            errorCode = this.simpleMethod.getDefaultErrorCode();
        }
        return errorCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<check-errors ");
        if (!this.errorCodeFse.isEmpty()) {
            sb.append("error-code=\"").append(this.errorCodeFse).append("\" ");
        }
        if (!"error_list".equals(this.errorListNameFse.getOriginal())) {
            sb.append("error-list-name=\"").append(this.errorListNameFse).append("\" ");
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
