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
package org.apache.ofbiz.minilang.method.callops;

import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;return&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class Return extends MethodOperation {

    private final FlexibleStringExpander responseCodeFse;

    public Return(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "response-code");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        responseCodeFse = FlexibleStringExpander.getInstance(element.getAttribute("response-code"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String responseCode = responseCodeFse.expandString(methodContext.getEnvMap());
        if (responseCode.isEmpty()) {
            responseCode = simpleMethod.getDefaultSuccessCode();
        }
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
        } else {
            methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
        }
        return false;
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

    /**
     * A factory for the &lt;return&gt; element.
     */
    public static final class ReturnFactory implements Factory<Return> {
        @Override
        public Return createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Return(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "return";
        }
    }
}
