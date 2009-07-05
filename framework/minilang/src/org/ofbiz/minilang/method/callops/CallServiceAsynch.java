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

import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.service.GenericServiceException;
import org.w3c.dom.Element;

/**
 * Calls a service using the given parameters
 */
public class CallServiceAsynch extends MethodOperation {
    public static final class CallServiceAsynchFactory implements Factory<CallServiceAsynch> {
        public CallServiceAsynch createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new CallServiceAsynch(element, simpleMethod);
        }

        public String getName() {
            return "call-service-asynch";
        }
    }

    public static final String module = CallServiceAsynch.class.getName();

    protected String serviceName;
    protected ContextAccessor<Map<String, Object>> inMapAcsr;
    protected String includeUserLoginStr;

    public CallServiceAsynch(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        inMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("in-map-name"));
        includeUserLoginStr = element.getAttribute("include-user-login");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String serviceName = methodContext.expandString(this.serviceName);
        boolean includeUserLogin = !"false".equals(methodContext.expandString(includeUserLoginStr));

        Map<String, Object> inMap = null;
        if (inMapAcsr.isEmpty()) {
            inMap = FastMap.newInstance();
        } else {
            inMap = inMapAcsr.get(methodContext);
            if (inMap == null) {
                inMap = FastMap.newInstance();
                inMapAcsr.put(methodContext, inMap);
            }
        }

        // add UserLogin to context if expected
        if (includeUserLogin) {
            GenericValue userLogin = methodContext.getUserLogin();

            if (userLogin != null && inMap.get("userLogin") == null) {
                inMap.put("userLogin", userLogin);
            }
        }

        // always add Locale to context unless null
        Locale locale = methodContext.getLocale();
        if (locale != null) {
            inMap.put("locale", locale);
        }

        // invoke the service
        try {
            methodContext.getDispatcher().runAsync(serviceName, inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem invoking the " + serviceName + " service: " + e.getMessage() + "]";

            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }

        return true;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-service-asynch/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
