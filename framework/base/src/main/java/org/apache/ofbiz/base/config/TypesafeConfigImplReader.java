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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;

import javax.xml.xpath.XPathExpressionException;

public class TypesafeConfigImplReader implements ConfigurationReaderInterface {
    private static final String MODULE = TypesafeConfigImplReader.class.getName();
    private Config globalConfig = ConfigFactory.empty();
    private static final UtilCache<String, String> CONFIGURATION_CACHE = UtilCache.createUtilCache("base.config", 0, 0);
    private static final int MAX_PATH_LENGTH = 500;

    public TypesafeConfigImplReader() {
        this((String) null);
    }

    public TypesafeConfigImplReader(String confOverload) {
        init(confOverload);
    }

    public TypesafeConfigImplReader(File confOverload) {
        init(List.of(confOverload));
    }

    public TypesafeConfigImplReader(List<File> confOverloads) {
        init(confOverloads);
    }

    @Override
    public void init(String resourceName) {
        CONFIGURATION_CACHE.clear();
        globalConfig = UtilValidate.isNotEmpty(resourceName)
                ? ConfigFactory.load(resourceName)
                : ConfigFactory.load();
    }

    /**
     * Specific initializer for Typesafe reader implementation, since its must be able to load multiple files.
     *
     * @param confFiles the list of files that will be loaded.
     */
    public void init(List<File> confFiles) {
        CONFIGURATION_CACHE.clear();
        Debug.logInfo("Collect available configuration", MODULE);
        List<Config> configsToLoad = UtilMisc.toList(
                ConfigFactory.systemEnvironment(),
                ConfigFactory.systemProperties());
        if (UtilValidate.isNotEmpty(confFiles)) {
            confFiles.sort(Comparator.comparing(File::getName));
            for (File confFile : confFiles) {
                configsToLoad.add(ConfigFactory.parseFile(confFile));
            }
        }
        configsToLoad.stream()
                .filter(Objects::nonNull)
                .toList()
                .forEach(config -> {
                    globalConfig = globalConfig.withFallback(config);
                    Debug.logInfo("Loaded config element with origin: " + config.origin(), MODULE);
                });
        globalConfig = globalConfig.resolve();
        Debug.logInfo("Configurations loaded and merged", MODULE);
    }

    @Override
    public String getValue(String key) {
        return getValue("", key);
    }

    @Override
    public <T> List<T> findElementsWithTypeInParent(String key, Object parent, Class<T> targetClass) {
        return List.of();
    }

    @Override
    public <T> T findSingleElementWithClassInParent(String xPath, Object parent, Class<T> targetClass) {
        return null;
    }

    /**
     * Retrieve a value from the loaded typesafe configs.
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine').
     * @param key          the key associated with the value that is looked for. Can be in standard java property format or xPath.
     * @return a {@code String} representation of the value at key or {@code null} if it was not found
     */
    public String getValue(String resourceName, String key) {
        String configMapKey = String.format("%s#%s", resourceName, key);
        if (CONFIGURATION_CACHE.get(configMapKey) != null) {
            return CONFIGURATION_CACHE.get(configMapKey);
        }

        String configPath = convertToConfigPath(resourceName, key);
        String value = null;
        if (configPath == null) {
            return "";
        } else {
            if (!globalConfig.hasPath(configPath) && !key.startsWith("/")) { // property case, escaping xml xpaths
                configPath = convertToConfigPath(resourceName, '"' + key + '"');
            }
            if (globalConfig.hasPath(configPath)) {
                value = globalConfig.getString(configPath).intern();
            }
        }
        CONFIGURATION_CACHE.put(configMapKey, value);
        return value;
    }

    /**
     * Return a value in {@code configObject} using {@code key}. Here, {@code configObject} is a Map, likely comming from
     * a typesafe config object.
     *
     * @param configObject the representation of a config loaded in the system.
     * @param key          the key that is wanted in the wanted map
     * @return a value that can be either {@code String}, {@code Number}, {@code Boolean}, {@code Map<String, Object>},
     * {@code List<Object>} or {@code null} if the value was not found.
     */
    public Object getValue(Map<String, Object> configObject, String key) {
        return configObject.get(key);
    }

    /**
     * Collects all elements of wanted type, regardless of their configuration origin or file.
     * Applies all overloads from typesafe config system.
     *
     * @param resourceName       the name of the resource to look in for the element (eg: 'entityengine').
     * @param xPath              the xPath of the elements
     * @param targetClass        the class of the wanted elements, that will be used in the return List
     * @param existingObjectList the list (if any) of already existing objects (loaded from XML for example). It will be used to
     *                           check if any overload must be applied to an existing object.
     * @return a list of objects of the wanted class, with all configs applied
     */
    public <T> List<T> collectElementsAsListEntriesWithConfigsApplied(String resourceName, String xPath,
                                                                      Class<T> targetClass, List<T> existingObjectList) {
        Map<String, T> existingObjectMap = new HashMap<>();
        try {
            String childElementName = (String) targetClass.getDeclaredField("ELEMENT_NAME").get(null);
            String confPath = convertToConfigPath(resourceName, xPath.concat("/" + childElementName));
            if (!globalConfig.hasPath(confPath)) {
                return existingObjectList;
            }
            Method nameGetter = targetClass.getMethod("getName");
            Method multipleSourceGetter = targetClass.getMethod("allowMultipleSources");
            boolean allowMultipleSources;
            for (T object : existingObjectList) {
                String objectKey = (String) nameGetter.invoke(object);
                existingObjectMap.put(objectKey, object);
                if (object == existingObjectList.get(0)) {
                    allowMultipleSources = (boolean) multipleSourceGetter.invoke(object);
                    if (!allowMultipleSources) {
                        return collectAnonymousElementList(resourceName, xPath, targetClass);
                    }
                }
            }
            existingObjectMap.putAll(collectElementsAsMapValuesWithConfigsApplied(resourceName, xPath, targetClass, existingObjectMap));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return new LinkedList<>(existingObjectMap.values());
    }

    /**
     * Collects simple config elements that don't have precise naming keys (such as run-from-pool, or read-data)
     *
     * @param resourceName the name of the resource to look in for the element (eg: 'entityengine').
     * @param xPath        the xPath of the elements
     * @param targetClass  the class of the wanted elements, that will be used in the return List
     * @return a List of objects of {@code targetClass}
     */
    public <T> List<T> collectAnonymousElementList(String resourceName, String xPath, Class<T> targetClass) {
        List<T> objectList = new LinkedList<>();
        try {
            String childElementName = (String) targetClass.getDeclaredField("ELEMENT_NAME").get(null);
            String childElementFieldIdName = null;
            if (Arrays.stream(targetClass.getDeclaredFields())
                    .anyMatch(field -> field.getName().equals("ELEMENT_FIELD_ID_NAME"))) {
                childElementFieldIdName = (String) targetClass.getDeclaredField("ELEMENT_FIELD_ID_NAME").get(null);
            }
            if (childElementFieldIdName == null) {
                childElementFieldIdName = "name";
            }
            String confPath = convertToConfigPath(resourceName, xPath.concat("/" + childElementName));
            if (!globalConfig.hasPath(confPath)) {
                return objectList;
            }
            ConfigValue configValue = globalConfig.getValue(confPath);
            List<? extends ConfigObject> configObjectList = configValue.valueType().equals(ConfigValueType.LIST)
                    ? globalConfig.getObjectList(confPath)
                    : List.of(globalConfig.getObject(confPath));

            for (ConfigObject configObject : configObjectList) {
                String newXPath = getNewXPath(xPath, childElementFieldIdName, childElementName,
                        String.valueOf(configObject.get(childElementFieldIdName).unwrapped()));
                T tObject = UtilGenerics.cast(targetClass.getMethod("loadFromConfig", Map.class, String.class)
                        .invoke(null, configObject.unwrapped(), newXPath));
                objectList.add(tObject);
            }
        } catch (IllegalAccessException | NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return objectList;
    }


    /**
     * Collects all elements of wanted type, regardless of their configuration origin or file.
     * Applies all overloads from typesafe config system.
     *
     * @param resourceName      the name of the resource to look in for the element (eg: 'entityengine').
     * @param xPath             the xPath of the elements
     * @param targetClass       the class of the wanted elements, that will be used in the return List
     * @param existingObjectMap the map (if any) of already existing objects (loaded from XML for example). It will be used to
     *                          check if any overload must be applied to an existing object.
     * @return a Map of objects of the wanted class, with all configs applied. The config object name will be used as map key.
     */
    public <T> Map<String, T> collectElementsAsMapValuesWithConfigsApplied(String resourceName, String xPath,
                                                                           Class<T> targetClass, Map<String, T> existingObjectMap) {
        Map<String, T> objectMap = new LinkedHashMap<>();
        try {
            String childElementName = (String) targetClass.getDeclaredField("ELEMENT_NAME").get(null);
            String childElementFieldIdName = null;
            if (Arrays.stream(targetClass.getDeclaredFields())
                    .anyMatch(field -> field.getName().equals("ELEMENT_FIELD_ID_NAME"))) {
                childElementFieldIdName = (String) targetClass.getDeclaredField("ELEMENT_FIELD_ID_NAME").get(null);
            }
            if (childElementFieldIdName == null) {
                childElementFieldIdName = "name";
            }
            String confPath = convertToConfigPath(resourceName, xPath.concat("/" + childElementName));
            if (!globalConfig.hasPath(confPath)) {
                return objectMap;
            }
            ConfigValue configValue = globalConfig.getValue(confPath);
            List<? extends ConfigObject> configObjectList = configValue.valueType().equals(ConfigValueType.LIST)
                    ? globalConfig.getObjectList(confPath)
                    : List.of(globalConfig.getObject(confPath));
            for (ConfigObject configObject : configObjectList) {

                Map<String, Object> configMap = configObject.unwrapped();
                configMap.keySet().removeAll(existingObjectMap.keySet());
                Set<String> configKeySet = configMap.keySet();
                for (String configKey : configKeySet) {
                    Object currentObj = configMap.get(configKey);
                    Map<String, Object> currentMap;
                    if (currentObj instanceof Map) {
                        currentMap = UtilGenerics.cast(currentObj);
                        if (!currentMap.containsKey(childElementFieldIdName)) {
                            currentMap.put(childElementFieldIdName, configKey);
                        }
                    } else {
                        currentMap = Map.of(childElementFieldIdName, currentObj);
                    }
                    String newXPath = getNewXPath(xPath, childElementFieldIdName, childElementName, configKey);
                    T tObject = UtilGenerics.cast(targetClass.getMethod("loadFromConfig", Map.class, String.class)
                            .invoke(null, currentMap, newXPath));
                    objectMap.put(configKey, tObject);
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return objectMap;
    }

    private static String getNewXPath(String xPath, String configKey, String childElementName, String elementValue) {
        return xPath + "/" + childElementName + "[@" + configKey + "='" + elementValue + "']";
    }

    /**
     * Convert an XML xPath to Hocon syntax by doing the followings :
     * <ul>
     *     <li>Replace all {@code /} by {@code .}</li>
     *     <li>Remove the {@code /} (or {@code .} depending on if the above step is already done) at position 0</li>
     *     <li>Replace all instances of {@code [x]} by {@code .y} (considering {@code y=x-1})</li>
     *     <li>Replace all instances of {@code foo[@name='bar']} by {@code foo.bar} (a sequence other than {@code @name}
     *     is considered as an error)</li>
     *     <li>Remove all {@code @}</li>
     * </ul>
     *
     * @param path the key (properties) or xPath (XML) of the file to override
     * @return a converted path understandable by Lightbend config methods or null if there is an error in the path
     */
    public static String convertToConfigPath(String resourceName, String path) {
        try {
            if (UtilValidate.isEmpty(resourceName)) {
                return convertToConfigPath(path);
            }
            return (resourceName.contains(".")
                    ? resourceName.substring(0, resourceName.lastIndexOf('.'))
                    : resourceName)
                    + "." + convertToConfigPath(path);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * See {@link TypesafeConfigImplReader#convertToConfigPath(String, String)}
     */
    public static String convertToConfigPath(String path) throws XPathExpressionException {
        if (path == null || !path.contains("/")) {
            return path;
        }
        if (path.length() >= MAX_PATH_LENGTH) {
            throw new XPathExpressionException("Security error, given to long, max path size allowed is " + MAX_PATH_LENGTH);
        }

        Pattern pattern = Pattern.compile("\\[@(.+)=([^\"'].+[^\"'])]", Pattern.CASE_INSENSITIVE);
        String convertedPath = pattern.matcher(path)
                .replaceAll("[@$1=\"$2\"]")
                .replace("'", "\"")
                .replaceAll("\\[@name=|\\[@group-name=|\\[@reader-name=", ".");
        pattern = Pattern.compile("\\[@[a-z]+=.+]", Pattern.CASE_INSENSITIVE);
        if (pattern.matcher(convertedPath).find()) {
            return null;
        }

        // find all number list and decrease the number
        pattern = Pattern.compile("\\[\\d+]");
        List<MatchResult> matchResults = pattern.matcher(convertedPath).results().toList();
        if (!matchResults.isEmpty()) {
            int index = 0;
            StringBuilder parsedListPath = new StringBuilder();
            for (MatchResult matchResult : matchResults) {
                int listIndex = Integer.parseInt(matchResult.group().replaceAll("[\\[\\]]", ""));
                listIndex--;
                parsedListPath.append(convertedPath, index, matchResult.start())
                        .append(".")
                        .append(listIndex);
                index = matchResult.end();
            }
            convertedPath = parsedListPath.append(convertedPath.substring(index)).toString();
        }

        String jsonPath = convertedPath.replace("/", ".")
                .replaceAll("[@|,\\]]", "");
        return jsonPath.substring(1);
    }

    /**
     * Clear config cache
     */
    public void clearCache() {
        CONFIGURATION_CACHE.clear();
    }
}
