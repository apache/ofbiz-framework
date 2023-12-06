package org.apache.ofbiz.base.util;

import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class UtilPropertiesRuntime {
    /**
     * Sets the specified value of the specified property name to the specified resource/properties in memory, does not persist it
     * @param delegator Default delegator, mostly used in tests
     * @param resource The name of the resource
     * @param name     The name of the property in the resource
     * @param value    The value of the property to set in memory
     */
    public static void setPropertyValueInMemory(Delegator delegator, String resource, String name, String value) {
        if (UtilValidate.isEmpty(resource)) {
            return;
        }
        if (UtilValidate.isEmpty(name)) {
            return;
        }

        Properties properties = EntityUtilProperties.getProperties(delegator, resource);
        if (properties == null) {
            return;
        }
        properties.setProperty(name, value);
    }

    /** Returns the specified resource/properties file as a Map with the original
     *  ResourceBundle in the Map under the key _RESOURCE_BUNDLE_
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return Map containing all entries in The ResourceBundle
     */
    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale) {
        return new ResourceBundleMapWrapper(UtilProperties.getResourceBundle(resource, locale));
    }

    /** Returns the specified resource/properties file as a Map with the original
     *  ResourceBundle in the Map under the key _RESOURCE_BUNDLE_
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @param context The screen rendering context
     * @return Map containing all entries in The ResourceBundle
     */
    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale, Map<String, Object> context) {
        return new ResourceBundleMapWrapper(UtilProperties.getResourceBundle(resource, locale), context);
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the FlexibleStringExpander class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param context A Map of Objects to insert into the message place holders using the ${} syntax of the FlexibleStringExpander
     * @param locale The locale that the given resource will correspond to
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Map<String, ? extends Object> context, Locale locale) {
        String value = UtilProperties.getMessage(resource, name, locale);

        if (UtilValidate.isEmpty(value)) {
            return "";
        }
        if (UtilValidate.isNotEmpty(context)) {
            value = FlexibleStringExpander.expandString(value, context, locale);
        }
        return value;
    }

    public static String getMessageMap(String resource, String name, Locale locale, Object... context) {
        return getMessage(resource, name, UtilMisc.toMap(context), locale);
    }
}
