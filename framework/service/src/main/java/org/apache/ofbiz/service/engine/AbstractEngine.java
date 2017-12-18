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
package org.apache.ofbiz.service.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.GenericServiceCallback;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.ServiceLocation;

/**
 * Abstract Service Engine
 */
public abstract class AbstractEngine implements GenericEngine {

    public static final String module = AbstractEngine.class.getName();
    protected static final Map<String, String> locationMap = createLocationMap();

    protected ServiceDispatcher dispatcher = null;

    protected AbstractEngine(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // creates the location alias map
    protected static Map<String, String> createLocationMap() {
        Map<String, String> tmpMap = new HashMap<>();

        List<ServiceLocation> locationsList = null;
        try {
            locationsList = ServiceConfigUtil.getServiceEngine().getServiceLocations();
        } catch (GenericConfigException e) {
            // FIXME: Refactor API so exceptions can be thrown and caught.
            Debug.logError(e, module);
            throw new RuntimeException(e.getMessage());
        }
        for (ServiceLocation e: locationsList) {
            tmpMap.put(e.getName(), e.getLocation());
        }

        Debug.logInfo("Loaded Service Locations: " + tmpMap, module);
        return Collections.unmodifiableMap(tmpMap);
    }

    // uses the lookup map to determine if the location has been aliased by a service-location element in serviceengine.xml
    protected String getLocation(ModelService model) {
        if (locationMap.containsKey(model.location)) {
            return locationMap.get(model.location);
        }
        return model.location;
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#sendCallbacks(org.apache.ofbiz.service.ModelService, java.util.Map, int)
     */
    public void sendCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        if (!allowCallbacks(model, context, mode)) {
            return;
        }
        List<GenericServiceCallback> callbacks = dispatcher.getCallbacks(model.name);
        if (callbacks != null) {
            Iterator<GenericServiceCallback> i = callbacks.iterator();
            while (i.hasNext()) {
                GenericServiceCallback gsc = i.next();
                if (gsc.isEnabled()) {
                    gsc.receiveEvent(context);
                } else {
                    i.remove();
                }
            }
        }
    }

    public void sendCallbacks(ModelService model, Map<String, Object> context, Throwable t, int mode) throws GenericServiceException {
        if (!allowCallbacks(model, context, mode)) {
            return;
        }
        List<GenericServiceCallback> callbacks = dispatcher.getCallbacks(model.name);
        if (callbacks != null) {
            Iterator<GenericServiceCallback> i = callbacks.iterator();
            while (i.hasNext()) {
                GenericServiceCallback gsc = i.next();
                if (gsc.isEnabled()) {
                    gsc.receiveEvent(context,t);
                } else {
                    i.remove();
                }
            }
        }
    }

    public void sendCallbacks(ModelService model, Map<String, Object> context, Map<String, Object> result, int mode) throws GenericServiceException {
        if (!allowCallbacks(model, context, mode)) {
            return;
        }
        List<GenericServiceCallback> callbacks = dispatcher.getCallbacks(model.name);
        if (callbacks != null) {
            Iterator<GenericServiceCallback> i = callbacks.iterator();
            while (i.hasNext()) {
                GenericServiceCallback gsc = i.next();
                if (gsc.isEnabled()) {
                    gsc.receiveEvent(context, result);
                } else {
                    i.remove();
                }
            }
        }
    }

    protected boolean allowCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        return true;
    }
}
