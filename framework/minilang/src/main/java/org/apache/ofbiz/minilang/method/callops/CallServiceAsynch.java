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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.service.GenericServiceException;
import org.w3c.dom.Element;

/**
 * Implements the &lt;call-service-asynch&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Ccallserviceasynch%3E}}">Mini-language Reference</a>
 */
public final class CallServiceAsynch extends MethodOperation {

    public static final String module = CallServiceAsynch.class.getName();

    private final boolean includeUserLogin;
    private final FlexibleMapAccessor<Map<String, Object>> inMapFma;
    private final FlexibleStringExpander serviceNameFse;

    public CallServiceAsynch(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "serviceName", "in-map-name", "include-user-login");
            MiniLangValidate.constantAttributes(simpleMethod, element, "include-user-login");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "service-name", "in-map-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "service-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        serviceNameFse = FlexibleStringExpander.getInstance(element.getAttribute("service-name"));
        inMapFma = FlexibleMapAccessor.getInstance(element.getAttribute("in-map-name"));
        includeUserLogin = !"false".equals(element.getAttribute("include-user-login"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "Begin call-service-asynch.");
        }
        String serviceName = serviceNameFse.expandString(methodContext.getEnvMap());
        Map<String, Object> inMap = inMapFma.get(methodContext.getEnvMap());
        if (inMap == null) {
            inMap = new HashMap<String, Object>();
        }
        if (includeUserLogin) {
            GenericValue userLogin = methodContext.getUserLogin();
            if (userLogin != null && inMap.get("userLogin") == null) {
                inMap.put("userLogin", userLogin);
            }
        }
        Locale locale = methodContext.getLocale();
        if (locale != null) {
            inMap.put("locale", locale);
        }
        try {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Invoking service \"" + serviceName + "\", IN attributes:", inMap.toString());
            }
            methodContext.getDispatcher().runAsync(serviceName, inMap);
        } catch (GenericServiceException e) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Service engine threw an exception: " + e.getMessage() + ", halting script execution. End call-service-asynch.");
            }
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem invoking the " + serviceName + " service: " + e.getMessage() + "]";
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "End call-service-asynch.");
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addServiceName(this.serviceNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<call-service-asynch ");
        sb.append("service-name=\"").append(this.serviceNameFse).append("\" ");
        if (!this.inMapFma.isEmpty()) {
            sb.append("in-map-name=\"").append(this.inMapFma).append("\" ");
        }
        if (!this.includeUserLogin) {
            sb.append("include-user-login=\"false\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;call-service-asynch&gt; element.
     */
    public static final class CallServiceAsynchFactory implements Factory<CallServiceAsynch> {
        @Override
        public CallServiceAsynch createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallServiceAsynch(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "call-service-asynch";
        }
    }
}
