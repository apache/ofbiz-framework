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
package org.apache.ofbiz.base.container;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * A container configuration.
 */
public final class ContainerConfig {
    /**
     * The global container configuration store.
     */
    private static final Map<String, Configuration> CONFIGURATIONS = new LinkedHashMap<>();

    private ContainerConfig() { }

    /**
     * Retrieves the container configuration element corresponding to a container name.
     * @param containerName the name of the container to retrieve
     * @param configFile    the file name corresponding to the global container configuration file
     * @return the corresponding configuration element.
     * @throws ContainerException when no configuration element are found.
     * @deprecated Use {@link #getConfiguration(String)} instead.
     */
    @Deprecated
    public static Configuration getConfiguration(String containerName, String configFile)
            throws ContainerException {
        return getConfiguration(containerName);
    }

    /**
     * Retrieves the container configuration element corresponding to a container name.
     * @param containerName the name of the container to retrieve
     * @return the corresponding configuration element.
     * @throws ContainerException when no configuration element are found.
     */
    public static Configuration getConfiguration(String containerName) throws ContainerException {
        Configuration configuration = CONFIGURATIONS.get(containerName);
        if (configuration == null) {
            throw new ContainerException("No container found with the name : " + containerName);
        }
        return configuration;
    }

    /**
     * Finds the {@code <container>} configuration elements in a XML element.
     * @param root the XML element which cannot be {@code null}
     * @return a list of container configuration
     */
    public static List<Configuration> getConfigurations(Element root) {
        List<Configuration> res = UtilXml.childElementList(root, "container").stream()
                .map(Configuration::new)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        synchronized (ContainerConfig.class) {
            res.forEach(cfg -> CONFIGURATIONS.put(cfg.name(), cfg));
        }
        return res;
    }

    public static String getPropertyValue(PropertyChildren parentProp, String name, String defaultValue) {
        Configuration.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value())) {
            return defaultValue;
        }
        return prop.value;
    }

    public static int getPropertyValue(PropertyChildren parentProp, String name, int defaultValue) {
        Configuration.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value())) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(prop.value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getPropertyValue(PropertyChildren parentProp, String name, boolean defaultValue) {
        Configuration.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value())) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(prop.value);
    }

    interface PropertyChildren {
        /**
         * Provides the child property corresponding to a specified identifier.
         * @param name the child property identifier
         * @return the property corresponding to {@code name} or {@code null} if the identifier is absent.
         */
        Configuration.Property getProperty(String name);
    }

    /**
     * A container configuration.
     */
    public static final class Configuration implements PropertyChildren {
        //ALLOW PUBLIC FIELDS
        /**
         * The identifier of the configuration.
         */
        @Deprecated
        public final String name;
        /**
         * The name of class the configuration.
         */
        @Deprecated
        public final String className;
        /**
         * The list of loader names triggering the launch of the container.
         */
        @Deprecated
        public final List<String> loaders;
        /**
         * The container property elements.
         */
        @Deprecated
        public final Map<String, Property> properties;
        //FORBID PUBLIC FIELDS

        /**
         * Constructs a container configuration.
         * @param element the {@code <container>} XML element to parse
         */
        public Configuration(Element element) {
            name = element.getAttribute("name");
            className = element.getAttribute("class");
            loaders = Collections.unmodifiableList(StringUtil.split(element.getAttribute("loaders"), ","));
            properties = Property.parseProps(element);
        }

        /**
         * @return the name
         */
        public String name() {
            return name;
        }

        /**
         * @return the className
         */
        public String className() {
            return className;
        }
        /**
         * @return the loaders
         */
        public List<String> loaders() {
            return loaders;
        }

        /**
         * @return the properties
         */
        public Map<String, Property> properties() {
            return properties;
        }
        @Override
        public Configuration.Property getProperty(String name) {
            return properties().get(name);
        }
        /**
         * Provides all the child properties whose values are equal a specified value.
         * @param value the value to match
         * @return a list of matching properties
         */
        public List<Property> getPropertiesWithValue(String value) {
            return Property.getPropertiesWithValue(properties(), value);
        }
        /**
         * A tree of container configuration properties.
         */
        public static final class Property implements PropertyChildren {
            //ALLOW PUBLIC FIELDS
            /**
             * The identifier of the configuration element
             */
            @Deprecated
            public final String name;
            /**
             * The value associated with the {@code name} identifier.
             */
            @Deprecated
            public final String value;
            /**
             * The properties children
             */
            @Deprecated
            public final Map<String, Property> properties;
            //FORBID PUBLIC FIELDS
            /**
             * Constructs a container configuration element.
             * @param element the {@code <property>} XML element containing the configuration.
             */
            public Property(Element element) {
                name = element.getAttribute("name");
                String value = element.getAttribute("value");
                if (UtilValidate.isEmpty(value)) {
                    value = UtilXml.childElementValue(element, "property-value");
                }
                this.value = value;
                this.properties = parseProps(element);
            }
            /**
             * Aggregates the {@code <property>} XML elements in a Map.
             * @param root the root XML Element containing {@code <property>} children
             * @return a map of property elements
             */
            private static Map<String, Property> parseProps(Element root) {
                LinkedHashMap<String, Property> res = new LinkedHashMap<>();
                UtilXml.childElementList(root, "property").forEach(el -> {
                    Property p = new Property(el);
                    res.put(p.name(), p);
                });
                return Collections.unmodifiableMap(res);
            }
            /**
             * Provides all the child properties whose values are equal a specified value.
             * @param value the value to match
             * @return a list of matching properties
             */
            private static List<Property> getPropertiesWithValue(Map<String, Property> propkvs, String value) {
                return propkvs.values().stream()
                        .filter(Objects::nonNull)
                        .filter(p -> value.equals(p.value()))
                        .collect(toList());
            }
            /**
             * @return the name
             */
            public String name() {
                return name;
            }

            /**
             * @return the value
             */
            public String value() {
                return value;
            }

            /**
             * @return the properties
             */
            public Map<String, Property> properties() {
                return properties;
            }

            @Override
            public Configuration.Property getProperty(String name) {
                return properties().get(name);
            }
            /**
             * Provides all the child properties whose values are equal a specified value.
             * @param value the value to match
             * @return a list of matching properties
             */
            public List<Property> getPropertiesWithValue(String value) {
                return getPropertiesWithValue(properties(), value);
            }
        }
    }
}
