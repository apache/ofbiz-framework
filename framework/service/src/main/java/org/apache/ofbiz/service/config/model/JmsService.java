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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;jms-service&gt;</code> element.
 */
@ThreadSafe
public final class JmsService extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "jms-service";
    private final String xPath;

    private final String name;
    private final String sendMode;
    private final List<Server> servers;

    JmsService(Element jmsServiceElement, String xPathParent) throws ServiceConfigException {
        String name = jmsServiceElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<jms-service> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/jms-service[@name='" + name + "']");
        sendMode = config.getValue(xPath.concat("/@send-mode"), "none", String.class);
        List<Server> serverList = config.getSubElementsAsListEntries(xPath, jmsServiceElement, Server.class);
        servers = Collections.unmodifiableList(serverList);
    }

    JmsService(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<jms-service> element name attribute is empty");
        }
        this.name = name;
        sendMode = config.getValue(configObject, "/@send-mode", "none", String.class);
        List<Server> serverList = config.getSubElementsAsListEntries(xPath, null, Server.class);
        servers = Collections.unmodifiableList(serverList);
    }

    public static JmsService loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new JmsService(element, xPathParent);
    }

    public static JmsService loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new JmsService(configMap, xPath);
    }

    public String getSendMode() {
        return sendMode;
    }

    public List<Server> getServers() {
        return servers;
    }

    @Override
    public String getName() {
        return name;
    }

}
