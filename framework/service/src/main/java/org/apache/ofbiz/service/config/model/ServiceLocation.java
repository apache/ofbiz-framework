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
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-location&gt;</code> element.
 */
@ThreadSafe
public final class ServiceLocation extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "service-location";
    private final String xPath;

    private final String location;
    private final String name;

    ServiceLocation(Element serviceLocationElement, String xPathParent) throws ServiceConfigException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        String name = serviceLocationElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<service-location> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/service-location[@name='" + name + "']");
        String location = config.getValue(xPath.concat("/@location"));
        if (location.isEmpty() && checkStructure) {
            throw new ServiceConfigException("<service-location> element location attribute is empty");
        }
        if (location.contains("localhost") && Start.getInstance().getConfig().getPortOffset() != 0) {
            String s = location.substring(location.lastIndexOf(":") + 1);
            int locationPort = Integer.parseInt(s.substring(0, s.indexOf("/")));
            int port = locationPort + Start.getInstance().getConfig().getPortOffset();
            location = location.replace(Integer.toString(locationPort), Integer.toString(port));
        }
        this.location = location;
    }

    ServiceLocation(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<service-location> element name attribute is empty");
        }
        this.name = name;
        String location = config.getValue(configObject, "/@location");
        if (location.isEmpty()) {
            throw new ServiceConfigException("<service-location> element location attribute is empty");
        }
        if (location.contains("localhost") && Start.getInstance().getConfig().getPortOffset() != 0) {
            String s = location.substring(location.lastIndexOf(":") + 1);
            int locationPort = Integer.parseInt(s.substring(0, s.indexOf("/")));
            int port = locationPort + Start.getInstance().getConfig().getPortOffset();
            location = location.replace(Integer.toString(locationPort), Integer.toString(port));
        }
        this.location = location;
    }

    public static ServiceLocation loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new ServiceLocation(element, xPathParent);
    }

    public static ServiceLocation loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new ServiceLocation(configMap, xPath);
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }
}
