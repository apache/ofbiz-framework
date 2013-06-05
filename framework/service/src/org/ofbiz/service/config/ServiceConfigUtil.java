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
package org.ofbiz.service.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.service.config.model.Engine;
import org.ofbiz.service.config.model.NotificationGroup;
import org.ofbiz.service.config.model.RunFromPool;
import org.ofbiz.service.config.model.ServiceConfig;
import org.ofbiz.service.config.model.ServiceEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the serviceengine.xml file
 */
public final class ServiceConfigUtil {

    public static final String module = ServiceConfigUtil.class.getName();
    public static final String engine = "default";
    public static final String SERVICE_ENGINE_XML_FILENAME = "serviceengine.xml";
    // Keep the ServiceConfig instance in a cache - so the configuration can be reloaded at run-time. There will be only one ServiceConfig instance in the cache.
    private static final UtilCache<String, ServiceConfig> serviceConfigCache = UtilCache.createUtilCache("service.ServiceConfig", 0, 0, false);
    private static final List<ServiceConfigListener> configListeners = new CopyOnWriteArrayList<ServiceConfigListener>();

    /**
     * Returns the service engine configuration (currently always the default one).
     * @throws GenericConfigException 
     */
    private static ServiceEngine getServiceEngine() throws GenericConfigException {
        return getServiceConfig().getServiceEngine(engine);
    }

    /**
     * Returns the <code>ServiceConfig</code> instance.
     * @throws GenericConfigException
     */
    public static ServiceConfig getServiceConfig() throws GenericConfigException {
        ServiceConfig instance = serviceConfigCache.get("instance");
        if (instance == null) {
            Element serviceConfigElement = getXmlDocument().getDocumentElement();
            instance = ServiceConfig.create(serviceConfigElement);
            serviceConfigCache.putIfAbsent("instance", instance);
            instance = serviceConfigCache.get("instance");
            for (ServiceConfigListener listener : configListeners) {
                try {
                    listener.onServiceConfigChange(instance);
                } catch (Exception e) {
                    Debug.logError(e, "Exception thrown while notifying listener " + listener + ": ", module);
                }
            }
        }
        return instance;
    }

    private static Document getXmlDocument() throws GenericConfigException {
        URL confUrl = UtilURL.fromResource(SERVICE_ENGINE_XML_FILENAME);
        if (confUrl == null) {
            throw new GenericConfigException("Could not find the " + SERVICE_ENGINE_XML_FILENAME + " file");
        }
        try {
            return UtilXml.readXmlDocument(confUrl, true, true);
        } catch (Exception e) {
            throw new GenericConfigException("Exception thrown while reading " + SERVICE_ENGINE_XML_FILENAME + ": ", e);
        }
    }

    /**
     * Returns the specified <code>ServiceEngine</code> instance, or <code>null</code>
     * if the engine does not exist.
     * 
     * @throws GenericConfigException
     */
    public static ServiceEngine getServiceEngine(String name) throws GenericConfigException {
        return getServiceConfig().getServiceEngine(name);
    }

    public static void registerServiceConfigListener(ServiceConfigListener listener) {
        Assert.notNull("listener", listener);
        configListeners.add(listener);
    }

    public static String getAuthorizationServiceName() throws GenericConfigException {
        return getServiceEngine().getAuthorization().getServiceName();
    }

    public static boolean getPollEnabled() throws GenericConfigException {
        return getServiceEngine().getThreadPool().getPollEnabled();
    }

    public static String getSendPool() throws GenericConfigException {
        return getServiceEngine().getThreadPool().getSendToPool();
    }

    public static List<String> getRunPools() throws GenericConfigException {
        List<RunFromPool> runFromPools = getServiceEngine().getThreadPool().getRunFromPools();
        List<String> readPools = new ArrayList<String>(runFromPools.size());
        for (RunFromPool runFromPool : runFromPools) {
            readPools.add(runFromPool.getName());
        }
        return readPools;
    }

    public static int getPurgeJobDays() throws GenericConfigException {
        return getServiceEngine().getThreadPool().getPurgeJobDays();
    }

    public static int getFailedRetryMin() throws GenericConfigException {
        return getServiceEngine().getThreadPool().getFailedRetryMin();
    }

    public static NotificationGroup getNotificationGroup(String group) throws GenericConfigException {
        List<NotificationGroup> notificationGroups;
        notificationGroups = getServiceEngine().getNotificationGroups();
        for (NotificationGroup notificationGroup : notificationGroups) {
            if (notificationGroup.getName().equals(group)) {
                return notificationGroup;
            }
        }
        return null;
    }

    public static String getEngineParameter(String engineName, String parameterName) throws GenericConfigException {
        Engine engine = getServiceEngine().getEngine(engineName);
        if (engine != null) {
            return engine.getParameterValue(parameterName);
        }
        return null;
    }

    public static Element getXmlRootElement() throws GenericConfigException {
        Element root = ResourceLoader.getXmlRootElement(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME);
        return UtilXml.firstChildElement(root, "service-engine"); // only look at the first one for now
    }

    public static Element getElement(String elementName) {
        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
        }
        return  UtilXml.firstChildElement(rootElement, elementName);
    }

    public static String getElementAttr(String elementName, String attrName) {
        Element element = getElement(elementName);

        if (element == null) return null;
        return element.getAttribute(attrName);
    }
}
