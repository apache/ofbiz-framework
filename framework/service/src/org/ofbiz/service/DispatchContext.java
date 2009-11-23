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
package org.ofbiz.service;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.wsdl.WSDLException;

import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;
import org.ofbiz.security.Security;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Dispatcher Context
 */
public class DispatchContext implements Serializable {

    public static final String module = DispatchContext.class.getName();

    protected static final String GLOBAL_KEY = "global.services";
    public static UtilCache<String, Map<String, ModelService>> modelServiceMapByDispatcher = UtilCache.createUtilCache("service.ModelServiceMapByDispatcher", 0, 0, false);

    protected transient LocalDispatcher dispatcher;
    protected transient ClassLoader loader;
    protected Collection<URL> localReaders;
    protected Map<String, Object> attributes;
    protected String name;

    /**
     * Creates new DispatchContext
     * @param localReaders a collection of reader URLs
     * @param loader the classloader to use for dispatched services
     */
    public DispatchContext(String name, Collection<URL> localReaders, ClassLoader loader, LocalDispatcher dispatcher) {
        this.name = name;
        this.localReaders = localReaders;
        this.loader = loader;
        this.dispatcher = dispatcher;
        this.attributes = FastMap.newInstance();
    }

    public void loadReaders() {
        this.getLocalServiceMap();
        this.getGlobalServiceMap();
    }

    /**
     * Returns the service attribute for the given name, or null if there is no attribute by that name.
     * @param name a String specifying the name of the attribute
     * @return an Object containing the value of the attribute, or null if there is no attribute by that name.
     */
    public Object getAttribute(String name) {
        if (attributes.containsKey(name))
            return attributes.get(name);
        return null;
    }

    /**
     * Binds an object to a given attribute name in this context.
     * @param name a String specifying the name of the attribute
     * @param object an Object representing the attribute to be bound.
     */
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    /**
     * Gets the classloader of this context
     * @return ClassLoader of the context
     */
    public ClassLoader getClassLoader() {
        return this.loader;
    }

    /**
     * Gets the collection of readers associated with this context
     * @return Collection of reader URLs
     */
    public Collection<URL> getReaders() {
        return localReaders;
    }

    /**
     * Gets the name of the local dispatcher
     * @return String name of the LocalDispatcher object
     */
    public String getName() {
        return name;
    }

    /**
     * Uses an existing map of name value pairs and extracts the keys which are used in serviceName
     * Note: This goes not guarantee the context will be 100% valid, there may be missing fields
     * @param serviceName The name of the service to obtain parameters for
     * @param mode The mode to use for building the new map (i.e. can be IN or OUT)
     * @param context The initial set of values to pull from
     * @return Map contains any valid values
     * @throws GenericServiceException
     */
    public Map<String, Object> makeValidContext(String serviceName, String mode, Map<String, ? extends Object> context) throws GenericServiceException {
        ModelService model = this.getModelService(serviceName);
        return makeValidContext(model, mode, context);

    }

    /**
     * Uses an existing map of name value pairs and extracts the keys which are used in serviceName
     * Note: This goes not guarantee the context will be 100% valid, there may be missing fields
     * @param model The ModelService object of the service to obtain parameters for
     * @param mode The mode to use for building the new map (i.e. can be IN or OUT)
     * @param context The initial set of values to pull from
     * @return Map contains any valid values
     * @throws GenericServiceException
     */
    public Map<String, Object> makeValidContext(ModelService model, String mode, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> newContext;

        int modeInt = 0;
        if (mode.equalsIgnoreCase("in")) {
            modeInt = 1;
        } else if (mode.equalsIgnoreCase("out")) {
            modeInt = 2;
        }

        if (model == null) {
            throw new GenericServiceException("Model service is null! Should never happen.");
        } else {
            switch (modeInt) {
                case 2:
                    newContext = model.makeValid(context, ModelService.OUT_PARAM, true, null);
                    break;
                case 1:
                    newContext = model.makeValid(context, ModelService.IN_PARAM, true, null);
                    break;
                default:
                    throw new GenericServiceException("Invalid mode, should be either IN or OUT");
            }
            return newContext;
        }
    }

    /**
     * Gets the ModelService instance that corresponds to given the name
     * @param serviceName Name of the service
     * @return GenericServiceModel that corresponds to the serviceName
     */
    public ModelService getModelService(String serviceName) throws GenericServiceException {
        //long timeStart = System.currentTimeMillis();
        ModelService retVal = getLocalModelService(serviceName);
        if (retVal == null) {
            retVal = getGlobalModelService(serviceName);
        }

        if (retVal == null) {
            throw new GenericServiceException("Cannot locate service by name (" + serviceName + ")");
        }

        //Debug.logTiming("Got ModelService for name [" + serviceName + "] in [" + (System.currentTimeMillis() - timeStart) + "] milliseconds", module);
        return retVal;
    }

    private ModelService getLocalModelService(String serviceName) throws GenericServiceException {
        Map<String, ModelService> serviceMap = this.getLocalServiceMap();

        ModelService retVal = null;
        if (serviceMap != null) {
            retVal = serviceMap.get(serviceName);
            if (retVal != null && !retVal.inheritedParameters()) {
                retVal.interfaceUpdate(this);
            }
        }

        return retVal;
    }

    private ModelService getGlobalModelService(String serviceName) throws GenericServiceException {
        Map<String, ModelService> serviceMap = this.getGlobalServiceMap();

        ModelService retVal = null;
        if (serviceMap != null) {
            retVal = serviceMap.get(serviceName);
            if (retVal != null && !retVal.inheritedParameters()) {
                retVal.interfaceUpdate(this);
            }
        }

        return retVal;
    }

    /**
     * Gets the LocalDispatcher used with this context
     * @return LocalDispatcher that was used to create this context
     */
    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * Sets the LocalDispatcher used with this context
     * @param dispatcher The LocalDispatcher to re-assign to this context
     */
    public void setDispatcher(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Gets the Delegator associated with this context/dispatcher
     * @return Delegator associated with this context
     */
    public Delegator getDelegator() {
        return dispatcher.getDelegator();
    }

    /**
     * Gets the Authorization object associated with this dispatcher
     * @return Authorization object associated with this dispatcher
     */
    public Authorization getAuthorization() {
        return dispatcher.getAuthorization();
    }
    
    /**
     * Gets the Security object associated with this dispatcher
     * @return Security object associated with this dispatcher
     */
    public Security getSecurity() {
        return dispatcher.getSecurity();
    }

    private Map<String, ModelService> getLocalServiceMap() {
        Map<String, ModelService> serviceMap = modelServiceMapByDispatcher.get(name);
        if (serviceMap == null) {
            synchronized (this) {
                serviceMap = modelServiceMapByDispatcher.get(name);
                if (serviceMap == null) {
                    if (this.localReaders != null) {
                        serviceMap = FastMap.newInstance();
                        for (URL readerURL: this.localReaders) {
                            Map<String, ModelService> readerServiceMap = ModelServiceReader.getModelServiceMap(readerURL, this);
                            if (readerServiceMap != null) {
                                serviceMap.putAll(readerServiceMap);
                            }
                        }
                    }
                    if (serviceMap != null) {
                        modelServiceMapByDispatcher.put(name, serviceMap);
                        // NOTE: the current ECA per dispatcher for local services stuff is a bit broken, so now just doing this on the global def load: ServiceEcaUtil.reloadConfig();
                    }
                }
            }
        }

        return serviceMap;
    }

    private Map<String, ModelService> getGlobalServiceMap() {
        Map<String, ModelService> serviceMap = modelServiceMapByDispatcher.get(GLOBAL_KEY);
        if (serviceMap == null) {
            synchronized (this) {
                serviceMap = modelServiceMapByDispatcher.get(GLOBAL_KEY);
                if (serviceMap == null) {
                    serviceMap = FastMap.newInstance();

                    Element rootElement;

                    try {
                        rootElement = ServiceConfigUtil.getXmlRootElement();
                    } catch (GenericConfigException e) {
                        Debug.logError(e, "Error getting Service Engine XML root element", module);
                        return null;
                    }

                    for (Element globalServicesElement: UtilXml.childElementList(rootElement, "global-services")) {
                        ResourceHandler handler = new MainResourceHandler(
                                ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, globalServicesElement);

                        Map<String, ModelService> servicesMap = ModelServiceReader.getModelServiceMap(handler, this);
                        if (servicesMap != null) {
                            serviceMap.putAll(servicesMap);
                        }
                    }

                    // get all of the component resource model stuff, ie specified in each ofbiz-component.xml file
                    for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("model")) {
                        Map<String, ModelService> servicesMap = ModelServiceReader.getModelServiceMap(componentResourceInfo.createResourceHandler(), this);
                        if (servicesMap != null) {
                            serviceMap.putAll(servicesMap);
                        }
                    }

                    if (serviceMap != null) {
                        modelServiceMapByDispatcher.put(GLOBAL_KEY, serviceMap);
                        ServiceEcaUtil.reloadConfig();
                    }
                }
            }
        }

        return serviceMap;
    }

    public Set<String> getAllServiceNames() {
        Set<String> serviceNames = new TreeSet<String>();

        Map<String, ModelService> globalServices = modelServiceMapByDispatcher.get(GLOBAL_KEY);
        Map<String, ModelService> localServices = modelServiceMapByDispatcher.get(name);
        if (globalServices != null) {
            serviceNames.addAll(globalServices.keySet());
        }
        if (localServices != null) {
            serviceNames.addAll(localServices.keySet());
        }
        return serviceNames;
    }

    public Document getWSDL(String serviceName, String locationURI) throws GenericServiceException, WSDLException {
        ModelService model = this.getModelService(serviceName);
        return model.toWSDL(locationURI);
    }
}
