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
package org.ofbiz.service.engine;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import javolution.util.FastMap;

import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.GenericServiceCallback;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;

import org.w3c.dom.Element;

/**
 * Abstract Service Engine
 */
public abstract class AbstractEngine implements GenericEngine {

    public static final String module = AbstractEngine.class.getName();
    protected static Map<String, String> locationMap = null;

    protected ServiceDispatcher dispatcher = null;

    protected AbstractEngine(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        initLocations();
    }

    // creates the location alias map
    protected synchronized void initLocations() {
        if (locationMap == null) {
            locationMap = FastMap.newInstance();

            Element root = null;
            try {
                root = ServiceConfigUtil.getXmlRootElement();
            } catch (GenericConfigException e) {
                Debug.logError(e, module);
            }

            if (root != null) {
                List<? extends Element> locationElements = UtilXml.childElementList(root, "service-location");
                if (locationElements != null) {
                    for (Element e: locationElements) {
                        locationMap.put(e.getAttribute("name"), e.getAttribute("location"));
                    }
                }
            }
            Debug.logInfo("Loaded Service Locations : " + locationMap, module);
        }
    }

    // uses the lookup map to determin if the location has been aliased in serviceconfig.xml
    protected String getLocation(ModelService model) {
        if (locationMap.containsKey(model.location)) {
            return locationMap.get(model.location);
        } else {
            return model.location;
        }
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#sendCallbacks(org.ofbiz.service.ModelService, java.util.Map, java.lang.Object, int)
     */
    public void sendCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        if (!allowCallbacks(model, context, mode)) return;
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
        if (!allowCallbacks(model, context, mode)) return;
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
        if (!allowCallbacks(model, context, mode)) return;
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
