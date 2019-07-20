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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ComponentLoaderConfig - Component Loader configuration utility class
 * to handle component-load.xml files
 *
 */
public final class ComponentLoaderConfig {

    public static final String module = ComponentLoaderConfig.class.getName();
    public static final String COMPONENT_LOAD_XML_FILENAME = "component-load.xml";

    public enum ComponentType { SINGLE_COMPONENT, COMPONENT_DIRECTORY }

    public static List<ComponentDef> getRootComponents() throws ComponentException {
        URL xmlUrl = UtilURL.fromResource(COMPONENT_LOAD_XML_FILENAME);
        return getComponentsFromConfig(xmlUrl);
    }

    public static List<ComponentDef> getComponentsFromConfig(URL configUrl) throws ComponentException {
        Document document = parseDocumentFromUrl(configUrl);
        List<? extends Element> toLoad = UtilXml.childElementList(document.getDocumentElement());
        List<ComponentDef> componentsFromConfig = new ArrayList<>();

        for (Element element : toLoad) {
            componentsFromConfig.add(retrieveComponentDefFromElement(element, configUrl));
        }
        return Collections.unmodifiableList(componentsFromConfig);
    }

    public static class ComponentDef {
        public String name;
        public final String location;
        public final ComponentType type;

        private ComponentDef(String name, String location, ComponentType type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }
    }

    private static Document parseDocumentFromUrl(URL configUrl) throws ComponentException {
        if (configUrl == null) {
            throw new ComponentException("configUrl cannot be null");
        }
        try {
            return UtilXml.readXmlDocument(configUrl, true);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        }
    }

    private static ComponentDef retrieveComponentDefFromElement(Element element, URL configUrl) throws ComponentException {
        Map<String, ? extends Object> systemProps = UtilGenerics.cast(System.getProperties());
        String nodeName = element.getNodeName();

        String name = null;
        String location = null;
        ComponentType type = null;

        if ("load-component".equals(nodeName)) {
            name = element.getAttribute("component-name");
            location = FlexibleStringExpander.expandString(element.getAttribute("component-location"), systemProps);
            type = ComponentType.SINGLE_COMPONENT;
        } else if ("load-components".equals(nodeName)) {
            location = FlexibleStringExpander.expandString(element.getAttribute("parent-directory"), systemProps);
            type = ComponentType.COMPONENT_DIRECTORY;
        } else {
            throw new ComponentException("Invalid element '" + nodeName + "' found in component-load file " + configUrl);
        }
        return new ComponentDef(name, location, type);
    }
}
