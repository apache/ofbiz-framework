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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;jms-service&gt;</code> element.
 */
@ThreadSafe
public final class JmsService {

    private final String name;
    private final String sendMode;
    private final List<Server> servers;

    JmsService(Element jmsServiceElement) throws ServiceConfigException {
        String name = jmsServiceElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<jms-service> element name attribute is empty");
        }
        this.name = name;
        String sendMode = jmsServiceElement.getAttribute("send-mode").intern();
        if (sendMode.isEmpty()) {
            sendMode = "none";
        }
        this.sendMode = sendMode;
        List<? extends Element> serverElementList = UtilXml.childElementList(jmsServiceElement, "server");
        if (serverElementList.isEmpty()) {
            this.servers = Collections.emptyList();
        } else {
            List<Server> servers = new ArrayList<>(serverElementList.size());
            for (Element serverElement : serverElementList) {
                servers.add(new Server(serverElement));
            }
            this.servers = Collections.unmodifiableList(servers);
        }
    }

    public String getName() {
        return name;
    }

    public String getSendMode() {
        return this.sendMode;
    }

    public List<Server> getServers() {
        return this.servers;
    }
}
