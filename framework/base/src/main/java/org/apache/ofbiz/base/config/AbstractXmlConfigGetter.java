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
package org.apache.ofbiz.base.config;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * Abstract class that mustn't be instantiated.
 * Instead, it must be extended for each resource file, such as entityengine or serviceengine.
 * The child ConfigGetters define the resource name, and the root xPath
 */
public abstract class AbstractXmlConfigGetter {
    private final String ressource;
    private final String rootXPath;
    private final ConfigurationInterface config;

    protected AbstractXmlConfigGetter(String ressource, String rootXPath) {
        this.ressource = ressource;
        this.rootXPath = rootXPath;
        config = ConfigurationFactory.getInstance();
    }

    /**
     * @param key the key of the value that is looked up for.
     * @return the value that is found
     */
    public String getValue(String key) {
        return config.getValue(ressource, key);
    }

    /**
     * See {@link AbstractXmlConfigGetter#getValue(Map, String, Object, Class)}
     */
    public String getValue(Map<String, Object> configObject, String key) {
        return getValue(configObject, key, "", String.class);
    }

    /**
     * Get a config value in the {@code configObject} if it has the key, else search the config system.
     *
     * @param configObject a Map coming from the config system.
     * @param key          the key of the value that is looked up for.
     * @param defaultValue the value return if not found
     * @param targetClass  the output class of the looked up element.
     * @return the single value found for {@code key}
     */
    public <T> T getValue(Map<String, Object> configObject, String key, T defaultValue, Class<T> targetClass) {
        if (configObject == null) {
            return getValue(key, defaultValue, targetClass);
        }
        return config.getValue(configObject, key, targetClass, defaultValue);
    }

    /**
     * Get a config value in the config system.
     *
     * @param key          the key of the value that is looked up for.
     * @param defaultValue the value return if not found
     * @param targetClass  the output class of the looked up element.
     * @return the single value found for {@code key}
     */
    public <T> T getValue(String key, T defaultValue, Class<T> targetClass) {
        return config.getValue(ressource, key, targetClass, defaultValue);
    }

    /**
     * See {@link AbstractXmlConfigGetter#getObjectSubElement(String, Element, Class)}
     */
    public <T> T getObjectSubElement(Element parent, Class<T> targetClass) {
        return getObjectSubElement(rootXPath, parent, targetClass);
    }

    /**
     * Get a config value in the config system.
     *
     * @param parentXPath the xPath that serves as root for the wanted config element
     * @param parent      the parent element to look into
     * @param targetClass the output class of the looked up element.
     * @return the single value found for {@code xPath} in {@code parent}
     */
    public <T> T getObjectSubElement(String parentXPath, Element parent, Class<T> targetClass) {
        return config.getConfigElementAsObjectOfClass(ressource, parentXPath, parent, targetClass);
    }

    /**
     * See {@link AbstractXmlConfigGetter#getSubElementsAsListEntries(String, Element, Class)}
     */
    public <T> List<T> getSubElementsAsListEntries(Element parent, Class<T> targetClass) {
        return getSubElementsAsListEntries(rootXPath, parent, targetClass);
    }

    /**
     * Get a config List in the config system.
     *
     * @param parentXPath the xPath that serves as root for the wanted config element
     * @param parent      the parent element to look into
     * @param targetClass the output class of the looked up element.
     * @return a list of {@code targetClass} found in {@code parent}
     */
    public <T> List<T> getSubElementsAsListEntries(String parentXPath, Element parent, Class<T> targetClass) {
        return config.getConfigElementsAsListEntriesOfClass(ressource, parentXPath, parent, targetClass);
    }

    /**
     * See {@link AbstractXmlConfigGetter#getSubElementsAsMapValues(String, Element, Class)}
     */
    public <T> Map<String, T> getSubElementsAsMapValues(Element parent, Class<T> targetClass) {
        return getSubElementsAsMapValues(rootXPath, parent, targetClass);
    }

    /**
     * Get a config Map from the config system.
     *
     * @param parentXPath the xPath that serves as root for the wanted config element
     * @param parent      the parent element to look into
     * @param targetClass the output class of the looked up element.
     * @return a Map of {@code targetClass} found in {@code parent}. The element names serves as key.
     */
    public <T> Map<String, T> getSubElementsAsMapValues(String parentXPath, Element parent, Class<T> targetClass) {
        return config.getConfigElementsAsMapValuesOfClass(ressource, parentXPath, parent, targetClass);
    }

}
