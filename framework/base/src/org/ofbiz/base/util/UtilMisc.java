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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.collections.MapComparator;

/**
 * UtilMisc - Misc Utility Functions
 */
public class UtilMisc {

    public static final String module = UtilMisc.class.getName();

    public static final BigDecimal ZERO_BD = BigDecimal.ZERO;

    public static final <T extends Throwable> T initCause(T throwable, Throwable cause) {
        throwable.initCause(cause);
        return throwable;
    }

    public static <T> int compare(Comparable<T> obj1, T obj2) {
        if (obj1 == null) {
            if (obj2 == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return obj1.compareTo(obj2);
        }
    }

    public static <E> int compare(List<E> obj1, List<E> obj2) {
        if (obj1 == obj2) {
            return 0;
        }
        try {
            if (obj1.size() == obj2.size() && obj1.containsAll(obj2) && obj2.containsAll(obj1)) {
                return 0;
            }

        } catch (Exception e) {}
        return 1;
    }

    /**
     * Get an iterator from a collection, returning null if collection is null
     * @param col The collection to be turned in to an iterator
     * @return The resulting Iterator
     */
    public static <T> Iterator<T> toIterator(Collection<T> col) {
        if (col == null)
            return null;
        else
            return col.iterator();
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V> Map<String, V> toMap(String name1, V1 value1) {
        return new UtilMisc.SimpleMap<V>(name1, value1);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V, V2 extends V> Map<String, V> toMap(String name1, V1 value1, String name2, V2 value2) {
        return new UtilMisc.SimpleMap<V>(name1, value1, name2, value2);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         fields.put(name2, value2);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V, V2 extends V, V3 extends V> Map<String, V> toMap(String name1, V1 value1, String name2, V2 value2, String name3, V3 value3) {
        return new UtilMisc.SimpleMap<V>(name1, value1, name2, value2, name3, value3);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         fields.put(name2, value2);
         fields.put(name3, value3);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V, V2 extends V, V3 extends V, V4 extends V> Map<String, V> toMap(String name1, V1 value1, String name2, V2 value2, String name3, V3 value3, String name4, V4 value4) {
        return new UtilMisc.SimpleMap<V>(name1, value1, name2, value2, name3, value3, name4, value4);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         fields.put(name2, value2);
         fields.put(name3, value3);
         fields.put(name4, value4);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V, V2 extends V, V3 extends V, V4 extends V, V5 extends V> Map<String, V> toMap(String name1, V1 value1, String name2, V2 value2, String name3, V3 value3, String name4, V4 value4, String name5, V5 value5) {
        Map<String, V> fields = FastMap.newInstance();

        fields.put(name1, value1);
        fields.put(name2, value2);
        fields.put(name3, value3);
        fields.put(name4, value4);
        fields.put(name5, value5);
        return fields;
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static <V, V1 extends V, V2 extends V, V3 extends V, V4 extends V, V5 extends V, V6 extends V> Map<String, V> toMap(String name1, V1 value1, String name2, V2 value2, String name3, V3 value3, String name4, V4 value4, String name5, V5 value5, String name6, V6 value6) {
        Map<String, V> fields = FastMap.newInstance();

        fields.put(name1, value1);
        fields.put(name2, value2);
        fields.put(name3, value3);
        fields.put(name4, value4);
        fields.put(name5, value5);
        fields.put(name6, value6);
        return fields;
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<String, V> toMap(Object... data) {
        if (data == null) {
            return null;
        }
        if (data.length == 1 && data[0] instanceof Map) {
            // Fix for javac's broken type inferring
            return UtilGenerics.<String, V>checkMap(data[0]);
        }
        if (data.length % 2 == 1) {
            throw new IllegalArgumentException("You must pass an even sized array to the toMap method");
        }
        Map<String, V> map = FastMap.newInstance();
        for (int i = 0; i < data.length;) {
            map.put((String) data[i++], (V) data[i++]);
        }
        return map;
    }

    public static <K, V> String printMap(Map<? extends K, ? extends V> theMap) {
        StringBuilder theBuf = new StringBuilder();
        for (Map.Entry<? extends K, ? extends V> entry: theMap.entrySet()) {
            theBuf.append(entry.getKey());
            theBuf.append(" --> ");
            theBuf.append(entry.getValue());
            theBuf.append(System.getProperty("line.separator"));
        }
        return theBuf.toString();
    }

    public static <T> List<T> makeListWritable(Collection<? extends T> col) {
        List<T> result = FastList.newInstance();
        if (col != null) result.addAll(col);
        return result;
    }

    public static <K, V> Map<K, V> makeMapWritable(Map<K, ? extends V> map) {
        Map<K, V> result = FastMap.newInstance();
        if (map != null) result.putAll(map);
        return result;
    }

    public static <T> Set<T> makeSetWritable(Collection<? extends T> col) {
        Set<T> result = FastSet.newInstance();
        if (col != null) result.addAll(col);
        return result;
    }

    /**
     * This change a Map to be Serializable by removing all entries with values that are not Serializable.
     *
     * @param <V>
     * @param map
     */
    public static <V> void makeMapSerializable(Map<String, V> map) {
        // now filter out all non-serializable values
        Set<String> keysToRemove = FastSet.newInstance();
        for (Map.Entry<String, V> mapEntry: map.entrySet()) {
            Object entryValue = mapEntry.getValue();
            if (entryValue != null && !(entryValue instanceof Serializable)) {
                keysToRemove.add(mapEntry.getKey());
                //Debug.logInfo("Found Map value that is not Serializable: " + mapEntry.getKey() + "=" + mapEntry.getValue(), module);
            }
        }
        for (String keyToRemove: keysToRemove) { map.remove(keyToRemove); }
        //if (!(map instanceof Serializable)) {
        //    Debug.logInfo("Parameter Map is not Serializable!", module);
        //}

        //for (Map.Entry<String, V> mapEntry: map.entrySet()) {
        //    Debug.logInfo("Entry in Map made serializable: " + mapEntry.getKey() + "=" + mapEntry.getValue(), module);
        //}
    }

    /**
     * Sort a List of Maps by specified consistent keys.
     * @param listOfMaps List of Map objects to sort.
     * @param sortKeys List of Map keys to sort by.
     * @return a new List of sorted Maps.
     */
    public static List<Map<Object, Object>> sortMaps(List<Map<Object, Object>> listOfMaps, List<? extends String> sortKeys) {
        if (listOfMaps == null || sortKeys == null)
            return null;
        List<Map<Object, Object>> toSort = FastList.newInstance();
        toSort.addAll(listOfMaps);
        try {
            MapComparator mc = new MapComparator(sortKeys);
            Collections.sort(toSort, mc);
        } catch (Exception e) {
            Debug.logError(e, "Problems sorting list of maps; returning null.", module);
            return null;
        }
        return toSort;
    }

    /**
     * Assuming outerMap not null; if null will throw a NullPointerException
     */
    public static <K, IK, V> Map<IK, V> getMapFromMap(Map<K, Object> outerMap, K key) {
        Map<IK, V> innerMap = UtilGenerics.<IK, V>checkMap(outerMap.get(key));
        if (innerMap == null) {
            innerMap = FastMap.newInstance();
            outerMap.put(key, innerMap);
        }
        return innerMap;
    }

    /**
     * Assuming outerMap not null; if null will throw a NullPointerException
     */
    public static <K, V> List<V> getListFromMap(Map<K, Object> outerMap, K key) {
        List<V> innerList = UtilGenerics.<V>checkList(outerMap.get(key));
        if (innerList == null) {
            innerList = FastList.newInstance();
            outerMap.put(key, innerList);
        }
        return innerList;
    }

    /**
     * Assuming theMap not null; if null will throw a NullPointerException
     */
    public static <K> BigDecimal addToBigDecimalInMap(Map<K, Object> theMap, K mapKey, BigDecimal addNumber) {
        Object currentNumberObj = theMap.get(mapKey);
        BigDecimal currentNumber = null;
        if (currentNumberObj == null) {
            currentNumber = ZERO_BD;
        } else if (currentNumberObj instanceof BigDecimal) {
            currentNumber = (BigDecimal) currentNumberObj;
        } else if (currentNumberObj instanceof Double) {
            currentNumber = new BigDecimal(((Double) currentNumberObj).doubleValue());
        } else if (currentNumberObj instanceof Long) {
            currentNumber = new BigDecimal(((Long) currentNumberObj).longValue());
        } else {
            throw new IllegalArgumentException("In addToBigDecimalInMap found a Map value of a type not supported: " + currentNumberObj.getClass().getName());
        }

        if (addNumber == null || ZERO_BD.compareTo(addNumber) == 0) {
            return currentNumber;
        }
        currentNumber = currentNumber.add(addNumber);
        theMap.put(mapKey, currentNumber);
        return currentNumber;
    }

    public static <T> T removeFirst(List<T> lst) {
        return lst.remove(0);
    }

    public static <T> Set<T> collectionToSet(Collection<T> c) {
        if (c == null) return null;
        Set<T> theSet = null;
        if (c instanceof Set) {
            theSet = (Set<T>) c;
        } else {
            theSet = FastSet.newInstance();
            c.remove(null);
            theSet.addAll(c);
        }
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1, T obj2) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        theSet.add(obj2);
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1, T obj2, T obj3) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        theSet.add(obj2);
        theSet.add(obj3);
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1, T obj2, T obj3, T obj4) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        theSet.add(obj2);
        theSet.add(obj3);
        theSet.add(obj4);
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1, T obj2, T obj3, T obj4, T obj5) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        theSet.add(obj2);
        theSet.add(obj3);
        theSet.add(obj4);
        theSet.add(obj5);
        return theSet;
    }

    /**
     * Create a Set from passed objX parameters
     * @return The resulting Set
     */
    public static <T> Set<T> toSet(T obj1, T obj2, T obj3, T obj4, T obj5, T obj6) {
        Set<T> theSet = FastSet.newInstance();
        theSet.add(obj1);
        theSet.add(obj2);
        theSet.add(obj3);
        theSet.add(obj4);
        theSet.add(obj5);
        theSet.add(obj6);
        return theSet;
    }

    public static <T> Set<T> toSet(Collection<T> collection) {
        if (collection == null) return null;
        if (collection instanceof Set) {
            return (Set<T>) collection;
        } else {
            Set<T> theSet = FastSet.newInstance();
            theSet.addAll(collection);
            return theSet;
        }
    }

    public static <T> Set<T> toSetArray(T[] data) {
        if (data == null) {
            return null;
        }
        Set<T> set = FastSet.newInstance();
        for (T value: data) {
            set.add(value);
        }
        return set;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        list.add(obj2);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4, T obj5) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        list.add(obj5);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static <T> List<T> toList(T obj1, T obj2, T obj3, T obj4, T obj5, T obj6) {
        List<T> list = FastList.newInstance();

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        list.add(obj5);
        list.add(obj6);
        return list;
    }

    public static <T> List<T> toList(Collection<T> collection) {
        if (collection == null) return null;
        if (collection instanceof List) {
            return (List<T>) collection;
        } else {
            List<T> list = FastList.newInstance();
            list.addAll(collection);
            return list;
        }
    }

    public static <T> List<T> toListArray(T[] data) {
        if (data == null) {
            return null;
        }
        List<T> list = FastList.newInstance();
        for (T value: data) {
            list.add(value);
        }
        return list;
    }

    public static <K, V> void addToListInMap(V element, Map<K, Object> theMap, K listKey) {
        List<V> theList = UtilGenerics.checkList(theMap.get(listKey));
        if (theList == null) {
            theList = FastList.newInstance();
            theMap.put(listKey, theList);
        }
        theList.add(element);
    }

    public static <K, V> void addToSetInMap(V element, Map<K, Set<V>> theMap, K setKey) {
        Set<V> theSet = UtilGenerics.checkSet(theMap.get(setKey));
        if (theSet == null) {
            theSet = FastSet.newInstance();
            theMap.put(setKey, theSet);
        }
        theSet.add(element);
    }

    public static <K, V> void addToSortedSetInMap(V element, Map<K, Set<V>> theMap, K setKey) {
        Set<V> theSet = UtilGenerics.checkSet(theMap.get(setKey));
        if (theSet == null) {
            theSet = new TreeSet<V>();
            theMap.put(setKey, theSet);
        }
        theSet.add(element);
    }

    /** Converts an <code>Object</code> to a <code>double</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return double value
     */
    public static double toDouble(Object obj) {
        Double result = toDoubleObject(obj);
        return result == null ? 0.0 : result.doubleValue();
    }

    /** Converts an <code>Object</code> to a <code>Double</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Double
     */
    public static Double toDoubleObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Number) {
            return new Double(((Number)obj).doubleValue());
        }
        Double result = null;
        try {
            result = Double.parseDouble(obj.toString());
        } catch (Exception e) {}
        return result;
    }

    /** Converts an <code>Object</code> to an <code>int</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return int value
     */
    public static int toInteger(Object obj) {
        Integer result = toIntegerObject(obj);
        return result == null ? 0 : result.intValue();
    }

    /** Converts an <code>Object</code> to an <code>Integer</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Integer
     */
    public static Integer toIntegerObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number)obj).intValue();
        }
        Integer result = null;
        try {
            result = Integer.parseInt(obj.toString());
        } catch (Exception e) {}
        return result;
    }

    /** Converts an <code>Object</code> to a <code>long</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return long value
     */
    public static long toLong(Object obj) {
        Long result = toLongObject(obj);
        return result == null ? 0 : result.longValue();
    }

    /** Converts an <code>Object</code> to a <code>Long</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Long
     */
    public static Long toLongObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return new Long(((Number)obj).longValue());
        }
        Long result = null;
        try {
            result = Long.parseLong(obj.toString());
        } catch (Exception e) {}
        return result;
    }

    /**
     * Adds value to the key entry in theMap, or creates a new one if not already there
     * @param theMap
     * @param key
     * @param value
     */
    public static <K> void addToDoubleInMap(Map<K, Object> theMap, K key, Double value) {
        Double curValue = (Double) theMap.get(key);
        if (curValue != null) {
            theMap.put(key, curValue + value);
        } else {
            theMap.put(key, value);
        }
    }

    /**
     * Parse a locale string Locale object
     * @param localeString The locale string (en_US)
     * @return Locale The new Locale object or null if no valid locale can be interpreted
     */
    public static Locale parseLocale(String localeString) {
        if (UtilValidate.isEmpty(localeString)) {
            return null;
        }

        Locale locale = null;
        if (localeString.length() == 2) {
            // two letter language code
            locale = new Locale(localeString);
        } else if (localeString.length() == 5) {
            // positions 0-1 language, 3-4 are country
            String language = localeString.substring(0, 2);
            String country = localeString.substring(3, 5);
            locale = new Locale(language, country);
        } else if (localeString.length() > 6) {
            // positions 0-1 language, 3-4 are country, 6 and on are special extensions
            String language = localeString.substring(0, 2);
            String country = localeString.substring(3, 5);
            String extension = localeString.substring(6);
            locale = new Locale(language, country, extension);
        } else {
            Debug.logWarning("Do not know what to do with the localeString [" + localeString + "], should be length 2, 5, or greater than 6, returning null", module);
        }

        return locale;
    }

    /** The input can be a String, Locale, or even null and a valid Locale will always be returned; if nothing else works, returns the default locale.
     * @param localeObject An Object representing the locale
     */
    public static Locale ensureLocale(Object localeObject) {
        if (localeObject != null && localeObject instanceof String) {
            localeObject = UtilMisc.parseLocale((String) localeObject);
        }
        if (localeObject != null && localeObject instanceof Locale) {
            return (Locale) localeObject;
        }
        return Locale.getDefault();
    }

    public static List<Locale> availableLocaleList = null;
    /** Returns a List of available locales sorted by display name */
    public static List<Locale> availableLocales() {
        if (availableLocaleList == null) {
            synchronized(UtilMisc.class) {
                if (availableLocaleList == null) {
                    TreeMap<String, Locale> localeMap = new TreeMap<String, Locale>();
                    String localesString = UtilProperties.getPropertyValue("general", "locales.available");
                    if (UtilValidate.isNotEmpty(localesString)) { // check if available locales need to be limited according general.properties file
                        int end = -1;
                        int start = 0;
                        for (int i=0; start < localesString.length(); i++) {
                            end = localesString.indexOf(",", start);
                            if (end == -1) {
                                end = localesString.length();
                            }
                            Locale curLocale = UtilMisc.ensureLocale(localesString.substring(start, end));
                            localeMap.put(curLocale.getDisplayName(), curLocale);
                            start = end + 1;
                        }
                    } else {
                        Locale[] locales = Locale.getAvailableLocales();
                        for (int i = 0; i < locales.length && locales[i] != null; i++) {
                            localeMap.put(locales[i].getDisplayName(), locales[i]);
                        }
                    }
                    availableLocaleList = new LinkedList<Locale>(localeMap.values());
                }
            }
        }
        return availableLocaleList;
    }

    /** This is meant to be very quick to create and use for small sized maps, perfect for how we usually use UtilMisc.toMap */
    @SuppressWarnings("serial")
    protected static class SimpleMap<V> implements Map<String, V>, java.io.Serializable {
        protected Map<String, V> realMapIfNeeded = null;

        String[] names;
        Object[] values;

        public SimpleMap() {
            names = new String[0];
            values = new Object[0];
        }

        public SimpleMap(String name1, Object value1) {
            names = new String[1];
            values = new Object[1];
            this.names[0] = name1;
            this.values[0] = value1;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2) {
            names = new String[2];
            values = new Object[2];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
            names = new String[3];
            values = new Object[3];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
            this.names[2] = name3;
            this.values[2] = value3;
        }

        public SimpleMap(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) {
            names = new String[4];
            values = new Object[4];
            this.names[0] = name1;
            this.values[0] = value1;
            this.names[1] = name2;
            this.values[1] = value2;
            this.names[2] = name3;
            this.values[2] = value3;
            this.names[3] = name4;
            this.values[3] = value4;
        }

        @SuppressWarnings("unchecked")
        protected void makeRealMap() {
            realMapIfNeeded = FastMap.newInstance();
            for (int i = 0; i < names.length; i++) {
                realMapIfNeeded.put(names[i], (V) values[i]);
            }
            this.names = null;
            this.values = null;
        }

        public void clear() {
            if (realMapIfNeeded != null) {
                realMapIfNeeded.clear();
            } else {
                realMapIfNeeded = FastMap.newInstance();
                names = null;
                values = null;
            }
        }

        public boolean containsKey(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.containsKey(obj);
            } else {
                for (String name: names) {
                    if (obj == null && name == null) return true;
                    if (name != null && name.equals(obj)) return true;
                }
                return false;
            }
        }

        public boolean containsValue(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.containsValue(obj);
            } else {
                for (Object value: values) {
                    if (obj == null && value == null) return true;
                    if (value != null && value.equals(obj)) return true;
                }
                return false;
            }
        }

        public java.util.Set<Map.Entry<String, V>> entrySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.entrySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.entrySet();
            }
        }

        @SuppressWarnings("unchecked")
        public V get(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.get(obj);
            } else {
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && names[i] == null) return (V) values[i];
                    if (names[i] != null && names[i].equals(obj)) return (V) values[i];
                }
                return null;
            }
        }

        public boolean isEmpty() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.isEmpty();
            } else {
                if (this.names.length == 0) return true;
                return false;
            }
        }

        public java.util.Set<String> keySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.keySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.keySet();
            }
        }

        public V put(String obj, V obj1) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.put(obj, obj1);
            } else {
                this.makeRealMap();
                return realMapIfNeeded.put(obj, obj1);
            }
        }

        public void putAll(java.util.Map<? extends String, ? extends V> map) {
            if (realMapIfNeeded != null) {
                realMapIfNeeded.putAll(map);
            } else {
                this.makeRealMap();
                realMapIfNeeded.putAll(map);
            }
        }

        public V remove(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.remove(obj);
            } else {
                this.makeRealMap();
                return realMapIfNeeded.remove(obj);
            }
        }

        public int size() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.size();
            } else {
                return this.names.length;
            }
        }

        public java.util.Collection<V> values() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.values();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.values();
            }
        }

        @Override
        public String toString() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.toString();
            } else {
                StringBuilder outString = new StringBuilder("{");
                for (int i = 0; i < names.length; i++) {
                    if (i > 0) outString.append(',');
                    outString.append('{');
                    outString.append(names[i]);
                    outString.append(',');
                    outString.append(values[i]);
                    outString.append('}');
                }
                outString.append('}');
                return outString.toString();
            }
        }

        @Override
        public int hashCode() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.hashCode();
            } else {
                int hashCode = 0;
                for (int i = 0; i < names.length; i++) {
                    //note that this calculation is done based on the calc specified in the Java java.util.Map interface
                    int tempNum = (names[i] == null   ? 0 : names[i].hashCode()) ^
                            (values[i] == null ? 0 : values[i].hashCode());
                    hashCode += tempNum;
                }
                return hashCode;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.equals(obj);
            } else {
                Map<String, V> mapObj = UtilGenerics.<String, V>checkMap(obj);

                //first check the size
                if (mapObj.size() != names.length) return false;

                //okay, same size, now check each entry
                for (int i = 0; i < names.length; i++) {
                    //first check the name
                    if (!mapObj.containsKey(names[i])) return false;

                    //if that passes, check the value
                    Object mapValue = mapObj.get(names[i]);
                    if (mapValue == null) {
                        if (values[i] != null) return false;
                    } else {
                        if (!mapValue.equals(values[i])) return false;
                    }
                }

                return true;
            }
        }
    }

    public static void staticWait(long timeout) throws InterruptedException {
        new UtilMiscWaiter().safeWait(timeout);
    }
    protected static class UtilMiscWaiter {
        public synchronized void safeWait(long timeout) throws InterruptedException {
            this.wait(timeout);
        }
    }
    public static void copyFile(File sourceLocation , File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            throw new IOException("File is a directory, not a file, cannot copy") ;
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
