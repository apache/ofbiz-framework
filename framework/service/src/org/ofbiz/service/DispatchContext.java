/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.service;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.security.Security;
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
    protected static UtilCache modelService = new UtilCache("service.ModelServices", 0, 0, false);

    protected transient LocalDispatcher dispatcher;
    protected transient ClassLoader loader;
    protected Collection readers;
    protected Map attributes;
    protected String name;

    /** 
     * Creates new DispatchContext
     * @param readers a collection of reader URLs
     * @param loader the classloader to use for dispatched services
     */
    public DispatchContext(String name, Collection readers, ClassLoader loader, LocalDispatcher dispatcher) {
        this.name = name;
        this.readers = readers;
        this.loader = loader;
        this.dispatcher = dispatcher;
        this.attributes = FastMap.newInstance();                
    }

    public void loadReaders() {
        Map localService = addReaders(readers);
        if (localService != null) {
            modelService.put(name, localService);
        }
        
        Map globalService = addGlobal();
        if (globalService != null) {
            modelService.put(GLOBAL_KEY, globalService);
        }
    }

    /** 
     * Returns the service attribute for the given name, or null if there is no attribute by that name.
     * @param name a String specifying the name of the attribute
     * @return an Object conatining the value of the attribute, or null if there is no attribute by that name.
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
    public Collection getReaders() {
        return readers;
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
    public Map makeValidContext(String serviceName, String mode, Map context) throws GenericServiceException {        
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
    public Map makeValidContext(ModelService model, String mode, Map context) throws GenericServiceException {
        Map newContext = null;
        
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
        ModelService retVal = getLocalModelService(serviceName);
        if (retVal == null) {
            retVal = getGlobalModelService(serviceName);
        }
        
        if (retVal == null) {
            throw new GenericServiceException("Cannot locate service by name (" + serviceName + ")");
        }
        
        return retVal;                
    }
    
    private ModelService getLocalModelService(String serviceName) throws GenericServiceException {
        Map serviceMap = (Map) modelService.get(name);
        if (serviceMap == null) {
            synchronized (this) {
                serviceMap = (Map) modelService.get(name);
                if (serviceMap == null) {
                    serviceMap = addReaders(readers);
                    if (serviceMap != null) {
                        modelService.put(name, serviceMap);
                        ServiceEcaUtil.reloadConfig();
                    }
                }
            }
        }
        
        ModelService retVal = null;
        if (serviceMap != null) {
            retVal = (ModelService) serviceMap.get(serviceName); 
            if (retVal != null && !retVal.inheritedParameters()) {
                retVal.interfaceUpdate(this);     
            }
        }
        
        return retVal;
    }

    private ModelService getGlobalModelService(String serviceName) throws GenericServiceException {
        Map serviceMap = (Map) modelService.get(GLOBAL_KEY);
        if (serviceMap == null) {
            synchronized (this) {
                serviceMap = (Map) modelService.get(GLOBAL_KEY);
                if (serviceMap == null) {
                    serviceMap = addGlobal();
                    if (serviceMap != null) {
                        modelService.put(GLOBAL_KEY, serviceMap);
                        ServiceEcaUtil.reloadConfig();
                    }
                }
            }
        }

        ModelService retVal = null;
        if (serviceMap != null) {
            retVal = (ModelService) serviceMap.get(serviceName);
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
     * Gets the GenericDelegator associated with this context/dispatcher
     * @return GenericDelegator associated with this context
     */
    public GenericDelegator getDelegator() {
        return dispatcher.getDelegator();
    }

    /** 
     * Gets the Security object associated with this dispatcher
     * @return Security object associated with this dispatcher
     */
    public Security getSecurity() {
        return dispatcher.getSecurity();
    }

    private Map addReaders(Collection readerURLs) {
        Map serviceMap = FastMap.newInstance();

        if (readerURLs == null)
            return null;
        Iterator urlIter = readerURLs.iterator();

        while (urlIter.hasNext()) {
            URL readerURL = (URL) urlIter.next();

            serviceMap.putAll(addReader(readerURL));
        }
        return serviceMap;
    }

    private Map addReader(URL readerURL) {
        if (readerURL == null) {
            Debug.logError("Cannot add reader with a null reader URL", module);
            return null;
        }

        ModelServiceReader reader = ModelServiceReader.getModelServiceReader(readerURL, this);

        if (reader == null) {
            Debug.logError("Could not load the reader for the reader URL " + readerURL, module);
            return null;
        }

        Map serviceMap = reader.getModelServices();

        return serviceMap;
    }

    private Map addReader(ResourceHandler handler) {
        ModelServiceReader reader = ModelServiceReader.getModelServiceReader(handler, this);

        if (reader == null) {
            Debug.logError("Could not load the reader for " + handler, module);
            return null;
        }

        Map serviceMap = reader.getModelServices();

        return serviceMap;
    }

    private Map addGlobal() {
        Map globalMap = FastMap.newInstance();

        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
            return null;
        }

        List globalServicesElements = UtilXml.childElementList(rootElement, "global-services");
        Iterator gseIter = globalServicesElements.iterator();
        while (gseIter.hasNext()) {
            Element globalServicesElement = (Element) gseIter.next();
            ResourceHandler handler = new MainResourceHandler(
                    ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, globalServicesElement);

            Map servicesMap = addReader(handler);
            if (servicesMap != null) {
                globalMap.putAll(servicesMap);
            }
        }
        
        // get all of the component resource model stuff, ie specified in each ofbiz-component.xml file
        List componentResourceInfos = ComponentConfig.getAllServiceResourceInfos("model");
        Iterator componentResourceInfoIter = componentResourceInfos.iterator();
        while (componentResourceInfoIter.hasNext()) {
            ComponentConfig.ServiceResourceInfo componentResourceInfo = (ComponentConfig.ServiceResourceInfo) componentResourceInfoIter.next();
            Map servicesMap = addReader(componentResourceInfo.createResourceHandler());
            if (servicesMap != null) {
                globalMap.putAll(servicesMap);
            }
        }

        return globalMap;
    }

    public Set getAllServiceNames() {
        Set serviceNames = new TreeSet();

        Map globalServices = (Map) modelService.get(GLOBAL_KEY);
        Map localServices = (Map) modelService.get(name);
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
