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

/**
 * Main interface for configuration overload system.
 */
public interface ConfigurationInterface {

    /**
     * See {@link ConfigurationInterface#getValue(String, String, Class, Object)}
     */
    String getValue(String resourceName, String key);

    /**
     * See {@link ConfigurationInterface#getValue(String, String, Class, Object)}
     */
    String getValue(String resourceName, String key, String defaultValue);

    /**
     * See {@link ConfigurationInterface#getValue(String, String, Class, Object)}
     */
    <T> T getValue(String resourceName, String key, Class<T> targetClass);

    /**
     * Retrieve the value contained in {@code resourceName} using {@code key}
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine')
     * @param key          the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @param targetClass  the output class of the looked up element.
     * @param defaultValue the value to return if no value was found for the given key.
     * @return the value contained in the override file if any is found. <br/>
     * Else, returns the value at {@code key} in {@code resourceName} <br/>
     * Else return {@code defaultValue}
     */
    <T> T getValue(String resourceName, String key, Class<T> targetClass, T defaultValue);

    /**
     * Retrieve the value contained in <code>resourceName</code> using <code>key</code>
     *
     * @param configObject an object representation of an XML element. Likely comes from an overload config file.
     * @param key          the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @param targetClass  the output class of the looked up element.
     * @param defaultValue the value to return if no value was found for the given key.
     * @return the value contained in the override file if any is found. <br/>
     * Else, returns the value at {@code key} in {@code resourceName} <br/>
     * Else return {@code defaultValue}
     */
    <T> T getValue(Map<String, Object> configObject, String key, Class<T> targetClass, T defaultValue);

    /**
     * Get the attribute {@code useOverride}
     *
     * @return the attribute indicating if the overload config system should be enabled
     */
    boolean getUseOverrideValue();

    /**
     * Set the attribute {@code useOverride}
     *
     * @param useOverride set the parameter indicating if the overload config system should be enabled
     */
    void setUseOverrideValue(boolean useOverride);

    /**
     * Clears the configuration cache
     */
    void clearCache();

    /**
     * Constructs and / or gets  an inheritor of {@link AbstractConfigElement}. All the configurations will be read and used.
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine')
     * @param xPath        the xPath of the wanted value
     * @param parent       the {@code Element} from which to retrieve the child, must be {@code null} when creating a new element using a conf file
     * @param targetClass  the class of the child object that will be created from {@code parent}
     * @return an instance of {@code targetClass} or {@code null} if no element was found
     */
    <T> T getConfigElementAsObjectOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass);

    /**
     * Retrieve one or more XML elements and create a list of objects out of them using a class specified in parameter
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine')
     * @param xPath        the xPath of the wanted value
     * @param parent       the {@code Element} from which to retrieve the child, must be {@code null} when creating a new element using a conf file
     * @param targetClass  the class of the child object that will be created from {@code parent}
     * @return a list of objects of <code>targetClass</code> or an empty list no elements were found
     * @return a List of {@code targetClass} or an empty List if no element was found
     */
    <T> List<T> getConfigElementsAsListEntriesOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass);

    /**
     * Retrieve one or more XML elements and create a map of objects paired with their name (or unique attribute) out of
     * them using a class specified in parameter
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine')
     * @param xPath        the xPath of the wanted value
     * @param parent       the {@code Element} from which to retrieve the child, must be {@code null} when creating a new element using a conf file
     * @param targetClass  the class of the child object that will be created from {@code parent}
     * @return a Map of objects of {@code targetClass}, with their names as keys, or an empty Map if no element was found
     */
    <T> Map<String, T> getConfigElementsAsMapValuesOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass);
}
