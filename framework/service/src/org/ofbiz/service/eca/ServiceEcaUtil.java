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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;

import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;

import org.w3c.dom.Element;

/**
 * ServiceEcaUtil
 */
public class ServiceEcaUtil {

    public static final String module = ServiceEcaUtil.class.getName();

    // using a cache is dangerous here because if someone clears it the ECAs won't run: public static UtilCache ecaCache = new UtilCache("service.ServiceECAs", 0, 0, false);
    public static Map ecaCache = FastMap.newInstance();

    public static void reloadConfig() {
        ecaCache.clear();
        readConfig();
    }

    public static void readConfig() {
        Element rootElement = null;
        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
            return;
        }

        List serviceEcasElements = UtilXml.childElementList(rootElement, "service-ecas");
        Iterator secasIter = serviceEcasElements.iterator();
        while (secasIter.hasNext()) {
            Element serviceEcasElement = (Element) secasIter.next();
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, serviceEcasElement);
            addEcaDefinitions(handler);
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        List componentResourceInfos = ComponentConfig.getAllServiceResourceInfos("eca");
        Iterator componentResourceInfoIter = componentResourceInfos.iterator();
        while (componentResourceInfoIter.hasNext()) {
            ComponentConfig.ServiceResourceInfo componentResourceInfo = (ComponentConfig.ServiceResourceInfo) componentResourceInfoIter.next();
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

        List ecaList = UtilXml.childElementList(rootElement, "eca");
        Iterator ecaIt = ecaList.iterator();
        int numDefs = 0;
        while (ecaIt.hasNext()) {
            Element e = (Element) ecaIt.next();
            String serviceName = e.getAttribute("service");
            String eventName = e.getAttribute("event");
            Map eventMap = (Map) ecaCache.get(serviceName);
            List rules = null;

            if (eventMap == null) {
                eventMap = new HashMap();
                rules = new LinkedList();
                ecaCache.put(serviceName, eventMap);
                eventMap.put(eventName, rules);
            } else {
                rules = (List) eventMap.get(eventName);
                if (rules == null) {
                    rules = new LinkedList();
                    eventMap.put(eventName, rules);
                }
            }
            rules.add(new ServiceEcaRule(e));
            numDefs++;
        }
        if (Debug.importantOn()) {
            String resourceLocation = handler.getLocation();
            try {
                resourceLocation = handler.getURL().toExternalForm();
            } catch (GenericConfigException e) {
                Debug.logError(e, "Could not get resource URL", module);
            }
            Debug.logImportant("Loaded " + numDefs + " Service ECA definitions from " + resourceLocation, module);
        }
    }

    public static Map getServiceEventMap(String serviceName) {
        if (ServiceEcaUtil.ecaCache == null) ServiceEcaUtil.readConfig();
        return (Map) ServiceEcaUtil.ecaCache.get(serviceName);
    }

    public static Collection getServiceEventRules(String serviceName, String event) {
        Map eventMap = getServiceEventMap(serviceName);
        if (eventMap != null) {
            if (event != null) {
                return (Collection) eventMap.get(event);
            } else {
                return eventMap.values();
            }
        }
        return null;
    }

    public static void evalRules(String serviceName, Map eventMap, String event, DispatchContext dctx, Map context, Map result, boolean isError, boolean isFailure) throws GenericServiceException {
        // if the eventMap is passed we save a HashMap lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = getServiceEventMap(serviceName);
        if (eventMap == null || eventMap.size() == 0) {
            return;
        }

        List rules = (List) eventMap.get(event);
        if (rules == null || rules.size() == 0) {
            return;
        }

        Iterator i = rules.iterator();
        if (i.hasNext() && Debug.verboseOn()) Debug.logVerbose("Running ECA (" + event + ").", module);
        Set actionsRun = new TreeSet();
        while (i.hasNext()) {
            ServiceEcaRule eca = (ServiceEcaRule) i.next();
            eca.eval(serviceName, dctx, context, result, isError, isFailure, actionsRun);
        }
    }
}
