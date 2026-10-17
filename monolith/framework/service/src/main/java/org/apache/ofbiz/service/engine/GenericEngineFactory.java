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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.config.ServiceConfigUtil;

/**
 * Generic Engine Factory
 */
public class GenericEngineFactory {

    private ServiceDispatcher dispatcher = null;
    private Map<String, GenericEngine> engines = null;

    public GenericEngineFactory(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        engines = new HashMap<>();
    }

    /**
     * Gets the GenericEngine instance that corresponds to given the name
     *@param engineName Name of the engine
     *@return GenericEngine that corresponds to the engineName
     */
    public GenericEngine getGenericEngine(String engineName) throws GenericServiceException {
        String className = null;
        try {
            className = ServiceConfigUtil.getServiceEngine().getEngine(engineName).getClassName();
        } catch (GenericConfigException e) {
            throw new GenericServiceException(e);
        }

        GenericEngine engine = engines.get(engineName);
        if (engine == null) {
            synchronized (GenericEngineFactory.class) {
                engine = engines.get(engineName);
                if (engine == null) {
                    try {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        Class<?> c = loader.loadClass(className);
                        Constructor<GenericEngine> cn = UtilGenerics.cast(c.getConstructor(ServiceDispatcher.class));
                        engine = cn.newInstance(dispatcher);
                    } catch (Exception e) {
                        throw new GenericServiceException(e.getMessage(), e);
                    }
                    if (engine != null) {
                        engines.put(engineName, engine);
                    }
                }
            }
        }

        return engine;
    }
}

