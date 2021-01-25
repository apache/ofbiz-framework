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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.ValidationException;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;call-simple-method&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class CallSimpleMethod extends MethodOperation {

    private static final String MODULE = CallSimpleMethod.class.getName();

    private final String methodName;
    private final String xmlResource;
    private final URL xmlURL;
    private final String scope;
    private final List<ResultToField> resultToFieldList;

    public CallSimpleMethod(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "method-name", "xml-resource", "scope");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "method-name");
            MiniLangValidate.constantAttributes(simpleMethod, element, "method-name", "xml-resource", "scope");
            MiniLangValidate.childElements(simpleMethod, element, "result-to-field");
        }
        this.methodName = element.getAttribute("method-name");
        String xmlResourceAttribute = element.getAttribute("xml-resource");
        if (xmlResourceAttribute.isEmpty()) {
            xmlResourceAttribute = simpleMethod.getFromLocation();
        }
        this.xmlResource = xmlResourceAttribute;
        URL xmlURL = null;
        try {
            xmlURL = FlexibleLocation.resolveLocation(this.xmlResource);
        } catch (MalformedURLException e) {
            MiniLangValidate.handleError("Could not find SimpleMethod XML document in resource: " + this.xmlResource
                    + "; error was: " + e.toString(), simpleMethod, element);
        }
        this.xmlURL = xmlURL;
        this.scope = element.getAttribute("scope");
        List<? extends Element> resultToFieldElements = UtilXml.childElementList(element, "result-to-field");
        if (UtilValidate.isNotEmpty(resultToFieldElements)) {
            if (!"function".equals(this.scope)) {
                MiniLangValidate.handleError("Inline scope cannot include <result-to-field> elements.", simpleMethod, element);
            }
            List<ResultToField> resultToFieldList = new ArrayList<>(resultToFieldElements.size());
            for (Element resultToFieldElement : resultToFieldElements) {
                resultToFieldList.add(new ResultToField(resultToFieldElement, simpleMethod));
            }
            this.resultToFieldList = resultToFieldList;
        } else {
            this.resultToFieldList = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (UtilValidate.isEmpty(this.methodName)) {
            throw new MiniLangRuntimeException("method-name attribute is empty", this);
        }
        SimpleMethod simpleMethodToCall = SimpleMethod.getSimpleMethod(this.xmlURL, this.methodName);
        if (simpleMethodToCall == null) {
            throw new MiniLangRuntimeException("Could not find <simple-method name=\"" + this.methodName + "\"> in XML document "
                    + this.xmlResource, this);
        }
        MethodContext localContext = methodContext;
        if ("function".equals(this.scope)) {
            Map<String, Object> localEnv = new HashMap<>();
            localEnv.putAll(methodContext.getEnvMap());
            localEnv.remove(this.getSimpleMethod().getEventResponseCodeName());
            localEnv.remove(this.getSimpleMethod().getServiceResponseMessageName());
            localContext = new MethodContext(localEnv, methodContext.getLoader(), methodContext.getMethodType());
        }
        String returnVal = simpleMethodToCall.exec(localContext);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Called simple-method named [" + this.methodName + "] in resource [" + this.xmlResource + "], returnVal is ["
                     + returnVal + "]", MODULE);
        }
        if (simpleMethodToCall.getDefaultErrorCode().equals(returnVal)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(getSimpleMethod().getEventResponseCodeName(), getSimpleMethod().getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(getSimpleMethod().getServiceResponseMessageName(), getSimpleMethod().getDefaultErrorCode());
            }
            return false;
        }
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            // FIXME: This doesn't make sense. We are comparing the called method's response code with this method's
            // response code. Since response codes are configurable per method, this code will fail.
            String responseCode = (String) localContext.getEnv(this.getSimpleMethod().getEventResponseCodeName());
            if (this.getSimpleMethod().getDefaultErrorCode().equals(responseCode)) {
                Debug.logWarning("Got error [" + responseCode + "] calling inline simple-method named [" + this.methodName + "] in resource ["
                        + this.xmlResource + "], message is " + methodContext.getEnv(this.getSimpleMethod().getEventErrorMessageName()), MODULE);
                return false;
            }
        } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
            // FIXME: This doesn't make sense. We are comparing the called method's response message with this method's
            // response message. Since response messages are configurable per method, this code will fail.
            String responseMessage = (String) localContext.getEnv(this.getSimpleMethod().getServiceResponseMessageName());
            if (this.getSimpleMethod().getDefaultErrorCode().equals(responseMessage)) {
                Debug.logWarning("Got error [" + responseMessage + "] calling inline simple-method named [" + this.methodName + "] in resource ["
                        + this.xmlResource + "], message is " + methodContext.getEnv(this.getSimpleMethod().getServiceErrorMessageName())
                        + ", and the error message list is: "
                        + methodContext.getEnv(this.getSimpleMethod().getServiceErrorMessageListName()), MODULE);
                return false;
            }
        }
        if ("function".equals(this.scope) && this.resultToFieldList != null) {
            Map<String, Object> results = localContext.getResults();
            if (results != null) {
                for (ResultToField resultToField : this.resultToFieldList) {
                    resultToField.exec(methodContext.getEnvMap(), results);
                }
            }
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        SimpleMethod simpleMethodToCall;
        try {
            simpleMethodToCall = SimpleMethod.getSimpleMethod(this.xmlURL, this.methodName);
            if (simpleMethodToCall != null) {
                if (!aic.hasVisited(simpleMethodToCall)) {
                    aic.addSimpleMethod(simpleMethodToCall);
                    simpleMethodToCall.gatherArtifactInfo(aic);
                }
            }
        } catch (MiniLangException e) {
            Debug.logWarning("Could not find <simple-method name=\"" + this.methodName + "\"> in XML document " + this.xmlResource + ": "
                    + e.toString(), MODULE);
        }
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getXmlResource() {
        return this.xmlResource;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<call-simple-method ");
        if (!this.methodName.isEmpty()) {
            sb.append("method-name=\"").append(this.methodName).append("\" ");
        }
        if (!this.xmlResource.isEmpty()) {
            sb.append("xml-resource=\"").append(this.xmlResource).append("\" ");
        }
        if (!this.scope.isEmpty()) {
            sb.append("scope=\"").append(this.scope).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;call-simple-method&gt; element.
     */
    public static final class CallSimpleMethodFactory implements Factory<CallSimpleMethod> {
        @Override
        public CallSimpleMethod createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallSimpleMethod(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "call-simple-method";
        }
    }

    private final class ResultToField {

        private final FlexibleMapAccessor<Object> fieldFma;
        private final FlexibleMapAccessor<Object> resultNameFma;

        private ResultToField(Element element, SimpleMethod simpleMethod) throws ValidationException {
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, element, "result-name", "field");
                MiniLangValidate.requiredAttributes(simpleMethod, element, "result-name");
                MiniLangValidate.expressionAttributes(simpleMethod, element, "result-name", "field");
                MiniLangValidate.noChildElements(simpleMethod, element);
            }
            this.resultNameFma = FlexibleMapAccessor.getInstance(element.getAttribute("result-name"));
            String fieldAttribute = element.getAttribute("field");
            if (fieldAttribute.isEmpty()) {
                this.fieldFma = this.resultNameFma;
            } else {
                this.fieldFma = FlexibleMapAccessor.getInstance(fieldAttribute);
            }
        }

        private void exec(Map<String, Object> context, Map<String, Object> results) throws MiniLangException {
            Object value = this.resultNameFma.get(results);
            if (value != null) {
                this.fieldFma.put(context, value);
            }
        }
    }
}
