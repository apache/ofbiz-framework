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

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
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
import org.ofbiz.minilang.method.MethodOperation.DeprecatedOperation;
import org.ofbiz.minilang.method.callops.CallService;
import org.ofbiz.minilang.method.callops.CallServiceAsynch;
import org.ofbiz.minilang.method.callops.CallSimpleMethod;
import org.ofbiz.minilang.method.callops.SetServiceFields;
import org.ofbiz.minilang.method.conditional.MasterIf;
import org.ofbiz.minilang.method.conditional.While;
import org.ofbiz.minilang.method.entityops.GetRelated;
import org.ofbiz.minilang.method.entityops.GetRelatedOne;
import org.ofbiz.minilang.method.entityops.EntityAnd;
import org.ofbiz.minilang.method.entityops.EntityCondition;
import org.ofbiz.minilang.method.entityops.EntityCount;
import org.ofbiz.minilang.method.entityops.EntityOne;
import org.ofbiz.minilang.method.entityops.FindByAnd;
import org.ofbiz.minilang.method.entityops.FindByPrimaryKey;
import org.ofbiz.minilang.method.entityops.MakeValue;
import org.ofbiz.minilang.method.envops.Iterate;
import org.ofbiz.minilang.method.envops.IterateMap;
import org.ofbiz.minilang.method.envops.Loop;
import org.ofbiz.minilang.method.ifops.IfCompare;
import org.ofbiz.minilang.method.ifops.IfCompareField;
import org.ofbiz.minilang.method.ifops.IfEmpty;
import org.ofbiz.minilang.method.ifops.IfHasPermission;
import org.ofbiz.minilang.method.ifops.IfInstanceOf;
import org.ofbiz.minilang.method.ifops.IfNotEmpty;
import org.ofbiz.minilang.method.ifops.IfRegexp;
import org.ofbiz.minilang.method.ifops.IfValidateMethod;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SimpleMethod Mini Language Core Object
 */
public class SimpleMethod {
    private static final Map<String, MethodOperation.Factory<MethodOperation>> methodOperationFactories;
    // never read locally: private static final Method simpleMethodExecMethod;
    private static final Method methodOperationExecMethod;
    static {
        Map<String, MethodOperation.Factory<MethodOperation>> mapFactories = new HashMap<String, MethodOperation.Factory<MethodOperation>>();
        Iterator<MethodOperation.Factory<MethodOperation>> it = UtilGenerics.cast(ServiceLoader.load(MethodOperation.Factory.class, SimpleMethod.class.getClassLoader()).iterator());
        while (it.hasNext()) {
            MethodOperation.Factory<MethodOperation> factory = it.next();
            mapFactories.put(factory.getName(), factory);
        }
        methodOperationFactories = Collections.unmodifiableMap(mapFactories);
        try {
            // never read locally: simpleMethodExecMethod = SimpleMethod.class.getDeclaredMethod("exec", MethodContext.class);
            methodOperationExecMethod = MethodOperation.class.getDeclaredMethod("exec", MethodContext.class);
        } catch (NoSuchMethodException e) {
            throw UtilMisc.initCause(new InternalError(e.getMessage()), e);
        }
    }

    public static final String module = SimpleMethod.class.getName();
    public static final String err_resource = "MiniLangErrorUiLabels";

    protected static UtilCache<String, Map<String, SimpleMethod>> simpleMethodsDirectCache = UtilCache.createUtilCache("minilang.SimpleMethodsDirect", 0, 0);
    protected static UtilCache<String, Map<String, SimpleMethod>> simpleMethodsResourceCache = UtilCache.createUtilCache("minilang.SimpleMethodsResource", 0, 0);
    protected static UtilCache<URL, Map<String, SimpleMethod>> simpleMethodsURLCache = UtilCache.createUtilCache("minilang.SimpleMethodsURL", 0, 0);

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

    // ----- General Method Invokers -----

    public static String runSimpleMethod(String xmlResource, String methodName, MethodContext methodContext) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = getSimpleMethods(xmlResource, methodContext.getLoader());
        SimpleMethod simpleMethod = simpleMethods.get(methodName);
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document in resource: " + xmlResource);
        }
        return simpleMethod.exec(methodContext);
    }

    public static String runSimpleMethod(URL xmlURL, String methodName, MethodContext methodContext) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = getSimpleMethods(xmlURL);
        SimpleMethod simpleMethod = simpleMethods.get(methodName);
        if (simpleMethod == null) {
            throw new MiniLangException("Could not find SimpleMethod " + methodName + " in XML document from URL: " + xmlURL.toString());
        }
        return simpleMethod.exec(methodContext);
    }

    public static Map<String, SimpleMethod> getSimpleMethods(String xmlResource, ClassLoader loader) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = simpleMethodsResourceCache.get(xmlResource);
        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = simpleMethodsResourceCache.get(xmlResource);
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

    public static List<SimpleMethod> getSimpleMethodsList(String xmlResource, ClassLoader loader) throws MiniLangException {
        List<SimpleMethod> simpleMethods = FastList.newInstance();

        // Let the standard Map returning method take care of caching and compilation
        Map<String, SimpleMethod> simpleMethodMap = SimpleMethod.getSimpleMethods(xmlResource, loader);

        // Load and traverse the document again to get a correctly ordered list of methods
        URL xmlURL = null;
        try {
            xmlURL = FlexibleLocation.resolveLocation(xmlResource, loader);
        } catch (MalformedURLException e) {
            throw new MiniLangException("Could not find SimpleMethod XML document in resource: " + xmlResource + "; error was: " + e.toString(), e);
        }
        // read in the file
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(xmlURL, true, true);
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
        for (Element simpleMethodElement: UtilXml.childElementList(rootElement, "simple-method")) {
            simpleMethods.add(simpleMethodMap.get(simpleMethodElement.getAttribute("method-name")));
        }

        return simpleMethods;
    }

    public static Map<String, SimpleMethod> getSimpleMethods(URL xmlURL) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = simpleMethodsURLCache.get(xmlURL);

        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = simpleMethodsURLCache.get(xmlURL);
                if (simpleMethods == null) {
                    simpleMethods = getAllSimpleMethods(xmlURL);

                    // put it in the cache
                    simpleMethodsURLCache.put(xmlURL, simpleMethods);
                }
            }
        }

        return simpleMethods;
    }

    protected static Map<String, SimpleMethod> getAllSimpleMethods(URL xmlURL) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = FastMap.newInstance();

        // read in the file
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(xmlURL, true, true);
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
        for (Element simpleMethodElement: UtilXml.childElementList(rootElement, "simple-method")) {
            SimpleMethod simpleMethod = compileSimpleMethod(simpleMethodElement, simpleMethods, xmlURL.toString());
            simpleMethods.put(simpleMethod.getMethodName(), simpleMethod);
        }

        return simpleMethods;
    }

    protected static SimpleMethod compileSimpleMethod(Element simpleMethodElement, Map<String, SimpleMethod> simpleMethods, String location) {
        return new SimpleMethod(simpleMethodElement, simpleMethods, location);
    }

    public static Map<String, SimpleMethod> getDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        Map<String, SimpleMethod> simpleMethods = simpleMethodsDirectCache.get(name);

        if (simpleMethods == null) {
            synchronized (SimpleMethod.class) {
                simpleMethods = simpleMethodsDirectCache.get(name);
                if (simpleMethods == null) {
                    simpleMethods = getAllDirectSimpleMethods(name, content, fromLocation);

                    // put it in the cache
                    simpleMethodsDirectCache.put(name, simpleMethods);
                }
            }
        }

        return simpleMethods;
    }

    protected static Map<String, SimpleMethod> getAllDirectSimpleMethods(String name, String content, String fromLocation) throws MiniLangException {
        if (UtilValidate.isEmpty(fromLocation)) {
            fromLocation = "<location not known>";
        }

        Map<String, SimpleMethod> simpleMethods = FastMap.newInstance();

        // read in the file
        Document document = null;

        try {
            if (content != null) {
                document = UtilXml.readXmlDocument(content, true, true);
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
        for (Element simpleMethodElement: UtilXml.childElementList(rootElement, "simple-method")) {
            SimpleMethod simpleMethod = compileSimpleMethod(simpleMethodElement, simpleMethods, fromLocation);
            simpleMethods.put(simpleMethod.getMethodName(), simpleMethod);
        }

        return simpleMethods;
    }

    // Member fields begin here...
    protected List<MethodOperation> methodOperations = FastList.newInstance();
    protected Map<String, SimpleMethod> parentSimpleMethodsMap;
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

    public SimpleMethod(Element simpleMethodElement, Map<String, SimpleMethod> parentSimpleMethodsMap, String fromLocation) {
        this.parentSimpleMethodsMap = parentSimpleMethodsMap;
        this.fromLocation = fromLocation;
        this.methodName = simpleMethodElement.getAttribute("method-name");
        this.shortDescription = simpleMethodElement.getAttribute("short-description");

        defaultErrorCode = UtilXml.elementAttribute(simpleMethodElement, "default-error-code", "error");
        defaultSuccessCode = UtilXml.elementAttribute(simpleMethodElement, "default-success-code", "success");

        parameterMapName = UtilXml.elementAttribute(simpleMethodElement, "parameter-map-name", "parameters");

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

        localeName = UtilXml.elementAttribute(simpleMethodElement, "locale-name", "locale");
        delegatorName = UtilXml.elementAttribute(simpleMethodElement, "delegator-name", "delegator");
        securityName = UtilXml.elementAttribute(simpleMethodElement, "security-name", "security");
        dispatcherName = UtilXml.elementAttribute(simpleMethodElement, "dispatcher-name", "dispatcher");
        userLoginName = UtilXml.elementAttribute(simpleMethodElement, "user-login-name", "userLogin");

        readOperations(simpleMethodElement, this.methodOperations, this);
    }

    public String getFromLocation() {
        return this.fromLocation;
    }
    public String getMethodName() {
        return this.methodName;
    }
    public String getLocationAndName() {
        return this.fromLocation + "#" + this.methodName;
    }

    public SimpleMethod getSimpleMethodInSameFile(String simpleMethodName) {
        if (parentSimpleMethodsMap == null) return null;
        return parentSimpleMethodsMap.get(simpleMethodName);
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

    public String getServiceErrorMessageMapName() {
        return this.serviceErrorMessageMapName;
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

    public Set<String> getAllServiceNamesCalled() throws MiniLangException {
        Set<String> allServiceNames = FastSet.newInstance();
        Set<String> simpleMethodsVisited = FastSet.newInstance();
        findServiceNamesCalled(this.methodOperations, allServiceNames, simpleMethodsVisited);
        return allServiceNames;
    }
    protected static void findServiceNamesCalled(List<MethodOperation> methodOperations, Set<String> allServiceNames, Set<String> simpleMethodsVisited) throws MiniLangException {
        for (MethodOperation methodOperation: methodOperations) {
            if (methodOperation instanceof CallService) {
                String svcName = ((CallService) methodOperation).getServiceName();
                if (UtilValidate.isNotEmpty(svcName)) allServiceNames.add(svcName);
            } else if (methodOperation instanceof CallServiceAsynch) {
                String svcName = ((CallServiceAsynch) methodOperation).getServiceName();
                if (UtilValidate.isNotEmpty(svcName)) allServiceNames.add(svcName);
            } else if (methodOperation instanceof SetServiceFields) {
                String svcName = ((SetServiceFields) methodOperation).getServiceName();
                if (UtilValidate.isNotEmpty(svcName)) allServiceNames.add(svcName);

            } else if (methodOperation instanceof CallSimpleMethod) {
                CallSimpleMethod csm = (CallSimpleMethod) methodOperation;
                try {
                    SimpleMethod calledMethod = csm.getSimpleMethodToCall(methodOperations.getClass().getClassLoader());
                    if (calledMethod == null) {
                        Debug.logWarning("Could not find simple-method [" + csm.getMethodName() + "] in [" + csm.getXmlResource() + "] from the SimpleMethod [" + csm.getSimpleMethod().getMethodName() + "] in [" + csm.getSimpleMethod().getFromLocation() + "]", module);
                    } else {
                        if (!simpleMethodsVisited.contains(calledMethod.getLocationAndName())) {
                            simpleMethodsVisited.add(calledMethod.getLocationAndName());
                            findServiceNamesCalled(calledMethod.methodOperations, allServiceNames, simpleMethodsVisited);
                        }
                    }
                } catch (MiniLangException e) {
                    Debug.logWarning("Error getting simple-method info in the [" + csm.getSimpleMethod().getMethodName() + "] in [" + csm.getSimpleMethod().getFromLocation() + "]: " + e.toString(), module);
                }
            } else if (methodOperation instanceof Iterate) {
                findServiceNamesCalled(((Iterate) methodOperation).getSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IterateMap) {
                findServiceNamesCalled(((IterateMap) methodOperation).getSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof Loop) {
                findServiceNamesCalled(((Loop) methodOperation).getSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof MasterIf) {
                findServiceNamesCalled(((MasterIf) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof While) {
                findServiceNamesCalled(((While) methodOperation).getThenSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfValidateMethod) {
                findServiceNamesCalled(((IfValidateMethod) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfInstanceOf) {
                findServiceNamesCalled(((IfInstanceOf) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfCompare) {
                findServiceNamesCalled(((IfCompare) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfCompareField) {
                findServiceNamesCalled(((IfCompareField) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfRegexp) {
                findServiceNamesCalled(((IfRegexp) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfEmpty) {
                findServiceNamesCalled(((IfEmpty) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfNotEmpty) {
                findServiceNamesCalled(((IfNotEmpty) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfHasPermission) {
                findServiceNamesCalled(((IfHasPermission) methodOperation).getAllSubOps(), allServiceNames, simpleMethodsVisited);
            }
        }
    }

    public Set<String> getAllEntityNamesUsed() throws MiniLangException {
        Set<String> allEntityNames = FastSet.newInstance();
        Set<String> simpleMethodsVisited = FastSet.newInstance();
        findEntityNamesUsed(this.methodOperations, allEntityNames, simpleMethodsVisited);
        return allEntityNames;
    }
    protected static void findEntityNamesUsed(List<MethodOperation> methodOperations, Set<String> allEntityNames, Set<String> simpleMethodsVisited) throws MiniLangException {
        for (MethodOperation methodOperation: methodOperations) {
            if (methodOperation instanceof FindByPrimaryKey) {
                String entName = ((FindByPrimaryKey) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof FindByAnd) {
                String entName = ((FindByAnd) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof EntityOne) {
                String entName = ((EntityOne) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof EntityAnd) {
                String entName = ((EntityAnd) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof EntityCondition) {
                String entName = ((EntityCondition) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof EntityCount) {
                String entName = ((EntityCount) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof MakeValue) {
                String entName = ((MakeValue) methodOperation).getEntityName();
                if (UtilValidate.isNotEmpty(entName)) allEntityNames.add(entName);
            } else if (methodOperation instanceof GetRelated) {
                String relationName = ((GetRelated) methodOperation).getRelationName();
                if (UtilValidate.isNotEmpty(relationName)) allEntityNames.add(relationName);
            } else if (methodOperation instanceof GetRelatedOne) {
                String relationName = ((GetRelatedOne) methodOperation).getRelationName();
                if (UtilValidate.isNotEmpty(relationName)) allEntityNames.add(relationName);

            } else if (methodOperation instanceof CallSimpleMethod) {
                CallSimpleMethod csm = (CallSimpleMethod) methodOperation;
                try {
                    SimpleMethod calledMethod = csm.getSimpleMethodToCall(null);
                    if (calledMethod == null) {
                        Debug.logWarning("Could not find simple-method [" + csm.getMethodName() + "] in [" + csm.getXmlResource() + "] from the SimpleMethod [" + csm.getSimpleMethod().getMethodName() + "] in [" + csm.getSimpleMethod().getFromLocation() + "]", module);
                    } else {
                        if (!simpleMethodsVisited.contains(calledMethod.getLocationAndName())) {
                            simpleMethodsVisited.add(calledMethod.getLocationAndName());
                            findEntityNamesUsed(calledMethod.methodOperations, allEntityNames, simpleMethodsVisited);
                        }
                    }
                } catch (MiniLangException e) {
                    Debug.logWarning("Error getting simple-method info in the [" + csm.getSimpleMethod().getMethodName() + "] in [" + csm.getSimpleMethod().getFromLocation() + "]: " + e.toString(), module);
                }
            } else if (methodOperation instanceof Iterate) {
                findEntityNamesUsed(((Iterate) methodOperation).getSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IterateMap) {
                findEntityNamesUsed(((IterateMap) methodOperation).getSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof Loop) {
                findEntityNamesUsed(((Loop) methodOperation).getSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof MasterIf) {
                findEntityNamesUsed(((MasterIf) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof While) {
                findEntityNamesUsed(((While) methodOperation).getThenSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfValidateMethod) {
                findEntityNamesUsed(((IfValidateMethod) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfInstanceOf) {
                findEntityNamesUsed(((IfInstanceOf) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfCompare) {
                findEntityNamesUsed(((IfCompare) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfCompareField) {
                findEntityNamesUsed(((IfCompareField) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfRegexp) {
                findEntityNamesUsed(((IfRegexp) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfEmpty) {
                findEntityNamesUsed(((IfEmpty) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfNotEmpty) {
                findEntityNamesUsed(((IfNotEmpty) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            } else if (methodOperation instanceof IfHasPermission) {
                findEntityNamesUsed(((IfHasPermission) methodOperation).getAllSubOps(), allEntityNames, simpleMethodsVisited);
            }
        }
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
                Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("shortDescription", shortDescription);
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
                    //override response code, always use error code
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
        } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
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

    public static void readOperations(Element simpleMethodElement, List<MethodOperation> methodOperations, SimpleMethod simpleMethod) {
        List<? extends Element> operationElements = UtilXml.childElementList(simpleMethodElement);

        if (UtilValidate.isNotEmpty(operationElements)) {
            for (Element curOperElem: operationElements) {
                String nodeName = curOperElem.getNodeName();
                MethodOperation methodOp = null;

                MethodOperation.Factory<MethodOperation> factory = methodOperationFactories.get(nodeName);
                if (factory != null) {
                    methodOp = factory.createMethodOperation(curOperElem, simpleMethod);
                } else if ("else".equals(nodeName)) {
                    // don't add anything, but don't complain either, this one is handled in the individual operations
                } else {
                    Debug.logWarning("Operation element \"" + nodeName + "\" no recognized", module);
                }
                if (methodOp == null) continue;
                methodOperations.add(methodOp);
                DeprecatedOperation depOp = methodOp.getClass().getAnnotation(DeprecatedOperation.class);
                if (depOp != null) Debug.logInfo("The " + nodeName + " operation has been deprecated in favor of the " + depOp.value() + " operation; found use of this in [" + simpleMethod.getShortDescription() + "]: " + methodOp.rawString(), module);
            }
        }
    }

    /** Execs the given operations returning true if all return true, or returning
     *  false and stopping if any return false.
     */
    public static boolean runSubOps(List<MethodOperation> methodOperations, MethodContext methodContext) {
        for (MethodOperation methodOperation: methodOperations) {
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
