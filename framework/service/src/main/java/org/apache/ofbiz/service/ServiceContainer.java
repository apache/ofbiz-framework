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
package org.apache.ofbiz.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.job.JobManager;

/**
 * A container for the service engine.
 */
public class ServiceContainer implements Container {
    private static final String module = ServiceContainer.class.getName();
    private static final ConcurrentHashMap<String, LocalDispatcher> dispatcherCache = new ConcurrentHashMap<>();
    private static LocalDispatcherFactory dispatcherFactory;

    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        // initialize the LocalDispatcherFactory
        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(name);
        ContainerConfig.Configuration.Property dispatcherFactoryProperty = cfg.getProperty("dispatcher-factory");
        if (dispatcherFactoryProperty == null || UtilValidate.isEmpty(dispatcherFactoryProperty.value)) {
            throw new ContainerException("Unable to initialize container " + name + ": dispatcher-factory property is not set");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> c = loader.loadClass(dispatcherFactoryProperty.value);
            dispatcherFactory = (LocalDispatcherFactory) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
        JobManager.shutDown();
        Set<String> dispatcherNames = getAllDispatcherNames();
        for (String dispatcherName: dispatcherNames) {
            deregister(dispatcherName);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public static LocalDispatcher getLocalDispatcher(String dispatcherName, Delegator delegator) {
        if (dispatcherName == null) {
            dispatcherName = delegator.getDelegatorName();
            Debug.logWarning("ServiceContainer.getLocalDispatcher method called with a null dispatcherName, defaulting to delegator name.", module);
        }
        if (UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) {
            dispatcherName = dispatcherName.concat("#").concat(delegator.getDelegatorTenantId());
        }
        LocalDispatcher dispatcher = dispatcherCache.get(dispatcherName);
        if (dispatcher == null) {
            dispatcher = dispatcherFactory.createLocalDispatcher(dispatcherName, delegator);
            dispatcherCache.putIfAbsent(dispatcherName, dispatcher);
            dispatcher = dispatcherCache.get(dispatcherName);
            Debug.logInfo("Created new dispatcher: " + dispatcherName, module);
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
        Debug.logInfo("Removing from cache dispatcher: " + dispatcherName, module);
        return dispatcherCache.remove(dispatcherName);
    }

    public static Set<String> getAllDispatcherNames() {
        return Collections.unmodifiableSet(dispatcherCache.keySet());
    }
}
