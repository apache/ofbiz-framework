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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.cache.UtilCache;

public class DefaultConfiguration implements ConfigurationInterface {
    public static final String MODULE = DefaultConfiguration.class.getName();
    private static final UtilCache<String, ConfigurationReaderInterface> CONFIGURATION_TYPE = UtilCache.createUtilCache("base.config.type", 0, 0);
    private final TypesafeConfigImplReader typesafeConfReader;
    private boolean useOverrideValue = true;

    public DefaultConfiguration() {
        typesafeConfReader = new TypesafeConfigImplReader();
    }

    public DefaultConfiguration(File configPath) {
        typesafeConfReader = new TypesafeConfigImplReader(configPath);
    }

    public DefaultConfiguration(List<File> configPaths) {
        typesafeConfReader = new TypesafeConfigImplReader(configPaths);
    }

    @Override
    public String getValue(String resourceName, String key) {
        return getValue(resourceName, key, String.class, "");
    }

    @Override
    public String getValue(String resourceName, String key, String defaultValue) {
        return getValue(resourceName, key, String.class, defaultValue);
    }

    @Override
    public <T> T getValue(String resourceName, String key, Class<T> targetClass) {
        return getValue(resourceName, key, targetClass, null);
    }

    @Override
    public <T> T getValue(String resourceName, String key, Class<T> targetClass, T defaultValue) {
        String value = null;
        if (getUseOverrideValue()) {
            value = getValueFromOverride(resourceName, key);
        }
        if (value == null) {
            value = getRelevantReader(resourceName).getValue(key);
        }
        if (targetClass == String.class) {
            return value == null
                    ? defaultValue
                    : UtilGenerics.cast(value);
        }
        try {
            return value == null
                    ? defaultValue
                    : Converters.getConverter(String.class, targetClass).convert(value);
        } catch (ClassNotFoundException | ConversionException e) {
            Debug.logError(String.format("Try to convert a String %s to %s with error %s", value, targetClass, e), MODULE);
        }
        return null;
    }

    @Override
    public <T> T getValue(Map<String, Object> configObject, String key, Class<T> targetClass, T defaultValue) {
        Object value = typesafeConfReader.getValue(configObject, TypesafeConfigImplReader.convertToConfigPath(key));
        if (targetClass == String.class) {
            return value == null ? defaultValue : UtilGenerics.cast(value);
        }
        try {
            return value == null
                    ? defaultValue
                    : Converters.getConverter(String.class, targetClass).convert(value.toString());
        } catch (ClassNotFoundException | ConversionException e) {
            Debug.logError(String.format("Try to convert a String %s to %s with error %s", value, targetClass, e), MODULE);
        }
        return null;
    }

    /**
     * Retrieve the value contained in {@code resourceName} at {@code key} in the override reader.
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine')
     * @param key          the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @return the value contained by the key or {@code null} if it was not found
     */
    private String getValueFromOverride(String resourceName, String key) {
        return typesafeConfReader.getValue(resourceName, key);
    }

    /**
     * Find the correct type of config reader implementation to use given a resource file
     *
     * @param resourceName the XML or Java properties file to read
     * @return the appropriate implementation of {@code ConfigurationReaderInterface} depending on the extension of {@code resourceName}
     */
    private ConfigurationReaderInterface getRelevantReader(String resourceName) {
        ConfigurationReaderInterface readerToUse = CONFIGURATION_TYPE.get(resourceName);
        if (readerToUse != null) {
            return readerToUse;
        }
        String ext = FilenameUtils.getExtension(resourceName);
        readerToUse = switch (ext) {
        case "xml" -> new XmlConfigurationReader(resourceName);
        default -> new PropertiesConfigurationReader(resourceName);
        };
        CONFIGURATION_TYPE.put(resourceName, readerToUse);
        return readerToUse;
    }

    /**
     * return true if we need to return the override value or origin value
     *
     * @return true is we override values
     */
    public boolean getUseOverrideValue() {
        return useOverrideValue; //use for test
    }

    /**
     * Set true if we want override values
     *
     * @param useOverrideValue parameter indicating if the class should override its configuration (true by default)
     */
    public void setUseOverrideValue(boolean useOverrideValue) {
        this.useOverrideValue = useOverrideValue; //use for test
    }

    /**
     * Empty all config cache
     */
    public void clearCache() {
        CONFIGURATION_TYPE.clear();
        typesafeConfReader.clearCache();
    }

    @Override
    public <T> T getConfigElementAsObjectOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass) {
        return getRelevantReader(resourceName).findSingleElementWithClassInParent(xPath, parent, targetClass);
    }

    @Override
    public <T> List<T> getConfigElementsAsListEntriesOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass) {
        return typesafeConfReader.collectElementsAsListEntriesWithConfigsApplied(resourceName, xPath, targetClass,
                getRelevantReader(resourceName).findElementsWithTypeInParent(xPath, parent, targetClass));
    }

    @Override
    public <T> Map<String, T> getConfigElementsAsMapValuesOfClass(String resourceName, String xPath, Object parent, Class<T> targetClass) {
        List<T> childList = getConfigElementsAsListEntriesOfClass(resourceName, xPath, parent, targetClass);
        Map<String, T> uniqueMap = new LinkedHashMap<>();
        try {
            Method method = targetClass.getMethod("getName");
            for (T value : childList) {
                uniqueMap.put((String) method.invoke(value), value);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        uniqueMap.putAll(typesafeConfReader.collectElementsAsMapValuesWithConfigsApplied(resourceName, xPath, targetClass, uniqueMap));
        return uniqueMap;
    }
}
