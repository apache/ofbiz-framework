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

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;server&gt;</code> element.
 */
@ThreadSafe
public final class Server {

    private final String clientId;
    private final String jndiName;
    private final String jndiServerName;
    private final boolean listen;
    private final String listenerClass;
    private final String password;
    private final String topicQueue;
    private final String type;
    private final String username;

    Server(Element serverElement) throws ServiceConfigException {
        String jndiServerName = serverElement.getAttribute("jndi-server-name").intern();
        if (jndiServerName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-server-name attribute is empty");
        }
        this.jndiServerName = jndiServerName;
        String jndiName = serverElement.getAttribute("jndi-name").intern();
        if (jndiName.isEmpty()) {
            throw new ServiceConfigException("<server> element jndi-name attribute is empty");
        }
        this.jndiName = jndiName;
        String topicQueue = serverElement.getAttribute("topic-queue").intern();
        if (topicQueue.isEmpty()) {
            throw new ServiceConfigException("<server> element topic-queue attribute is empty");
        }
        this.topicQueue = topicQueue;
        String type = serverElement.getAttribute("type").intern();
        if (type.isEmpty()) {
            throw new ServiceConfigException("<server> element type attribute is empty");
        }
        this.type = type;
        this.username = serverElement.getAttribute("username").intern();
        this.password = serverElement.getAttribute("password").intern();
        this.clientId = serverElement.getAttribute("client-id").intern();
        this.listen = "true".equals(serverElement.getAttribute("listen"));
        this.listenerClass = serverElement.getAttribute("listener-class").intern();
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

}
