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
package org.ofbiz.minilang;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;

/**
 * SimpleMethod Mini Language Core Object
 */
public class SimpleMethod {
    
    public static final String module = SimpleMethod.class.getName();
    public static final String err_resource = "MiniLangErrorUiLabels";

    protected static UtilCache simpleMethodsDirectCache = new UtilCache("minilang.SimpleMethodsDirect", 0, 0);
    protected static UtilCache simpleMethodsResourceCache = new UtilCache("minilang.SimpleMethodsResource", 0, 0);
    protected static UtilCache simpleMethodsURLCache = new UtilCache("minilang.SimpleMethodsURL", 0, 0);

    // ----- Event Context Invokers -----

    public static String runSimpleEvent(String xmlResource, String methodName, HttpServletRequest request, HttpServletResponse response) throws MiniLangException {
        return runSimpleMethod(xmlResource, methodName, new MethodContext(request, response, null));
    }

    public static String runSimpleEvent(String xmlResource, String methodName, HttpServletRequest request, HttpServletResponse response, ClassLoader loader) throws MiniLangException {
        return runSimpleMethod(xmlResource, methodName, new MethodContext(request, response, loader));
    }

    public static String runSimpleEvent(URL xmlURL, String methodName, HttpServletRequest request, HttpServletResponse response, ClassLoader loader) throws MiniLangException {
        return runSimpleMethod(xmlURL, methodName, new MethodContext(request, response, loader));
    }

    // ----- Service Context Invokers -----

    public static Map runSimpleService(String xmlResource, String methodName, DispatchContext ctx, Map context) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, null);
        runSimpleMethod(xmlResource, methodName, methodContext);
        return methodContext.getResults();
    }

    public static Map runSimpleService(String xmlResource, String methodName, DispatchContext ctx, Map context, ClassLoader loader) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, loader);
        runSimpleMethod(xmlResource, methodName, methodContext);
        return methodContext.getResults();
    }

    public static Map runSimpleService(URL xmlURL, String methodName, DispatchContext ctx, Map context, ClassLoader loader) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, loader);
        runSimpleMethod(xmlURL, methodName, methodContext);
        return methodContext.getResults();
    }

    // ----- General Method Invokers -----

    public static String runSimpleMethod(String xmlResource, String methodName, MethodContext methodContext) throws MiniLangException {
        Map simpleMethods = getSimpleMethods(xmlResource, methodName, methodContext.getLoader());
        SimpleMethod simpleMethod = (SimpleMethod) simpleMethods.get(methodName);
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document in resource: " + xmlResource);
        }
        return simpleMethod.exec(methodContext);
    }

    public static String runSimpleMethod(URL xmlURL, String methodName, MethodContext methodContext) throws MiniLangException {
        Map simpleMethods = getSimpleMethods(xmlURL, methodName);
        SimpleMethod simpleMethod = (SimpleMethod) simpleMethods.get(methodName);
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document from URL: " + xmlURL.toString());
        }
        return simpleMethod.exec(methodContext);
    }

    public static Map getSimpleMethods(String xmlResource, String methodName, ClassLoader loader) throws MiniLangException {
        Map simpleMethods = (Map) simpleMethodsResourceCache.get(xmlResource);
        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = (Map) simpleMethodsResourceCache.get(xmlResource);
                if (simpleMethods == null) {
                    //URL xmlURL = UtilURL.fromResource(xmlResource, loader);
                    URL xmlURL = null;
                    try {
                        xmlURL = FlexibleLocation.resolveLocation(xmlResource, loader);
                    } catch (MalformedURLException e) {
                        throw new MiniLangException("Could not find SimpleMethod XML document in resource: " + xmlResource + "; error was: " + e.toString(), e);
                    }

                    if (xmlURL == null) {
                        throw new MiniLangException("Could not find SimpleMethod XML document in resource: " + xmlResource);
                    }
                    simpleMethods = getAllSimpleMethods(xmlURL);

                    // put it in the cache
                    simpleMethodsResourceCache.put(xmlResource, simpleMethods);
                }
            }
        }

        return simpleMethods;
    }

    public static Map getSimpleMethods(URL xmlURL, String methodName) throws MiniLangException {
        Map simpleMethods = (Map) simpleMethodsURLCache.get(xmlURL);

        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = (Map) simpleMethodsURLCache.get(xmlURL);
                if (simpleMethods == null) {
                    simpleMethods = getAllSimpleMethods(xmlURL);

                    // put it in the cache
                    simpleMethodsURLCache.put(xmlURL, simpleMethods);
                }
            }
        }

        return simpleMethods;
    }

    protected static Map getAllSimpleMethods(URL xmlURL) throws MiniLangException {
        Map simpleMethods = FastMap.newInstance();

        // read in the file
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(xmlURL, true);
        } catch (java.io.IOException e) {
            throw new MiniLangException("Could not read XML file", e);
        } catch (org.xml.sax.SAXException e) {
            throw new MiniLangException("Could not parse XML file", e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new MiniLangException("XML parser not setup correctly", e);
        }

        if (document == null) {
            throw new MiniLangException("Could not find SimpleMethod XML document: " + xmlURL.toString());
        }

        Element rootElement = document.getDocumentElement();
        List simpleMethodElements = UtilXml.childElementList(rootElement, "simple-method");

        Iterator simpleMethodIter = simpleMethodElements.iterator();

        while (simpleMethodIter.hasNext()) {
            Element simpleMethodElement = (Element) simpleMethodIter.next();
            SimpleMethod simpleMethod = new SimpleMethod(simpleMethodElement, simpleMethods, xmlURL.toString());
            simpleMethods.put(simpleMethod.getMethodName(), simpleMethod);
        }

        return simpleMethods;
    }

    public static Map getDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        Map simpleMethods = (Map) simpleMethodsDirectCache.get(name);

        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = (Map) simpleMethodsDirectCache.get(name);
                if (simpleMethods == null) {
                    simpleMethods = getAllDirectSimpleMethods(name, content, fromLocation);

                    // put it in the cache
                    simpleMethodsDirectCache.put(name, simpleMethods);
                }
            }
        }

        return simpleMethods;
    }

    protected static Map getAllDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        if (UtilValidate.isEmpty(fromLocation)) {
            fromLocation = "<location not known>";
        }
        
        Map simpleMethods = FastMap.newInstance();

        // read in the file
        Document document = null;

        try {
            if (content != null) {
                document = UtilXml.readXmlDocument(content, true);
            }
        } catch (java.io.IOException e) {
            throw new MiniLangException("Could not read XML content", e);
        } catch (org.xml.sax.SAXException e) {
            throw new MiniLangException("Could not parse direct XML content", e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new MiniLangException("XML parser not setup correctly", e);
        }

        if (document == null) {
            throw new MiniLangException("Could not load SimpleMethod XML document: " + name);
        }

        Element rootElement = document.getDocumentElement();
        List simpleMethodElements = UtilXml.childElementList(rootElement, "simple-method");

        Iterator simpleMethodIter = simpleMethodElements.iterator();

        while (simpleMethodIter.hasNext()) {
            Element simpleMethodElement = (Element) simpleMethodIter.next();
            SimpleMethod simpleMethod = new SimpleMethod(simpleMethodElement, simpleMethods, fromLocation);
            simpleMethods.put(simpleMethod.getMethodName(), simpleMethod);
        }

        return simpleMethods;
    }

    // Member fields begin here...
    protected List methodOperations = FastList.newInstance();
    protected Map parentSimpleMethodsMap;
    protected String fromLocation;
    protected String methodName;
    protected String shortDescription;
    protected String defaultErrorCode;
    protected String defaultSuccessCode;

    protected String parameterMapName;

    // event fields
    protected String eventRequestName;
    protected String eventSessionName;
    protected String eventResponseName;
    protected String eventResponseCodeName;
    protected String eventErrorMessageName;
    protected String eventErrorMessageListName;
    protected String eventEventMessageName;
    protected String eventEventMessageListName;

    // service fields
    protected String serviceResponseMessageName;
    protected String serviceErrorMessageName;
    protected String serviceErrorMessageListName;
    protected String serviceErrorMessageMapName;
    protected String serviceSuccessMessageName;
    protected String serviceSuccessMessageListName;

    protected boolean loginRequired = true;
    protected boolean useTransaction = true;

    protected String localeName;
    protected String delegatorName;
    protected String securityName;
    protected String dispatcherName;
    protected String userLoginName;

    public SimpleMethod(Element simpleMethodElement, Map parentSimpleMethodsMap, String fromLocation) {
        this.parentSimpleMethodsMap = parentSimpleMethodsMap;
        this.fromLocation = fromLocation;
        this.methodName = simpleMethodElement.getAttribute("method-name");
        this.shortDescription = simpleMethodElement.getAttribute("short-description");

        defaultErrorCode = simpleMethodElement.getAttribute("default-error-code");
        if (defaultErrorCode == null || defaultErrorCode.length() == 0) {
            defaultErrorCode = "error";
        }
        defaultSuccessCode = simpleMethodElement.getAttribute("default-success-code");
        if (defaultSuccessCode == null || defaultSuccessCode.length() == 0) {
            defaultSuccessCode = "success";
        }

        parameterMapName = simpleMethodElement.getAttribute("parameter-map-name");
        if (parameterMapName == null || parameterMapName.length() == 0) {
            parameterMapName = "parameters";
        }

        eventRequestName = simpleMethodElement.getAttribute("event-request-object-name");
        if (eventRequestName == null || eventRequestName.length() == 0) {
            eventRequestName = "request";
        }
        eventSessionName = simpleMethodElement.getAttribute("event-session-object-name");
        if (eventSessionName == null || eventSessionName.length() == 0) {
            eventSessionName = "session";
        }
        eventResponseName = simpleMethodElement.getAttribute("event-response-object-name");
        if (eventResponseName == null || eventResponseName.length() == 0) {
            eventResponseName = "response";
        }
        eventResponseCodeName = simpleMethodElement.getAttribute("event-response-code-name");
        if (eventResponseCodeName == null || eventResponseCodeName.length() == 0) {
            eventResponseCodeName = "_response_code_";
        }
        eventErrorMessageName = simpleMethodElement.getAttribute("event-error-message-name");
        if (eventErrorMessageName == null || eventErrorMessageName.length() == 0) {
            eventErrorMessageName = "_error_message_";
        }
        eventErrorMessageListName = simpleMethodElement.getAttribute("event-error-message-list-name");
        if (eventErrorMessageListName == null || eventErrorMessageListName.length() == 0) {
            eventErrorMessageListName = "_error_message_list_";
        }
        eventEventMessageName = simpleMethodElement.getAttribute("event-event-message-name");
        if (eventEventMessageName == null || eventEventMessageName.length() == 0) {
            eventEventMessageName = "_event_message_";
        }
        eventEventMessageListName = simpleMethodElement.getAttribute("event-event-message-list-name");
        if (eventEventMessageListName == null || eventEventMessageListName.length() == 0) {
            eventEventMessageListName = "_event_message_list_";
        }

        serviceResponseMessageName = simpleMethodElement.getAttribute("service-response-message-name");
        if (serviceResponseMessageName == null || serviceResponseMessageName.length() == 0) {
            serviceResponseMessageName = "responseMessage";
        }
        serviceErrorMessageName = simpleMethodElement.getAttribute("service-error-message-name");
        if (serviceErrorMessageName == null || serviceErrorMessageName.length() == 0) {
            serviceErrorMessageName = "errorMessage";
        }
        serviceErrorMessageListName = simpleMethodElement.getAttribute("service-error-message-list-name");
        if (serviceErrorMessageListName == null || serviceErrorMessageListName.length() == 0) {
            serviceErrorMessageListName = "errorMessageList";
        }
        serviceErrorMessageMapName = simpleMethodElement.getAttribute("service-error-message-map-name");
        if (serviceErrorMessageMapName == null || serviceErrorMessageMapName.length() == 0) {
            serviceErrorMessageMapName = "errorMessageMap";
        }

        serviceSuccessMessageName = simpleMethodElement.getAttribute("service-success-message-name");
        if (serviceSuccessMessageName == null || serviceSuccessMessageName.length() == 0) {
            serviceSuccessMessageName = "successMessage";
        }
        serviceSuccessMessageListName = simpleMethodElement.getAttribute("service-success-message-list-name");
        if (serviceSuccessMessageListName == null || serviceSuccessMessageListName.length() == 0) {
            serviceSuccessMessageListName = "successMessageList";
        }

        loginRequired = !"false".equals(simpleMethodElement.getAttribute("login-required"));
        useTransaction = !"false".equals(simpleMethodElement.getAttribute("use-transaction"));

        localeName = simpleMethodElement.getAttribute("locale-name");
        if (localeName == null || localeName.length() == 0) {
            localeName = "locale";
        }
        delegatorName = simpleMethodElement.getAttribute("delegator-name");
        if (delegatorName == null || delegatorName.length() == 0) {
            delegatorName = "delegator";
        }
        securityName = simpleMethodElement.getAttribute("security-name");
        if (securityName == null || securityName.length() == 0) {
            securityName = "security";
        }
        dispatcherName = simpleMethodElement.getAttribute("dispatcher-name");
        if (dispatcherName == null || dispatcherName.length() == 0) {
            dispatcherName = "dispatcher";
        }
        userLoginName = simpleMethodElement.getAttribute("user-login-name");
        if (userLoginName == null || userLoginName.length() == 0) {
            userLoginName = "userLogin";
        }

        readOperations(simpleMethodElement, this.methodOperations, this);
    }

    public String getFromLocation() {
        return this.fromLocation;
    }
    public String getMethodName() {
        return this.methodName;
    }

    public SimpleMethod getSimpleMethodInSameFile(String simpleMethodName) {
        if (parentSimpleMethodsMap == null) return null;
        return (SimpleMethod) parentSimpleMethodsMap.get(simpleMethodName);
    }

    public String getShortDescription() {
        return this.shortDescription + " [" + this.fromLocation + "#" + this.methodName + "]";
    }

    public String getDefaultErrorCode() {
        return this.defaultErrorCode;
    }

    public String getDefaultSuccessCode() {
        return this.defaultSuccessCode;
    }

    public String getParameterMapName() {
        return this.parameterMapName;
    }

    // event fields
    public String getEventRequestName() {
        return this.eventRequestName;
    }

    public String getEventSessionName() {
        return this.eventSessionName;
    }

    public String getEventResponseCodeName() {
        return this.eventResponseCodeName;
    }

    public String getEventErrorMessageName() {
        return this.eventErrorMessageName;
    }
    public String getEventErrorMessageListName() {
        return this.eventErrorMessageListName;
    }

    public String getEventEventMessageName() {
        return this.eventEventMessageName;
    }
    public String getEventEventMessageListName() {
        return this.eventEventMessageListName;
    }

    // service fields
    public String getServiceResponseMessageName() {
        return this.serviceResponseMessageName;
    }

    public String getServiceErrorMessageName() {
        return this.serviceErrorMessageName;
    }

    public String getServiceErrorMessageListName() {
        return this.serviceErrorMessageListName;
    }

    public String getServiceSuccessMessageName() {
        return this.serviceSuccessMessageName;
    }

    public String getServiceSuccessMessageListName() {
        return this.serviceSuccessMessageListName;
    }

    public boolean getLoginRequired() {
        return this.loginRequired;
    }

    public boolean getUseTransaction() {
        return this.useTransaction;
    }

    public String getDelegatorEnvName() {
        return this.delegatorName;
    }

    public String getSecurityEnvName() {
        return this.securityName;
    }

    public String getDispatcherEnvName() {
        return this.dispatcherName;
    }

    public String getUserLoginEnvName() {
        return this.userLoginName;
    }

    /** Execute the Simple Method operations */
    public String exec(MethodContext methodContext) {
        // always put the null field object in as "null"
        methodContext.putEnv("null", GenericEntity.NULL_FIELD);
        methodContext.putEnv("nullField", GenericEntity.NULL_FIELD);
        
        methodContext.putEnv(delegatorName, methodContext.getDelegator());
        methodContext.putEnv(securityName, methodContext.getSecurity());
        methodContext.putEnv(dispatcherName, methodContext.getDispatcher());
        methodContext.putEnv(localeName, methodContext.getLocale());
        methodContext.putEnv(parameterMapName, methodContext.getParameters());

        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.putEnv(eventRequestName, methodContext.getRequest());
            methodContext.putEnv(eventSessionName, methodContext.getRequest().getSession());
            methodContext.putEnv(eventResponseName, methodContext.getResponse());
        }
        
        methodContext.putEnv("methodName", this.getMethodName());
        methodContext.putEnv("methodShortDescription", this.getShortDescription());


        GenericValue userLogin = methodContext.getUserLogin();
        Locale locale = methodContext.getLocale();
                
        if (userLogin != null) {
            methodContext.putEnv(userLoginName, userLogin);
        }
        if (loginRequired) {
            if (userLogin == null) {
                Map messageMap = UtilMisc.toMap("shortDescription", shortDescription);
                String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.must_logged_process", messageMap, locale) + ".";

                if (methodContext.getMethodType() == MethodContext.EVENT) {
                     methodContext.getRequest().setAttribute("_ERROR_MESSAGE_", errMsg);
                    return defaultErrorCode;
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putResult(ModelService.ERROR_MESSAGE, errMsg);
                    methodContext.putResult(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    return null;
                }
            }
        }

        // if using transaction, try to start here
        boolean beganTransaction = false;

        if (useTransaction) {
            try {
                beganTransaction = TransactionUtil.begin();
            } catch (GenericTransactionException e) {
                String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.error_begin_transaction", locale) + ": " + e.getMessage();
                Debug.logWarning(errMsg, module);
                Debug.logWarning(e, module);
                if (methodContext.getMethodType() == MethodContext.EVENT) {
                    methodContext.getRequest().setAttribute("_ERROR_MESSAGE_", errMsg);
                    return defaultErrorCode;
                } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                    methodContext.putResult(ModelService.ERROR_MESSAGE, errMsg);
                    methodContext.putResult(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    return null;
                }
            }
        }

        // declare errorMsg here just in case transaction ops fail
        String errorMsg = "";

        boolean finished = false;
        try {
            finished = runSubOps(methodOperations, methodContext);
        } catch (Throwable t) {
            // make SURE nothing gets thrown through
            String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.error_running", locale) + ": " + t.getMessage();                                
            Debug.logError(errMsg, module);
            finished = false;
            errorMsg += errMsg + "<br/>";
        }
        
        String returnValue = null;
        String response = null;
        StringBuffer summaryErrorStringBuffer = new StringBuffer();
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            boolean forceError = false;
            
            String tempErrorMsg = (String) methodContext.getEnv(eventErrorMessageName);
            if (errorMsg.length() > 0 || (tempErrorMsg != null && tempErrorMsg.length() > 0)) {
                errorMsg += tempErrorMsg;
                methodContext.getRequest().setAttribute("_ERROR_MESSAGE_", errorMsg);
                forceError = true;
                
                summaryErrorStringBuffer.append(errorMsg);
            }
            List tempErrorMsgList = (List) methodContext.getEnv(eventErrorMessageListName);
            if (tempErrorMsgList != null && tempErrorMsgList.size() > 0) {
                methodContext.getRequest().setAttribute("_ERROR_MESSAGE_LIST_", tempErrorMsgList);
                forceError = true;
                
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(tempErrorMsgList.toString());
            }

            String eventMsg = (String) methodContext.getEnv(eventEventMessageName);
            if (eventMsg != null && eventMsg.length() > 0) {
                methodContext.getRequest().setAttribute("_EVENT_MESSAGE_", eventMsg);
            }
            List eventMsgList = (List) methodContext.getEnv(eventEventMessageListName);
            if (eventMsgList != null && eventMsgList.size() > 0) {
                methodContext.getRequest().setAttribute("_EVENT_MESSAGE_LIST_", eventMsgList);
            }

            response = (String) methodContext.getEnv(eventResponseCodeName);
            if (response == null || response.length() == 0) {
                if (forceError) {
                    //override response code, always use error code
                    Debug.logInfo("No response code string found, but error messages found so assuming error; returning code [" + defaultErrorCode + "]", module);
                    response = defaultErrorCode;
                } else {
                    Debug.logInfo("No response code string or errors found, assuming success; returning code [" + defaultSuccessCode + "]", module);
                    response = defaultSuccessCode;
                }
            }
            returnValue = response;
        } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
            boolean forceError = false;
            
            String tempErrorMsg = (String) methodContext.getEnv(serviceErrorMessageName);
            if (errorMsg.length() > 0 || (tempErrorMsg != null && tempErrorMsg.length() > 0)) {
                errorMsg += tempErrorMsg;
                methodContext.putResult(ModelService.ERROR_MESSAGE, errorMsg);
                forceError = true;

                summaryErrorStringBuffer.append(errorMsg);
            }

            List errorMsgList = (List) methodContext.getEnv(serviceErrorMessageListName);
            if (errorMsgList != null && errorMsgList.size() > 0) {
                methodContext.putResult(ModelService.ERROR_MESSAGE_LIST, errorMsgList);
                forceError = true;
                
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(errorMsgList.toString());
            }

            Map errorMsgMap = (Map) methodContext.getEnv(serviceErrorMessageMapName);
            if (errorMsgMap != null && errorMsgMap.size() > 0) {
                methodContext.putResult(ModelService.ERROR_MESSAGE_MAP, errorMsgMap);
                forceError = true;
                
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(errorMsgMap.toString());
            }

            String successMsg = (String) methodContext.getEnv(serviceSuccessMessageName);
            if (successMsg != null && successMsg.length() > 0) {
                methodContext.putResult(ModelService.SUCCESS_MESSAGE, successMsg);
            }

            List successMsgList = (List) methodContext.getEnv(serviceSuccessMessageListName);
            if (successMsgList != null && successMsgList.size() > 0) {
                methodContext.putResult(ModelService.SUCCESS_MESSAGE_LIST, successMsgList);
            }

            response = (String) methodContext.getEnv(serviceResponseMessageName);
            if (response == null || response.length() == 0) {
                if (forceError) {
                    //override response code, always use error code
                    Debug.logVerbose("No response code string found, but error messages found so assuming error; returning code [" + defaultErrorCode + "]", module);
                    response = defaultErrorCode;
                } else {
                    Debug.logVerbose("No response code string or errors found, assuming success; returning code [" + defaultSuccessCode + "]", module);
                    response = defaultSuccessCode;
                }
            }
            methodContext.putResult(ModelService.RESPONSE_MESSAGE, response);
            returnValue = null;
        } else {
            response = defaultSuccessCode;
            returnValue = defaultSuccessCode;
        }

        // decide whether or not to commit based on the response message, ie only rollback if error is returned and not finished
        boolean doCommit = true;
        if (!finished && defaultErrorCode.equals(response)) {
            doCommit = false;
        }

        if (doCommit) {
            // commit here passing beganTransaction to perform it properly
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                String errMsg = "Error trying to commit transaction, could not process method: " + e.getMessage();
                Debug.logWarning(e, errMsg, module);
                errorMsg += errMsg + "<br/>";
            }
        } else {
            // rollback here passing beganTransaction to either rollback, or set rollback only
            try {
                TransactionUtil.rollback(beganTransaction, "Error in simple-method [" + this.getShortDescription() + "]: " + summaryErrorStringBuffer, null);
            } catch (GenericTransactionException e) {
                String errMsg = "Error trying to rollback transaction, could not process method: " + e.getMessage();
                Debug.logWarning(e, errMsg, module);
                errorMsg += errMsg + "<br/>";
            }
        }
        
        return returnValue;
    }

    public static void readOperations(Element simpleMethodElement, List methodOperations, SimpleMethod simpleMethod) {
        List operationElements = UtilXml.childElementList(simpleMethodElement);

        if (operationElements != null && operationElements.size() > 0) {
            Iterator operElemIter = operationElements.iterator();

            while (operElemIter.hasNext()) {
                Element curOperElem = (Element) operElemIter.next();
                String nodeName = curOperElem.getNodeName();

                if ("call-map-processor".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallSimpleMapProcessor(curOperElem, simpleMethod));
                } else if ("check-errors".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CheckErrors(curOperElem, simpleMethod));
                } else if ("add-error".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.AddError(curOperElem, simpleMethod));
                } else if ("return".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.Return(curOperElem, simpleMethod));
                } else if ("set-service-fields".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.SetServiceFields(curOperElem, simpleMethod));
                } else if ("call-service".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallService(curOperElem, simpleMethod));
                } else if ("call-service-asynch".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallServiceAsynch(curOperElem, simpleMethod));
                } else if ("call-bsh".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallBsh(curOperElem, simpleMethod));
                } else if ("call-simple-method".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallSimpleMethod(curOperElem, simpleMethod));

                } else if ("call-object-method".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallObjectMethod(curOperElem, simpleMethod));
                } else if ("call-class-method".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CallClassMethod(curOperElem, simpleMethod));
                } else if ("create-object".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.callops.CreateObject(curOperElem, simpleMethod));
                    
                } else if ("field-to-request".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.FieldToRequest(curOperElem, simpleMethod));
                } else if ("field-to-session".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.FieldToSession(curOperElem, simpleMethod));
                } else if ("request-to-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.RequestToField(curOperElem, simpleMethod));
                } else if ("request-parameters-to-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.RequestParametersToList(curOperElem, simpleMethod));                    
                } else if ("session-to-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.SessionToField(curOperElem, simpleMethod));
                } else if ("webapp-property-to-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.eventops.WebappPropertyToField(curOperElem, simpleMethod));

                } else if ("field-to-result".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.serviceops.FieldToResult(curOperElem, simpleMethod));

                } else if ("map-to-map".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.MapToMap(curOperElem, simpleMethod));
                } else if ("field-to-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.FieldToList(curOperElem, simpleMethod));
                } else if ("list-to-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.ListToList(curOperElem, simpleMethod));
                } else if ("order-map-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.OrderMapList(curOperElem, simpleMethod));

                } else if ("set".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.SetOperation(curOperElem, simpleMethod));
                } else if ("env-to-env".equals(nodeName)) {
                    MethodOperation mop = new org.ofbiz.minilang.method.envops.EnvToEnv(curOperElem, simpleMethod);
                    methodOperations.add(mop);
                    Debug.logInfo("The env-to-env operation has been deprecated in favor of the set operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + mop.rawString(), module);
                } else if ("env-to-field".equals(nodeName)) {
                    MethodOperation mop = new org.ofbiz.minilang.method.envops.EnvToField(curOperElem, simpleMethod);
                    methodOperations.add(mop);
                    Debug.logInfo("The env-to-field operation has been deprecated in favor of the set operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + mop.rawString(), module);
                } else if ("field-to-env".equals(nodeName)) {
                    MethodOperation mop = new org.ofbiz.minilang.method.envops.FieldToEnv(curOperElem, simpleMethod);
                    methodOperations.add(mop);
                    Debug.logInfo("The field-to-env operation has been deprecated in favor of the set operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + mop.rawString(), module);
                } else if ("field-to-field".equals(nodeName)) {
                    MethodOperation mop = new org.ofbiz.minilang.method.envops.FieldToField(curOperElem, simpleMethod);
                    methodOperations.add(mop);
                    Debug.logInfo("The field-to-field operation has been deprecated in favor of the set operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + mop.rawString(), module);
                } else if ("string-to-field".equals(nodeName)) {
                    MethodOperation mop = new org.ofbiz.minilang.method.envops.StringToField(curOperElem, simpleMethod);
                    methodOperations.add(mop);
                    Debug.logInfo("The string-to-field operation has been deprecated in favor of the set operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + mop.rawString(), module);

                } else if ("string-append".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.StringAppend(curOperElem, simpleMethod));
                } else if ("string-to-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.StringToList(curOperElem, simpleMethod));
                } else if ("to-string".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.ToString(curOperElem, simpleMethod));
                } else if ("clear-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.ClearField(curOperElem, simpleMethod));
                } else if ("iterate".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.Iterate(curOperElem, simpleMethod));
                } else if ("iterate-map".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.IterateMap(curOperElem, simpleMethod));
                } else if ("loop".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.Loop(curOperElem, simpleMethod));
                } else if ("first-from-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.envops.FirstFromList(curOperElem, simpleMethod));

                } else if ("transaction-begin".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.TransactionBegin(curOperElem, simpleMethod));
                } else if ("transaction-commit".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.TransactionCommit(curOperElem, simpleMethod));
                } else if ("transaction-rollback".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.TransactionRollback(curOperElem, simpleMethod));
                    
                } else if ("now-timestamp-to-env".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.NowTimestampToEnv(curOperElem, simpleMethod));
                } else if ("now-date-to-env".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.NowDateToEnv(curOperElem, simpleMethod));
                } else if ("sequenced-id-to-env".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.SequencedIdToEnv(curOperElem, simpleMethod));
                } else if ("make-next-seq-id".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.MakeNextSeqId(curOperElem, simpleMethod));
                } else if ("set-current-user-login".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.SetCurrentUserLogin(curOperElem, simpleMethod));

                } else if ("find-by-primary-key".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.FindByPrimaryKey(curOperElem, simpleMethod));
                } else if ("find-by-and".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.FindByAnd(curOperElem, simpleMethod));
                } else if ("entity-one".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.EntityOne(curOperElem, simpleMethod));
                } else if ("entity-and".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.EntityAnd(curOperElem, simpleMethod));
                } else if ("entity-condition".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.EntityCondition(curOperElem, simpleMethod));
                } else if ("entity-count".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.EntityCount(curOperElem, simpleMethod));
                } else if ("get-related-one".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.GetRelatedOne(curOperElem, simpleMethod));
                } else if ("get-related".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.GetRelated(curOperElem, simpleMethod));
                } else if ("filter-list-by-and".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.FilterListByAnd(curOperElem, simpleMethod));
                } else if ("filter-list-by-date".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.FilterListByDate(curOperElem, simpleMethod));
                } else if ("order-value-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.OrderValueList(curOperElem, simpleMethod));

                } else if ("make-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.MakeValue(curOperElem, simpleMethod));
                } else if ("clone-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.CloneValue(curOperElem, simpleMethod));
                } else if ("create-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.CreateValue(curOperElem, simpleMethod));
                } else if ("store-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.StoreValue(curOperElem, simpleMethod));
                } else if ("refresh-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.RefreshValue(curOperElem, simpleMethod));
                } else if ("remove-value".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.RemoveValue(curOperElem, simpleMethod));
                } else if ("remove-related".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.RemoveRelated(curOperElem, simpleMethod));
                } else if ("remove-by-and".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.RemoveByAnd(curOperElem, simpleMethod));
                } else if ("clear-cache-line".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.ClearCacheLine(curOperElem, simpleMethod));
                } else if ("clear-entity-caches".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.ClearEntityCaches(curOperElem, simpleMethod));
                } else if ("set-pk-fields".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.SetPkFields(curOperElem, simpleMethod));
                } else if ("set-nonpk-fields".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.SetNonpkFields(curOperElem, simpleMethod));

                } else if ("store-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.StoreList(curOperElem, simpleMethod));
                } else if ("remove-list".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.entityops.RemoveList(curOperElem, simpleMethod));

                } else if ("assert".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.conditional.Assert(curOperElem, simpleMethod));
                } else if ("if".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.conditional.MasterIf(curOperElem, simpleMethod));
                } else if ("while".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.conditional.While(curOperElem, simpleMethod));
                } else if ("if-validate-method".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfValidateMethod(curOperElem, simpleMethod));
                } else if ("if-instance-of".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfInstanceOf(curOperElem, simpleMethod));
                } else if ("if-compare".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfCompare(curOperElem, simpleMethod));
                } else if ("if-compare-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfCompareField(curOperElem, simpleMethod));
                } else if ("if-regexp".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfRegexp(curOperElem, simpleMethod));
                } else if ("if-empty".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfEmpty(curOperElem, simpleMethod));
                } else if ("if-not-empty".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfNotEmpty(curOperElem, simpleMethod));
                } else if ("if-has-permission".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.IfHasPermission(curOperElem, simpleMethod));
                } else if ("check-permission".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.CheckPermission(curOperElem, simpleMethod));
                } else if ("check-id".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.ifops.CheckId(curOperElem, simpleMethod));
                } else if ("else".equals(nodeName)) {
                    // don't add anything, but don't complain either, this one is handled in the individual operations
                } else if ("property-to-field".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.otherops.PropertyToField(curOperElem, simpleMethod));
                } else if ("calculate".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.otherops.Calculate(curOperElem, simpleMethod));
                } else if ("log".equals(nodeName)) {
                    methodOperations.add(new org.ofbiz.minilang.method.otherops.Log(curOperElem, simpleMethod));
                } else {
                    Debug.logWarning("Operation element \"" + nodeName + "\" no recognized", module);
                }
            }
        }
    }

    /** Execs the given operations returning true if all return true, or returning 
     *  false and stopping if any return false.
     */
    public static boolean runSubOps(List methodOperations, MethodContext methodContext) {
        Iterator methodOpsIter = methodOperations.iterator();
        while (methodOpsIter.hasNext()) {
            MethodOperation methodOperation = (MethodOperation) methodOpsIter.next();
            try {
                if (!methodOperation.exec(methodContext)) {
                    return false;
                }
            } catch (Throwable t) {
                String errMsg = "Error in simple-method operation [" + methodOperation.rawString() + "]: " + t.toString();
                Debug.logError(t, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        }
        return true;
    }
}
