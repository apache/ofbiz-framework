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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-engine&gt;</code> element.
 */
@ThreadSafe
public final class ServiceEngine {

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

    ServiceEngine(Element engineElement) throws ServiceConfigException {
        String name = engineElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<service-engine> element name attribute is empty");
        }
        this.name = name;
        Element authElement = UtilXml.firstChildElement(engineElement, "authorization");
        if (authElement == null) {
            throw new ServiceConfigException("<authorization> element is missing");
        }
        this.authorization = new Authorization(authElement);
        Element poolElement = UtilXml.firstChildElement(engineElement, "thread-pool");
        if (poolElement == null) {
            throw new ServiceConfigException("<thread-pool> element is missing");
        }
        this.threadPool = new ThreadPool(poolElement);
        List<? extends Element> engineElementList = UtilXml.childElementList(engineElement, "engine");
        if (engineElementList.isEmpty()) {
            this.engines = Collections.emptyList();
            this.engineMap = Collections.emptyMap();
        } else {
            List<Engine> engines = new ArrayList<Engine>(engineElementList.size());
            Map<String, Engine> engineMap = new HashMap<String, Engine>();
            for (Element childEngineElement : engineElementList) {
                Engine engine = new Engine(childEngineElement);
                engines.add(engine);
                engineMap.put(engine.getName(), engine);
            }
            this.engines = Collections.unmodifiableList(engines);
            this.engineMap = Collections.unmodifiableMap(engineMap);
        }
        List<? extends Element> serviceLocationElementList = UtilXml.childElementList(engineElement, "service-location");
        if (serviceLocationElementList.isEmpty()) {
            this.serviceLocations = Collections.emptyList();
        } else {
            List<ServiceLocation> serviceLocations = new ArrayList<ServiceLocation>(serviceLocationElementList.size());
            for (Element serviceLocationElement : serviceLocationElementList) {
                String location = serviceLocationElement.getAttribute("location").intern();
                if (location.contains("localhost") && Start.getInstance().getConfig().portOffset != 0) {
                    String s = location.substring(location.lastIndexOf(":") + 1);
                    Integer locationPort = Integer.valueOf(s.substring(0, s.indexOf("/")));
                    Integer port = locationPort + Start.getInstance().getConfig().portOffset;
                    location = location.replace(locationPort.toString(), port.toString());
                }
                serviceLocations.add(new ServiceLocation(serviceLocationElement, location));
            }
            this.serviceLocations = Collections.unmodifiableList(serviceLocations);
        }
        List<? extends Element> notificationGroupElementList = UtilXml.childElementList(engineElement, "notification-group");
        if (notificationGroupElementList.isEmpty()) {
            this.notificationGroups = Collections.emptyList();
        } else {
            List<NotificationGroup> notificationGroups = new ArrayList<NotificationGroup>(notificationGroupElementList.size());
            for (Element notificationGroupElement : notificationGroupElementList) {
                notificationGroups.add(new NotificationGroup(notificationGroupElement));
            }
            this.notificationGroups = Collections.unmodifiableList(notificationGroups);
        }
        List<? extends Element> startupServiceElementList = UtilXml.childElementList(engineElement, "startup-service");
        if (startupServiceElementList.isEmpty()) {
            this.startupServices = Collections.emptyList();
        } else {
            List<StartupService> startupServices = new ArrayList<StartupService>(startupServiceElementList.size());
            for (Element startupServiceElement : startupServiceElementList) {
                startupServices.add(new StartupService(startupServiceElement));
            }
            this.startupServices = Collections.unmodifiableList(startupServices);
        }
        List<? extends Element> resourceLoaderElementList = UtilXml.childElementList(engineElement, "resource-loader");
        if (resourceLoaderElementList.isEmpty()) {
            this.resourceLoaders = Collections.emptyList();
        } else {
            List<ResourceLoader> resourceLoaders = new ArrayList<ResourceLoader>(resourceLoaderElementList.size());
            for (Element resourceLoaderElement : resourceLoaderElementList) {
                resourceLoaders.add(new ResourceLoader(resourceLoaderElement));
            }
            this.resourceLoaders = Collections.unmodifiableList(resourceLoaders);
        }
        List<? extends Element> globalServicesElementList = UtilXml.childElementList(engineElement, "global-services");
        if (globalServicesElementList.isEmpty()) {
            this.globalServices = Collections.emptyList();
        } else {
            List<GlobalServices> globalServices = new ArrayList<GlobalServices>(globalServicesElementList.size());
            for (Element globalServicesElement : globalServicesElementList) {
                globalServices.add(new GlobalServices(globalServicesElement));
            }
            this.globalServices = Collections.unmodifiableList(globalServices);
        }
        List<? extends Element> serviceGroupsElementList = UtilXml.childElementList(engineElement, "service-groups");
        if (serviceGroupsElementList.isEmpty()) {
            this.serviceGroups = Collections.emptyList();
        } else {
            List<ServiceGroups> serviceGroups = new ArrayList<ServiceGroups>(serviceGroupsElementList.size());
            for (Element serviceGroupsElement : serviceGroupsElementList) {
                serviceGroups.add(new ServiceGroups(serviceGroupsElement));
            }
            this.serviceGroups = Collections.unmodifiableList(serviceGroups);
        }
        List<? extends Element> serviceEcasElementList = UtilXml.childElementList(engineElement, "service-ecas");
        if (serviceEcasElementList.isEmpty()) {
            this.serviceEcas = Collections.emptyList();
        } else {
            List<ServiceEcas> serviceEcas = new ArrayList<ServiceEcas>(serviceEcasElementList.size());
            for (Element serviceEcasElement : serviceEcasElementList) {
                serviceEcas.add(new ServiceEcas(serviceEcasElement));
            }
            this.serviceEcas = Collections.unmodifiableList(serviceEcas);
        }
        List<? extends Element> jmsServiceElementList = UtilXml.childElementList(engineElement, "jms-service");
        if (jmsServiceElementList.isEmpty()) {
            this.jmsServices = Collections.emptyList();
        } else {
            List<JmsService> jmsServices = new ArrayList<JmsService>(jmsServiceElementList.size());
            for (Element jmsServiceElement : jmsServiceElementList) {
                jmsServices.add(new JmsService(jmsServiceElement));
            }
            this.jmsServices = Collections.unmodifiableList(jmsServices);
        }
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public Engine getEngine(String engineName) {
        return engineMap.get(engineName);
    }

    public List<Engine> getEngines() {
        return this.engines;
    }

    public List<GlobalServices> getGlobalServices() {
        return this.globalServices;
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

    public String getName() {
        return name;
    }

    public List<NotificationGroup> getNotificationGroups() {
        return this.notificationGroups;
    }

    public List<ResourceLoader> getResourceLoaders() {
        return this.resourceLoaders;
    }

    public List<ServiceEcas> getServiceEcas() {
        return this.serviceEcas;
    }
    public List<ServiceGroups> getServiceGroups() {
        return this.serviceGroups;
    }

    public List<ServiceLocation> getServiceLocations() {
        return this.serviceLocations;
    }

    public List<StartupService> getStartupServices() {
        return this.startupServices;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }
}
