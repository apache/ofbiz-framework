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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.wsdl.WSDLException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Dispatcher Context
 */
@SuppressWarnings("serial")
public class DispatchContext implements Serializable {

    public static final String module = DispatchContext.class.getName();

    private static final UtilCache<String, Map<String, ModelService>> modelServiceMapByModel = UtilCache.createUtilCache("service.ModelServiceMapByModel", 0, 0, false);

    protected transient LocalDispatcher dispatcher;
    protected transient ClassLoader loader;
    protected Map<String, Object> attributes;
    protected String name;
    private String model;

    /**
     * Creates new DispatchContext
     */
    public DispatchContext(String name, ClassLoader loader) {
        this.name = name;
        this.model = name; // this will change when a dispatcher is set to match the model name associated to the delegator's dispatcher
        this.loader = loader;
        this.attributes = FastMap.newInstance();
    }

    public void loadReaders() {
        getGlobalServiceMap();
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
        ModelService model = getModelService(serviceName);
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
    public static Map<String, Object> makeValidContext(ModelService model, String mode, Map<String, ? extends Object> context) throws GenericServiceException {
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
        ModelService retVal = getGlobalModelService(serviceName);

        if (retVal == null) {
            throw new GenericServiceException("Cannot locate service by name (" + serviceName + ")");
        }
        //Debug.logTiming("Got ModelService for name [" + serviceName + "] in [" + (System.currentTimeMillis() - timeStart) + "] milliseconds", module);
        return retVal;
    }

    private ModelService getGlobalModelService(String serviceName) throws GenericServiceException {
        Map<String, ModelService> serviceMap = getGlobalServiceMap();

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
    public synchronized void setDispatcher(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        if (this.dispatcher != null) {
            Delegator delegator = dispatcher.getDelegator();
            if (delegator != null) {
                DelegatorInfo delegatorInfo = EntityConfigUtil.getDelegatorInfo(delegator.getDelegatorBaseName());
                if (delegatorInfo != null) {
                    this.model = delegatorInfo.entityModelReader;
                }
            }
        }
    }

    /**
     * Gets the Delegator associated with this context/dispatcher
     * @return Delegator associated with this context
     */
    public Delegator getDelegator() {
        return dispatcher.getDelegator();
    }

    /**
     * Gets the Security object associated with this dispatcher
     * @return Security object associated with this dispatcher
     */
    public Security getSecurity() {
        return dispatcher.getSecurity();
    }

    private Callable<Map<String, ModelService>> createServiceReaderCallable(final ResourceHandler handler) {
        return new Callable<Map<String, ModelService>>() {
            public Map<String, ModelService> call() throws Exception {
                return ModelServiceReader.getModelServiceMap(handler, DispatchContext.this);
            }
        };
    }

    private synchronized Map<String, ModelService> getGlobalServiceMap() {
        Map<String, ModelService> serviceMap = modelServiceMapByModel.get(this.model);
        if (serviceMap == null) {
            serviceMap = FastMap.newInstance();

            Element rootElement;

            try {
                rootElement = ServiceConfigUtil.getXmlRootElement();
            } catch (GenericConfigException e) {
                Debug.logError(e, "Error getting Service Engine XML root element", module);
                return null;
            }

            List<Future<Map<String, ModelService>>> futures = FastList.newInstance();
            for (Element globalServicesElement: UtilXml.childElementList(rootElement, "global-services")) {
                ResourceHandler handler = new MainResourceHandler(
                        ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, globalServicesElement);

                futures.add(ExecutionPool.GLOBAL_EXECUTOR.submit(createServiceReaderCallable(handler)));
            }

            // get all of the component resource model stuff, ie specified in each ofbiz-component.xml file
            for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("model")) {
                futures.add(ExecutionPool.GLOBAL_EXECUTOR.submit(createServiceReaderCallable(componentResourceInfo.createResourceHandler())));
            }
            for (Map<String, ModelService> servicesMap: ExecutionPool.getAllFutures(futures)) {
                if (servicesMap != null) {
                    serviceMap.putAll(servicesMap);
                }
            }

            if (serviceMap != null) {
                Map<String, ModelService> cachedServiceMap = modelServiceMapByModel.putIfAbsentAndGet(this.model, serviceMap);
                if (cachedServiceMap == serviceMap) { // same object: this means that the object created by this thread was actually added to the cache
                    ServiceEcaUtil.reloadConfig();
                }
            }
        }
        return serviceMap;
    }

    public synchronized Set<String> getAllServiceNames() {
        Set<String> serviceNames = new TreeSet<String>();

        Map<String, ModelService> globalServices = modelServiceMapByModel.get(this.model);
        if (globalServices != null) {
            serviceNames.addAll(globalServices.keySet());
        }
        return serviceNames;
    }

    public Document getWSDL(String serviceName, String locationURI) throws GenericServiceException, WSDLException {
        ModelService model = this.getModelService(serviceName);
        return model.toWSDL(locationURI);
    }
}
