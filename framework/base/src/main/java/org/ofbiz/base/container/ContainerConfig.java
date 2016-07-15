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
package org.ofbiz.base.container;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.lang.LockedBy;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ContainerConfig - Container configuration for ofbiz.xml
 *
 */
public class ContainerConfig {

    public static final String module = ContainerConfig.class.getName();

    @LockedBy("ContainerConfig.class")
    private static Map<String, Container> containers = new LinkedHashMap<String, Container>();

    public static Container getContainer(String containerName, String configFile) throws ContainerException {
        Container container = containers.get(containerName);
        if (container == null) {
            getContainers(configFile);
            container = containers.get(containerName);
        }
        if (container == null) {
            throw new ContainerException("No container found with the name : " + containerName);
        }
        return container;
    }

    public static Collection<Container> getContainers(String configFile) throws ContainerException {
        if (UtilValidate.isEmpty(configFile)) {
            throw new ContainerException("configFile argument cannot be null or empty");
        }
        URL xmlUrl = UtilURL.fromResource(configFile);
        if (xmlUrl == null) {
            throw new ContainerException("Could not find container config file " + configFile);
        }
        return getContainers(xmlUrl);
    }

    public static Collection<Container> getContainers(URL xmlUrl) throws ContainerException {
        if (xmlUrl == null) {
            throw new ContainerException("xmlUrl argument cannot be null");
        }
        Collection<Container> result = getContainerPropsFromXml(xmlUrl);
        synchronized (ContainerConfig.class) {
            for (Container container : result) {
                containers.put(container.name, container);
            }
        }
        return result;
    }

    public static String getPropertyValue(ContainerConfig.Container parentProp, String name, String defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return prop.value;
        }
    }

    public static int getPropertyValue(ContainerConfig.Container parentProp, String name, int defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            int num = defaultValue;
            try {
                num = Integer.parseInt(prop.value);
            } catch (Exception e) {
                return defaultValue;
            }
            return num;
        }
    }

    public static boolean getPropertyValue(ContainerConfig.Container parentProp, String name, boolean defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return "true".equalsIgnoreCase(prop.value);
        }
    }

    public static String getPropertyValue(ContainerConfig.Container.Property parentProp, String name, String defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return prop.value;
        }
    }

    public static int getPropertyValue(ContainerConfig.Container.Property parentProp, String name, int defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            int num = defaultValue;
            try {
                num = Integer.parseInt(prop.value);
            } catch (Exception e) {
                return defaultValue;
            }
            return num;
        }
    }

    public static boolean getPropertyValue(ContainerConfig.Container.Property parentProp, String name, boolean defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return "true".equalsIgnoreCase(prop.value);
        }
    }

    private ContainerConfig() {}

    private static Collection<Container> getContainerPropsFromXml(URL xmlUrl) throws ContainerException {
        Document containerDocument = null;
        try {
            containerDocument = UtilXml.readXmlDocument(xmlUrl, true);
        } catch (SAXException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        } catch (IOException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        }
        Element root = containerDocument.getDocumentElement();
        List<Container> result = new ArrayList<Container>();
        for (Element curElement: UtilXml.childElementList(root, "container")) {
            result.add(new Container(curElement));
        }
        return result;
    }

    public static class Container {
        public final String name;
        public final String className;
        public final List<String> loaders;
        public final Map<String, Property> properties;

        public Container(Element element) {
            this.name = element.getAttribute("name");
            this.className = element.getAttribute("class");
            this.loaders = StringUtil.split(element.getAttribute("loaders"), ",");

            properties = new LinkedHashMap<String, Property>();
            for (Element curElement: UtilXml.childElementList(element, "property")) {
                Property property = new Property(curElement);
                properties.put(property.name, property);
            }
        }

        public Property getProperty(String name) {
            return properties.get(name);
        }

        public List<Property> getPropertiesWithValue(String value) {
            List<Property> props = new LinkedList<Property>();
            if (UtilValidate.isNotEmpty(properties)) {
                for (Property p: properties.values()) {
                    if (p != null && value.equals(p.value)) {
                        props.add(p);
                    }
                }
            }
            return props;
        }

        public static class Property {
            public String name;
            public String value;
            public Map<String, Property> properties;

            public Property(Element element) {
                this.name = element.getAttribute("name");
                this.value = element.getAttribute("value");
                if (UtilValidate.isEmpty(this.value)) {
                    this.value = UtilXml.childElementValue(element, "property-value");
                }

                properties = new LinkedHashMap<String, Property>();
                for (Element curElement: UtilXml.childElementList(element, "property")) {
                    Property property = new Property(curElement);
                    properties.put(property.name, property);
                }
            }

            public Property getProperty(String name) {
                return properties.get(name);
            }

            public List<Property> getPropertiesWithValue(String value) {
                List<Property> props = new LinkedList<Property>();
                if (UtilValidate.isNotEmpty(properties)) {
                    for (Property p: properties.values()) {
                        if (p != null && value.equals(p.value)) {
                            props.add(p);
                        }
                    }
                }
                return props;
            }
        }
    }
}
