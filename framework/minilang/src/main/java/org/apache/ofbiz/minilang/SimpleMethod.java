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
package org.apache.ofbiz.minilang;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.minilang.method.MethodOperation.DeprecatedOperation;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implements the &lt;simple-method&gt; element.
 * <p>
 * The Mini-language script engine follows the
 * <a href="http://en.wikipedia.org/wiki/Flyweight_pattern">flyweight</a>
 * design pattern. Mini-language XML files are parsed twice - first into a W3C DOM
 * tree, then the DOM tree is parsed into element model objects. Each XML element
 * has a model class, and each model class has its own factory.
 * </p>
 * <p>
 * Mini-language can be extended by:</p>
 * <ul>
 * <li>Creating model classes that extend {@link org.apache.ofbiz.minilang.method.MethodOperation}</li>
 * <li>Creating factories for the model classes that implement {@link org.apache.ofbiz.minilang.method.MethodOperation.Factory}</li>
 * <li>Create a service provider information file for the factory classes
 * (see <a href="http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html" target="_blank">ServiceLoader</a>)
 * </li>
 * </ul>
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class SimpleMethod extends MiniLangElement {

    public static final String module = SimpleMethod.class.getName();
    private static final String err_resource = "MiniLangErrorUiLabels";
    private static final String[] DEPRECATED_ATTRIBUTES = {"parameter-map-name", "locale-name", "delegator-name", "security-name", "dispatcher-name", "user-login-name"};
    private static final Map<String, MethodOperation.Factory<MethodOperation>> methodOperationFactories;
    private static final UtilCache<String, Map<String, SimpleMethod>> simpleMethodsDirectCache = UtilCache.createUtilCache("minilang.SimpleMethodsDirect", 0, 0);
    private static final UtilCache<String, SimpleMethod> simpleMethodsResourceCache = UtilCache.createUtilCache("minilang.SimpleMethodsResource", 0, 0);

    static {
        Map<String, MethodOperation.Factory<MethodOperation>> mapFactories = new HashMap<>();
        Iterator<MethodOperation.Factory<MethodOperation>> it = UtilGenerics.cast(ServiceLoader.load(MethodOperation.Factory.class, SimpleMethod.class.getClassLoader()).iterator());
        while (it.hasNext()) {
            MethodOperation.Factory<MethodOperation> factory = it.next();
            mapFactories.put(factory.getName(), factory);
        }
        methodOperationFactories = Collections.unmodifiableMap(mapFactories);
    }

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        boolean elementModified = false;
        for (int i = 0; i < DEPRECATED_ATTRIBUTES.length; i++) {
            if (!element.getAttribute(DEPRECATED_ATTRIBUTES[i]).isEmpty()) {
                element.removeAttribute(DEPRECATED_ATTRIBUTES[i]);
                elementModified = true;
            }
        }
        return elementModified;
    }

    private static void compileAllSimpleMethods(Element rootElement, Map<String, SimpleMethod> simpleMethods, String location) throws MiniLangException {
        for (Element simpleMethodElement : UtilXml.childElementList(rootElement, "simple-method")) {
            SimpleMethod simpleMethod = new SimpleMethod(simpleMethodElement, location);
            if (simpleMethods.containsKey(simpleMethod.getMethodName())) {
                MiniLangValidate.handleError("Duplicate method name found", simpleMethod, simpleMethodElement);
            }
            simpleMethods.put(simpleMethod.getMethodName(), simpleMethod);
        }
    }

    private static Map<String, SimpleMethod> getAllDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        if (UtilValidate.isEmpty(fromLocation)) {
            fromLocation = "<location not known>";
        }
        Map<String, SimpleMethod> simpleMethods = new HashMap<>();
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(content, true, true);
        } catch (Exception e) {
            throw new MiniLangException("Could not read SimpleMethod XML document [" + name + "]: ", e);
        }
        compileAllSimpleMethods(document.getDocumentElement(), simpleMethods, fromLocation);
        return simpleMethods;
    }

    private static Map<String, SimpleMethod> getAllSimpleMethods(URL xmlURL) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = new LinkedHashMap<>();
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(xmlURL, true, true);
        } catch (Exception e) {
            throw new MiniLangException("Could not read SimpleMethod XML document [" + xmlURL + "]: ", e);
        }
        Element rootElement = document.getDocumentElement();
        if (!"simple-methods".equalsIgnoreCase(rootElement.getTagName())) {
            rootElement = UtilXml.firstChildElement(rootElement, "simple-methods");
        }
        
        compileAllSimpleMethods(rootElement, simpleMethods, xmlURL.toString());
        if (MiniLangUtil.isDocumentAutoCorrected(document)) {
            MiniLangUtil.writeMiniLangDocument(xmlURL, document);
        }
        return simpleMethods;
    }

    public static Map<String, SimpleMethod> getDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        Assert.notNull("name", name, "content", content);
        Map<String, SimpleMethod> simpleMethods = simpleMethodsDirectCache.get(name);
        if (simpleMethods == null) {
            simpleMethods = getAllDirectSimpleMethods(name, content, fromLocation);
            simpleMethods = simpleMethodsDirectCache.putIfAbsentAndGet(name, simpleMethods);
        }
        return simpleMethods;
    }

    public static SimpleMethod getSimpleMethod(String xmlResource, String methodName, ClassLoader loader) throws MiniLangException {
        Assert.notNull("methodName", methodName);
        String key = xmlResource.concat("#").concat(methodName);
        SimpleMethod method = simpleMethodsResourceCache.get(key);
        if (method == null) {
            Map<String, SimpleMethod> simpleMethods = getSimpleMethods(xmlResource, loader);
            for (Map.Entry<String, SimpleMethod> entry : simpleMethods.entrySet()) {
                String putKey = xmlResource.concat("#").concat(entry.getKey());
                simpleMethodsResourceCache.putIfAbsent(putKey, entry.getValue());
            }
        }
        return simpleMethodsResourceCache.get(key);
    }

    public static SimpleMethod getSimpleMethod(URL xmlUrl, String methodName) throws MiniLangException {
        Assert.notNull("methodName", methodName);
        String xmlResource = xmlUrl.toString();
        String key = xmlResource.concat("#").concat(methodName);
        SimpleMethod method = simpleMethodsResourceCache.get(key);
        if (method == null) {
            Map<String, SimpleMethod> simpleMethods = getAllSimpleMethods(xmlUrl);
            for (Map.Entry<String, SimpleMethod> entry : simpleMethods.entrySet()) {
                String putKey = xmlResource.concat("#").concat(entry.getKey());
                simpleMethodsResourceCache.putIfAbsent(putKey, entry.getValue());
            }
        }
        return simpleMethodsResourceCache.get(key);
    }

    private static Map<String, SimpleMethod> getSimpleMethods(String xmlResource, ClassLoader loader) throws MiniLangException {
        Assert.notNull("xmlResource", xmlResource);
        URL xmlURL = null;
        try {
            xmlURL = FlexibleLocation.resolveLocation(xmlResource, loader);
        } catch (MalformedURLException e) {
            throw new MiniLangException("Could not find SimpleMethod XML document in resource: " + xmlResource + "; error was: " + e.toString(), e);
        }
        if (xmlURL == null) {
            throw new MiniLangException("Could not find SimpleMethod XML document in resource: " + xmlResource);
        }
        return getAllSimpleMethods(xmlURL);
    }

    /**
     * Returns a List of <code>SimpleMethod</code> objects compiled from <code>xmlResource</code>.
     * The ordering in the List is the same as the XML file.
     * <p>This method is used by unit test framework to run tests in the order they appear in the XML file.
     * Method caching is bypassed since the methods are executed only once.</p>
     * 
     * @param xmlResource
     * @param loader
     * @return
     * @throws MiniLangException
     */
    public static List<SimpleMethod> getSimpleMethodsList(String xmlResource, ClassLoader loader) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethodMap = getSimpleMethods(xmlResource, loader);
        return new ArrayList<>(simpleMethodMap.values());
    }

    public static List<MethodOperation> readOperations(Element simpleMethodElement, SimpleMethod simpleMethod) throws MiniLangException {
        Assert.notNull("simpleMethodElement", simpleMethodElement, "simpleMethod", simpleMethod);
        List<? extends Element> operationElements = UtilXml.childElementList(simpleMethodElement);
        ArrayList<MethodOperation> methodOperations = new ArrayList<>(operationElements.size());
        if (UtilValidate.isNotEmpty(operationElements)) {
            for (Element curOperElem : operationElements) {
                String nodeName = UtilXml.getNodeNameIgnorePrefix(curOperElem);
                MethodOperation methodOp = null;
                MethodOperation.Factory<MethodOperation> factory = methodOperationFactories.get(nodeName);
                if (factory != null) {
                    methodOp = factory.createMethodOperation(curOperElem, simpleMethod);
                } else if ("else".equals(nodeName)) {
                    // don't add anything, but don't complain either, this one is handled in the individual operations
                } else {
                    MiniLangValidate.handleError("Invalid element found", simpleMethod, curOperElem);
                }
                if (methodOp == null) {
                    continue;
                }
                methodOperations.add(methodOp);
                DeprecatedOperation depOp = methodOp.getClass().getAnnotation(DeprecatedOperation.class);
                if (depOp != null) {
                    MiniLangValidate.handleError("The " + nodeName + " operation has been deprecated in favor of the " + depOp.value() + " operation", simpleMethod, curOperElem);
                }
            }
        }
        methodOperations.trimToSize();
        return methodOperations;
    }

    public static String runSimpleEvent(String xmlResource, String methodName, HttpServletRequest request, HttpServletResponse response) throws MiniLangException {
        return runSimpleMethod(xmlResource, methodName, new MethodContext(request, response, null));
    }

    public static String runSimpleEvent(String xmlResource, String methodName, HttpServletRequest request, HttpServletResponse response, ClassLoader loader) throws MiniLangException {
        return runSimpleMethod(xmlResource, methodName, new MethodContext(request, response, loader));
    }

    public static String runSimpleEvent(URL xmlURL, String methodName, HttpServletRequest request, HttpServletResponse response, ClassLoader loader) throws MiniLangException {
        return runSimpleMethod(xmlURL, methodName, new MethodContext(request, response, loader));
    }

    public static String runSimpleMethod(String xmlResource, String methodName, MethodContext methodContext) throws MiniLangException {
        Assert.notNull("methodContext", methodContext);
        SimpleMethod simpleMethod = getSimpleMethod(xmlResource, methodName, methodContext.getLoader());
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document in resource: " + xmlResource);
        }
        return simpleMethod.exec(methodContext);
    }

    public static String runSimpleMethod(URL xmlURL, String methodName, MethodContext methodContext) throws MiniLangException {
        SimpleMethod simpleMethod = getSimpleMethod(xmlURL, methodName);
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document from URL: " + xmlURL.toString());
        }
        return simpleMethod.exec(methodContext);
    }

    public static Map<String, Object> runSimpleService(String xmlResource, String methodName, DispatchContext ctx, Map<String, ? extends Object> context) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, null);
        runSimpleMethod(xmlResource, methodName, methodContext);
        return methodContext.getResults();
    }

    public static Map<String, Object> runSimpleService(String xmlResource, String methodName, DispatchContext ctx, Map<String, ? extends Object> context, ClassLoader loader) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, loader);
        runSimpleMethod(xmlResource, methodName, methodContext);
        return methodContext.getResults();
    }

    public static Map<String, Object> runSimpleService(URL xmlURL, String methodName, DispatchContext ctx, Map<String, ? extends Object> context, ClassLoader loader) throws MiniLangException {
        MethodContext methodContext = new MethodContext(ctx, context, loader);
        runSimpleMethod(xmlURL, methodName, methodContext);
        return methodContext.getResults();
    }

    /**
     * Execs the given operations returning true if all return true, or returning false and stopping if any return false.
     * @throws MiniLangException 
     */
    public static boolean runSubOps(List<MethodOperation> methodOperations, MethodContext methodContext) throws MiniLangException {
        Assert.notNull("methodOperations", methodOperations, "methodContext", methodContext);
        for (MethodOperation methodOperation : methodOperations) {
            if (!methodOperation.exec(methodContext)) {
                return false;
            }
        }
        return true;
    }

    private final String defaultErrorCode;
    private final String defaultSuccessCode;
    private final String eventErrorMessageListName;
    private final String eventErrorMessageName;
    private final String eventEventMessageListName;
    private final String eventEventMessageName;
    private final String eventRequestName;
    private final String eventResponseCodeName;
    private final String eventResponseName;
    private final String eventSessionName;
    private final String fromLocation;
    private final boolean loginRequired;
    private final String methodName;
    private final List<MethodOperation> methodOperations;
    private final String serviceErrorMessageListName;
    private final String serviceErrorMessageMapName;
    private final String serviceErrorMessageName;
    private final String serviceResponseMessageName;
    private final String serviceSuccessMessageListName;
    private final String serviceSuccessMessageName;
    private final String shortDescription;
    private final boolean useTransaction;

    public SimpleMethod(Element simpleMethodElement, String fromLocation) throws MiniLangException {
        super(simpleMethodElement, null);
        if (MiniLangValidate.validationOn()) {
            String locationMsg = " File = ".concat(fromLocation);
            if (simpleMethodElement.getAttribute("method-name").isEmpty()) {
                MiniLangValidate.handleError("Element must include the \"method-name\" attribute.".concat(locationMsg), null, simpleMethodElement);
            }
            for (int i = 0; i < DEPRECATED_ATTRIBUTES.length; i++) {
                if (!simpleMethodElement.getAttribute(DEPRECATED_ATTRIBUTES[i]).isEmpty()) {
                    MiniLangValidate.handleError("Attribute \"" + DEPRECATED_ATTRIBUTES[i] + "\" is deprecated (no replacement)." + locationMsg, null, simpleMethodElement);
                }
            }
        }
        boolean elementModified = autoCorrect(simpleMethodElement);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(simpleMethodElement);
        }
        this.fromLocation = fromLocation;
        methodName = simpleMethodElement.getAttribute("method-name");
        shortDescription = simpleMethodElement.getAttribute("short-description");
        defaultErrorCode = UtilXml.elementAttribute(simpleMethodElement, "default-error-code", "error");
        defaultSuccessCode = UtilXml.elementAttribute(simpleMethodElement, "default-success-code", "success");
        eventRequestName = UtilXml.elementAttribute(simpleMethodElement, "event-request-object-name", "request");
        eventSessionName = UtilXml.elementAttribute(simpleMethodElement, "event-session-object-name", "session");
        eventResponseName = UtilXml.elementAttribute(simpleMethodElement, "event-response-object-name", "response");
        eventResponseCodeName = UtilXml.elementAttribute(simpleMethodElement, "event-response-code-name", "_response_code_");
        eventErrorMessageName = UtilXml.elementAttribute(simpleMethodElement, "event-error-message-name", "_error_message_");
        eventErrorMessageListName = UtilXml.elementAttribute(simpleMethodElement, "event-error-message-list-name", "_error_message_list_");
        eventEventMessageName = UtilXml.elementAttribute(simpleMethodElement, "event-event-message-name", "_event_message_");
        eventEventMessageListName = UtilXml.elementAttribute(simpleMethodElement, "event-event-message-list-name", "_event_message_list_");
        serviceResponseMessageName = UtilXml.elementAttribute(simpleMethodElement, "service-response-message-name", "responseMessage");
        serviceErrorMessageName = UtilXml.elementAttribute(simpleMethodElement, "service-error-message-name", "errorMessage");
        serviceErrorMessageListName = UtilXml.elementAttribute(simpleMethodElement, "service-error-message-list-name", "errorMessageList");
        serviceErrorMessageMapName = UtilXml.elementAttribute(simpleMethodElement, "service-error-message-map-name", "errorMessageMap");
        serviceSuccessMessageName = UtilXml.elementAttribute(simpleMethodElement, "service-success-message-name", "successMessage");
        serviceSuccessMessageListName = UtilXml.elementAttribute(simpleMethodElement, "service-success-message-list-name", "successMessageList");
        loginRequired = !"false".equals(simpleMethodElement.getAttribute("login-required"));
        useTransaction = !"false".equals(simpleMethodElement.getAttribute("use-transaction"));
        methodOperations = Collections.unmodifiableList(readOperations(simpleMethodElement, this));
    }

    public void addErrorMessage(MethodContext methodContext, String message) {
        String messageListName = methodContext.getMethodType() == MethodContext.EVENT ? getEventErrorMessageListName() : getServiceErrorMessageListName();
        addMessage(methodContext, messageListName, message);
    }

    public void addMessage(MethodContext methodContext, String message) {
        String messageListName = methodContext.getMethodType() == MethodContext.EVENT ? getEventEventMessageListName() : getServiceSuccessMessageListName();
        addMessage(methodContext, messageListName, message);
    }

    private void addMessage(MethodContext methodContext, String messageListName, String message) {
        List<String> messages = methodContext.getEnv(messageListName);
        if (messages == null) {
            messages = new LinkedList<>();
            methodContext.putEnv(messageListName, messages);
        }
        messages.add(message);
    }

    /** Execute the Simple Method operations */
    public String exec(MethodContext methodContext) throws MiniLangException {
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "Begin simple-method. Script is running as " + (methodContext.getMethodType() == MethodContext.EVENT ? "an event." : "a service."));
        }
        Locale locale = methodContext.getLocale();
        GenericValue userLogin = methodContext.getUserLogin();
        if (loginRequired) {
            if (userLogin == null) {
                Map<String, Object> messageMap = UtilMisc.<String, Object> toMap("shortDescription", shortDescription);
                String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.must_logged_process", messageMap, locale) + ".";
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "login-required attribute set to \"true\" but UserLogin GenericValue was not found, returning error message:", errMsg);
                }
                return returnError(methodContext, errMsg);
            }
        }
        if (userLogin != null) {
            methodContext.putEnv(getUserLoginEnvName(), userLogin);
        }
        methodContext.putEnv("nullField", GenericEntity.NULL_FIELD);
        methodContext.putEnv(getDelegatorEnvName(), methodContext.getDelegator());
        methodContext.putEnv(getSecurityEnvName(), methodContext.getSecurity());
        methodContext.putEnv(getDispatcherEnvName(), methodContext.getDispatcher());
        methodContext.putEnv("locale", locale);
        methodContext.putEnv(getParameterMapName(), methodContext.getParameters());
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.putEnv(eventRequestName, methodContext.getRequest());
            methodContext.putEnv(eventSessionName, methodContext.getRequest().getSession());
            methodContext.putEnv(eventResponseName, methodContext.getResponse());
        }
        methodContext.putEnv("simpleMethod", this);
        methodContext.putEnv("methodName", this.getMethodName());
        methodContext.putEnv("methodShortDescription", this.getShortDescription());
        // if using transaction, try to start here
        boolean beganTransaction = false;
        if (useTransaction) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "use-transaction attribute set to \"true\", beginning transaction.");
            }
            try {
                beganTransaction = TransactionUtil.begin();
            } catch (GenericTransactionException e) {
                String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.error_begin_transaction", locale) + ": " + e.getMessage();
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "An exception was thrown while beginning a transaction, returning error message:", errMsg);
                }
                return returnError(methodContext, errMsg);
            }
        }
        // declare errorMsg here just in case transaction ops fail
        String errorMsg = "";
        boolean finished = false;
        try {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Begin running sub-elements.");
            }
            finished = runSubOps(methodOperations, methodContext);
        } catch (Throwable t) {
            // make SURE nothing gets thrown through
            String errMsg = UtilProperties.getMessage(SimpleMethod.err_resource, "simpleMethod.error_running", locale) + ": " + t.getMessage();
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "An exception was thrown while running sub-elements, error message was:", errMsg);
            }
            finished = false;
            errorMsg += errMsg;
        }
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "End running sub-elements.");
        }
        String returnValue = null;
        String response = null;
        StringBuilder summaryErrorStringBuffer = new StringBuilder();
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            boolean forceError = false;
            String tempErrorMsg = (String) methodContext.getEnv(eventErrorMessageName);
            if (errorMsg.length() > 0 || UtilValidate.isNotEmpty(tempErrorMsg)) {
                errorMsg += tempErrorMsg;
                methodContext.getRequest().setAttribute("_ERROR_MESSAGE_", errorMsg);
                forceError = true;
                summaryErrorStringBuffer.append(errorMsg);
            }
            List<Object> tempErrorMsgList = UtilGenerics.checkList(methodContext.getEnv(eventErrorMessageListName));
            if (UtilValidate.isNotEmpty(tempErrorMsgList)) {
                methodContext.getRequest().setAttribute("_ERROR_MESSAGE_LIST_", tempErrorMsgList);
                forceError = true;
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(tempErrorMsgList.toString());
            }
            String eventMsg = (String) methodContext.getEnv(eventEventMessageName);
            if (UtilValidate.isNotEmpty(eventMsg)) {
                methodContext.getRequest().setAttribute("_EVENT_MESSAGE_", eventMsg);
            }
            List<String> eventMsgList = UtilGenerics.checkList(methodContext.getEnv(eventEventMessageListName));
            if (UtilValidate.isNotEmpty(eventMsgList)) {
                methodContext.getRequest().setAttribute("_EVENT_MESSAGE_LIST_", eventMsgList);
            }
            response = (String) methodContext.getEnv(eventResponseCodeName);
            if (UtilValidate.isEmpty(response)) {
                if (forceError) {
                    // override response code, always use error code
                    Debug.logInfo("No response code string found, but error messages found so assuming error; returning code [" + defaultErrorCode + "]", module);
                    response = defaultErrorCode;
                } else {
                    Debug.logInfo("No response code string or errors found, assuming success; returning code [" + defaultSuccessCode + "]", module);
                    response = defaultSuccessCode;
                }
            } else if ("null".equalsIgnoreCase(response)) {
                response = null;
            }
            returnValue = response;
        } else {
            boolean forceError = false;
            String tempErrorMsg = (String) methodContext.getEnv(serviceErrorMessageName);
            if (errorMsg.length() > 0 || UtilValidate.isNotEmpty(tempErrorMsg)) {
                errorMsg += tempErrorMsg;
                methodContext.putResult(ModelService.ERROR_MESSAGE, errorMsg);
                forceError = true;
                summaryErrorStringBuffer.append(errorMsg);
            }
            List<Object> errorMsgList = UtilGenerics.checkList(methodContext.getEnv(serviceErrorMessageListName));
            if (UtilValidate.isNotEmpty(errorMsgList)) {
                methodContext.putResult(ModelService.ERROR_MESSAGE_LIST, errorMsgList);
                forceError = true;
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(errorMsgList.toString());
            }
            Map<String, Object> errorMsgMap = UtilGenerics.checkMap(methodContext.getEnv(serviceErrorMessageMapName));
            if (UtilValidate.isNotEmpty(errorMsgMap)) {
                methodContext.putResult(ModelService.ERROR_MESSAGE_MAP, errorMsgMap);
                forceError = true;
                summaryErrorStringBuffer.append("; ");
                summaryErrorStringBuffer.append(errorMsgMap.toString());
            }
            String successMsg = (String) methodContext.getEnv(serviceSuccessMessageName);
            if (UtilValidate.isNotEmpty(successMsg)) {
                methodContext.putResult(ModelService.SUCCESS_MESSAGE, successMsg);
            }
            List<Object> successMsgList = UtilGenerics.checkList(methodContext.getEnv(serviceSuccessMessageListName));
            if (UtilValidate.isNotEmpty(successMsgList)) {
                methodContext.putResult(ModelService.SUCCESS_MESSAGE_LIST, successMsgList);
            }
            response = (String) methodContext.getEnv(serviceResponseMessageName);
            if (UtilValidate.isEmpty(response)) {
                if (forceError) {
                    // override response code, always use error code
                    if (Debug.verboseOn()) Debug.logVerbose("No response code string found, but error messages found so assuming error; returning code [" + defaultErrorCode + "]", module);
                    response = defaultErrorCode;
                } else {
                    if (Debug.verboseOn()) Debug.logVerbose("No response code string or errors found, assuming success; returning code [" + defaultSuccessCode + "]", module);
                    response = defaultSuccessCode;
                }
            }
            methodContext.putResult(ModelService.RESPONSE_MESSAGE, response);
            returnValue = response;
        }
        // decide whether or not to commit based on the response message, ie only rollback if error is returned and not finished
        boolean doCommit = true;
        if (!finished && defaultErrorCode.equals(response)) {
            doCommit = false;
        }
        if (doCommit) {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Begin commit transaction.");
            }
            // commit here passing beganTransaction to perform it properly
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                String errMsg = "Error trying to commit transaction, could not process method: " + e.getMessage();
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "An exception was thrown while committing a transaction, returning error message:", errMsg);
                }
                errorMsg += errMsg;
            }
        } else {
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Begin roll back transaction.");
            }
            // rollback here passing beganTransaction to either rollback, or set rollback only
            try {
                TransactionUtil.rollback(beganTransaction, "Error in simple-method [" + this.getShortDescription() + "]: " + summaryErrorStringBuffer, null);
            } catch (GenericTransactionException e) {
                String errMsg = "Error trying to rollback transaction, could not process method: " + e.getMessage();
                if (methodContext.isTraceOn()) {
                    outputTraceMessage(methodContext, "An exception was thrown while rolling back a transaction, returning error message:", errMsg);
                }
                errorMsg += errMsg;
            }
        }
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "End simple-method.");
        }
        return returnValue;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        for (MethodOperation methodOp : this.methodOperations) {
            methodOp.gatherArtifactInfo(aic);
        }
    }

    @Deprecated
    public Set<String> getAllEntityNamesUsed() throws MiniLangException {
        ArtifactInfoContext aic = new ArtifactInfoContext();
        gatherArtifactInfo(aic);
        return aic.getEntityNames();
    }

    @Deprecated
    public Set<String> getAllServiceNamesCalled() throws MiniLangException {
        ArtifactInfoContext aic = new ArtifactInfoContext();
        gatherArtifactInfo(aic);
        return aic.getServiceNames();
    }

    public String getDefaultErrorCode() {
        return this.defaultErrorCode;
    }

    public String getDefaultSuccessCode() {
        return this.defaultSuccessCode;
    }

    public String getDelegatorEnvName() {
        return "delegator";
    }

    public String getDispatcherEnvName() {
        return "dispatcher";
    }

    public String getEventErrorMessageListName() {
        return this.eventErrorMessageListName;
    }

    public String getEventErrorMessageName() {
        return this.eventErrorMessageName;
    }

    public String getEventEventMessageListName() {
        return this.eventEventMessageListName;
    }

    public String getEventEventMessageName() {
        return this.eventEventMessageName;
    }

    // event fields
    public String getEventRequestName() {
        return this.eventRequestName;
    }

    public String getEventResponseCodeName() {
        return this.eventResponseCodeName;
    }

    public String getEventSessionName() {
        return this.eventSessionName;
    }

    public String getFileName() {
        return this.fromLocation.substring(this.fromLocation.lastIndexOf("/") + 1);
    }

    public String getFromLocation() {
        return this.fromLocation;
    }

    public String getLocationAndName() {
        return this.fromLocation + "#" + this.methodName;
    }

    public boolean getLoginRequired() {
        return this.loginRequired;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public List<MethodOperation> getMethodOperations() {
        return this.methodOperations;
    }

    public String getParameterMapName() {
        return "parameters";
    }

    public String getSecurityEnvName() {
        return "security";
    }

    public String getServiceErrorMessageListName() {
        return this.serviceErrorMessageListName;
    }

    public String getServiceErrorMessageMapName() {
        return this.serviceErrorMessageMapName;
    }

    public String getServiceErrorMessageName() {
        return this.serviceErrorMessageName;
    }

    public String getServiceResponseMessageName() {
        return this.serviceResponseMessageName;
    }

    public String getServiceSuccessMessageListName() {
        return this.serviceSuccessMessageListName;
    }

    public String getServiceSuccessMessageName() {
        return this.serviceSuccessMessageName;
    }

    public String getShortDescription() {
        return this.shortDescription + " [" + this.fromLocation + "#" + this.methodName + "]";
    }

    @Override
    public SimpleMethod getSimpleMethod() {
        return this;
    }

    public String getUserLoginEnvName() {
        return "userLogin";
    }

    public boolean getUseTransaction() {
        return this.useTransaction;
    }

    private String returnError(MethodContext methodContext, String errorMsg) {
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            methodContext.getRequest().setAttribute("_ERROR_MESSAGE_", errorMsg);
        } else {
            methodContext.putResult(ModelService.ERROR_MESSAGE, errorMsg);
            methodContext.putResult(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        }
        return defaultErrorCode;
    }
}
