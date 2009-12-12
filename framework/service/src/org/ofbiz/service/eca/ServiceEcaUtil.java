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
package org.ofbiz.service.eca;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.w3c.dom.Element;

import freemarker.template.utility.StringUtil;

/**
 * ServiceEcaUtil
 */
public class ServiceEcaUtil {

    public static final String module = ServiceEcaUtil.class.getName();

    // using a cache is dangerous here because if someone clears it the ECAs won't run: public static UtilCache ecaCache = new UtilCache("service.ServiceECAs", 0, 0, false);
    public static Map<String, Map<String, List<ServiceEcaRule>>> ecaCache = FastMap.newInstance();

    public static void reloadConfig() {
        ecaCache.clear();
        readConfig();
    }

    public static void readConfig() {
        // Only proceed if the cache hasn't already been populated, caller should be using reloadConfig() in that situation
        if (UtilValidate.isNotEmpty(ecaCache)) {
            return;
        }
        Element rootElement = null;
        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
            return;
        }

        for (Element serviceEcasElement: UtilXml.childElementList(rootElement, "service-ecas")) {
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, serviceEcasElement);
            addEcaDefinitions(handler);
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("eca")) {
            addEcaDefinitions(componentResourceInfo.createResourceHandler());
        }
    }

    public static void addEcaDefinitions(ResourceHandler handler) {
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }

        String resourceLocation = handler.getLocation();
        try {
            resourceLocation = handler.getURL().toExternalForm();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Could not get resource URL", module);
        }

        int numDefs = 0;
        for (Element e: UtilXml.childElementList(rootElement, "eca")) {
            String serviceName = e.getAttribute("service");
            String eventName = e.getAttribute("event");
            Map<String, List<ServiceEcaRule>> eventMap = ecaCache.get(serviceName);
            List<ServiceEcaRule> rules = null;

            if (eventMap == null) {
                eventMap = FastMap.newInstance();
                rules = FastList.newInstance();
                ecaCache.put(serviceName, eventMap);
                eventMap.put(eventName, rules);
            } else {
                rules = eventMap.get(eventName);
                if (rules == null) {
                    rules = FastList.newInstance();
                    eventMap.put(eventName, rules);
                }
            }
            rules.add(new ServiceEcaRule(e, resourceLocation));
            numDefs++;
        }
        if (Debug.importantOn()) {
            Debug.logImportant("Loaded [" + StringUtil.leftPad(Integer.toString(numDefs), 2) + "] Service ECA Rules from " + resourceLocation, module);
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
                List<ServiceEcaRule> rules = FastList.newInstance();
                for (Collection<ServiceEcaRule> col: eventMap.values()) {
                    rules.addAll(col);
                }
                return rules;
            }
        }
        return null;
    }

    public static void evalRules(String serviceName, Map<String, List<ServiceEcaRule>> eventMap, String event, DispatchContext dctx, Map<String, Object> context, Map<String, Object> result, boolean isError, boolean isFailure) throws GenericServiceException {
        // if the eventMap is passed we save a Map lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = getServiceEventMap(serviceName);
        if (UtilValidate.isEmpty(eventMap)) {
            return;
        }

        Collection<ServiceEcaRule> rules = eventMap.get(event);
        if (UtilValidate.isEmpty(rules)) {
            return;
        }

        if (Debug.verboseOn()) Debug.logVerbose("Running ECA (" + event + ").", module);
        Set<String> actionsRun = new TreeSet<String>();
        for (ServiceEcaRule eca: rules) {
            eca.eval(serviceName, dctx, context, result, isError, isFailure, actionsRun);
        }
    }
}
