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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.collections.FlexibleServletAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * Implements the &lt;call-service&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
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
    private final FlexibleMapAccessor<Map<String, Object>> inMapFma;
    private final FlexibleMessage messagePrefix;
    private final FlexibleMessage messageSuffix;
    private final boolean requireNewTransaction;
    private final List<String> resultsToMapList;
    private final List<ResultToField> resultToFieldList;
    private final List<ResultToRequest> resultToRequestList;
    private final List<ResultToResult> resultToResultList;
    private final List<ResultToSession> resultToSessionList;
    private final FlexibleStringExpander serviceNameFse;
    private final String successCode;
    private final FlexibleMessage successPrefix;
    private final FlexibleMessage successSuffix;
    private final int transactionTimeout;

    public CallService(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "service-name", "in-map-name", "include-user-login", "break-on-error", "error-code", "require-new-transaction", "transaction-timeout", "success-code");
            MiniLangValidate.constantAttributes(simpleMethod, element, "include-user-login", "break-on-error", "error-code", "require-new-transaction", "transaction-timeout", "success-code");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "service-name", "in-map-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "service-name");
            MiniLangValidate.childElements(simpleMethod, element, "error-prefix", "error-suffix", "success-prefix", "success-suffix", "message-prefix", "message-suffix", "default-message", "results-to-map", "result-to-field", "result-to-request", "result-to-session", "result-to-result");
        }
        serviceNameFse = FlexibleStringExpander.getInstance(element.getAttribute("service-name"));
        inMapFma = FlexibleMapAccessor.getInstance(element.getAttribute("in-map-name"));
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
                MiniLangValidate.handleError("Exception thrown while parsing transaction-timeout attribute: " + e.getMessage(), simpleMethod, element);
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
            List<String> resultsToMapList = new ArrayList<>(resultsToMapElements.size());
            for (Element resultsToMapElement : resultsToMapElements) {
                resultsToMapList.add(resultsToMapElement.getAttribute("map-name"));
            }
            this.resultsToMapList = Collections.unmodifiableList(resultsToMapList);
        } else {
            this.resultsToMapList = null;
        }
        List<? extends Element> resultToFieldElements = UtilXml.childElementList(element, "result-to-field");
        if (UtilValidate.isNotEmpty(resultToFieldElements)) {
            List<ResultToField> resultToFieldList = new ArrayList<>(resultToFieldElements.size());
            for (Element resultToFieldElement : resultToFieldElements) {
                resultToFieldList.add(new ResultToField(resultToFieldElement));
            }
            this.resultToFieldList = Collections.unmodifiableList(resultToFieldList);
        } else {
            this.resultToFieldList = null;
        }
        List<? extends Element> resultToRequestElements = UtilXml.childElementList(element, "result-to-request");
        if (UtilValidate.isNotEmpty(resultToRequestElements)) {
            List<ResultToRequest> resultToRequestList = new ArrayList<>(resultToRequestElements.size());
            for (Element resultToRequestElement : resultToRequestElements) {
                resultToRequestList.add(new ResultToRequest(resultToRequestElement));
            }
            this.resultToRequestList = Collections.unmodifiableList(resultToRequestList);
        } else {
            this.resultToRequestList = null;
        }
        List<? extends Element> resultToSessionElements = UtilXml.childElementList(element, "result-to-session");
        if (UtilValidate.isNotEmpty(resultToSessionElements)) {
            List<ResultToSession> resultToSessionList = new ArrayList<>(resultToSessionElements.size());
            for (Element resultToSessionElement : resultToSessionElements) {
                resultToSessionList.add(new ResultToSession(resultToSessionElement));
            }
            this.resultToSessionList = Collections.unmodifiableList(resultToSessionList);
        } else {
            this.resultToSessionList = null;
        }
        List<? extends Element> resultToResultElements = UtilXml.childElementList(element, "result-to-result");
        if (UtilValidate.isNotEmpty(resultToResultElements)) {
            List<ResultToResult> resultToResultList = new ArrayList<>(resultToResultElements.size());
            for (Element resultToResultElement : resultToResultElements) {
                resultToResultList.add(new ResultToResult(resultToResultElement));
            }
            this.resultToResultList = Collections.unmodifiableList(resultToResultList);
        } else {
            this.resultToResultList = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "Begin call-service.");
        }
        String serviceName = serviceNameFse.expandString(methodContext.getEnvMap());
        String errorCode = this.errorCode;
        if (errorCode.isEmpty()) {
            errorCode = simpleMethod.getDefaultErrorCode();
        }
        String successCode = this.successCode;
        if (successCode.isEmpty()) {
            successCode = simpleMethod.getDefaultSuccessCode();
        }
        Map<String, Object> inMap = inMapFma.get(methodContext.getEnvMap());
        if (inMap == null) {
            inMap = new HashMap<>();
        }
        // before invoking the service, clear messages
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.removeEnv(simpleMethod.getEventErrorMessageName());
            methodContext.removeEnv(simpleMethod.getEventEventMessageName());
            methodContext.removeEnv(simpleMethod.getEventResponseCodeName());
        } else {
            methodContext.removeEnv(simpleMethod.getServiceErrorMessageName());
            methodContext.removeEnv(simpleMethod.getServiceSuccessMessageName());
            methodContext.removeEnv(simpleMethod.getServiceResponseMessageName());
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
        Map<String, Object> result = null;
        try {
            ModelService modelService = methodContext.getDispatcher().getDispatchContext().getModelService(serviceName);
            int timeout = modelService.transactionTimeout;
            if (this.transactionTimeout >= 0) {
                timeout = this.transactionTimeout;
            }
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Invoking service \"" + serviceName + "\", require-new-transaction = " + requireNewTransaction + ", transaction-timeout = " + timeout + ", IN attributes:", inMap.toString());
            }
            result = methodContext.getDispatcher().runSync(serviceName, inMap, timeout, requireNewTransaction);
        } catch (GenericServiceException e) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Service engine threw an exception: " + e.getMessage());
            }
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem invoking the [" + serviceName + "] service with the map named [" + inMapFma + "] containing [" + inMap + "]: " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            if (breakOnError) {
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), errorCode);
                } else {
                    methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), errorCode);
                }
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "break-on-error set to \"true\", halting script execution. End call-service.");
                }
                return false;
            } else {
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "End call-service.");
                }
                return true;
            }
        }
        if (resultsToMapList != null) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Processing " + resultsToMapList.size() + " <results-to-map> elements.");
            }
            for (String mapName : resultsToMapList) {
                methodContext.putEnv(mapName, UtilMisc.makeMapWritable(result));
            }
        }
        if (resultToFieldList != null) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Processing " + resultToFieldList.size() + " <result-to-field> elements.");
            }
            for (ResultToField rtfDef : resultToFieldList) {
                rtfDef.exec(methodContext, result);
            }
        }
        if (resultToResultList != null) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Processing " + resultToResultList.size() + " <result-to-result> elements.");
            }
            for (ResultToResult rtrDef : resultToResultList) {
                rtrDef.exec(methodContext, result);
            }
        }
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            if (resultToRequestList != null) {
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "Processing " + resultToRequestList.size() + " <result-to-request> elements.");
                }
                for (ResultToRequest rtrDef : resultToRequestList) {
                    rtrDef.exec(methodContext, result);
                }
            }
            if (resultToSessionList != null) {
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "Processing " + resultToSessionList.size() + " <result-to-session> elements.");
                }
                for (ResultToSession rtsDef : resultToSessionList) {
                    rtsDef.exec(methodContext, result);
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
            errorMessageList = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_LIST));
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
            } else {
                ServiceUtil.addErrors(UtilMisc.<String, String> getListFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageListName()), UtilMisc.<String, String, Object> getMapFromMap(methodContext.getEnvMap(), this.simpleMethod.getServiceErrorMessageMapName()), result);
                Debug.logError(new Exception(errorMessage), module);
            }
        }
        String successMessage = ServiceUtil.makeSuccessMessage(result, messagePrefixStr, messageSuffixStr, successPrefixStr, successSuffixStr);
        if (UtilValidate.isNotEmpty(successMessage)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventEventMessageName(), successMessage);
            } else {
                methodContext.putEnv(simpleMethod.getServiceSuccessMessageName(), successMessage);
            }
        }
        String defaultMessageStr = defaultMessage.getMessage(methodContext.getLoader(), methodContext);
        if (UtilValidate.isEmpty(errorMessage) && UtilValidate.isEmpty(errorMessageList) && UtilValidate.isEmpty(successMessage) && UtilValidate.isNotEmpty(defaultMessageStr)) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventEventMessageName(), defaultMessageStr);
            } else {
                methodContext.putEnv(simpleMethod.getServiceSuccessMessageName(), defaultMessageStr);
            }
        }
        String responseCode = result.containsKey(ModelService.RESPONSE_MESSAGE) ? (String) result.get(ModelService.RESPONSE_MESSAGE) : successCode;
        if (errorCode.equals(responseCode)) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Service returned an error.");
            }
            if (breakOnError) {
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
                } else {
                    methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
                }
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "break-on-error set to \"true\", halting script execution. End call-service.");
                }
                return false;
            } else {
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "End call-service.");
                }
                return true;
            }
        } else {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Service ran successfully. End call-service.");
            }
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), responseCode);
            } else {
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), responseCode);
            }
            return true;
        }
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addServiceName(this.serviceNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<call-service ");
        sb.append("service-name=\"").append(this.serviceNameFse).append("\" ");
        if (!this.inMapFma.isEmpty()) {
            sb.append("in-map-name=\"").append(this.inMapFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;call-service&gt; element.
     */
    public static final class CallServiceFactory implements Factory<CallService> {
        @Override
        public CallService createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CallService(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "call-service";
        }
    }

    private final class ResultToField {
        private final FlexibleMapAccessor<Object> fieldFma;
        private final FlexibleMapAccessor<Object> resultFma;

        private ResultToField(Element element) {
            resultFma = FlexibleMapAccessor.getInstance(element.getAttribute("result-name"));
            String fieldAttribute = element.getAttribute("field");
            if (fieldAttribute.isEmpty()) {
                fieldFma = resultFma;
            } else {
                fieldFma = FlexibleMapAccessor.getInstance(fieldAttribute);
            }
        }

        private void exec(MethodContext methodContext, Map<String, Object> resultMap) {
            fieldFma.put(methodContext.getEnvMap(), resultFma.get(resultMap));
        }
    }

    private final class ResultToRequest {
        private final FlexibleMapAccessor<Object> resultFma;
        private final FlexibleServletAccessor<Object> requestFsa;

        private ResultToRequest(Element element) {
            requestFsa = new FlexibleServletAccessor<>(element.getAttribute("request-name"), element.getAttribute("result-name"));
            resultFma =FlexibleMapAccessor.getInstance(element.getAttribute("result-name"));
        }

        private void exec(MethodContext methodContext, Map<String, Object> resultMap) {
            requestFsa.put(methodContext.getRequest(), resultFma.get(resultMap), methodContext.getEnvMap());
        }
    }

    private final class ResultToResult {
        private final FlexibleMapAccessor<Object> resultFma;
        private final FlexibleMapAccessor<Object> serviceResultFma;

        private ResultToResult(Element element) {
            resultFma = FlexibleMapAccessor.getInstance(element.getAttribute("result-name"));
            String serviceResultAttribute = element.getAttribute("service-result-name");
            if (serviceResultAttribute.isEmpty()) {
                serviceResultFma = resultFma;
            } else {
                serviceResultFma = FlexibleMapAccessor.getInstance(serviceResultAttribute);
            }
        }

        private void exec(MethodContext methodContext, Map<String, Object> resultMap) {
            serviceResultFma.put(methodContext.getResults(), resultFma.get(resultMap));
        }
    }

    private final class ResultToSession {
        private final FlexibleMapAccessor<Object> resultFma;
        private final FlexibleServletAccessor<Object> requestFsa;

        private ResultToSession(Element element) {
            requestFsa = new FlexibleServletAccessor<>(element.getAttribute("session-name"), element.getAttribute("result-name"));
            resultFma =FlexibleMapAccessor.getInstance(element.getAttribute("result-name"));
        }

        private void exec(MethodContext methodContext, Map<String, Object> resultMap) {
            requestFsa.put(methodContext.getRequest().getSession(), resultFma.get(resultMap), methodContext.getEnvMap());
        }
    }
}
