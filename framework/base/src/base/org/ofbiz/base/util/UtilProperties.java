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
package org.ofbiz.base.util;

import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javolution.util.FastSet;

import org.ofbiz.base.util.collections.FlexibleProperties;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.cache.UtilCache;

/**
 * Generic Property Accessor with Cache - Utilities for working with properties files
 *
 */
public class UtilProperties implements java.io.Serializable {

    public static final String module = UtilProperties.class.getName();

    /** An instance of the generic cache for storing the FlexibleProperties
     *  corresponding to each properties file keyed by a String for the resource location.
     * This will be used for both non-locale and locale keyed FexibleProperties instances.
     */
    protected static UtilCache resourceCache = new UtilCache("properties.UtilPropertiesResourceCache");

    /** An instance of the generic cache for storing the FlexibleProperties
     *  corresponding to each properties file keyed by a URL object
     */
    protected static UtilCache urlCache = new UtilCache("properties.UtilPropertiesUrlCache");

    /** An instance of the generic cache for storing the ResourceBundle
     *  corresponding to each properties file keyed by a String for the resource location and the locale
     */
    protected static UtilCache bundleLocaleCache = new UtilCache("properties.UtilPropertiesBundleLocaleCache");


    /** Compares the specified property to the compareString, returns true if they are the same, false otherwise
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEquals(String resource, String name, String compareString) {
        String value = getPropertyValue(resource, name);

        if (value == null) return false;
        return value.trim().equals(compareString);
    }

    /** Compares Ignoring Case the specified property to the compareString, returns true if they are the same, false otherwise
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEqualsIgnoreCase(String resource, String name, String compareString) {
        String value = getPropertyValue(resource, name);

        if (value == null) return false;
        return value.trim().equalsIgnoreCase(compareString);
    }

    /** Returns the value of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultValue is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultValue The value to return if the property is not found
     * @return The value of the property in the properties file, or if not found then the defaultValue
     */
    public static String getPropertyValue(String resource, String name, String defaultValue) {
        String value = getPropertyValue(resource, name);

        if (value == null || value.length() == 0)
            return defaultValue;
        else
            return value;
    }

    public static double getPropertyNumber(String resource, String name) {
        String str = getPropertyValue(resource, name);
        double strValue = 0.00000;

        try {
            strValue = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {}
        return strValue;
    }

    /** Returns the value of the specified property name from the specified resource/properties file
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @return The value of the property in the properties file
     */
    public static String getPropertyValue(String resource, String name) {
        if (resource == null || resource.length() <= 0) return "";
        if (name == null || name.length() <= 0) return "";
        FlexibleProperties properties = (FlexibleProperties) resourceCache.get(resource);

        if (properties == null) {
            try {
                URL url = UtilURL.fromResource(resource);

                if (url == null) return "";
                properties = FlexibleProperties.makeFlexibleProperties(url);
                resourceCache.put(resource, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }
        if (properties == null) {
            Debug.log("[UtilProperties.getPropertyValue] could not find resource: " + resource, module);
            return "";
        }

        String value = null;

        try {
            value = properties.getProperty(name);
        } catch (Exception e) {
            Debug.log(e.getMessage(), module);
        }
        return value == null ? "" : value.trim();
    }   

    /** Returns the specified resource/properties file
     * @param resource The name of the resource - can be a file, class, or URL
     * @return The properties file
     */
    public static Properties getProperties(String resource) {
        if (resource == null || resource.length() <= 0)
            return null;
        Properties properties = (FlexibleProperties) resourceCache.get(resource);

        if (properties == null) {
            try {
                URL url = UtilURL.fromResource(resource);

                if (url == null)
                    return null;
                properties = FlexibleProperties.makeFlexibleProperties(url);
                resourceCache.put(resource, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }
        if (properties == null) {
            Debug.log("[UtilProperties.getProperties] could not find resource: " + resource, module);
            return null;
        }
        return properties;
    }

    /** Returns the specified resource/properties file
     * @param url The URL to the resource
     * @return The properties file
     */
    public static Properties getProperties(URL url) {
        if (url == null)
            return null;
        Properties properties = (FlexibleProperties) resourceCache.get(url);

        if (properties == null) {
            try {
                properties = FlexibleProperties.makeFlexibleProperties(url);
                resourceCache.put(url, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }
        if (properties == null) {
            Debug.log("[UtilProperties.getProperties] could not find resource: " + url, module);
            return null;
        }
        return properties;
    }


    // ========= URL Based Methods ==========

    /** Compares the specified property to the compareString, returns true if they are the same, false otherwise
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEquals(URL url, String name, String compareString) {
        String value = getPropertyValue(url, name);

        if (value == null) return false;
        return value.trim().equals(compareString);
    }

    /** Compares Ignoring Case the specified property to the compareString, returns true if they are the same, false otherwise
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEqualsIgnoreCase(URL url, String name, String compareString) {
        String value = getPropertyValue(url, name);

        if (value == null) return false;
        return value.trim().equalsIgnoreCase(compareString);
    }

    /** Returns the value of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultValue is returned.
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param defaultValue The value to return if the property is not found
     * @return The value of the property in the properties file, or if not found then the defaultValue
     */
    public static String getPropertyValue(URL url, String name, String defaultValue) {
        String value = getPropertyValue(url, name);

        if (value == null || value.length() <= 0)
            return defaultValue;
        else
            return value;
    }

    public static double getPropertyNumber(URL url, String name) {
        String str = getPropertyValue(url, name);
        double strValue = 0.00000;

        try {
            strValue = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {}
        return strValue;
    }

    /** Returns the value of the specified property name from the specified resource/properties file
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @return The value of the property in the properties file
     */
    public static String getPropertyValue(URL url, String name) {
        if (url == null) return "";
        if (name == null || name.length() <= 0) return "";
        FlexibleProperties properties = (FlexibleProperties) urlCache.get(url);

        if (properties == null) {
            try {
                properties = FlexibleProperties.makeFlexibleProperties(url);
                urlCache.put(url, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }
        if (properties == null) {
            Debug.log("[UtilProperties.getPropertyValue] could not find resource: " + url, module);
            return null;
        }

        String value = null;

        try {
            value = properties.getProperty(name);
        } catch (Exception e) {
            Debug.log(e.getMessage(), module);
        }
        return value == null ? "" : value.trim();
    }

    /** Returns the value of a split property name from the specified resource/properties file
     * Rather than specifying the property name the value of a name.X property is specified which
     * will correspond to a value.X property whose value will be returned. X is a number from 1 to
     * whatever and all values are checked until a name.X for a certain X is not found.
     * @param url URL object specifying the location of the resource
     * @param name The name of the split property in the properties file
     * @return The value of the split property from the properties file
     */
    public static String getSplitPropertyValue(URL url, String name) {
        if (url == null) return "";
        if (name == null || name.length() <= 0) return "";

        FlexibleProperties properties = (FlexibleProperties) urlCache.get(url);

        if (properties == null) {
            try {
                properties = FlexibleProperties.makeFlexibleProperties(url);
                urlCache.put(url, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }
        if (properties == null) {
            Debug.log("[UtilProperties.getPropertyValue] could not find resource: " + url, module);
            return null;
        }

        String value = null;

        try {
            int curIdx = 1;
            String curName = null;

            while ((curName = properties.getProperty("name." + curIdx)) != null) {
                if (name.equals(curName)) {
                    value = properties.getProperty("value." + curIdx);
                    break;
                }
                curIdx++;
            }
        } catch (Exception e) {
            Debug.log(e.getMessage(), module);
        }
        return value == null ? "" : value.trim();
    }
    
    /** Sets the specified value of the specified property name to the specified resource/properties file
    * @param resource The name of the resource - must be a file
    * @param name The name of the property in the properties file
    * @param value The value of the property in the properties file */ 
    public static void setPropertyValue(String resource, String name, String value) {
        if (resource == null || resource.length() <= 0) return;
        if (name == null || name.length() <= 0) return;
        FlexibleProperties properties = (FlexibleProperties) resourceCache.get(resource);

        if (properties == null) {
            try {
                URL url = UtilURL.fromResource(resource);

                if (url == null) return;
                properties = FlexibleProperties.makeFlexibleProperties(url);
                resourceCache.put(resource, properties);
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
        }

        if (properties == null) {
            Debug.log("[UtilProperties.setPropertyValue] could not find resource: " + resource, module);
            return;
        }

        try {
            properties.setProperty(name, value);
            FileOutputStream propFile = new FileOutputStream(resource);
            properties.store(propFile,             
            "##############################################################################\n"
            +"# Licensed to the Apache Software Foundation (ASF) under one                   \n"
            +"# or more contributor license agreements.  See the NOTICE file                 \n"
            +"# distributed with this work for additional information                        \n"
            +"# regarding copyright ownership.  The ASF licenses this file                   \n"
            +"# to you under the Apache License, Version 2.0 (the                            \n"
            +"# \"License\"); you may not use this file except in compliance                 \n"
            +"# with the License.  You may obtain a copy of the License at                   \n"
            +"#                                                                              \n"
            +"# http://www.apache.org/licenses/LICENSE-2.0                                   \n"
            +"#                                                                              \n"
            +"# Unless required by applicable law or agreed to in writing,                   \n"
            +"# software distributed under the License is distributed on an                  \n"
            +"# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                     \n"
            +"# KIND, either express or implied.  See the License for the                    \n"
            +"# specific language governing permissions and limitations                      \n"
            +"# under the License.                                                           \n"
            +"###############################################################################\n"
            +"#                                                                              \n"
            +"#Dynamically modified by OFBiz Framework (org.ofbiz.base.util : UtilProperties.setPropertyValue)");
            
            propFile.close();
        } catch (FileNotFoundException e) {
            Debug.log(e, "Unable to located the resource file.", module);
        } catch (IOException e) {
            Debug.logError(e, module);
        }
    }

    // ========= Locale & Resource Based Methods ==========

    /** Returns the value of the specified property name from the specified resource/properties file corresponding to the given locale.
     *  <br/>
     *  <br/> Two reasons why we do not use the FlexibleProperties class for this:
     *  <ul>
     *    <li>Doesn't support flexible locale based naming: try fname_locale (5 letter), then fname_locale (2 letter lang only), then fname</li>
     *    <li>Does not support parent properties/bundles so that if the fname_locale5 file doesn't have it then fname_locale2 is tried, then the fname bundle</li>
     *  </ul>
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Locale locale) {
        if (resource == null || resource.length() <= 0) return "";
        if (name == null || name.length() <= 0) return "";

        Map bundle = getResourceBundleMap(resource, locale);

        if (bundle == null) return "";

        String value = null;
        try {
            value = (String)bundle.get(name);
        } catch (Exception e) {
            Debug.log(e.getMessage(), module);
        }
        return value == null ? "" : value.trim();
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the MessageFormat class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param arguments An array of Objects to insert into the message argument place holders
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Object[] arguments, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (value == null || value.length() == 0) {
            return "";
        } else {
            if (arguments != null && arguments.length > 0) {
                value = MessageFormat.format(value, arguments);
            }
            return value;
        }
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the MessageFormat class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param arguments A List of Objects to insert into the message argument place holders
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, List arguments, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (value == null || value.length() == 0) {
            return "";
        } else {
            if (arguments != null && arguments.size() > 0) {
                value = MessageFormat.format(value, arguments.toArray());
            }
            return value;
        }
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the FlexibleStringExpander class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param context A Map of Objects to insert into the message place holders using the ${} syntax of the FlexibleStringExpander
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Map context, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (value == null || value.length() == 0) {
            return "";
        } else {
            if (context != null && context.size() > 0) {
                value = FlexibleStringExpander.expandString(value, context, locale);
            }
            return value;
        }
    }

    /** Returns the specified resource/properties file as a ResourceBundle
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return The ResourceBundle
     */
    public static ResourceBundle getResourceBundle(String resource, Locale locale) {
        ResourceBundleMapWrapper.InternalRbmWrapper bundleMap = getInternalRbmWrapper(resource, locale);
        if (bundleMap == null) {
            return null;
        }
        ResourceBundle theBundle = bundleMap.getResourceBundle();
        return theBundle;
    }

    /** Returns the specified resource/properties file as a Map with the original ResourceBundle in the Map under the key _RESOURCE_BUNDLE_
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return Map containing all entries in The ResourceBundle
     */
    public static Map getResourceBundleMap(String resource, Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null");
        }

        ResourceBundleMapWrapper.InternalRbmWrapper bundleMap = getInternalRbmWrapper(resource, locale);
        return new ResourceBundleMapWrapper(bundleMap);
    }

    public static ResourceBundleMapWrapper.InternalRbmWrapper getInternalRbmWrapper(String resource, Locale locale) {
        String resourceCacheKey = resource + "_" + locale.toString();
        ResourceBundleMapWrapper.InternalRbmWrapper bundleMap = (ResourceBundleMapWrapper.InternalRbmWrapper) bundleLocaleCache.get(resourceCacheKey);
        if (bundleMap == null) {
            synchronized (UtilProperties.class) {
                bundleMap = (ResourceBundleMapWrapper.InternalRbmWrapper) bundleLocaleCache.get(resourceCacheKey);
                if (bundleMap == null) {
                    ResourceBundle bundle = getBaseResourceBundle(resource, locale);
                    if (bundle == null) {
                        throw new IllegalArgumentException("Could not find resource bundle [" + resource + "] in the locale [" + locale + "]");
                    }
                    bundleMap = new ResourceBundleMapWrapper.InternalRbmWrapper(bundle);
                    if (bundleMap != null) {
                        bundleLocaleCache.put(resourceCacheKey, bundleMap);
                    }
                }
            }
        }
        return bundleMap;
    }

    protected static Set resourceNotFoundMessagesShown = FastSet.newInstance();
    protected static ResourceBundle getBaseResourceBundle(String resource, Locale locale) {
        if (resource == null || resource.length() <= 0) return null;
        if (locale == null) locale = Locale.getDefault();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle(resource, locale, loader);
        } catch (MissingResourceException e) {
            String resourceFullName = resource + "_" + locale.toString();
            if (!resourceNotFoundMessagesShown.contains(resourceFullName)) {
                resourceNotFoundMessagesShown.add(resourceFullName);
                Debug.log("[UtilProperties.getPropertyValue] could not find resource: " + resource + " for locale " + locale.toString() + ": " + e.toString(), module);
                return null;
            }
        }
        if (bundle == null) {
            String resourceFullName = resource + "_" + locale.toString();
            if (!resourceNotFoundMessagesShown.contains(resourceFullName)) {
                resourceNotFoundMessagesShown.add(resourceFullName);
                Debug.log("[UtilProperties.getPropertyValue] could not find resource: " + resource + " for locale " + locale.toString(), module);
                return null;
            }
        }

        return bundle;
    }

    /** Returns the specified resource/properties file
     *
     * NOTE: This is NOT fully implemented yet to fulfill all of the requirements
     *  for i18n messages. Do NOT use.
     *
     * To be used in an i18n context this still needs to be extended quite
     *  a bit. The behavior needed is that for each getMessage the most specific
     *  locale (with fname_en_US for instance) is searched first, then the next
     *  less specific (fname_en for instance), then without the locale if it is
     *  still not found (plain fname for example, not that these examples would
     *  have .properties appended to them).
     * This would be accomplished by returning the following structure:
     *    1. Get "fname" FlexibleProperties object
     *    2. Get the "fname_en" FlexibleProperties object and if the "fname" one
     *      is not null, set it as the default/parent of the "fname_en" object
     *    3. Get the "fname_en_US" FlexibleProperties object and if the
     *      "fname_en" one is not null, set it as the default/parent of the
     *      "fname_en_US" object; if the "fname_en" one is null, but the "fname"
     *      one is not, set the "fname" object as the default/parent of the
     *      "fname_en_US" object
     * Then return the fname_en_US object if not null, else the fname_en, else the fname.
     *
     * To make this all more fun, the default locale should be the parent of
     *  the "fname" object in this example so that there is an even higher
     *  chance of finding something for each request.
     *
     * For efficiency all of these should be cached indendependently so the same
     *  instance can be shared, speeding up loading time/efficiency.
     *
     * All of this should work with the setDefaultProperties method of the
     *  FlexibleProperties class, but it should be tested and updated as
     *  necessary. It's a bit tricky, so chances are it won't work as desired...
     *
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return The Properties class
     */
    public static Properties getProperties(String resource, Locale locale) {
        if (resource == null || resource.length() <= 0) return null;
        if (locale == null) locale = Locale.getDefault();

        String localeString = locale.toString();
        String resourceLocale = resource + "_" + localeString;
        Properties properties = (FlexibleProperties) resourceCache.get(resourceLocale);

        if (properties == null) {
            try {
                URL url = UtilURL.fromResource(resourceLocale);
                if (url == null) {
                    properties = getProperties(resource);
                } else {
                    properties = FlexibleProperties.makeFlexibleProperties(url);
                }
            } catch (MissingResourceException e) {
                Debug.log(e.getMessage(), module);
            }
            resourceCache.put(resourceLocale, properties);
        }

        if (properties == null)
            Debug.logInfo("[UtilProperties.getProperties] could not find resource: " + resource + ", locale: " + locale, module);

        return properties;
    }
}
