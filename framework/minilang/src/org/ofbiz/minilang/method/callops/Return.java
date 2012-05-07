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

import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * An event operation that returns the given response code
 */
public final class Return extends MethodOperation {

    private final FlexibleStringExpander responseCodeFse;

    public Return(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "response-code");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        responseCodeFse = FlexibleStringExpander.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("response-code"), "success"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String responseCode = responseCodeFse.expandString(methodContext.getEnvMap());
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
        } else {
            methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
        }
        return false;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    @Override
    public String rawString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<return ");
        if (!"success".equals(responseCodeFse.getOriginal())) {
            sb.append("response-code=\"").append(responseCodeFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    public static final class ReturnFactory implements Factory<Return> {
        public Return createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Return(element, simpleMethod);
        }

        public String getName() {
            return "return";
        }
    }
}
