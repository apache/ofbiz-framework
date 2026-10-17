/*
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
 */
package org.apache.ofbiz.base.component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * This class is a MODULE for manipulating component loader files.
 *
 * <p> The component loader files are named {@code component-load.xml}
 * and are present either in the classpath when defining the top-level component
 * directories or inside those component directories when defining the loading order
 * of the enabled simple components inside those directories.
 *
 * Simple components are directories containing a component configuration file
 * named {@code ofbiz-component.xml} and mapped to a {@link ComponentConfig} object.
 *
 * @see ComponentConfig
 */
public final class ComponentLoaderConfig {

    private static final String MODULE = ComponentLoaderConfig.class.getName();
    public static final String COMPONENT_LOAD_XML_FILENAME = "component-load.xml";

    public enum ComponentType { SINGLE_COMPONENT, COMPONENT_DIRECTORY }

    private ComponentLoaderConfig() { }

    /**
     * Provides the list of root directory components defined in the classpath.
     * @return the list of root directory components.
     * @throws ComponentException if the main {@code component-load.xml} file is either invalid
     *         or refer to non-existent component directory.
     */
    public static List<ComponentDef> getRootComponents() throws ComponentException {
        URL xmlUrl = UtilURL.fromResource(COMPONENT_LOAD_XML_FILENAME);
        return getComponentsFromConfig(xmlUrl);
    }

    /**
     * Collects the component definitions from a {@code component-load.xml} file
     * @param configUrl  the location of the {@code component-load.xml} file
     * @return a list of component definitions
     * @throws ComponentException when the {@code component-load.xml} file is invalid.
     */
    public static List<ComponentDef> getComponentsFromConfig(URL configUrl) throws ComponentException {
        Document document = parseDocumentFromUrl(configUrl);
        List<? extends Element> toLoad = UtilXml.childElementList(document.getDocumentElement());
        List<ComponentDef> componentsFromConfig = new ArrayList<>();

        for (Element element : toLoad) {
            componentsFromConfig.add(ComponentDef.of(element, configUrl));
        }
        return Collections.unmodifiableList(componentsFromConfig);
    }

    /**
     * Represents a simple component or a component directory.
     */
    public static final class ComponentDef {
        /** The location of the component. */
        private final Path location;
        /** The type of component. */
        private final ComponentType type;

        public Path getLocation() {
            return location;
        }

        public ComponentType getType() {
            return type;
        }

        /**
         * Constructs a component definition.
         * @param location  the location of the component
         * @param type  the type of the component
         */
        private ComponentDef(Path location, ComponentType type) {
            this.location = location;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("ComponentDef [location=%s, type=%s]", location, type);
        }

        /**
         * Converts a string based location to a proper {@code Path}.
         * @param location  the string based location
         * @return the corresponding {@code Path} object.
         */
        private static Path locationToPath(String location) {
            Map<String, ?> systemProps = UtilGenerics.cast(System.getProperties());
            return Paths.get(FlexibleStringExpander.expandString(location, systemProps));
        }

        /**
         * Constructs a component definition object from an XML element.
         * @param element  an XML element which must have either a "load-component" or "load-components" label.
         * @param configUrl  the location of the file containing the XML element
         * @return the corresponding component definition object.
         * @throws ComponentException when {@code element} has an invalid label.
         */
        private static ComponentDef of(Element element, URL configUrl) throws ComponentException {
            String nodeName = element.getNodeName();
            switch (nodeName) {
            case "load-component":
                return new ComponentDef(locationToPath(element.getAttribute("component-location")),
                        ComponentType.SINGLE_COMPONENT);
            case "load-components":
                return new ComponentDef(locationToPath(element.getAttribute("parent-directory")),
                        ComponentType.COMPONENT_DIRECTORY);
            default:
                throw new ComponentException(
                        String.format("Invalid element '%s' found in component-load file %s", nodeName, configUrl));
            }
        }
    }

    /**
     * Parses a {@code component-load.xml} resource.
     * @param configUrl the {@code component-load.xml} resource.
     * @return the parsed XML document
     * @throws ComponentException when {@code configUrl} is {@code null} or is invalid.
     */
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
}
