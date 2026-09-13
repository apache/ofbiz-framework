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

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * An object that models the <code>&lt;startup-service&gt;</code> element.
 */
@ThreadSafe
public final class StartupService extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "startup-service";
    private final String xPath;

    private final String name;
    private final String runInPool;
    private final String runtimeDataId;
    private final int runtimeDelay;

    StartupService(Element startupServiceElement, String xPathParent) throws ServiceConfigException {
        String name = startupServiceElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<startup-service> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/startup-service[@name='" + name + "']");
        runtimeDataId = config.getValue(xPath.concat("/@runtime-data-id"), null, String.class);
        runtimeDelay = config.getValue(xPath.concat("/@runtime-delay"), 0, Integer.class);
        this.runInPool = config.getValue(xPath.concat("/@run-in-pool"));
    }

    StartupService(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<startup-service> element name attribute is empty");
        }
        this.name = name;
        runtimeDataId = config.getValue(configObject, "/@runtime-data-id", null, String.class);
        runtimeDelay = config.getValue(configObject, "/@runtime-delay", 0, Integer.class);
        this.runInPool = config.getValue(configObject, "/@run-in-pool");
    }

    public static StartupService loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new StartupService(element, xPathParent);
    }

    public static StartupService loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new StartupService(configMap, xPath);
    }

    public String getRunInPool() {
        return runInPool;
    }

    public String getRuntimeDataId() {
        return runtimeDataId;
    }

    public int getRuntimeDelay() {
        return runtimeDelay;
    }

    @Override
    public String getName() {
        return name;
    }

}
