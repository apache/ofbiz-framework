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
package org.apache.ofbiz.service.config.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-config&gt;</code> element.
 */
@ThreadSafe
public final class ServiceConfig {

    public static ServiceConfig create(Element serviceConfigElement) throws ServiceConfigException {
        Map<String, ServiceEngine> serviceEngineMap = new HashMap<String, ServiceEngine>();
        List<? extends Element> engineElementList = UtilXml.childElementList(serviceConfigElement, "service-engine");
        for (Element engineElement : engineElementList) {
            ServiceEngine engineModel = new ServiceEngine(engineElement);
            serviceEngineMap.put(engineModel.getName(), engineModel);
        }
        return new ServiceConfig(serviceEngineMap);
    }

    private final Map<String, ServiceEngine> serviceEngineMap;

    private ServiceConfig(Map<String, ServiceEngine> serviceEngineMap) {
        this.serviceEngineMap = Collections.unmodifiableMap(serviceEngineMap);
    }

    public Collection<ServiceEngine> getServiceEngines() {
        return this.serviceEngineMap.values();
    }

    public ServiceEngine getServiceEngine(String name) {
        return this.serviceEngineMap.get(name);
    }


}
