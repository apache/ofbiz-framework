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
package org.ofbiz.service.group;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.config.model.ServiceGroups;
import org.w3c.dom.Element;

/**
 * ServiceGroupReader.java
 */
public class ServiceGroupReader {

    public static final String module = ServiceGroupReader.class.getName();

    // using a cache is dangerous here because if someone clears it the groups won't work at all: public static UtilCache groupsCache = new UtilCache("service.ServiceGroups", 0, 0, false);
    public static Map<String, GroupModel> groupsCache = new ConcurrentHashMap<String, GroupModel>();

    public static void readConfig() {
        List<ServiceGroups> serviceGroupsList = null;
        try {
            serviceGroupsList = ServiceConfigUtil.getServiceEngine().getServiceGroups();
        } catch (GenericConfigException e) {
            // FIXME: Refactor API so exceptions can be thrown and caught.
            Debug.logError(e, module);
            throw new RuntimeException(e.getMessage());
        }
        for (ServiceGroups serviceGroup : serviceGroupsList) {
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.getServiceEngineXmlFileName(), serviceGroup.getLoader(), serviceGroup.getLocation());
            addGroupDefinitions(handler);
        }

        // get all of the component resource group stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("group")) {
            addGroupDefinitions(componentResourceInfo.createResourceHandler());
        }
    }

    public static void addGroupDefinitions(ResourceHandler handler) {
        Element rootElement = null;

        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }
        int numDefs = 0;

        for (Element group: UtilXml.childElementList(rootElement, "group")) {
            String groupName = group.getAttribute("name");
            if (groupName == null || groupName.isEmpty()) {
                Debug.logError("XML Parsing error: <group> element 'name' attribute null or empty", module);
                continue;
            }
            groupsCache.put(groupName, new GroupModel(group));
            numDefs++;
        }
        if (Debug.infoOn()) {
            String resourceLocation = handler.getLocation();
            try {
                resourceLocation = handler.getURL().toExternalForm();
            } catch (GenericConfigException e) {
                Debug.logError(e, "Could not get resource URL", module);
            }
            Debug.logInfo("Loaded [" + numDefs + "] Group definitions from " + resourceLocation, module);
        }
    }

    public static GroupModel getGroupModel(String serviceName) {
        if (groupsCache.size() == 0) {
            ServiceGroupReader.readConfig();
        }
        return groupsCache.get(serviceName);
    }
}
