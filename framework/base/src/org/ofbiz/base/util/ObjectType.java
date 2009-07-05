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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.nio.*;

import org.w3c.dom.Node;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

/**
 * Utilities for analyzing and converting Object types in Java
 * Takes advantage of reflection
 */
public class ObjectType {

    public static final String module = ObjectType.class.getName();

    public static final Object NULL = new NullObject();

    protected static Map<String, Class<?>> classCache = FastMap.newInstance();

    public static final String LANG_PACKAGE = "java.lang."; // We will test both the raw value and this + raw value
    public static final String SQL_PACKAGE = "java.sql.";   // We will test both the raw value and this + raw value

    /**
     * Loads a class with the current thread's context classloader.
     * @param className The name of the class to load
     * @return The requested class
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
        Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

        if (theClass != null) return theClass;

        return loadClass(className, null);
    }

    /**
     * Loads a class with the current thread's context classloader.
     * @param className The name of the class to load
     * @param loader The ClassLoader to use
     * @return The requested class
     * @throws ClassNotFoundException
     */
    public static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
        // Handle array classes. Details in http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/types.html#wp16437
        if (className.endsWith("[]")) {
            if (Character.isLowerCase(className.charAt(0)) && className.indexOf(".") < 0) {
               String prefix = className.substring(0, 1).toUpperCase();
               // long and boolean have other prefix than first letter
               if (className.startsWith("long")) {
                   prefix = "J";
               } else if (className.startsWith("boolean")) {
                   prefix = "Z";
               }
               className = "[" + prefix;
            } else {
                Class arrayClass = loadClass(className.replace("[]", ""), loader);
                className = "[L" + arrayClass.getName().replace("[]", "") + ";";
            }
        }

        // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
        Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

        if (theClass != null) return theClass;

        if (loader == null) loader = Thread.currentThread().getContextClassLoader();

        try {
            theClass = loader.loadClass(className);
        } catch (Exception e) {
            theClass = classCache.get(className);
            if (theClass == null) {
                synchronized (ObjectType.class) {
                    theClass = classCache.get(className);
                    if (theClass == null) {
                        theClass = Class.forName(className);
                        if (theClass != null) {
                            if (Debug.verboseOn()) Debug.logVerbose("Loaded Class: " + theClass.getName(), module);
                            classCache.put(className, theClass);
                        }
                    }
                }
            }
        }

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
     */
    public static Object getInstance(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Class<?> c = loadClass(className);
        Object o = c.newInstance();

        if (Debug.verboseOn()) Debug.logVerbose("Instantiated object: " + o.toString(), module);
        return o;
    }

    /**
     * Tests if a class properly implements the specified interface.
     * @param objectClass Class to test
     * @param interfaceName Name of the interface to test against
     * @return true if interfaceName is an interface of objectClass
     * @throws ClassNotFoundException
     */
    public static boolean interfaceOf(Class<?> objectClass, String interfaceName) throws ClassNotFoundException {
        Class<?> interfaceClass = loadClass(interfaceName);

        return interfaceOf(objectClass, interfaceClass);
    }

    /**
     * Tests if a class properly implements the specified interface.
     * @param objectClass Class to test
     * @param interfaceObject to test against
     * @return true if interfaceObject is an interface of the objectClass
     */
    public static boolean interfaceOf(Class<?> objectClass, Object interfaceObject) {
        Class<?> interfaceClass = interfaceObject.getClass();

        return interfaceOf(objectClass, interfaceClass);
    }

    /**
     * Returns an instance of the specified class using the constructor matching the specified parameters.
     * @param className Name of the class to instantiate
     * @param parameters Parameters passed to the constructor
     * @return An instance of the className
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static Object getInstance(String className, Object[] parameters) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?>[] sig = new Class<?>[parameters.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = parameters[i].getClass();
        }
        Class<?> c = loadClass(className);
        Constructor con = c.getConstructor(sig);
        Object o = con.newInstance(parameters);

        if (Debug.verboseOn()) Debug.logVerbose("Instantiated object: " + o.toString(), module);
        return o;
    }

    /**
     * Tests if an object properly implements the specified interface.
     * @param obj Object to test
     * @param interfaceName Name of the interface to test against
     * @return true if interfaceName is an interface of obj
     * @throws ClassNotFoundException
     */
    public static boolean interfaceOf(Object obj, String interfaceName) throws ClassNotFoundException {
        Class<?> interfaceClass = loadClass(interfaceName);

        return interfaceOf(obj, interfaceClass);
    }

    /**
     * Tests if an object properly implements the specified interface.
     * @param obj Object to test
     * @param interfaceObject to test against
     * @return true if interfaceObject is an interface of obj
     */
    public static boolean interfaceOf(Object obj, Object interfaceObject) {
        Class<?> interfaceClass = interfaceObject.getClass();

        return interfaceOf(obj, interfaceClass);
    }

    /**
     * Tests if an object properly implements the specified interface.
     * @param obj Object to test
     * @param interfaceClass Class to test against
     * @return true if interfaceClass is an interface of obj
     */
    public static boolean interfaceOf(Object obj, Class<?> interfaceClass) {
        Class<?> objectClass = obj.getClass();

        return interfaceOf(objectClass, interfaceClass);
    }

    /**
     * Tests if a class properly implements the specified interface.
     * @param objectClass Class to test
     * @param interfaceClass Class to test against
     * @return true if interfaceClass is an interface of objectClass
     */
    public static boolean interfaceOf(Class<?> objectClass, Class<?> interfaceClass) {
        while (objectClass != null) {
            Class<?>[] ifaces = objectClass.getInterfaces();

            for (Class<?> iface: ifaces) {
                if (iface == interfaceClass) return true;
            }
            objectClass = objectClass.getSuperclass();
        }
        return false;
    }

    /**
     * Tests if a class is a class of or a sub-class of the parent.
     * @param objectClass Class to test
     * @param parentName Name of the parent class to test against
     * @return true if objectClass is a class of or a sub-class of the parent
     * @throws ClassNotFoundException
     */
    public static boolean isOrSubOf(Class<?> objectClass, String parentName) throws ClassNotFoundException {
        Class<?> parentClass = loadClass(parentName);

        return isOrSubOf(objectClass, parentClass);
    }

    /**
     * Tests if a class is a class of or a sub-class of the parent.
     * @param objectClass Class to test
     * @param parentObject Object to test against
     * @return true if objectClass is a class of or a sub-class of the parent
     */
    public static boolean isOrSubOf(Class<?> objectClass, Object parentObject) {
        Class<?> parentClass = parentObject.getClass();

        return isOrSubOf(objectClass, parentClass);
    }

    /**
     * Tests if an object is an instance of or a sub-class of the parent.
     * @param obj Object to test
     * @param parentName Name of the parent class to test against
     * @return true if obj is an instance of or a sub-class of the parent
     * @throws ClassNotFoundException
     */
    public static boolean isOrSubOf(Object obj, String parentName) throws ClassNotFoundException {
        Class<?> parentClass = loadClass(parentName);

        return isOrSubOf(obj, parentClass);
    }

    /**
     * Tests if an object is an instance of or a sub-class of the parent.
     * @param obj Object to test
     * @param parentObject Object to test against
     * @return true if obj is an instance of or a sub-class of the parent
     */
    public static boolean isOrSubOf(Object obj, Object parentObject) {
        Class<?> parentClass = parentObject.getClass();

        return isOrSubOf(obj, parentClass);
    }

    /**
     * Tests if an object is an instance of or a sub-class of the parent.
     * @param obj Object to test
     * @param parentClass Class to test against
     * @return true if obj is an instance of or a sub-class of the parent
     */
    public static boolean isOrSubOf(Object obj, Class<?> parentClass) {
        Class<?> objectClass = obj.getClass();

        return isOrSubOf(objectClass, parentClass);
    }

    /**
     * Tests if a class is a class of or a sub-class of the parent.
     * @param objectClass Class to test
     * @param parentClass Class to test against
     * @return true if objectClass is a class of or a sub-class of the parent
     */
    public static boolean isOrSubOf(Class<?> objectClass, Class<?> parentClass) {
        //Debug.logInfo("Checking isOrSubOf for [" + objectClass.getName() + "] and [" + objectClass.getName() + "]", module);
        while (objectClass != null) {
            if (objectClass == parentClass) return true;
            objectClass = objectClass.getSuperclass();
        }
        return false;
    }

    /**
     * Tests if a class is a class of a sub-class of or properly implements an interface.
     * @param objectClass Class to test
     * @param typeObject Object to test against
     * @return true if objectClass is a class of a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Class<?> objectClass, Object typeObject) {
        Class<?> typeClass = typeObject.getClass();

        return instanceOf(objectClass, typeClass);
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
     * @param typeObject Object to test against
     * @return true if obj is an instance of a sub-class of, or properly implements an interface
     */
    public static boolean instanceOf(Object obj, Object typeObject) {
        Class<?> typeClass = typeObject.getClass();

        return instanceOf(obj, typeClass);
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

        if (infoClass == null)
            throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");

        return instanceOf(objectClass, infoClass);
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

        return instanceOf(obj, infoClass);
    }

    public static Class<?> loadInfoClass(String typeName, ClassLoader loader) {
        //Class infoClass = null;
        try {
            return ObjectType.loadClass(typeName, loader);
        } catch (SecurityException se1) {
            throw new IllegalArgumentException("Problems with classloader: security exception (" +
                    se1.getMessage() + ")");
        } catch (ClassNotFoundException e1) {
            try {
                return ObjectType.loadClass(LANG_PACKAGE + typeName, loader);
            } catch (SecurityException se2) {
                throw new IllegalArgumentException("Problems with classloader: security exception (" +
                        se2.getMessage() + ")");
            } catch (ClassNotFoundException e2) {
                try {
                    return ObjectType.loadClass(SQL_PACKAGE + typeName, loader);
                } catch (SecurityException se3) {
                    throw new IllegalArgumentException("Problems with classloader: security exception (" +
                            se3.getMessage() + ")");
                } catch (ClassNotFoundException e3) {
                    throw new IllegalArgumentException("Cannot find and load the class of type: " + typeName +
                            " or of type: " + LANG_PACKAGE + typeName + " or of type: " + SQL_PACKAGE + typeName +
                            ":  (" + e3.getMessage() + ")");
                }
            }
        }
    }

    /**
     * Tests if an object is an instance of a sub-class of or properly implements an interface.
     * @param obj Object to test
     * @param typeClass Class to test against
     * @return true if obj is an instance of a sub-class of typeClass
     */
    public static boolean instanceOf(Object obj, Class<?> typeClass) {
        if (obj == null) return true;
        Class<?> objectClass = obj.getClass();
        return instanceOf(objectClass, typeClass);
    }

    /**
     * Tests if a class is a class of a sub-class of or properly implements an interface.
     * @param objectClass Class to test
     * @param typeClass Class to test against
     * @return true if objectClass is a class or sub-class of, or implements typeClass
     */
    public static boolean instanceOf(Class<?> objectClass, Class<?> typeClass) {
        if (typeClass.isInterface()) {
            return interfaceOf(objectClass, typeClass);
        } else {
            return isOrSubOf(objectClass, typeClass);
        }
    }

    public static Object simpleTypeConvert(Object obj, String type, String format, Locale locale, boolean noTypeFail) throws GeneralException {
        return simpleTypeConvert(obj, type, format, null, locale, noTypeFail);
    }

    /**
     * Converts the passed object to the named simple type.  Supported types
     * include: String, Boolean, Double, Float, Long, Integer, Date (java.sql.Date),
     * Time, Timestamp, TimeZone;
     * @param obj Object to convert
     * @param type Name of type to convert to
     * @param format Optional (can be null) format string for Date, Time, Timestamp
     * @param timeZone Optional (can be null) TimeZone for converting dates and times
     * @param locale Optional (can be null) Locale for formatting and parsing Double, Float, Long, Integer
     * @param noTypeFail Fail (Exception) when no type conversion is available, false will return the primary object
     * @return the converted value
     * @throws GeneralException
     */
    public static Object simpleTypeConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail) throws GeneralException {
        if (obj == null) {
            return null;
        }

        if (obj.getClass().getName().equals(type)) {
            return obj;
        }
        if ("PlainString".equals(type)) {
            return obj.toString();
        }
        if ("Object".equals(type) || "java.lang.Object".equals(type)) {
            return obj;
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        String fromType = null;

        if ((type.equals("List") || type.equals("java.util.List")) && obj.getClass().isArray()) {
            List<Object> newObj = FastList.newInstance();
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                newObj.add(Array.get(obj, i));
            }
            return newObj;
        } else if (obj instanceof java.lang.String) {
            fromType = "String";
            String str = (String) obj;
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                return obj;
            }
            if (str.length() == 0) {
                return null;
            }

            if ("Boolean".equals(type) || "java.lang.Boolean".equals(type)) {
                str = StringUtil.removeSpaces(str);
                return str.equalsIgnoreCase("TRUE") ? Boolean.TRUE : Boolean.FALSE;
            } else if ("Locale".equals(type) || "java.util.Locale".equals(type)) {
                Locale loc = UtilMisc.parseLocale(str);
                if (loc != null) {
                    return loc;
                } else {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ");
                }
            } else if ("TimeZone".equals(type) || "java.util.TimeZone".equals(type)) {
                TimeZone tz = UtilDateTime.toTimeZone(str);
                if (tz != null) {
                    return tz;
                } else {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ");
                }
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                str = StringUtil.removeSpaces(str);
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    Number tempNum = nf.parse(str);
                    return new BigDecimal(tempNum.toString());
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                str = StringUtil.removeSpaces(str);
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    Number tempNum = nf.parse(str);

                    return Double.valueOf(tempNum.doubleValue());
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                str = StringUtil.removeSpaces(str);
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    Number tempNum = nf.parse(str);

                    return Float.valueOf(tempNum.floatValue());
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                str = StringUtil.removeSpaces(str);
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    nf.setMaximumFractionDigits(0);
                    Number tempNum = nf.parse(str);

                    return Long.valueOf(tempNum.longValue());
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                str = StringUtil.removeSpaces(str);
                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(locale);
                    nf.setMaximumFractionDigits(0);
                    Number tempNum = nf.parse(str);

                    return Integer.valueOf(tempNum.intValue());
                } catch (ParseException e) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e);
                }
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
                } else {
                    df = UtilDateTime.toDateFormat(format, timeZone, locale);
                }
                try {
                    Date fieldDate = df.parse(str);
                    return new java.sql.Date(fieldDate.getTime());
                } catch (ParseException e1) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e1);
                }
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
                } else {
                    df = UtilDateTime.toTimeFormat(format, timeZone, locale);
                }
                try {
                    Date fieldDate = df.parse(str);
                    return new java.sql.Time(fieldDate.getTime());
                } catch (ParseException e1) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e1);
                }
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
                    // if time is missing add zeros
                    if (str.length() > 0 && !str.contains(":")) {
                        str = str + " 00:00:00.00";
                    }
                    // hack to mimic Timestamp.valueOf() method
                    if (str.length() > 0 && !str.contains(".")) {
                        str = str + ".0";
                    } else {
                        // DateFormat has a funny way of parsing milliseconds:
                        // 00:00:00.2 parses to 00:00:00.002
                        // so we'll add zeros to the end to get 00:00:00.200
                        String[] timeSplit = str.split("[.]");
                        if (timeSplit.length > 1 && timeSplit[1].length() < 3) {
                            str = str + "000".substring(timeSplit[1].length());
                        }
                    }
                } else {
                    df = UtilDateTime.toDateTimeFormat(format, timeZone, locale);
                }
                try {
                    Date fieldDate = df.parse(str);
                    return new java.sql.Timestamp(fieldDate.getTime());
                } catch (ParseException e1) {
                    throw new GeneralException("Could not convert " + str + " to " + type + ": ", e1);
                }
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                if (str.startsWith("[") && str.endsWith("]")) {
                    return StringUtil.toList(str);
                } else {
                    List<String> tempList = FastList.newInstance();
                    tempList.add(str);
                    return tempList;
                }
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                if (str.startsWith("[") && str.endsWith("]")) {
                    return StringUtil.toSet(str);
                } else {
                    Set<String> tempSet = FastSet.newInstance();
                    tempSet.add(str);
                    return tempSet;
                }
            } else if (("Map".equals(type) || "java.util.Map".equals(type)) &&
                    (str.startsWith("{") && str.endsWith("}"))) {
                return StringUtil.toMap(str);
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof Double) {
            fromType = "Double";
            Double dbl = (Double) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                return nf.format(dbl.doubleValue());
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                return new BigDecimal(dbl.doubleValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return obj;
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return Float.valueOf(dbl.floatValue());
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Long.valueOf(Math.round(dbl.doubleValue()));
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return Integer.valueOf((int) Math.round(dbl.doubleValue()));
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Double> tempList = FastList.newInstance();
                tempList.add(dbl);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Double> tempSet = FastSet.newInstance();
                tempSet.add(dbl);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof Float) {
            fromType = "Float";
            Float flt = (Float) obj;

            if ("String".equals(type)) {
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                return nf.format(flt.doubleValue());
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                return new BigDecimal(flt.doubleValue());
            } else if ("Double".equals(type)) {
                return Double.valueOf(flt.doubleValue());
            } else if ("Float".equals(type)) {
                return obj;
            } else if ("Long".equals(type)) {
                return Long.valueOf(Math.round(flt.doubleValue()));
            } else if ("Integer".equals(type)) {
                return Integer.valueOf((int) Math.round(flt.doubleValue()));
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Float> tempList = FastList.newInstance();
                tempList.add(flt);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Float> tempSet = FastSet.newInstance();
                tempSet.add(flt);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof Long) {
            fromType = "Long";
            Long lng = (Long) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                return nf.format(lng.longValue());
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                return BigDecimal.valueOf(lng.longValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return Double.valueOf(lng.doubleValue());
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return Float.valueOf(lng.floatValue());
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return obj;
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return Integer.valueOf(lng.intValue());
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Long> tempList = FastList.newInstance();
                tempList.add(lng);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Long> tempSet = FastSet.newInstance();
                tempSet.add(lng);
                return tempSet;
            } else if ("Date".equals(type) || "java.util.Date".equals(type)) {
                return new Date(lng.longValue());
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return new java.sql.Timestamp(lng.longValue());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof Integer) {
            fromType = "Integer";
            Integer intgr = (Integer) obj;
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                return nf.format(intgr.longValue());
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                return BigDecimal.valueOf(intgr.longValue());
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return Double.valueOf(intgr.doubleValue());
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return Float.valueOf(intgr.floatValue());
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Long.valueOf(intgr.longValue());
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return obj;
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Integer> tempList = FastList.newInstance();
                tempList.add(intgr);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Integer> tempSet = FastSet.newInstance();
                tempSet.add(intgr);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof BigDecimal) {
            fromType = "BigDecimal";
            BigDecimal bigDec = (BigDecimal) obj;
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                return nf.format(bigDec.doubleValue());
            } else if ("BigDecimal".equals(type) || "java.math.BigDecimal".equals(type)) {
                return obj;
            } else if ("Double".equals(type) || "java.lang.Double".equals(type)) {
                return Double.valueOf(bigDec.doubleValue());
            } else if ("Float".equals(type) || "java.lang.Float".equals(type)) {
                return Float.valueOf(bigDec.floatValue());
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Long.valueOf(bigDec.longValue());
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                return Integer.valueOf(bigDec.intValue());
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<BigDecimal> tempList = FastList.newInstance();
                tempList.add(bigDec);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<BigDecimal> tempSet = FastSet.newInstance();
                tempSet.add(bigDec);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Date) {
            fromType = "Date";
            java.sql.Date dte = (java.sql.Date) obj;
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
                } else {
                    df = UtilDateTime.toDateFormat(format, timeZone, locale);
                }
                return df.format(new java.util.Date(dte.getTime()));
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                return obj;
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return new java.sql.Timestamp(dte.getTime());
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<java.sql.Date> tempList = FastList.newInstance();
                tempList.add(dte);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<java.sql.Date> tempSet = FastSet.newInstance();
                tempSet.add(dte);
                return tempSet;
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Long.valueOf(dte.getTime());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Time) {
            fromType = "Time";
            java.sql.Time tme = (java.sql.Time) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
                } else {
                    df = UtilDateTime.toTimeFormat(format, timeZone, locale);
                }
                return df.format(new java.util.Date(tme.getTime()));
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                return obj;
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return new java.sql.Timestamp(tme.getTime());
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<java.sql.Time> tempList = FastList.newInstance();
                tempList.add(tme);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<java.sql.Time> tempSet = FastSet.newInstance();
                tempSet.add(tme);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.sql.Timestamp) {
            fromType = "Timestamp";
            java.sql.Timestamp tme = (java.sql.Timestamp) obj;

            if ("String".equals(type) || "java.lang.String".equals(type)) {
                DateFormat df = null;
                if (format == null || format.length() == 0) {
                    df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
                } else {
                    df = UtilDateTime.toDateTimeFormat(format, timeZone, locale);
                }
                return df.format(new java.util.Date(tme.getTime()));
            } else if ("Date".equals(type) || "java.sql.Date".equals(type)) {
                return new java.sql.Date(tme.getTime());
            } else if ("Time".equals(type) || "java.sql.Time".equals(type)) {
                return new java.sql.Time(tme.getTime());
            } else if ("Timestamp".equals(type) || "java.sql.Timestamp".equals(type)) {
                return obj;
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<java.sql.Timestamp> tempList = FastList.newInstance();
                tempList.add(tme);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<java.sql.Timestamp> tempSet = FastSet.newInstance();
                tempSet.add(tme);
                return tempSet;
            } else if ("Long".equals(type) || "java.lang.Long".equals(type)) {
                return Long.valueOf(tme.getTime());
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.lang.Boolean) {
            fromType = "Boolean";
            Boolean bol = (Boolean) obj;
            if ("Boolean".equals(type) || "java.lang.Boolean".equals(type)) {
                return bol;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return bol.toString();
            } else if ("Integer".equals(type) || "java.lang.Integer".equals(type)) {
                if (bol.booleanValue()) {
                    return Integer.valueOf(1);
                } else {
                    return Integer.valueOf(0);
                }
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Boolean> tempList = FastList.newInstance();
                tempList.add(bol);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Boolean> tempSet = FastSet.newInstance();
                tempSet.add(bol);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.util.Locale) {
            fromType = "Locale";
            Locale loc = (Locale) obj;
            if ("Locale".equals(type) || "java.util.Locale".equals(type)) {
                return loc;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return loc.toString();
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.util.TimeZone) {
            fromType = "TimeZone";
            TimeZone tz = (TimeZone) obj;
            if ("TimeZone".equals(type) || "java.util.TimeZone".equals(type)) {
                return tz;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return tz.getID();
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj.getClass().getName().equals("org.ofbiz.entity.GenericValue")) {
            fromType = "GenericValue";
            if ("GenericValue".equals(type) || "org.ofbiz.entity.GenericValue".equals(type)) {
                return obj;
            } else if ("Map".equals(type) || "java.util.Map".equals(type)) {
                return obj;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return obj.toString();
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Object> tempList = FastList.newInstance();
                tempList.add(obj);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Object> tempSet = FastSet.newInstance();
                tempSet.add(obj);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.util.Map) {
            fromType = "Map";
            Map map = (Map) obj;
            if ("Map".equals(type) || "java.util.Map".equals(type)) {
                return map;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return map.toString();
            } else if ("List".equals(type) || "java.util.List".equals(type)) {
                List<Map> tempList = FastList.newInstance();
                tempList.add(map);
                return tempList;
            } else if ("Set".equals(type) || "java.util.Set".equals(type)) {
                Set<Map> tempSet = FastSet.newInstance();
                tempSet.add(map);
                return tempSet;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.util.List) {
            fromType = "List";
            List list = (List) obj;
            if ("List".equals(type) || "java.util.List".equals(type)) {
                return list;
            } else if ("String".equals(type) || "java.lang.String".equals(type)) {
                return list.toString();
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof java.nio.Buffer) {
            fromType = "Buffer";
            Buffer buffer = (Buffer) obj;
            if ("java.nio.ByteBuffer".equals(type)) {
                return buffer;
            } else {
                throw new GeneralException("Conversion from " + fromType + " to " + type + " not currently supported");
            }
        } else if (obj instanceof Node) {
            Node node = (Node) obj;
            String nodeValue =  node.getTextContent();
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                return nodeValue;
            } else {
                return simpleTypeConvert(nodeValue, type, format, timeZone, locale, noTypeFail);
            }
        } else {
            // we can pretty much always do a conversion to a String, so do that here
            if ("String".equals(type) || "java.lang.String".equals(type)) {
                Debug.logWarning("No special conversion available for " + obj.getClass().getName() + " to String, returning object.toString().", module);
                return obj.toString();
            }

            if (noTypeFail) {
                throw new GeneralException("Conversion from " + obj.getClass().getName() + " to " + type + " not currently supported");
            } else {
                if (Debug.infoOn()) Debug.logInfo("No type conversion available for " + obj.getClass().getName() + " to " + type + ", returning original object.", module);
                return obj;
            }
        }
    }

    public static Object simpleTypeConvert(Object obj, String type, String format, Locale locale) throws GeneralException {
        return simpleTypeConvert(obj, type, format, locale, true);
    }

    public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
        List<Object> messages, Locale locale, ClassLoader loader, boolean value2InlineConstant) {
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) Debug.logVerbose("Comparing value1: \"" + value1 + "\" " + operator + " value2:\"" + value2 + "\"", module);

        try {
            if (!"PlainString".equals(type)) {
                Class<?> clz = ObjectType.loadClass(type, loader);
                type = clz.getName();
            }
        } catch (ClassNotFoundException e) {
            Debug.logWarning("The specified type [" + type + "] is not a valid class or a known special type, may see more errors later because of this: " + e.getMessage(), module);
        }

        // some default behavior for null values, results in a bit cleaner operation
        if ("is-null".equals(operator) && value1 == null) {
            return Boolean.TRUE;
        } else if ("is-not-null".equals(operator) && value1 == null) {
            return Boolean.FALSE;
        } else if ("is-empty".equals(operator) && value1 == null) {
            return Boolean.TRUE;
        } else if ("is-not-empty".equals(operator) && value1 == null) {
            return Boolean.FALSE;
        } else if ("contains".equals(operator) && value1 == null) {
            return Boolean.FALSE;
        }

        int result = 0;

        Object convertedValue2 = null;
        if (value2 != null) {
            Locale value2Locale = locale;
            if (value2InlineConstant) {
                value2Locale = UtilMisc.parseLocale("en");
            }
            try {
                convertedValue2 = ObjectType.simpleTypeConvert(value2, type, format, value2Locale);
            } catch (GeneralException e) {
                Debug.logError(e, module);
                messages.add("Could not convert value2 for comparison: " + e.getMessage());
                return null;
            }
        }

        // have converted value 2, now before converting value 1 see if it is a Collection and we are doing a contains comparison
        if ("contains".equals(operator) && value1 instanceof Collection) {
            Collection col1 = (Collection) value1;
            return col1.contains(convertedValue2) ? Boolean.TRUE : Boolean.FALSE;
        }

        Object convertedValue1 = null;
        try {
            convertedValue1 = ObjectType.simpleTypeConvert(value1, type, format, locale);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            messages.add("Could not convert value1 for comparison: " + e.getMessage());
            return null;
        }

        // handle null values...
        if (convertedValue1 == null || convertedValue2 == null) {
            if ("equals".equals(operator)) {
                return convertedValue1 == null && convertedValue2 == null ? Boolean.TRUE : Boolean.FALSE;
            } else if ("not-equals".equals(operator)) {
                return convertedValue1 == null && convertedValue2 == null ? Boolean.FALSE : Boolean.TRUE;
            } else if ("is-not-empty".equals(operator) || "is-empty".equals(operator)) {
                // do nothing, handled later...
            } else {
                if (convertedValue1 == null) {
                    messages.add("Left value is null, cannot complete compare for the operator " + operator);
                    return null;
                }
                if (convertedValue2 == null) {
                    messages.add("Right value is null, cannot complete compare for the operator " + operator);
                    return null;
                }
            }
        }

        if ("contains".equals(operator)) {
            if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
                String str1 = (String) convertedValue1;
                String str2 = (String) convertedValue2;

                return str1.indexOf(str2) < 0 ? Boolean.FALSE : Boolean.TRUE;
            } else {
                messages.add("Error in XML file: cannot do a contains compare between a String and a non-String type");
                return null;
            }
        } else if ("is-empty".equals(operator)) {
            if (convertedValue1 == null)
                return Boolean.TRUE;
            if (convertedValue1 instanceof String && ((String) convertedValue1).length() == 0)
                return Boolean.TRUE;
            if (convertedValue1 instanceof List && ((List) convertedValue1).size() == 0)
                return Boolean.TRUE;
            if (convertedValue1 instanceof Map && ((Map) convertedValue1).size() == 0)
                return Boolean.TRUE;
            return Boolean.FALSE;
        } else if ("is-not-empty".equals(operator)) {
            if (convertedValue1 == null)
                return Boolean.FALSE;
            if (convertedValue1 instanceof String && ((String) convertedValue1).length() == 0)
                return Boolean.FALSE;
            if (convertedValue1 instanceof List && ((List) convertedValue1).size() == 0)
                return Boolean.FALSE;
            if (convertedValue1 instanceof Map && ((Map) convertedValue1).size() == 0)
                return Boolean.FALSE;
            return Boolean.TRUE;
        }

        if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
            String str1 = (String) convertedValue1;
            String str2 = (String) convertedValue2;

            if (str1.length() == 0 || str2.length() == 0) {
                if ("equals".equals(operator)) {
                    return str1.length() == 0 && str2.length() == 0 ? Boolean.TRUE : Boolean.FALSE;
                } else if ("not-equals".equals(operator)) {
                    return str1.length() == 0 && str2.length() == 0 ? Boolean.FALSE : Boolean.TRUE;
                } else {
                    messages.add("ERROR: Could not do a compare between strings with one empty string for the operator " + operator);
                    return null;
                }
            }
            result = str1.compareTo(str2);
        } else if ("java.lang.Double".equals(type) || "java.lang.Float".equals(type) || "java.lang.Long".equals(type) || "java.lang.Integer".equals(type) || "java.math.BigDecimal".equals(type)) {
            Number tempNum = (Number) convertedValue1;
            double value1Double = tempNum.doubleValue();

            tempNum = (Number) convertedValue2;
            double value2Double = tempNum.doubleValue();

            if (value1Double < value2Double)
                result = -1;
            else if (value1Double > value2Double)
                result = 1;
            else
                result = 0;
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
                if ((value1Boolean.booleanValue() && value2Boolean.booleanValue()) || (!value1Boolean.booleanValue() && !value2Boolean.booleanValue()))
                    result = 0;
                else
                    result = 1;
            } else if ("not-equals".equals(operator)) {
                if ((!value1Boolean.booleanValue() && value2Boolean.booleanValue()) || (value1Boolean.booleanValue() && !value2Boolean.booleanValue()))
                    result = 0;
                else
                    result = 1;
            } else {
                messages.add("Can only compare Booleans using the operators 'equals' or 'not-equals'");
                return null;
            }
        } else if ("java.lang.Object".equals(type)) {
            if (convertedValue1.equals(convertedValue2)) {
                result = 0;
            } else {
                result = 1;
            }
        } else {
            messages.add("Type \"" + type + "\" specified for compare not supported.");
            return null;
        }

        if (verboseOn) Debug.logVerbose("Got Compare result: " + result + ", operator: " + operator, module);
        if ("less".equals(operator)) {
            if (result >= 0)
                return Boolean.FALSE;
        } else if ("greater".equals(operator)) {
            if (result <= 0)
                return Boolean.FALSE;
        } else if ("less-equals".equals(operator)) {
            if (result > 0)
                return Boolean.FALSE;
        } else if ("greater-equals".equals(operator)) {
            if (result < 0)
                return Boolean.FALSE;
        } else if ("equals".equals(operator)) {
            if (result != 0)
                return Boolean.FALSE;
        } else if ("not-equals".equals(operator)) {
            if (result == 0)
                return Boolean.FALSE;
        } else {
            messages.add("Specified compare operator \"" + operator + "\" not known.");
            return null;
        }

        if (verboseOn) Debug.logVerbose("Returning true", module);
        return Boolean.TRUE;
    }

    public static boolean isEmpty(Object value) {
        if (value == null) return true;

        if (value instanceof String) {
            if (((String) value).length() == 0) {
                return true;
            }
        } else if (value instanceof Collection) {
            if (((Collection) value).size() == 0) {
                return true;
            }
        } else if (value instanceof Map) {
            if (((Map) value).size() == 0) {
                return true;
            }
        }
        return false;
    }

    public static final class NullObject {
        public NullObject() { }

        @Override
        public String toString() {
            return "ObjectType.NullObject";
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof NullObject) {
                // should do equality of object? don't think so, just same type
                return true;
            } else {
                return false;
            }
        }
    }
}
