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
package org.apache.ofbiz.service.config;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.service.config.model.Engine;
import org.apache.ofbiz.service.config.model.ServiceConfig;
import org.apache.ofbiz.service.config.model.ServiceEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A <code>ServiceConfig</code> factory and related utility methods.
 * <p>The <code>ServiceConfig</code> instance models the <code>serviceengine.xml</code> file
 * and the instance is kept in the "service.ServiceConfig" cache. Clearing the cache will reload
 * the service configuration file. Client code that depends on the <code>serviceengine.xml</code>
 * file can be notified when the file is reloaded by implementing <code>ServiceConfigListener</code>
 * and registering itself using the {@link #registerServiceConfigListener(ServiceConfigListener)}
 * method.<p>
 */
public final class ServiceConfigUtil {

    private static final String MODULE = ServiceConfigUtil.class.getName();
    private static final String ENGINE = "default";
    private static final String SERVICE_ENGINE_XML_FILENAME = "serviceengine.xml";
    // Keep the ServiceConfig instance in a cache - so the configuration can be reloaded at run-time.
    // There will be only one ServiceConfig instance in the cache.
    private static final UtilCache<String, ServiceConfig> SERVICE_CONFIG_CACHE = UtilCache.createUtilCache("service.ServiceConfig", 0, 0, false);
    private static final List<ServiceConfigListener> CONFIG_LISTENERS = new CopyOnWriteArrayList<>();

    private ServiceConfigUtil() { }

    /**
     * Returns the specified parameter value from the specified engine, or <code>null</code>
     * if the engine or parameter are not found.
     * @param engineName
     * @param parameterName
     * @return
     * @throws GenericConfigException
     */
    public static String getEngineParameter(String engineName, String parameterName) throws GenericConfigException {
        Engine engine = getServiceEngine().getEngine(engineName);
        if (engine != null) {
            return engine.getParameterValue(parameterName);
        }
        return null;
    }

    /**
     * Returns the <code>ServiceConfig</code> instance.
     * @throws GenericConfigException
     */
    public static ServiceConfig getServiceConfig() throws GenericConfigException {
        ServiceConfig instance = SERVICE_CONFIG_CACHE.get("instance");
        if (instance == null) {
            Element serviceConfigElement = getXmlDocument().getDocumentElement();
            instance = ServiceConfig.create(serviceConfigElement);
            SERVICE_CONFIG_CACHE.putIfAbsent("instance", instance);
            instance = SERVICE_CONFIG_CACHE.get("instance");
            for (ServiceConfigListener listener : CONFIG_LISTENERS) {
                try {
                    listener.onServiceConfigChange(instance);
                } catch (Exception e) {
                    Debug.logError(e, "Exception thrown while notifying listener " + listener + ": ", MODULE);
                }
            }
        }
        return instance;
    }

    /**
     * Returns the default service engine configuration (named "default").
     * @throws GenericConfigException
     */
    public static ServiceEngine getServiceEngine() throws GenericConfigException {
        return getServiceConfig().getServiceEngine(ENGINE);
    }

    /**
     * Returns the specified <code>ServiceEngine</code> configuration instance,
     * or <code>null</code> if the configuration does not exist.
     * @throws GenericConfigException
     */
    public static ServiceEngine getServiceEngine(String name) throws GenericConfigException {
        return getServiceConfig().getServiceEngine(name);
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
     * Register a <code>ServiceConfigListener</code> instance. The instance will be notified
     * when the <code>serviceengine.xml</code> file is reloaded.
     * @param listener
     */
    public static void registerServiceConfigListener(ServiceConfigListener listener) {
        Assert.notNull("listener", listener);
        CONFIG_LISTENERS.add(listener);
    }

    public static String getEngine() {
        return ENGINE;
    }

    public static String getServiceEngineXmlFileName() {
        return SERVICE_ENGINE_XML_FILENAME;
    }
}
