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

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceContainer implements Container {
    private static final String module = ServiceContainer.class.getName();
    private static ConcurrentHashMap<String, LocalDispatcher> dispatcherCache = new ConcurrentHashMap<String, LocalDispatcher>();
    private static LocalDispatcherFactory dispatcherFactory;

    private String name;

    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        // initialize the LocalDispatcherFactory
        ContainerConfig.Container cfg = ContainerConfig.getContainer(name, configFile);
        ContainerConfig.Container.Property dispatcherFactoryProperty = cfg.getProperty("dispatcher-factory");

        if (dispatcherFactoryProperty == null || UtilValidate.isEmpty(dispatcherFactoryProperty.value)) {
            throw new ContainerException("Unable to initialize container " + name + ": dispatcher-factory property is not set");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> c = loader.loadClass(dispatcherFactoryProperty.value);
            dispatcherFactory = (LocalDispatcherFactory) c.newInstance();
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    public boolean start() throws ContainerException {
        return true;
    }

    public void stop() throws ContainerException {
        Set<String> dispatcherNames = getAllDispatcherNames();
        for (String dispatcherName: dispatcherNames) {
            ServiceContainer.deregister(dispatcherName);
        }
    }

    public String getName() {
        return name;
    }

    public static LocalDispatcher getLocalDispatcher(String dispatcherName, Delegator delegator) {
        if (dispatcherName == null) {
            dispatcherName = delegator.getDelegatorName();
            Debug.logWarning("Got a getLocalDispatcher call with a null dispatcherName, assuming default for the name.", module);
        }
        if (UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) {
            dispatcherName += "#" + delegator.getDelegatorTenantId();
        }
        LocalDispatcher dispatcher = dispatcherCache.get(dispatcherName);
        if (dispatcher == null) {
            if (Debug.infoOn()) Debug.logInfo("Creating new dispatcher [" + dispatcherName + "] (" + Thread.currentThread().getName() + ")", module);
            dispatcher = dispatcherFactory.createLocalDispatcher(dispatcherName, delegator);
            dispatcherCache.putIfAbsent(dispatcherName, dispatcher);
        }
        return dispatcher;
    }

    public static void deregister(String dispatcherName) {
        LocalDispatcher dispatcher = dispatcherCache.get(dispatcherName);
        if (dispatcher != null) {
            dispatcher.deregister();
        }
    }

    public static LocalDispatcher removeFromCache(String dispatcherName) {
        if (Debug.infoOn()) Debug.logInfo("Removing from cache dispatcher: " + dispatcherName, module);
        return dispatcherCache.remove(dispatcherName);
    }

    public static Set<String> getAllDispatcherNames() {
        return dispatcherCache.keySet();
    }
}
