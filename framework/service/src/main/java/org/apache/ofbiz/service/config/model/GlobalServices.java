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

import java.util.Map;
import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;global-services&gt;</code> element.
 */
@ThreadSafe
public final class GlobalServices extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "global-services";
    private final String xPath;

    private final String loader;
    private final String location;

    GlobalServices(Element globalServicesElement, String xPathParent) throws ServiceConfigException {
        String location = globalServicesElement.getAttribute("location").intern();
        if (location.isEmpty()) {
            throw new ServiceConfigException("<global-services> element location attribute is empty");
        }
        this.location = location;
        xPath = xPathParent.concat("/global-services[@location='" + location + "']");
        String loader = config.getValue(xPath.concat("/@loader"));
        if (loader.isEmpty()) {
            throw new ServiceConfigException("<global-services> element loader attribute is empty");
        }
        this.loader = loader;
    }

    GlobalServices(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String location = getNameFromXPath(xPath);
        if (location.isEmpty()) {
            throw new ServiceConfigException("<global-services> element location attribute is empty");
        }
        this.location = location;
        String loader = config.getValue(configObject, "/@loader");
        if (loader.isEmpty()) {
            throw new ServiceConfigException("<global-services> element loader attribute is empty");
        }
        this.loader = loader;
    }

    public static GlobalServices loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new GlobalServices(element, xPathParent);
    }

    public static GlobalServices loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new GlobalServices(configMap, xPath);
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return location;
    }
}
