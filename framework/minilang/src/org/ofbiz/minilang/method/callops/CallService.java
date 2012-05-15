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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleServletAccessor;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * Calls a service using the given parameters
 */
public final class CallService extends MethodOperation {

    public static final String module = CallService.class.getName();
    public static final String resource = "MiniLangErrorUiLabels";

    private final boolean breakOnError;
    private final FlexibleMessage defaultMessage;
    private final String errorCode;
    private final FlexibleMessage errorPrefix;
    private final FlexibleMessage errorSuffix;
    private final boolean includeUserLogin;
    private final ContextAccessor<Map<String, Object>> inMapAcsr;
    private final FlexibleMessage messagePrefix;
    private final FlexibleMessage messageSuffix;
    /** Require a new transaction for this service */
    private final boolean requireNewTransaction;
    /** A list of strings with names of new maps to create */
    private final List<String> resultsToMapList;
    /** A list of ResultToFieldDef objects */
    private final List<ResultToFieldDef> resultToFieldList;
    /** the key is the request attribute name, the value is the result name to get */
    private final Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToRequestMap;
    /** the key is the result entry name, the value is the result name to get */
    private final Map<ContextAccessor<Object>, ContextAccessor<Object>> resultToResultMap;
    /** the key is the session attribute name, the value is the result name to get */
    private final Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToSessionMap;
    private final String serviceName;
    private final String successCode;
    private final FlexibleMessage successPrefix;
    private final FlexibleMessage successSuffix;
    /** Override the default transaction timeout, only works if we start the transaction */
    private final int transactionTimeout;

    public CallService(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        inMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("in-map-name"));
        includeUserLogin = !"false".equals(element.getAttribute("include-user-login"));
        breakOnError = !"false".equals(element.getAttribute("break-on-error"));
        errorCode = element.getAttribute("error-code");
        requireNewTransaction = "true".equals(element.getAttribute("require-new-transaction"));
        String timeoutStr = UtilXml.checkEmpty(element.getAttribute("transaction-timeout"));
        int timeout = -1;
        if (!timeoutStr.isEmpty()) {
            try {
                timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting timeout to 0 (default)", module);
                timeout = 0;
            }
        }
        transactionTimeout = timeout;
        successCode = element.getAttribute("success-code");
        errorPrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-prefix"), "service.error.prefix");
        errorSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-suffix"), "service.error.suffix");
        successPrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "success-prefix"), "service.success.prefix");
        successSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "success-suffix"), "service.success.suffix");
        messagePrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-prefix"), "service.message.prefix");
        messageSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-suffix"), "service.message.suffix");
        defaultMessage = new FlexibleMessage(UtilXml.firstChildElement(element, "default-message"), null);// "service.default.message"
        List<? extends Element> resultsToMapElements = UtilXml.childElementList(element, "results-to-map");
        if (UtilValidate.isNotEmpty(resultsToMapElements)) {
            List<String> resultsToMapList = new ArrayList<String>(resultsToMapElements.size());
            for (Element resultsToMapElement : resultsToMapElements) {
                resultsToMapList.add(resultsToMapElement.getAttribute("map-name"));
            }
            this.resultsToMapList = Collections.unmodifiableList(resultsToMapList);
        } else {
            this.resultsToMapList = null;
        }
        List<? extends Element> resultToFieldElements = UtilXml.childElementList(element, "result-to-field");
        if (UtilValidate.isNotEmpty(resultToFieldElements)) {
            List<ResultToFieldDef> resultToFieldList = new ArrayList<ResultToFieldDef>(resultToFieldElements.size());
            for (Element resultToFieldElement : resultToFieldElements) {
                // TODO: Clean this up.
                ResultToFieldDef rtfDef = new ResultToFieldDef();
                rtfDef.resultName = resultToFieldElement.getAttribute("result-name");
                rtfDef.mapAcsr = new ContextAccessor<Map<String, Object>>(resultToFieldElement.getAttribute("map-name"));
                String field = resultToFieldElement.getAttribute("field");
                if (UtilValidate.isEmpty(field))
                    field = resultToFieldElement.getAttribute("field-name");
                rtfDef.fieldAcsr = new ContextAccessor<Object>(field, rtfDef.resultName);
                resultToFieldList.add(rtfDef);
            }
            this.resultToFieldList = Collections.unmodifiableList(resultToFieldList);
        } else {
            this.resultToFieldList = null;
        }
        List<? extends Element> resultToRequestElements = UtilXml.childElementList(element, "result-to-request");
        if (UtilValidate.isNotEmpty(resultToRequestElements)) {
            Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToRequestMap = new HashMap<FlexibleServletAccessor<Object>, ContextAccessor<Object>>(resultToRequestElements.size());
            for (Element resultToRequestElement : resultToRequestElements) {
                FlexibleServletAccessor<Object> reqAcsr = new FlexibleServletAccessor<Object>(resultToRequestElement.getAttribute("request-name"), resultToRequestElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToRequestElement.getAttribute("result-name"));
                resultToRequestMap.put(reqAcsr, resultAcsr);
            }
            this.resultToRequestMap = Collections.unmodifiableMap(resultToRequestMap);
        } else {
            this.resultToRequestMap = null;
        }
        List<? extends Element> resultToSessionElements = UtilXml.childElementList(element, "result-to-session");
        if (UtilValidate.isNotEmpty(resultToSessionElements)) {
            Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToSessionMap = new HashMap<FlexibleServletAccessor<Object>, ContextAccessor<Object>>(resultToSessionElements.size());
            for (Element resultToSessionElement : resultToSessionElements) {
                FlexibleServletAccessor<Object> sesAcsr = new FlexibleServletAccessor<Object>(resultToSessionElement.getAttribute("session-name"), resultToSessionElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToSessionElement.getAttribute("result-name"));
                resultToSessionMap.put(sesAcsr, resultAcsr);
            }
            this.resultToSessionMap = Collections.unmodifiableMap(resultToSessionMap);
        } else {
            this.resultToSessionMap = null;
        }
        List<? extends Element> resultToResultElements = UtilXml.childElementList(element, "result-to-result");
        if (UtilValidate.isNotEmpty(resultToResultElements)) {
            Map<ContextAccessor<Object>, ContextAccessor<Object>> resultToResultMap = new HashMap<ContextAccessor<Object>, ContextAccessor<Object>>(resultToResultElements.size());
            for (Element resultToResultElement : resultToResultElements) {
                ContextAccessor<Object> serResAcsr = new ContextAccessor<Object>(resultToResultElement.getAttribute("service-result-name"), resultToResultElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToResultElement.getAttribute("result-name"));
                resultToResultMap.put(serResAcsr, resultAcsr);
            }
            this.resultToResultMap = Collections.unmodifiableMap(resultToResultMap);
        } else {
            this.resultToResultMap = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String serviceName = methodContext.expandString(this.serviceName);
        String errorCode = this.errorCode;
        if (errorCode.isEmpty()) {
            errorCode = simpleMethod.getDefaultErrorCode();
        }
        String successCode = this.successCode;
        if (successCode.isEmpty()) {
            successCode = simpleMethod.getDefaultSuccessCode();
        }
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
        // before invoking the service, clear messages
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.removeEnv(simpleMethod.getEventErrorMessageName());
            methodContext.removeEnv(simpleMethod.getEventEventMessageName());
            methodContext.removeEnv(simpleMethod.getEventResponseCodeName());
        } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
            methodContext.removeEnv(simpleMethod.getServiceErrorMessageName());
            methodContext.removeEnv(simpleMethod.getServiceSuccessMessageName());
            methodContext.removeEnv(simpleMethod.getServiceResponseMessageName());
        }
        // invoke the service
        Map<String, Object> result = null;
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
        try {
            ModelService modelService = methodContext.getDispatcher().getDispatchContext().getModelService(serviceName);
            int timeout = modelService.transactionTimeout;
            if (this.transactionTimeout >= 0) {
                timeout = this.transactionTimeout;
            }
            result = methodContext.getDispatcher().runSync(serviceName, inMap, timeout, requireNewTransaction);
        } catch (GenericServiceException e) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem invoking the [" + serviceName + "] service with the map named [" + inMapAcsr + "] containing [" + inMap + "]: " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            if (breakOnError) {
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), errorCode);
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), errorCode);
                }
                return false;
            } else {
                return true;
            }
        }
        if (resultsToMapList != null) {
            for (String mapName : resultsToMapList) {
                methodContext.putEnv(mapName, UtilMisc.makeMapWritable(result));
            }
        }
        if (resultToFieldList != null) {
            for (ResultToFieldDef rtfDef : resultToFieldList) {
                if (!rtfDef.mapAcsr.isEmpty()) {
                    Map<String, Object> tempMap = rtfDef.mapAcsr.get(methodContext);
                    if (tempMap == null) {
                        tempMap = FastMap.newInstance();
                        rtfDef.mapAcsr.put(methodContext, tempMap);
                    }
                    rtfDef.fieldAcsr.put(tempMap, result.get(rtfDef.resultName), methodContext);
                } else {
                    rtfDef.fieldAcsr.put(methodContext, result.get(rtfDef.resultName));
                }
            }
        }
        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            if (resultToRequestMap != null) {
                for (Map.Entry<FlexibleServletAccessor<Object>, ContextAccessor<Object>> entry : resultToRequestMap.entrySet()) {
                    FlexibleServletAccessor<Object> requestAcsr = entry.getKey();
                    ContextAccessor<Object> resultAcsr = entry.getValue();
                    requestAcsr.put(methodContext.getRequest(), resultAcsr.get(result, methodContext), methodContext.getEnvMap());
                }
            }
            if (resultToSessionMap != null) {
                for (Map.Entry<FlexibleServletAccessor<Object>, ContextAccessor<Object>> entry : resultToSessionMap.entrySet()) {
                    FlexibleServletAccessor<Object> sessionAcsr = entry.getKey();
                    ContextAccessor<Object> resultAcsr = entry.getValue();
                    sessionAcsr.put(methodContext.getRequest().getSession(), resultAcsr.get(result, methodContext), methodContext.getEnvMap());
                }
            }
        }
        // only run this if it is in an SERVICE context
        if (methodContext.getMethodType() == MethodContext.SERVICE) {
            if (resultToResultMap != null) {
                for (Map.Entry<ContextAccessor<Object>, ContextAccessor<Object>> entry : resultToResultMap.entrySet()) {
                    ContextAccessor<Object> targetResultAcsr = entry.getKey();
                    ContextAccessor<Object> resultAcsr = entry.getValue();
                    targetResultAcsr.put(methodContext.getResults(), resultAcsr.get(result, methodContext), methodContext);
                }
            }
        }
        String errorPrefixStr = errorPrefix.getMessage(methodContext.getLoader(), methodContext);
        String errorSuffixStr = errorSuffix.getMessage(methodContext.getLoader(), methodContext);
        String successPrefixStr = successPrefix.getMessage(methodContext.getLoader(), methodContext);
        String successSuffixStr = successSuffix.getMessage(methodContext.getLoader(), methodContext);
        String messagePrefixStr = messagePrefix.getMessage(methodContext.getLoader(), methodContext);
        String messageSuffixStr = messageSuffix.getMessage(methodContext.getLoader(), methodContext);
        String errorMessage = null;
        List<String> errorMessageList = null;
        // See if there is a single message
        if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            errorMessage = ServiceUtil.makeErrorMessage(result, messagePrefixStr, messageSuffixStr, errorPrefixStr, errorSuffixStr);
        } else if (result.containsKey(ModelService.ERROR_MESSAGE_LIST)) {
            errorMessageList = UtilGenerics.checkList(result.get(ModelService.ERROR_MESSAGE_LIST));
        }
        if ((UtilValidate.isNotEmpty(errorMessage) || UtilValidate.isNotEmpty(errorMessageList)) && breakOnError) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                if (UtilValidate.isNotEmpty(errorMessage)) {
                    if (Debug.verboseOn()) {
                        errorMessage += UtilProperties.getMessage(resource, "simpleMethod.error_show_service_name", UtilMisc.toMap("serviceName", serviceName, "methodName", simpleMethod.getMethodName()), locale);
                    }
                    methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errorMessage);
                } else {
                    if (Debug.verboseOn()) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "simpleMethod.error_show_service_name", UtilMisc.toMap("serviceName", serviceName, "methodName", simpleMethod.getMethodName()), locale));
                    }
                    methodContext.putEnv(simpleMethod.getEventErrorMessageListName(), errorMessageList);
                }
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                ServiceUtil.addErrors(UtilMisc.<String, String> getListFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageListName()), UtilMisc.<String, String, Object> getMapFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageMapName()), result);
                // the old way, makes a mess of messages passed up the stack:
                // methodContext.putEnv(simpleMethod.getServiceErrorMessageName(),
                // errorMessage);
                Debug.logError(new Exception(errorMessage), module);
            }
        }
        String successMessage = ServiceUtil.makeSuccessMessage(result, messagePrefixStr, messageSuffixStr, successPrefixStr, successSuffixStr);
        if (UtilValidate.isNotEmpty(successMessage)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventEventMessageName(), successMessage);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceSuccessMessageName(), successMessage);
            }
        }
        String defaultMessageStr = defaultMessage.getMessage(methodContext.getLoader(), methodContext);
        if (UtilValidate.isEmpty(errorMessage) && UtilValidate.isEmpty(errorMessageList) && UtilValidate.isEmpty(successMessage) && UtilValidate.isNotEmpty(defaultMessageStr)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventEventMessageName(), defaultMessageStr);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceSuccessMessageName(), defaultMessageStr);
            }
        }
        // handle the result
        String responseCode = result.containsKey(ModelService.RESPONSE_MESSAGE) ? (String) result.get(ModelService.RESPONSE_MESSAGE) : successCode;
        if (errorCode.equals(responseCode)) {
            if (breakOnError) {
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
                }
                return false;
            } else {
                // avoid responseCode here since we are ignoring the error
                return true;
            }
        } else {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
            }
            return true;
        }
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-service/>";
    }

    public static final class CallServiceFactory implements Factory<CallService> {
        public CallService createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallService(element, simpleMethod);
        }

        public String getName() {
            return "call-service";
        }
    }

    public static class ResultToFieldDef {
        public ContextAccessor<Object> fieldAcsr;
        public ContextAccessor<Map<String, Object>> mapAcsr;
        public String resultName;
    }
}
