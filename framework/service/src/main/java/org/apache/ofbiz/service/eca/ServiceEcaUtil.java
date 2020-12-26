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
package org.apache.ofbiz.service.eca;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.MainResourceHandler;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.ServiceEcas;
import org.w3c.dom.Element;

/**
 * ServiceEcaUtil
 */
public final class ServiceEcaUtil {

    private static final String MODULE = ServiceEcaUtil.class.getName();

    // using a cache is dangerous here because if someone clears it the ECAs won't run: public static UtilCache ecaCache =
    // new UtilCache("service.ServiceECAs", 0, 0, false);
    private static Map<String, Map<String, List<ServiceEcaRule>>> ecaCache = new ConcurrentHashMap<>();

    private ServiceEcaUtil() { }

    public static void reloadConfig() {
        ecaCache.clear();
        readConfig();
    }

    public static void readConfig() {
        // Only proceed if the cache hasn't already been populated, caller should be using reloadConfig() in that situation
        if (UtilValidate.isNotEmpty(ecaCache)) {
            return;
        }

        List<Future<List<ServiceEcaRule>>> futures = new LinkedList<>();
        List<ServiceEcas> serviceEcasList = null;
        try {
            serviceEcasList = ServiceConfigUtil.getServiceEngine().getServiceEcas();
        } catch (GenericConfigException e) {
            // FIXME: Refactor API so exceptions can be thrown and caught.
            Debug.logError(e, MODULE);
            throw new RuntimeException(e.getMessage());
        }
        for (ServiceEcas serviceEcas : serviceEcasList) {
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.getServiceEngineXmlFileName(), serviceEcas.getLoader(),
                    serviceEcas.getLocation());
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(createEcaLoaderCallable(handler)));
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("eca")) {
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(createEcaLoaderCallable(componentResourceInfo.createResourceHandler())));
        }

        for (List<ServiceEcaRule> handlerRules: ExecutionPool.getAllFutures(futures)) {
            mergeEcaDefinitions(handlerRules);
        }
    }

    private static Callable<List<ServiceEcaRule>> createEcaLoaderCallable(final ResourceHandler handler) {
        return () -> getEcaDefinitions(handler);
    }

    public static void addEcaDefinitions(ResourceHandler handler) {
        List<ServiceEcaRule> handlerRules = getEcaDefinitions(handler);
        mergeEcaDefinitions(handlerRules);
    }

    private static List<ServiceEcaRule> getEcaDefinitions(ResourceHandler handler) {
        List<ServiceEcaRule> handlerRules = new LinkedList<>();
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, MODULE);
            return handlerRules;
        }

        String resourceLocation = handler.getLocation();
        try {
            resourceLocation = handler.getURL().toExternalForm();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Could not get resource URL", MODULE);
        }
        for (Element e: UtilXml.childElementList(rootElement, "eca")) {
            handlerRules.add(new ServiceEcaRule(e, resourceLocation));
        }
        if (Debug.infoOn()) {
            Debug.logInfo("Loaded [" + handlerRules.size() + "] Service ECA Rules from " + resourceLocation, MODULE);
        }
        return handlerRules;
    }

    private static void mergeEcaDefinitions(List<ServiceEcaRule> handlerRules) {
        for (ServiceEcaRule rule: handlerRules) {
            String serviceName = rule.getServiceName();
            String eventName = rule.getEventName();
            Map<String, List<ServiceEcaRule>> eventMap = ecaCache.get(serviceName);
            List<ServiceEcaRule> rules = null;

            if (eventMap == null) {
                eventMap = new HashMap<>();
                rules = new LinkedList<>();
                ecaCache.put(serviceName, eventMap);
                eventMap.put(eventName, rules);
            } else {
                rules = eventMap.get(eventName);
                if (rules == null) {
                    rules = new LinkedList<>();
                    eventMap.put(eventName, rules);
                }
            }
            //remove the old rule if found and keep the recent one
            //This will prevent duplicate rule execution along with enabled/disabled seca workflow
            if (rules.remove(rule)) {
                Debug.logWarning("Duplicate Service ECA [" + serviceName + "] on [" + eventName + "] ", MODULE);
            }
            rules.add(rule);
        }
    }

    public static Map<String, List<ServiceEcaRule>> getServiceEventMap(String serviceName) {
        if (ServiceEcaUtil.ecaCache == null) ServiceEcaUtil.readConfig();
        return ServiceEcaUtil.ecaCache.get(serviceName);
    }

    public static List<ServiceEcaRule> getServiceEventRules(String serviceName, String event) {
        Map<String, List<ServiceEcaRule>> eventMap = getServiceEventMap(serviceName);
        if (eventMap != null) {
            if (event != null) {
                return eventMap.get(event);
            } else {
                List<ServiceEcaRule> rules = new LinkedList<>();
                for (Collection<ServiceEcaRule> col: eventMap.values()) {
                    rules.addAll(col);
                }
                return rules;
            }
        }
        return null;
    }

    public static void evalRules(String serviceName, Map<String, List<ServiceEcaRule>> eventMap, String event, DispatchContext dctx,
            Map<String, Object> context, Map<String, Object> result, boolean isError, boolean isFailure) throws GenericServiceException {
        // if the eventMap is passed we save a Map lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = getServiceEventMap(serviceName);
        if (UtilValidate.isEmpty(eventMap)) {
            return;
        }

        Collection<ServiceEcaRule> rules = eventMap.get(event);
        if (UtilValidate.isEmpty(rules)) {
            return;
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Running ECA (" + event + ").", MODULE);
        }
        Set<String> actionsRun = new TreeSet<>();
        for (ServiceEcaRule eca: rules) {
            eca.eval(serviceName, dctx, context, result, isError, isFailure, actionsRun);
        }
    }
}
