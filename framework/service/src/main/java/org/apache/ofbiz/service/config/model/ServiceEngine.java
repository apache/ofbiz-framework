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
package org.apache.ofbiz.service.config.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-engine&gt;</code> element.
 */
@ThreadSafe
public final class ServiceEngine extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "service-engine";
    private final String xPath;

    private final Authorization authorization;
    private final List<Engine> engines;
    private final Map<String, Engine> engineMap;
    private final List<GlobalServices> globalServices;
    private final List<JmsService> jmsServices;
    private final String name;
    private final List<NotificationGroup> notificationGroups;
    private final List<ResourceLoader> resourceLoaders;
    private final List<ServiceEcas> serviceEcas;
    private final List<ServiceGroups> serviceGroups;
    private final List<ServiceLocation> serviceLocations;
    private final List<StartupService> startupServices;
    private final ThreadPool threadPool;

    ServiceEngine(Element engineElement, String xPathParent) throws ServiceConfigException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        String name = engineElement.getAttribute("name").intern();
        if (name.isEmpty() && checkStructure) {
            throw new ServiceConfigException("<service-engine> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/service-engine[@name='" + name + "']");
        Authorization auth = config.getObjectSubElement(xPath, engineElement, Authorization.class);
        if (auth == null && checkStructure) {
            throw new ServiceConfigException("<authorization> element is missing");
        }
        this.authorization = auth;
        ThreadPool pool = config.getObjectSubElement(xPath, engineElement, ThreadPool.class);
        if (pool == null && checkStructure) {
            throw new ServiceConfigException("<thread-pool> element is missing");
        }
        this.threadPool = pool;
        List<Engine> engineList = config.getSubElementsAsListEntries(xPath, engineElement, Engine.class);
        engines = Collections.unmodifiableList(engineList);
        if (engineList.isEmpty()) {
            this.engineMap = Collections.emptyMap();
        } else {
            this.engineMap = config.getSubElementsAsMapValues(xPath, engineElement, Engine.class);
        }
        List<ServiceLocation> serviceLocationList = config.getSubElementsAsListEntries(xPath,
                engineElement, ServiceLocation.class);
        serviceLocations = Collections.unmodifiableList(serviceLocationList);
        List<NotificationGroup> notificationGroupList = config.getSubElementsAsListEntries(xPath,
                engineElement, NotificationGroup.class);
        notificationGroups = Collections.unmodifiableList(notificationGroupList);
        List<StartupService> startupServiceList = config.getSubElementsAsListEntries(xPath,
                engineElement, StartupService.class);
        startupServices = Collections.unmodifiableList(startupServiceList);
        List<ResourceLoader> resourceLoaderList = config.getSubElementsAsListEntries(xPath,
                engineElement, ResourceLoader.class);
        resourceLoaders = Collections.unmodifiableList(resourceLoaderList);
        List<GlobalServices> globalServicesList = config.getSubElementsAsListEntries(xPath,
                engineElement, GlobalServices.class);
        globalServices = Collections.unmodifiableList(globalServicesList);
        List<ServiceGroups> serviceGroupsList = config.getSubElementsAsListEntries(xPath,
                engineElement, ServiceGroups.class);
        serviceGroups = Collections.unmodifiableList(serviceGroupsList);
        List<ServiceEcas> serviceEcasList = config.getSubElementsAsListEntries(xPath,
                engineElement, ServiceEcas.class);
        serviceEcas = Collections.unmodifiableList(serviceEcasList);
        List<JmsService> jmsServiceList = config.getSubElementsAsListEntries(xPath,
                engineElement, JmsService.class);
        jmsServices = Collections.unmodifiableList(jmsServiceList);
    }

    ServiceEngine(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<service-engine> element name attribute is empty");
        }
        this.name = name;
        Authorization auth = config.getObjectSubElement(xPath, null, Authorization.class);
        if (auth == null) {
            throw new ServiceConfigException("<authorization> element is missing");
        }
        this.authorization = auth;
        ThreadPool pool = config.getObjectSubElement(xPath, null, ThreadPool.class);
        if (pool == null) {
            throw new ServiceConfigException("<thread-pool> element is missing");
        }
        this.threadPool = pool;
        List<Engine> engineList = config.getSubElementsAsListEntries(xPath, null, Engine.class);
        engines = Collections.unmodifiableList(engineList);
        if (engineList.isEmpty()) {
            this.engineMap = Collections.emptyMap();
        } else {
            this.engineMap = config.getSubElementsAsMapValues(xPath, null, Engine.class);
        }
        List<ServiceLocation> serviceLocationList = config.getSubElementsAsListEntries(xPath,
                null, ServiceLocation.class);
        serviceLocations = Collections.unmodifiableList(serviceLocationList);
        List<NotificationGroup> notificationGroupList = config.getSubElementsAsListEntries(xPath,
                null, NotificationGroup.class);
        notificationGroups = Collections.unmodifiableList(notificationGroupList);
        List<StartupService> startupServiceList = config.getSubElementsAsListEntries(xPath,
                null, StartupService.class);
        startupServices = Collections.unmodifiableList(startupServiceList);
        List<ResourceLoader> resourceLoaderList = config.getSubElementsAsListEntries(xPath,
                null, ResourceLoader.class);
        resourceLoaders = Collections.unmodifiableList(resourceLoaderList);
        List<GlobalServices> globalServicesList = config.getSubElementsAsListEntries(xPath,
                null, GlobalServices.class);
        globalServices = Collections.unmodifiableList(globalServicesList);
        List<ServiceGroups> serviceGroupsList = config.getSubElementsAsListEntries(xPath,
                null, ServiceGroups.class);
        serviceGroups = Collections.unmodifiableList(serviceGroupsList);
        List<ServiceEcas> serviceEcasList = config.getSubElementsAsListEntries(xPath,
                null, ServiceEcas.class);
        serviceEcas = Collections.unmodifiableList(serviceEcasList);
        List<JmsService> jmsServiceList = config.getSubElementsAsListEntries(xPath,
                null, JmsService.class);
        jmsServices = Collections.unmodifiableList(jmsServiceList);
    }

    public static ServiceEngine loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new ServiceEngine(element, xPathParent);
    }

    public static ServiceEngine loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new ServiceEngine(configMap, xPath);
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public Engine getEngine(String engineName) {
        return engineMap.get(engineName);
    }

    public List<Engine> getEngines() {
        return engines;
    }

    public List<GlobalServices> getGlobalServices() {
        return globalServices;
    }

    public JmsService getJmsServiceByName(String name) {
        for (JmsService jmsService : jmsServices) {
            if (name.equals(jmsService.getName())) {
                return jmsService;
            }
        }
        return null;
    }

    public List<JmsService> getJmsServices() {
        return this.jmsServices;
    }

    public List<NotificationGroup> getNotificationGroups() {
        return notificationGroups;
    }

    public List<ResourceLoader> getResourceLoaders() {
        return resourceLoaders;
    }

    public List<ServiceEcas> getServiceEcas() {
        return serviceEcas;
    }

    public List<ServiceGroups> getServiceGroups() {
        return serviceGroups;
    }

    public List<ServiceLocation> getServiceLocations() {
        return serviceLocations;
    }

    public List<StartupService> getStartupServices() {
        return startupServices;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    @Override
    public String getName() {
        return name;
    }

}
