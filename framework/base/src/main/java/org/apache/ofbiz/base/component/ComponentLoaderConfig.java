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
package org.apache.ofbiz.base.component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ComponentLoaderConfig - Component Loader configuration
 *
 */
public class ComponentLoaderConfig {

    public static final String module = ComponentLoaderConfig.class.getName();
    public static final String COMPONENT_LOAD_XML_FILENAME = "component-load.xml";
    public static final int SINGLE_COMPONENT = 0;
    public static final int COMPONENT_DIRECTORY = 1;
    private static final AtomicReference<List<ComponentDef>> componentDefsRef = new AtomicReference<List<ComponentDef>>(null);

    public static List<ComponentDef> getRootComponents(String configFile) throws ComponentException {
        List<ComponentDef> existingInstance = componentDefsRef.get();
        if (existingInstance == null) {
            if (configFile == null) {
                configFile = COMPONENT_LOAD_XML_FILENAME;
            }
            URL xmlUrl = UtilURL.fromResource(configFile);
            List<ComponentDef> newInstance = getComponentsFromConfig(xmlUrl);
            if (componentDefsRef.compareAndSet(existingInstance, newInstance)) {
                existingInstance = newInstance;
            } else {
                existingInstance = componentDefsRef.get();
            }
        }
        return existingInstance;
    }

    public static List<ComponentDef> getComponentsFromConfig(URL configUrl) throws ComponentException {
        if (configUrl == null) {
            throw new IllegalArgumentException("configUrl cannot be null");
        }
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(configUrl, true);
        } catch (SAXException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        } catch (IOException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        }
        Element root = document.getDocumentElement();
        List<? extends Element> toLoad = UtilXml.childElementList(root);
        List<ComponentDef> componentsFromConfig = null;
        if (!toLoad.isEmpty()) {
            componentsFromConfig = new ArrayList<ComponentDef>(toLoad.size());
            Map<String, ? extends Object> systemProps = UtilGenerics.<String, Object> checkMap(System.getProperties());
            for (Element element : toLoad) {
                String nodeName = element.getNodeName();
                String name = null;
                String location = null;
                int type = SINGLE_COMPONENT;
                if ("load-component".equals(nodeName)) {
                    name = element.getAttribute("component-name");
                    location = FlexibleStringExpander.expandString(element.getAttribute("component-location"), systemProps);
                } else if ("load-components".equals(nodeName)) {
                    location = FlexibleStringExpander.expandString(element.getAttribute("parent-directory"), systemProps);
                    type = COMPONENT_DIRECTORY;
                } else {
                    throw new ComponentException("Invalid element '" + nodeName + "' found in component config file " + configUrl);
                }
                componentsFromConfig.add(new ComponentDef(name, location, type));
            }
        }
        return Collections.unmodifiableList(componentsFromConfig);
    }

    public static class ComponentDef {
        public String name;
        public final String location;
        public final int type;

        private ComponentDef(String name, String location, int type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }
    }
}
