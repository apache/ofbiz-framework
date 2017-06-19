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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.FieldObject;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodObject;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.minilang.method.StringObject;
import org.w3c.dom.Element;

/**
 * Implements the &lt;call-object-method&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class CallObjectMethod extends MethodOperation {

    public static final String module = CallClassMethod.class.getName();

    private final String methodName;
    private final FlexibleMapAccessor<Object> objFieldFma;
    private final List<MethodObject<?>> parameters;
    private final FlexibleMapAccessor<Object> retFieldFma;

    public CallObjectMethod(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.handleError("<call-object-method> element is deprecated (use <script>)", simpleMethod, element);
            MiniLangValidate.attributeNames(simpleMethod, element, "obj-field", "method-name", "ret-field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "method-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "obj-field", "method-name");
            MiniLangValidate.childElements(simpleMethod, element, "string", "field");
        }
        this.methodName = element.getAttribute("method-name");
        this.objFieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("obj-field"));
        this.retFieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("ret-field"));
        List<? extends Element> parameterElements = UtilXml.childElementList(element);
        if (parameterElements.size() > 0) {
            ArrayList<MethodObject<?>> parameterList = new ArrayList<MethodObject<?>>(parameterElements.size());
            for (Element parameterElement : parameterElements) {
                if ("string".equals(parameterElement.getNodeName())) {
                    parameterList.add(new StringObject(parameterElement, simpleMethod));
                } else if ("field".equals(parameterElement.getNodeName())) {
                    parameterList.add(new FieldObject<Object>(parameterElement, simpleMethod));
                }
            }
            parameterList.trimToSize();
            this.parameters = Collections.unmodifiableList(parameterList);
        } else {
            this.parameters = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object methodObject = this.objFieldFma.get(methodContext.getEnvMap());
        if (methodObject == null) {
            throw new MiniLangRuntimeException("Object not found: " + this.objFieldFma, this);
        }
        MiniLangUtil.callMethod(this, methodContext, parameters, methodObject.getClass(), methodObject, methodName, retFieldFma);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<call-class-method ");
        if (!this.objFieldFma.isEmpty()) {
            sb.append("obj-field=\"").append(this.objFieldFma).append("\" ");
        }
        if (!this.methodName.isEmpty()) {
            sb.append("method-name=\"").append(this.methodName).append("\" ");
        }
        if (!this.retFieldFma.isEmpty()) {
            sb.append("ret-field=\"").append(this.retFieldFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;call-object-method&gt; element.
     */
    public static final class CallObjectMethodFactory implements Factory<CallObjectMethod> {
        @Override
        public CallObjectMethod createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallObjectMethod(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "call-object-method";
        }
    }
}
