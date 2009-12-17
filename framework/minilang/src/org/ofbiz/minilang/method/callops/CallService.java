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
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleServletAccessor;
import org.ofbiz.entity.GenericValue;
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
public class CallService extends MethodOperation {
    public static final class CallServiceFactory implements Factory<CallService> {
        public CallService createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new CallService(element, simpleMethod);
        }

        public String getName() {
            return "call-service";
        }
    }

    public static final String module = CallService.class.getName();
    public static final String resource = "MiniLangErrorUiLabels";
    
    protected String serviceName;
    protected ContextAccessor<Map<String, Object>> inMapAcsr;
    protected String includeUserLoginStr;
    protected String breakOnErrorStr;
    protected String errorCode;
    protected String successCode;

    /** Require a new transaction for this service */
    protected  String requireNewTransactionStr;
    /** Override the default transaction timeout, only works if we start the transaction */
    protected  int transactionTimeout;

    protected FlexibleMessage errorPrefix;
    protected FlexibleMessage errorSuffix;
    protected FlexibleMessage successPrefix;
    protected FlexibleMessage successSuffix;
    protected FlexibleMessage messagePrefix;
    protected FlexibleMessage messageSuffix;
    protected FlexibleMessage defaultMessage;

    /** A list of strings with names of new maps to create */
    protected List<String> resultsToMap = FastList.newInstance();

    /** A list of ResultToFieldDef objects */
    protected List<ResultToFieldDef> resultToField = FastList.newInstance();

    /** the key is the request attribute name, the value is the result name to get */
    protected Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToRequest = FastMap.newInstance();

    /** the key is the session attribute name, the value is the result name to get */
    protected Map<FlexibleServletAccessor<Object>, ContextAccessor<Object>> resultToSession = FastMap.newInstance();

    /** the key is the result entry name, the value is the result name to get */
    protected Map<ContextAccessor<Object>, ContextAccessor<Object>> resultToResult = FastMap.newInstance();

    public CallService(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        serviceName = element.getAttribute("service-name");
        inMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("in-map-name"));
        includeUserLoginStr = element.getAttribute("include-user-login");
        breakOnErrorStr = element.getAttribute("break-on-error");
        errorCode = element.getAttribute("error-code");
        if (UtilValidate.isEmpty(errorCode)) errorCode = "error";
        this.requireNewTransactionStr = element.getAttribute("require-new-transaction");

        String timeoutStr = UtilXml.checkEmpty(element.getAttribute("transaction-timeout"), element.getAttribute("transaction-timout"));
        int timeout = -1;
        if (!UtilValidate.isEmpty(timeoutStr)) {
            try {
                timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {
                Debug.logWarning(e, "Setting timeout to 0 (default)", module);
                timeout = 0;
            }
        }
        this.transactionTimeout = timeout;

        successCode = element.getAttribute("success-code");
        if (UtilValidate.isEmpty(successCode)) successCode = "success";

        errorPrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-prefix"), "service.error.prefix");
        errorSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "error-suffix"), "service.error.suffix");
        successPrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "success-prefix"), "service.success.prefix");
        successSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "success-suffix"), "service.success.suffix");
        messagePrefix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-prefix"), "service.message.prefix");
        messageSuffix = new FlexibleMessage(UtilXml.firstChildElement(element, "message-suffix"), "service.message.suffix");
        defaultMessage = new FlexibleMessage(UtilXml.firstChildElement(element, "default-message"), "service.default.message");

        List<? extends Element> resultsToMapElements = UtilXml.childElementList(element, "results-to-map");
        if (UtilValidate.isNotEmpty(resultsToMapElements)) {
            for (Element resultsToMapElement: resultsToMapElements) {
                resultsToMap.add(resultsToMapElement.getAttribute("map-name"));
            }
        }

        List<? extends Element> resultToFieldElements = UtilXml.childElementList(element, "result-to-field");
        if (UtilValidate.isNotEmpty(resultToFieldElements)) {
            for (Element resultToFieldElement: resultToFieldElements) {
                ResultToFieldDef rtfDef = new ResultToFieldDef();

                rtfDef.resultName = resultToFieldElement.getAttribute("result-name");
                rtfDef.mapAcsr = new ContextAccessor<Map<String, Object>>(resultToFieldElement.getAttribute("map-name"));
                String field = resultToFieldElement.getAttribute("field");
                if (UtilValidate.isEmpty(field)) field = resultToFieldElement.getAttribute("field-name");
                rtfDef.fieldAcsr = new ContextAccessor<Object>(field, rtfDef.resultName);

                resultToField.add(rtfDef);
            }
        }

        // get result-to-request and result-to-session sub-ops
        List<? extends Element> resultToRequestElements = UtilXml.childElementList(element, "result-to-request");
        if (UtilValidate.isNotEmpty(resultToRequestElements)) {
            for (Element resultToRequestElement: resultToRequestElements) {
                FlexibleServletAccessor<Object> reqAcsr = new FlexibleServletAccessor<Object>(resultToRequestElement.getAttribute("request-name"), resultToRequestElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToRequestElement.getAttribute("result-name"));
                resultToRequest.put(reqAcsr, resultAcsr);
            }
        }

        List<? extends Element> resultToSessionElements = UtilXml.childElementList(element, "result-to-session");
        if (UtilValidate.isNotEmpty(resultToSessionElements)) {
            for (Element resultToSessionElement: resultToSessionElements) {
                FlexibleServletAccessor<Object> sesAcsr = new FlexibleServletAccessor<Object>(resultToSessionElement.getAttribute("session-name"), resultToSessionElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToSessionElement.getAttribute("result-name"));
                resultToSession.put(sesAcsr, resultAcsr);
            }
        }

        List<? extends Element> resultToResultElements = UtilXml.childElementList(element, "result-to-result");
        if (UtilValidate.isNotEmpty(resultToResultElements)) {
            for (Element resultToResultElement: resultToResultElements) {
                ContextAccessor<Object> serResAcsr = new ContextAccessor<Object>(resultToResultElement.getAttribute("service-result-name"), resultToResultElement.getAttribute("result-name"));
                ContextAccessor<Object> resultAcsr = new ContextAccessor<Object>(resultToResultElement.getAttribute("result-name"));
                resultToResult.put(serResAcsr, resultAcsr);
            }
        }
    }

    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        boolean includeUserLogin = !"false".equals(methodContext.expandString(includeUserLoginStr));
        boolean breakOnError = !"false".equals(methodContext.expandString(breakOnErrorStr));


        String serviceName = methodContext.expandString(this.serviceName);
        String errorCode = methodContext.expandString(this.errorCode);
        String successCode = methodContext.expandString(this.successCode);

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
            if (UtilValidate.isEmpty(this.requireNewTransactionStr) && this.transactionTimeout < 0) {
                result = methodContext.getDispatcher().runSync(serviceName, inMap);
            } else {
                ModelService modelService = methodContext.getDispatcher().getDispatchContext().getModelService(serviceName);
                boolean requireNewTransaction = modelService.requireNewTransaction;
                int timeout = modelService.transactionTimeout;
                if (UtilValidate.isNotEmpty(this.requireNewTransactionStr)) {
                    requireNewTransaction = "true".equalsIgnoreCase(this.requireNewTransactionStr) ? true : false;
                }
                if (this.transactionTimeout >= 0) {
                    timeout = this.transactionTimeout;
                }
                result = methodContext.getDispatcher().runSync(serviceName, inMap, timeout, requireNewTransaction);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem invoking the [" + serviceName + "] service with the map named [" + inMapAcsr + "] containing [" + inMap + "]: " + e.getMessage() + "]";
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), errorCode);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), errorCode);
            }
            return false;
        }

        if (resultsToMap.size() > 0) {
            for (String mapName: resultsToMap) {
                methodContext.putEnv(mapName, UtilMisc.makeMapWritable(result));
            }
        }

        if (resultToField.size() > 0) {
            for (ResultToFieldDef rtfDef: resultToField) {
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
            if (resultToRequest.size() > 0) {
                for (Map.Entry<FlexibleServletAccessor<Object>, ContextAccessor<Object>> entry: resultToRequest.entrySet()) {
                    FlexibleServletAccessor<Object> requestAcsr = entry.getKey();
                    ContextAccessor<Object> resultAcsr = entry.getValue();
                    requestAcsr.put(methodContext.getRequest(), resultAcsr.get(result, methodContext), methodContext.getEnvMap());
                }
            }

            if (resultToSession.size() > 0) {
                for (Map.Entry<FlexibleServletAccessor<Object>, ContextAccessor<Object>> entry: resultToSession.entrySet()) {
                    FlexibleServletAccessor<Object> sessionAcsr = entry.getKey();
                    ContextAccessor<Object> resultAcsr = entry.getValue();
                    sessionAcsr.put(methodContext.getRequest().getSession(), resultAcsr.get(result, methodContext), methodContext.getEnvMap());
                }
            }
        }

        // only run this if it is in an SERVICE context
        if (methodContext.getMethodType() == MethodContext.SERVICE) {
            if (resultToResult.size() > 0) {
                for (Map.Entry<ContextAccessor<Object>, ContextAccessor<Object>> entry: resultToResult.entrySet()) {
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
        
        String errorMessage = ServiceUtil.makeErrorMessage(result, messagePrefixStr, messageSuffixStr, errorPrefixStr, errorSuffixStr);
        if (UtilValidate.isNotEmpty(errorMessage)) {
            errorMessage += UtilProperties.getMessage(resource, "simpleMethod.error_show_service_name", UtilMisc.toMap("serviceName", serviceName, "methodName", simpleMethod.getMethodName()), locale);
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errorMessage);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                ServiceUtil.addErrors(UtilMisc.<String, String>getListFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageListName()), 
                        UtilMisc.<String, String, Object>getMapFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageMapName()), result);
                // the old way, makes a mess of messages passed up the stack: methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errorMessage);
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
        if (UtilValidate.isEmpty(errorMessage) && UtilValidate.isEmpty(successMessage) && UtilValidate.isNotEmpty(defaultMessageStr)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventEventMessageName(), defaultMessageStr);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceSuccessMessageName(), defaultMessageStr);
            }
        }

        // handle the result
        String responseCode = result.containsKey(ModelService.RESPONSE_MESSAGE) ? (String) result.get(ModelService.RESPONSE_MESSAGE) : successCode;
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
        } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
            methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
        }

        if (errorCode.equals(responseCode) && breakOnError) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-service/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    public static class ResultToFieldDef {
        public String resultName;
        public ContextAccessor<Map<String, Object>> mapAcsr;
        public ContextAccessor<Object> fieldAcsr;
    }
}
