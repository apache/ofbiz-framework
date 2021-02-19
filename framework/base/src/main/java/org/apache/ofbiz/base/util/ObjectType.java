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
package org.apache.ofbiz.base.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.Converter;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.conversion.LocalizedConverter;
import org.apache.ofbiz.base.lang.IsEmpty;
import org.apache.ofbiz.base.lang.SourceMonitored;
import org.w3c.dom.Node;

/**
 * Utilities for analyzing and converting Object types in Java
 * Takes advantage of reflection
 */
public class ObjectType {

    private static final String MODULE = ObjectType.class.getName();

    public static final Object NULL = new NullObject();

    public static final String LANG_PACKAGE = "java.lang."; // We will test both the raw value and this + raw value
    public static final String SQL_PACKAGE = "java.sql.";   // We will test both the raw value and this + raw value

    private static final Map<String, String> CLASS_ALIAS = new HashMap<>();
    private static final Map<String, Class<?>> PRIMITIVES = new HashMap<>();

    static {
        CLASS_ALIAS.put("Object", "java.lang.Object");
        CLASS_ALIAS.put("String", "java.lang.String");
        CLASS_ALIAS.put("Boolean", "java.lang.Boolean");
        CLASS_ALIAS.put("BigDecimal", "java.math.BigDecimal");
        CLASS_ALIAS.put("Double", "java.lang.Double");
        CLASS_ALIAS.put("Float", "java.lang.Float");
        CLASS_ALIAS.put("Long", "java.lang.Long");
        CLASS_ALIAS.put("Integer", "java.lang.Integer");
        CLASS_ALIAS.put("Short", "java.lang.Short");
        CLASS_ALIAS.put("Byte", "java.lang.Byte");
        CLASS_ALIAS.put("Character", "java.lang.Character");
        CLASS_ALIAS.put("Timestamp", "java.sql.Timestamp");
        CLASS_ALIAS.put("Time", "java.sql.Time");
        CLASS_ALIAS.put("Date", "java.sql.Date");
        CLASS_ALIAS.put("Locale", "java.util.Locale");
        CLASS_ALIAS.put("Collection", "java.util.Collection");
        CLASS_ALIAS.put("List", "java.util.List");
        CLASS_ALIAS.put("Set", "java.util.Set");
        CLASS_ALIAS.put("Map", "java.util.Map");
        CLASS_ALIAS.put("HashMap", "java.util.HashMap");
        CLASS_ALIAS.put("TimeZone", "java.util.TimeZone");
        CLASS_ALIAS.put("TimeDuration", "org.apache.ofbiz.base.util.TimeDuration");
        CLASS_ALIAS.put("GenericValue", "org.apache.ofbiz.entity.GenericValue");
        CLASS_ALIAS.put("GenericPK", "org.apache.ofbiz.entity.GenericPK");
        CLASS_ALIAS.put("GenericEntity", "org.apache.ofbiz.entity.GenericEntity");
        PRIMITIVES.put("boolean", Boolean.TYPE);
        PRIMITIVES.put("short", Short.TYPE);
        PRIMITIVES.put("int", Integer.TYPE);
        PRIMITIVES.put("long", Long.TYPE);
        PRIMITIVES.put("float", Float.TYPE);
        PRIMITIVES.put("double", Double.TYPE);
        PRIMITIVES.put("byte", Byte.TYPE);
        PRIMITIVES.put("char", Character.TYPE);
    }

    /**
     * Loads a class with the current thread's context classloader.
     * @param className The name of the class to load
     * @return The requested class
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, null);
    }

    /**
     * Loads a class with the specified classloader.
     * @param className The name of the class to load
     * @param loader The ClassLoader to use
     * @return The requested class
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
        Class<?> theClass = null;
        // if it is a primitive type, return the object from the "PRIMITIVES" map
        if (PRIMITIVES.containsKey(className)) {
            return PRIMITIVES.get(className);
        }

        int genericsStart = className.indexOf("<");
        if (genericsStart != -1) {
            className = className.substring(0, genericsStart);
        }

        // Handle array classes. Details in http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/types.html#wp16437
        if (className.endsWith("[]")) {
            if (Character.isLowerCase(className.charAt(0)) && className.indexOf(".") < 0) {
                String prefix = className.substring(0, 1).toUpperCase(Locale.getDefault());
                // long and boolean have other prefix than first letter
                if (className.startsWith("long")) {
                    prefix = "J";
                } else if (className.startsWith("boolean")) {
                    prefix = "Z";
                }
                className = "[" + prefix;
            } else {
                Class<?> arrayClass = loadClass(className.replace("[]", ""), loader);
                className = "[L" + arrayClass.getName().replace("[]", "") + ";";
            }
        }

        // if className is an alias (e.g. "String") then replace it with the proper class name (e.g. "java.lang.String")
        if (CLASS_ALIAS.containsKey(className)) {
            className = CLASS_ALIAS.get(className);
        }

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        theClass = Class.forName(className, true, loader);

        return theClass;
    }

    /**
     * Returns an instance of the specified class.  This uses the default
     * no-arg constructor to create the instance.
     * @param className Name of the class to instantiate
     * @return An instance of the named class
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static Object getInstance(String className) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> c = loadClass(className);
        Object o = c.getDeclaredConstructor().newInstance();

        if (Debug.verboseOn()) {
            Debug.logVerbose("Instantiated object: " + o.toString(), MODULE);
        }
        return o;
    }

    /**
     * Tests if a class is a class of a sub-class of or properly implements an interface.
     * @param objectClass Class to test
     * @param typeName name to test against
     * @return true if objectClass is a class or a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Class<?> objectClass, String typeName) {
        return instanceOf(objectClass, typeName, null);
    }

    /**
     * Tests if an object is an instance of a sub-class of or properly implements an interface.
     * @param obj Object to test
     * @param typeName name to test against
     * @return true if obj is an instance of a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Object obj, String typeName) {
        return instanceOf(obj, typeName, null);
    }

    /**
     * Tests if a class is a class of a sub-class of or properly implements an interface.
     * @param objectClass Class to test
     * @param typeName Object to test against
     * @param loader
     * @return true if objectClass is a class of a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Class<?> objectClass, String typeName, ClassLoader loader) {
        Class<?> infoClass = loadInfoClass(typeName, loader);

        if (infoClass == null) {
            throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");
        }

        return infoClass.isAssignableFrom(objectClass);
    }

    /**
     * Tests if an object is an instance of a sub-class of or properly implements an interface.
     * @param obj Object to test
     * @param typeName Object to test against
     * @param loader
     * @return true if obj is an instance of a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Object obj, String typeName, ClassLoader loader) {
        Class<?> infoClass = loadInfoClass(typeName, loader);

        if (infoClass == null) {
            throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");
        }

        return obj == null || infoClass.isInstance(obj);
    }

    public static Class<?> loadInfoClass(String typeName, ClassLoader loader) {
        try {
            return loadClass(typeName, loader);
        } catch (SecurityException se1) {
            throw new IllegalArgumentException("Problems with classloader: security exception ("
                    + se1.getMessage() + ")");
        } catch (ClassNotFoundException e1) {
            try {
                return loadClass(LANG_PACKAGE + typeName, loader);
            } catch (SecurityException se2) {
                throw new IllegalArgumentException("Problems with classloader: security exception ("
                        + se2.getMessage() + ")");
            } catch (ClassNotFoundException e2) {
                try {
                    return loadClass(SQL_PACKAGE + typeName, loader);
                } catch (SecurityException se3) {
                    throw new IllegalArgumentException("Problems with classloader: security exception ("
                            + se3.getMessage() + ")");
                } catch (ClassNotFoundException e3) {
                    throw new IllegalArgumentException("Cannot find and load the class of type: " + typeName
                            + " or of type: " + LANG_PACKAGE + typeName + " or of type: " + SQL_PACKAGE + typeName
                            + ":  (" + e3.getMessage() + ")");
                }
            }
        }
    }

    /** See also {@link #simpleTypeOrObjectConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail)}. */
    public static Object simpleTypeOrObjectConvert(Object obj, String type, String format, Locale locale, boolean noTypeFail)
            throws GeneralException {
        return simpleTypeOrObjectConvert(obj, type, format, null, locale, noTypeFail);
    }

    /**
     * Converts the passed object to the named type.
     * Initially created for only simple types but actually handle more types and not all simple types.
     * See ObjectTypeTests class for more, and (normally) up to date information
     * Supported types:
     * - All PRIMITIVES
     * - Simple types: String, Boolean, Double, Float, Long, Integer, BigDecimal.
     * - Other Objects: List, Map, Set, Calendar, Date (java.sql.Date), Time, Timestamp, TimeZone, Date (util.Date and sql.Date)
     * - Simple types (maybe) not handled: Short, BigInteger, Byte, Character, ObjectName and Void...
     * @param obj Object to convert
     * @param type Optional Java class name of type to convert to. A <code>null</code> or empty <code>String</code> will return the original object.
     * @param format Optional (can be null) format string for Date, Time, Timestamp
     * @param timeZone Optional (can be null) TimeZone for converting dates and times
     * @param locale Optional (can be null) Locale for formatting and parsing Double, Float, Long, Integer
     * @param noTypeFail Fail (Exception) when no type conversion is available, false will return the primary object
     * @return the converted value
     * @throws GeneralException
     */
    @SourceMonitored
    @SuppressWarnings("unchecked")
    public static Object simpleTypeOrObjectConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail)
            throws GeneralException {
        if (obj == null || UtilValidate.isEmpty(type) || "Object".equals(type) || "java.lang.Object".equals(type)) {
            return obj;
        }
        if ("PlainString".equals(type)
                || ("org.codehaus.groovy.runtime.GStringImpl".equals(obj.getClass().getName()) && "String".equals(type))) {
            return obj.toString();
        }
        if (obj instanceof Node) {
            Node node = (Node) obj;
            String nodeValue = node.getTextContent();

            if (nodeValue == null) {
                /* We can't get the text value of Document, Document Type and Notation Node,
                 * thus simply returning the same object from simpleTypeOrObjectConvert method.
                 * Please refer to OFBIZ-10832 Jira and
                 * https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Node.html#getTextContent() for more details.
                 */
                short nodeType = node.getNodeType();
                if (Node.DOCUMENT_NODE == nodeType || Node.DOCUMENT_TYPE_NODE == nodeType || Node.NOTATION_NODE == nodeType) {
                    return obj;
                }
            }
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                return nodeValue;
            }
            return simpleTypeOrObjectConvert(nodeValue, type, format, timeZone, locale, noTypeFail);
        }
        int genericsStart = type.indexOf("<");
        if (genericsStart != -1) {
            type = type.substring(0, genericsStart);
        }
        Class<?> sourceClass = obj.getClass();
        Class<?> targetClass = null;
        try {
            targetClass = loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new GeneralException("Conversion from " + sourceClass.getName() + " to " + type + " not currently supported", e);
        }
        if (sourceClass.equals(targetClass)) {
            return obj;
        }
        if (obj instanceof String && ((String) obj).isEmpty()) {
            return null;
        }
        Converter<Object, Object> converter = null;
        try {
            converter = (Converter<Object, Object>) Converters.getConverter(sourceClass, targetClass);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, MODULE);
        }

        if (converter != null) {
            if (converter instanceof LocalizedConverter) {
                LocalizedConverter<Object, Object> localizedConverter = UtilGenerics.cast(converter);
                if (timeZone == null) {
                    timeZone = TimeZone.getDefault();
                }
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                try {
                    return localizedConverter.convert(obj, locale, timeZone, format);
                } catch (ConversionException e) {
                    Debug.logWarning(e, "Exception thrown while converting type: ", MODULE);
                    throw new GeneralException(e.getMessage(), e);
                }
            }
            try {
                return converter.convert(obj);
            } catch (ConversionException e) {
                Debug.logWarning(e, "Exception thrown while converting type: ", MODULE);
                throw new GeneralException(e.getMessage(), e);
            }
        }
        // we can pretty much always do a conversion to a String, so do that here
        if (targetClass.equals(String.class)) {
            if (Debug.infoOn()) {
                Debug.logInfo("No special conversion required for " + obj.getClass().getName() + " to String, returning object.toString().", MODULE);
            }
            return obj.toString();
        }
        if (noTypeFail) {
            throw new GeneralException("Conversion from " + obj.getClass().getName() + " to " + type + " not currently supported");
        }
        if (Debug.infoOn()) {
            Debug.logInfo("No type conversion available for " + obj.getClass().getName() + " to " + targetClass.getName()
                    + ", returning original object.", MODULE);
        }
        return obj;
    }

    /** See also {@link #simpleTypeOrObjectConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail)}. */
    public static Object simpleTypeOrObjectConvert(Object obj, String type, String format, Locale locale) throws GeneralException {
        return simpleTypeOrObjectConvert(obj, type, format, locale, true);
    }

    public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
            List<Object> messages, Locale locale, ClassLoader loader, boolean value2InlineConstant) {
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) {
            Debug.logVerbose("Comparing value1: \"" + value1 + "\" " + operator + " value2:\"" + value2 + "\"", MODULE);
        }

        try {
            if (!"PlainString".equals(type)) {
                Class<?> clz = loadClass(type, loader);
                type = clz.getName();
            }
        } catch (ClassNotFoundException e) {
            Debug.logWarning("The specified type [" + type
                    + "] is not a valid class or a known special type, may see more errors later because of this: " + e.getMessage(), MODULE);
        }

        if (value1 == null) {
            // some default behavior for null values, results in a bit cleaner operation
            if ("is-null".equals(operator) || "is-empty".equals(operator)) {
                return Boolean.TRUE;
            } else if ("is-not-null".equals(operator) || "is-not-empty".equals(operator) || "contains".equals(operator)) {
                return Boolean.FALSE;
            }
        }

        int result = 0;

        Object convertedValue2 = null;
        if (value2 != null) {
            Locale value2Locale = locale;
            if (value2InlineConstant) {
                value2Locale = UtilMisc.parseLocale("en");
            }
            try {
                convertedValue2 = simpleTypeOrObjectConvert(value2, type, format, value2Locale);
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
                messages.add("Could not convert value2 for comparison: " + e.getMessage());
                return Boolean.FALSE;
            }
        }

        // have converted value 2, now before converting value 1 see if it is a Collection and we are doing a contains comparison
        if ("contains".equals(operator) && value1 instanceof Collection<?>) {
            Collection<?> col1 = (Collection<?>) value1;
            return col1.contains(convertedValue2) ? Boolean.TRUE : Boolean.FALSE;
        }

        Object convertedValue1 = null;
        try {
            convertedValue1 = simpleTypeOrObjectConvert(value1, type, format, locale);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            messages.add("Could not convert value1 for comparison: " + e.getMessage());
            return Boolean.FALSE;
        }

        // handle null values...
        if (convertedValue1 == null || convertedValue2 == null) {
            if ("equals".equals(operator)) {
                return convertedValue1 == null && convertedValue2 == null ? Boolean.TRUE : Boolean.FALSE;
            } else if ("not-equals".equals(operator)) {
                return convertedValue1 == null && convertedValue2 == null ? Boolean.FALSE : Boolean.TRUE;
            } else if ("is-not-empty".equals(operator) || "is-empty".equals(operator)) {
                // do nothing, handled later...Logging to avoid checkstyle issue.
                Debug.logInfo("Operator not handled:" + operator, MODULE);
            } else {
                if (convertedValue1 == null) {
                    messages.add("Left value is null, cannot complete compare for the operator " + operator);
                    return Boolean.FALSE;
                }
                if (convertedValue2 == null) {
                    messages.add("Right value is null, cannot complete compare for the operator " + operator);
                    return Boolean.FALSE;
                }
            }
        }

        if ("contains".equals(operator)) {
            if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
                String str1 = (String) convertedValue1;
                String str2 = (String) convertedValue2;

                return str1.indexOf(str2) < 0 ? Boolean.FALSE : Boolean.TRUE;
            }
            messages.add("Error in XML file: cannot do a contains compare between a String and a non-String type");
            return Boolean.FALSE;
        } else if ("is-empty".equals(operator)) {
            if (convertedValue1 == null) {
                return Boolean.TRUE;
            }
            if (convertedValue1 instanceof String && ((String) convertedValue1).isEmpty()) {
                return Boolean.TRUE;
            }
            if (convertedValue1 instanceof List<?> && ((List<?>) convertedValue1).isEmpty()) {
                return Boolean.TRUE;
            }
            if (convertedValue1 instanceof Map<?, ?> && ((Map<?, ?>) convertedValue1).isEmpty()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } else if ("is-not-empty".equals(operator)) {
            if (convertedValue1 == null) {
                return Boolean.FALSE;
            }
            if (convertedValue1 instanceof String && ((String) convertedValue1).isEmpty()) {
                return Boolean.FALSE;
            }
            if (convertedValue1 instanceof List<?> && ((List<?>) convertedValue1).isEmpty()) {
                return Boolean.FALSE;
            }
            if (convertedValue1 instanceof Map<?, ?> && ((Map<?, ?>) convertedValue1).isEmpty()) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
            String str1 = (String) convertedValue1;
            String str2 = (String) convertedValue2;

            if (str1.isEmpty() || str2.isEmpty()) {
                if ("equals".equals(operator)) {
                    return str1.isEmpty() && str2.isEmpty() ? Boolean.TRUE : Boolean.FALSE;
                } else if ("not-equals".equals(operator)) {
                    return str1.isEmpty() && str2.isEmpty() ? Boolean.FALSE : Boolean.TRUE;
                } else {
                    messages.add("ERROR: Could not do a compare between strings with one empty string for the operator " + operator);
                    return Boolean.FALSE;
                }
            }
            result = str1.compareTo(str2);
        } else if ("java.lang.Double".equals(type) || "java.lang.Float".equals(type) || "java.lang.Long".equals(type)
                || "java.lang.Integer".equals(type) || "java.math.BigDecimal".equals(type)) {
            Number tempNum = (Number) convertedValue1;
            double value1Double = tempNum.doubleValue();

            tempNum = (Number) convertedValue2;
            double value2Double = tempNum.doubleValue();

            if (value1Double < value2Double) {
                result = -1;
            } else if (value1Double > value2Double) {
                result = 1;
            } else {
                result = 0;
            }
        } else if ("java.sql.Date".equals(type)) {
            java.sql.Date value1Date = (java.sql.Date) convertedValue1;
            java.sql.Date value2Date = (java.sql.Date) convertedValue2;
            result = value1Date.compareTo(value2Date);
        } else if ("java.sql.Time".equals(type)) {
            java.sql.Time value1Time = (java.sql.Time) convertedValue1;
            java.sql.Time value2Time = (java.sql.Time) convertedValue2;
            result = value1Time.compareTo(value2Time);
        } else if ("java.sql.Timestamp".equals(type)) {
            java.sql.Timestamp value1Timestamp = (java.sql.Timestamp) convertedValue1;
            java.sql.Timestamp value2Timestamp = (java.sql.Timestamp) convertedValue2;
            result = value1Timestamp.compareTo(value2Timestamp);
        } else if ("java.lang.Boolean".equals(type)) {
            Boolean value1Boolean = (Boolean) convertedValue1;
            Boolean value2Boolean = (Boolean) convertedValue2;
            if ("equals".equals(operator)) {
                if ((value1Boolean && value2Boolean) || (!value1Boolean && !value2Boolean)) {
                    result = 0;
                } else {
                    result = 1;
                }
            } else if ("not-equals".equals(operator)) {
                if ((!value1Boolean && value2Boolean) || (value1Boolean && !value2Boolean)) {
                    result = 0;
                } else {
                    result = 1;
                }
            } else {
                messages.add("Can only compare Booleans using the operators 'equals' or 'not-equals'");
                return Boolean.FALSE;
            }
        } else if ("java.lang.Object".equals(type)) {
            if (convertedValue1.equals(convertedValue2)) {
                result = 0;
            } else {
                result = 1;
            }
        } else {
            messages.add("Type \"" + type + "\" specified for compare not supported.");
            return Boolean.FALSE;
        }

        if (verboseOn) {
            Debug.logVerbose("Got Compare result: " + result + ", operator: " + operator, MODULE);
        }
        if ("less".equals(operator)) {
            if (result >= 0) {
                return Boolean.FALSE;
            }
        } else if ("greater".equals(operator)) {
            if (result <= 0) {
                return Boolean.FALSE;
            }
        } else if ("less-equals".equals(operator)) {
            if (result > 0) {
                return Boolean.FALSE;
            }
        } else if ("greater-equals".equals(operator)) {
            if (result < 0) {
                return Boolean.FALSE;
            }
        } else if ("equals".equals(operator)) {
            if (result != 0) {
                return Boolean.FALSE;
            }
        } else if ("not-equals".equals(operator)) {
            if (result == 0) {
                return Boolean.FALSE;
            }
        } else {
            messages.add("Specified compare operator \"" + operator + "\" not known.");
            return Boolean.FALSE;
        }

        if (verboseOn) {
            Debug.logVerbose("Returning true", MODULE);
        }
        return Boolean.TRUE;
    }

    @SuppressWarnings("unchecked")
    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<? extends Object>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<? extends Object, ? extends Object>) value).isEmpty();
        }
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length() == 0;
        }
        if (value instanceof IsEmpty) {
            return ((IsEmpty) value).isEmpty();
        }

        // These types would flood the log
        // Number covers: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short
        if (value instanceof Boolean) {
            return false;
        }
        if (value instanceof Number) {
            return false;
        }
        if (value instanceof Character) {
            return false;
        }
        if (value instanceof java.util.Date) {
            return false;
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("In ObjectType.isEmpty(Object value) returning false for " + value.getClass() + " Object.", MODULE);
        }
        return false;
    }

    @SuppressWarnings("serial")
    public static final class NullObject implements Serializable {
        public NullObject() { }

        @Override
        public String toString() {
            return "ObjectType.NullObject";
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object other) {
                // should do equality of object? don't think so, just same type
            return other instanceof NullObject;
        }
    }
}
