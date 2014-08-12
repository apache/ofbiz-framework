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
package org.ofbiz.entity.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;

@SuppressWarnings("serial")
public class EntityUtilProperties implements Serializable {

    public final static String module = EntityUtilProperties.class.getName();
    
    protected static String getSystemPropertyValue(String resource, String name, Delegator delegator) {
        if (resource == null || resource.length() <= 0) {
            return null;
        }
        if (name == null || name.length() <= 0) return null;
        
        resource = resource.replace(".properties", "");
        
        // find system property
        try {
            GenericValue systemProperty = delegator.findOne("SystemProperty", UtilMisc.toMap("systemResourceId", resource, "systemPropertyId", name), true);
            if (systemProperty != null) {
                String systemPropertyValue = systemProperty.getString("systemPropertyValue");
                if (UtilValidate.isNotEmpty(systemPropertyValue)) {
                    return systemPropertyValue;
                }
            }
        } catch (Exception e) {
            Debug.logWarning("Could not get a system property for " + name + " : " + e.getMessage(), module);
        }
        return null;
    }
    
    public static boolean propertyValueEquals(String resource, String name, String compareString) {
        return UtilProperties.propertyValueEquals(resource, name, compareString);
    }

    public static boolean propertyValueEqualsIgnoreCase(String resource, String name, String compareString, Delegator delegator) {
        String value = getSystemPropertyValue(resource, name, delegator);
        if (UtilValidate.isNotEmpty(value)) {
            return value.trim().equalsIgnoreCase(compareString);
        } else {
            return UtilProperties.propertyValueEqualsIgnoreCase(resource, name, compareString);
        }
    }

    public static String getPropertyValue(String resource, String name, String defaultValue, Delegator delegator) {
        String value = getSystemPropertyValue(resource, name, delegator);
        if (UtilValidate.isEmpty(value)) {
            value = UtilProperties.getPropertyValue(resource, name, defaultValue);
        }
        return value;
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
        String value = getSystemPropertyValue(resource, name, delegator);
        if (UtilValidate.isEmpty(value)) {
            value = UtilProperties.getPropertyValue(resource, name);
        }
        return value;
    }

    public static Properties getProperties(String resource) {
        return UtilProperties.getProperties(resource);
    }

    public static Properties getProperties(URL url) {
        return UtilProperties.getProperties(url);
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

     public static void setPropertyValue(String resource, String name, String value) {
         UtilProperties.setPropertyValue(resource, name, value);
     }

      public static void setPropertyValueInMemory(String resource, String name, String value) {
          UtilProperties.setPropertyValueInMemory(resource, name, value);
      }

    public static String getMessage(String resource, String name, Locale locale, Delegator delegator) {
        String value = getSystemPropertyValue(resource, name, delegator);
        if (UtilValidate.isEmpty(value)) {
            value = UtilProperties.getMessage(resource, name, locale);
        }
        return value;
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

    public static Properties xmlToProperties(InputStream in, Locale locale, Properties properties) throws IOException, InvalidPropertiesFormatException {
        return UtilProperties.xmlToProperties(in, locale, properties);
    }
}
