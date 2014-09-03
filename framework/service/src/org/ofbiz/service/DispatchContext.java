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
import java.util.Map;
import java.util.Set;

import javax.wsdl.WSDLException;

import org.ofbiz.entity.Delegator;
import org.ofbiz.security.Security;
import org.w3c.dom.Document;

/**
 * Service dispatch context. A collection of objects and convenience methods
 * to be used by service implementations.
 */
@SuppressWarnings("serial")
public final class DispatchContext implements Serializable {

    public static final String module = DispatchContext.class.getName();

    private final LocalDispatcher dispatcher;

    DispatchContext(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Gets the classloader of this context
     * @return ClassLoader of the context
     */
    public ClassLoader getClassLoader() {
        return dispatcher.getClassLoader();
    }

    /**
     * Gets the name of the local dispatcher
     * @return String name of the LocalDispatcher object
     */
    public String getName() {
        return dispatcher.getName();
    }

    /**
     * Gets the LocalDispatcher used with this context
     * @return LocalDispatcher that was used to create this context
     */
    public LocalDispatcher getDispatcher() {
        return dispatcher;
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

    // All the methods that follow are helper methods to retrieve service model information from cache (and manage the cache)
    // The cache object is static but most of these methods are not because the same service definition, is used with different
    // DispatchContext objects may result in different in/out attributes: this happens because the DispatchContext is associated to
    // a LocalDispatcher that is associated to a Delegator that is associated to a ModelReader; different ModelReaders could load the
    // same entity name from different files with different fields, and the service definition could automatically get the input/output
    // attributes from an entity.

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
     * Gets the ModelService instance that corresponds to the given name
     * @param serviceName Name of the service
     * @return GenericServiceModel that corresponds to the serviceName
     */
    public ModelService getModelService(String serviceName) throws GenericServiceException {
        return dispatcher.getModelService(serviceName);
    }

    public Set<String> getAllServiceNames() {
        return dispatcher.getAllServiceNames();
    }

    public Document getWSDL(String serviceName, String locationURI) throws GenericServiceException, WSDLException {
        ModelService model = this.getModelService(serviceName);
        return model.toWSDL(locationURI);
    }
}
