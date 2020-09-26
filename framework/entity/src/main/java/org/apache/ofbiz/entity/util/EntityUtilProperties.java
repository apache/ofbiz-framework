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
package org.apache.ofbiz.entity.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

@SuppressWarnings("serial")
public final class EntityUtilProperties implements Serializable {

    private static final String MODULE = EntityUtilProperties.class.getName();

    private EntityUtilProperties() { }

    private static Map<String, String> getSystemPropertyValue(String resource, String name, Delegator delegator) {
        Map<String, String> results = new HashMap<>();
        results.put("isExistInDb", "N");
        results.put("value", "");

        if (UtilValidate.isEmpty(resource) || UtilValidate.isEmpty(name)) {
            return results;
        }
        resource = resource.replace(".properties", "");
        try {
            GenericValue systemProperty = EntityQuery.use(delegator)
                    .from("SystemProperty")
                    .where("systemResourceId", resource, "systemPropertyId", name)
                    .cache()
                    .queryOne();
            if (systemProperty != null) {
                //property exists in database
                results.put("isExistInDb", "Y");
                results.put("value", (systemProperty.getString("systemPropertyValue") != null)
                        ? systemProperty.getString("systemPropertyValue") : "");
            }
        } catch (GenericEntityException e) {
            Debug.logError("Could not get a system property for " + name + " : " + e.getMessage(), MODULE);
        }
        return results;
    }

    public static boolean propertyValueEquals(String resource, String name, String compareString) {
        return UtilProperties.propertyValueEquals(resource, name, compareString);
    }

    public static boolean propertyValueEqualsIgnoreCase(String resource, String name, String compareString, Delegator delegator) {
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            compareString = (compareString == null) ? "" : compareString;
            return propMap.get("value").equalsIgnoreCase(compareString);
        } else {
            return UtilProperties.propertyValueEqualsIgnoreCase(resource, name, compareString);
        }
    }

    public static String getPropertyValue(String resource, String name, String defaultValue, Delegator delegator) {
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            String s = propMap.get("value");
            return (UtilValidate.isEmpty(s)) ? defaultValue : s;
        } else {
            return UtilProperties.getPropertyValue(resource, name, defaultValue);
        }
    }

    public static String getPropertyValueFromDelegatorName(String resource, String name, String defaultValue, String delegatorName) {
        Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
        if (delegator == null) { // This should not happen, but in case...
            Debug.logError("Could not get a delegator. Using the 'default' delegator", MODULE);
            // this will be the common case for now as the delegator isn't available where we want to do this
            // we'll cheat a little here and assume the default delegator
            delegator = DelegatorFactory.getDelegator("default");
            Debug.logError("Could not get a delegator. Using the 'default' delegator", MODULE);
            if (delegator == null) {
                Debug.logError("Could not get a system property for " + name + ". Reason: the delegator is null", MODULE);
            }
        }
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            String s = propMap.get("value");
            return (UtilValidate.isEmpty(s)) ? defaultValue : s;
        } else {
            return UtilProperties.getPropertyValue(resource, name, defaultValue);
        }
    }

    public static double getPropertyNumber(String resource, String name, double defaultValue) {
        return UtilProperties.getPropertyNumber(resource, name, defaultValue);
    }

    public static double getPropertyNumber(String resource, String name) {
        return UtilProperties.getPropertyNumber(resource, name);
    }

    public static Boolean getPropertyAsBoolean(String resource, String name, boolean defaultValue) {
        return UtilProperties.getPropertyAsBoolean(resource, name, defaultValue);
    }

    public static Integer getPropertyAsInteger(String resource, String name, int defaultNumber) {
        return UtilProperties.getPropertyAsInteger(resource, name, defaultNumber);
    }

    public static Long getPropertyAsLong(String resource, String name, long defaultNumber) {
        return UtilProperties.getPropertyAsLong(resource, name, defaultNumber);
    }

    public static Float getPropertyAsFloat(String resource, String name, float defaultNumber) {
        return UtilProperties.getPropertyAsFloat(resource, name, defaultNumber);
    }

    public static Double getPropertyAsDouble(String resource, String name, double defaultNumber) {
        return UtilProperties.getPropertyAsDouble(resource, name, defaultNumber);
    }

    public static BigInteger getPropertyAsBigInteger(String resource, String name, BigInteger defaultNumber) {
        return UtilProperties.getPropertyAsBigInteger(resource, name, defaultNumber);
    }

    public static BigDecimal getPropertyAsBigDecimal(String resource, String name, BigDecimal defaultNumber) {
        return UtilProperties.getPropertyAsBigDecimal(resource, name, defaultNumber);
    }

    public static String getPropertyValue(String resource, String name, Delegator delegator) {
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            return propMap.get("value");
        } else {
            return UtilProperties.getPropertyValue(resource, name);
        }
    }

    public static String getPropertyValueFromDelegatorName(String resource, String name, String delegatorName) {
        Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
        if (delegator == null) { // This should not happen, but in case...
            Debug.logError("Could not get a delegator. Using the 'default' delegator", MODULE);
            // this will be the common case for now as the delegator isn't available where we want to do this
            // we'll cheat a little here and assume the default delegator
            delegator = DelegatorFactory.getDelegator("default");
            Debug.logError("Could not get a delegator. Using the 'default' delegator", MODULE);
            if (delegator == null) {
                Debug.logError("Could not get a system property for " + name + ". Reason: the delegator is null", MODULE);
            }
        }
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            return propMap.get("value");
        } else {
            return UtilProperties.getPropertyValue(resource, name);
        }
    }

    public static Properties getProperties(String resource) {
        return UtilProperties.getProperties(resource);
    }

    public static Properties getProperties(URL url) {
        return UtilProperties.getProperties(url);
    }

    public static Properties getProperties(Delegator delegator, String resourceName) {
        Properties properties = UtilProperties.getProperties(resourceName);
        List<GenericValue> gvList;
        try {
            gvList = EntityQuery.use(delegator)
                    .from("SystemProperty")
                    .where("systemResourceId", resourceName)
                    .cache()
                    .queryList();
            if (UtilValidate.isNotEmpty(gvList)) {
                for (Iterator<GenericValue> i = gvList.iterator(); i.hasNext();) {
                    GenericValue gv = i.next();
                    if (UtilValidate.isNotEmpty(gv.getString("systemPropertyValue"))) {
                        properties.setProperty(gv.getString("systemPropertyId"), gv.getString("systemPropertyValue"));
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
        return properties;
    }

    public static boolean propertyValueEquals(URL url, String name, String compareString) {
        return UtilProperties.propertyValueEquals(url, name, compareString);
    }

    public static boolean propertyValueEqualsIgnoreCase(URL url, String name, String compareString) {
        return UtilProperties.propertyValueEqualsIgnoreCase(url, name, compareString);
    }

    public static String getPropertyValue(URL url, String name, String defaultValue) {
        return UtilProperties.getPropertyValue(url, name, defaultValue);
    }

    public static double getPropertyNumber(URL url, String name, double defaultValue) {
        return UtilProperties.getPropertyNumber(url, name, defaultValue);
    }

    public static double getPropertyNumber(URL url, String name) {
        return UtilProperties.getPropertyNumber(url, name);
    }

    public static String getPropertyValue(URL url, String name) {
        return UtilProperties.getPropertyValue(url, name);
    }

    public static String getSplitPropertyValue(URL url, String name) {
        return UtilProperties.getSplitPropertyValue(url, name);
    }

    public static void setPropertyValueInMemory(String resource, String name, String value) {
        UtilProperties.setPropertyValueInMemory(resource, name, value);
    }

    public static String setPropertyValue(Delegator delegator, String resourceName, String name, String value) {
        GenericValue gv = null;
        String prevValue = null;
        try {
            gv = EntityQuery.use(delegator)
                    .from("SystemProperty")
                    .where("systemResourceId", resourceName, "systemPropertyId", name)
                    .queryOne();
            if (gv != null) {
                prevValue = gv.getString("systemPropertyValue");
                gv.set("systemPropertyValue", value);
            } else {
                gv = delegator.makeValue("SystemProperty", UtilMisc.toMap("systemResourceId", resourceName, "systemPropertyId",
                        name, "systemPropertyValue", value, "description", null));
            }
            gv.store();
        } catch (GenericEntityException e) {
            Debug.logError(String.format("tenantId=%s, exception=%s, message=%s", delegator.getDelegatorTenantId(), e.getClass().getName(),
                    e.getMessage()), MODULE);
        }
        return prevValue;
    }

    public static String getMessage(String resource, String name, Locale locale, Delegator delegator) {
        Map<String, String> propMap = getSystemPropertyValue(resource, name, delegator);
        if ("Y".equals(propMap.get("isExistInDb"))) {
            return propMap.get("value");
        } else {
            return UtilProperties.getMessage(resource, name, locale);
        }
    }

    public static String getMessage(String resource, String name, Object[] arguments, Locale locale) {
        return UtilProperties.getMessage(resource, name, arguments, locale);
    }

    public static <E> String getMessage(String resource, String name, List<E> arguments, Locale locale) {
        return UtilProperties.getMessage(resource, name, arguments, locale);
    }

    public static String getMessageList(String resource, String name, Locale locale, Object... arguments) {
        return UtilProperties.getMessageList(resource, name, locale, arguments);
    }

    public static String getMessage(String resource, String name, Map<String, ? extends Object> context, Locale locale) {
        return UtilProperties.getMessage(resource, name, context, locale);
    }

    public static String getMessageMap(String resource, String name, Locale locale, Object... context) {
        return UtilProperties.getMessageMap(resource, name, locale, context);
    }

    public static ResourceBundle getResourceBundle(String resource, Locale locale) {
        return UtilProperties.getResourceBundle(resource, locale);
    }

    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale) {
        return UtilProperties.getResourceBundleMap(resource, locale);
    }

    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale, Map<String, Object> context) {
        return UtilProperties.getResourceBundleMap(resource, locale, context);
    }

    public static Properties getProperties(String resource, Locale locale) {
        return UtilProperties.getProperties(resource, locale);
    }

    @Deprecated
    public static Locale getFallbackLocale() {
        return UtilProperties.getFallbackLocale();
    }

    public static List<Locale> localeToCandidateList(Locale locale) {
        return UtilProperties.localeToCandidateList(locale);
    }

    public static Set<Locale> getDefaultCandidateLocales() {
        return UtilProperties.getDefaultCandidateLocales();
    }

    @Deprecated
    public static List<Locale> getCandidateLocales(Locale locale) {
        return UtilProperties.getCandidateLocales(locale);
    }

    public static String createResourceName(String resource, Locale locale, boolean removeExtension) {
        return UtilProperties.createResourceName(resource, locale, removeExtension);
    }

    public static boolean isPropertiesResourceNotFound(String resource, Locale locale, boolean removeExtension) {
        return UtilProperties.isPropertiesResourceNotFound(resource, locale, removeExtension);
    }

    public static URL resolvePropertiesUrl(String resource, Locale locale) {
        return UtilProperties.resolvePropertiesUrl(resource, locale);
    }

    public static Properties xmlToProperties(InputStream in, Locale locale, Properties properties)
            throws IOException, InvalidPropertiesFormatException {
        return UtilProperties.xmlToProperties(in, locale, properties);
    }
}
