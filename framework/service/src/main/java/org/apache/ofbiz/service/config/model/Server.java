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
 * An object that models the <code>&lt;server&gt;</code> element.
 */
@ThreadSafe
public final class Server extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "server";
    private final String xPath;

    private final String clientId;
    private final String jndiName;
    private final String jndiServerName;
    private final boolean listen;
    private final String listenerClass;
    private final String password;
    private final String topicQueue;
    private final String type;
    private final String username;

    Server(Element serverElement, String xPathParent) throws ServiceConfigException {
        String jndiServerName = serverElement.getAttribute("jndi-server-name").intern();
        if (jndiServerName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-server-name attribute is empty");
        }
        this.jndiServerName = jndiServerName;
        xPath = xPathParent.concat("/server[@jndi-server-name='" + jndiServerName + "']");
        String jndiName = config.getValue(xPath.concat("/@jndi-name"));
        if (jndiName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-name attribute is empty");
        }
        this.jndiName = jndiName;
        String topicQueue = config.getValue(xPath.concat("/@topic-queue"));
        if (topicQueue.isEmpty()) {
            throw new ServiceConfigException("<server> element topic-queue attribute is empty");
        }
        this.topicQueue = topicQueue;
        String type = config.getValue(xPath.concat("/@type"));
        if (type.isEmpty()) {
            throw new ServiceConfigException("<server> element type attribute is empty");
        }
        this.type = type;
        this.username = config.getValue(xPath.concat("/@username"));
        this.password = config.getValue(xPath.concat("/@password"));
        this.clientId = config.getValue(xPath.concat("/@client-id"));
        this.listen = "true".equals(config.getValue(xPath.concat("/@listen")));
        this.listenerClass = config.getValue(xPath.concat("/@listener-class"));
    }

    Server(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String jndiServerName = getNameFromXPath(xPath);
        if (jndiServerName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-server-name attribute is empty");
        }
        this.jndiServerName = jndiServerName;
        String jndiName = config.getValue(configObject, "/@jndi-name");
        if (jndiName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-name attribute is empty");
        }
        this.jndiName = jndiName;
        String topicQueue = config.getValue(configObject, "/@topic-queue");
        if (topicQueue.isEmpty()) {
            throw new ServiceConfigException("<server> element topic-queue attribute is empty");
        }
        this.topicQueue = topicQueue;
        String type = config.getValue(configObject, "/@type");
        if (type.isEmpty()) {
            throw new ServiceConfigException("<server> element type attribute is empty");
        }
        this.type = type;
        this.username = config.getValue(configObject, "/@username");
        this.password = config.getValue(configObject, "/@password");
        this.clientId = config.getValue(configObject, "/@client-id");
        this.listen = "true".equals(config.getValue(configObject, "/@listen"));
        this.listenerClass = config.getValue(configObject, "/@listener-class");
    }

    public static Server loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new Server(element, xPathParent);
    }

    public static Server loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new Server(configMap, xPath);
    }

    public String getClientId() {
        return clientId;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getJndiServerName() {
        return jndiServerName;
    }

    public boolean getListen() {
        return listen;
    }

    public String getListenerClass() {
        return listenerClass;
    }

    public String getPassword() {
        return password;
    }

    public String getTopicQueue() {
        return topicQueue;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String getName() {
        return jndiServerName;
    }
}
