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

/**
 * Interface for classes that read configuration.
 * Each implementation handles a specific configuration type.
 */
public interface ConfigurationReaderInterface {

    /**
     * Inits a reader from a resource file.
     *
     * @param resourceName the XML, Java properties, JSON (or HOCON) file to load
     */
    void init(String resourceName);

    /**
     * Retrieve the value from reader at {@code key}
     *
     * @param key the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @return a {@code String} representation of the value contained in the key or {@code null} if it was not found
     */
    String getValue(String key);

    /**
     * Retrieves an inheritor of {@link AbstractConfigElement} representing a configuration element of the wanted class.
     * @param key the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @param parent the parent that will be searched for the wanted elements. Can be an {@link org.w3c.dom.Element} in case of xml representation
     *              or a {@link java.util.Map} in case of configuration overload.
     * @param targetClass the class of the element that is lookup up.
     * @return an instance of {@code targetClass} or {@code null} if the element wasn't found
     */
    <T> T findSingleElementWithClassInParent(String key, Object parent, Class<T> targetClass);

    /**
     * Retrieves an inheritor List of {@link AbstractConfigElement} representing a list of configuration elements of the wanted class.
     * @param key the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @param parent the parent that will be searched for the wanted elements. Can be an {@link org.w3c.dom.Element} in case of xml representation
     *              or a {@link java.util.Map} in case of configuration overload.
     * @param targetClass the class of the element that is lookup up.
     * @return a List of {@code targetClass} or an empty List if the element wasn't found
     */
    <T> List<T> findElementsWithTypeInParent(String key, Object parent, Class<T> targetClass);

    /**
     * Clears the cache of the reader.
     */
    void clearCache();
}
