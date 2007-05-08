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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.ofbiz.base.util.UtilProperties;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.collections.MapComparator;

/**
 * UtilMisc - Misc Utility Functions
 */
public class UtilMisc {

    public static final String module = UtilMisc.class.getName();
    
    public static final BigDecimal ZERO_BD = new BigDecimal(0.0);

    /**
     * Get an iterator from a collection, returning null if collection is null
     * @param col The collection to be turned in to an iterator
     * @return The resulting Iterator
     */
    public static Iterator toIterator(Collection col) {
        if (col == null)
            return null;
        else
            return col.iterator();
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static Map toMap(String name1, Object value1) {
        return new UtilMisc.SimpleMap(name1, value1);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static Map toMap(String name1, Object value1, String name2, Object value2) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2);

        /* Map fields = FastMap.newInstance();
         fields.put(name1, value1);
         fields.put(name2, value2);
         return fields;*/
    }

    /**
     * Create a map from passed nameX, valueX parameters
     * @return The resulting Map
     */
    public static Map toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2, name3, value3);

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
    public static Map toMap(String name1, Object value1, String name2, Object value2, String name3,
        Object value3, String name4, Object value4) {
        return new UtilMisc.SimpleMap(name1, value1, name2, value2, name3, value3, name4, value4);

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
    public static Map toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3,
        String name4, Object value4, String name5, Object value5) {
        Map fields = FastMap.newInstance();

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
    public static Map toMap(String name1, Object value1, String name2, Object value2, String name3, Object value3,
        String name4, Object value4, String name5, Object value5, String name6, Object value6) {
        Map fields = FastMap.newInstance();

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
    public static Map toMap(Object[] data) {
        if (data == null) {
            return null;
        }
        if (data.length % 2 == 1) {
            throw new IllegalArgumentException("You must pass an even sized array to the toMap method");
        }
        Map map = FastMap.newInstance();
        for (int i = 0; i < data.length; ) {
            map.put(data[i++], data[i++]);
        }
        return map;
    }

    public static String printMap(Map theMap) {
        StringBuffer theBuf = new StringBuffer();
        Iterator entryIter = theMap.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            theBuf.append(entry.getKey());
            theBuf.append(" --> ");
            theBuf.append(entry.getValue());
            theBuf.append(System.getProperty("line.separator"));            
        }
        return theBuf.toString();
    }
    
    /**
     * Sort a List of Maps by specified consistent keys.
     * @param listOfMaps List of Map objects to sort.
     * @param sortKeys List of Map keys to sort by.
     * @return a new List of sorted Maps.
     */
    public static List sortMaps(List listOfMaps, List sortKeys) {
        if (listOfMaps == null || sortKeys == null)
            return null;
        List toSort = FastList.newInstance();
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
    public static Map getMapFromMap(Map outerMap, Object key) {
        Map innerMap = (Map) outerMap.get(key);
        if (innerMap == null) {
            innerMap = FastMap.newInstance();
            outerMap.put(key, innerMap);
        }
        return innerMap;
    }

    /**
     * Assuming outerMap not null; if null will throw a NullPointerException 
     */
    public static List getListFromMap(Map outerMap, Object key) {
        List innerList = (List) outerMap.get(key);
        if (innerList == null) {
            innerList = FastList.newInstance();
            outerMap.put(key, innerList);
        }
        return innerList;
    }
    
    /**
     * Assuming theMap not null; if null will throw a NullPointerException 
     */
    public static void addToBigDecimalInMap(Map theMap, Object mapKey, BigDecimal addNumber) {
        if (addNumber == null || ZERO_BD.equals(addNumber)) {
            return;
        }
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
        
        currentNumber = currentNumber.add(addNumber);
        theMap.put(mapKey, currentNumber);
    }

    public static Object removeFirst(List lst) {
        return lst.remove(0);
    }
    
    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static List toList(Object obj1) {
        List list = new ArrayList(1);

        list.add(obj1);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static List toList(Object obj1, Object obj2) {
        List list = new ArrayList(2);

        list.add(obj1);
        list.add(obj2);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static List toList(Object obj1, Object obj2, Object obj3) {
        List list = new ArrayList(3);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        return list;
    }

    /**
     * Create a list from passed objX parameters
     * @return The resulting List
     */
    public static List toList(Object obj1, Object obj2, Object obj3, Object obj4) {
        List list = new ArrayList(4);

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
    public static List toList(Object obj1, Object obj2, Object obj3, Object obj4, Object obj5) {
        List list = new ArrayList(5);

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
    public static List toList(Object obj1, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
        List list = new ArrayList(6);

        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        list.add(obj4);
        list.add(obj5);
        list.add(obj6);
        return list;
    }

    public static List toList(Collection collection) {
        if (collection == null) return null;
        if (collection instanceof List) {
            return (List) collection;
        } else {
            return new ArrayList(collection);
        }
    }

    public static List toListArray(Object[] data) {
        if (data == null) {
            return null;
        }
        List list = new ArrayList(data.length);
        for (int i = 0; i < data.length; i++) {
            list.add(data[i]);
        }
        return list;
    }
    
    public static long toLong(Object value) {
        if (value != null) {
            if (value instanceof Long) {
                return ((Long) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        }
        return 0;
    }

    /**
     * Returns a double from value, where value could either be a Double or a String
     * @param value
     * @return
     */
    public static double toDouble(Object value) {
        if (value != null) {
            if (value instanceof Double) {
                return ((Double) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        }
        return 0.0;
    }

    /**
     * Adds value to the key entry in theMap, or creates a new one if not already there
     * @param theMap
     * @param key
     * @param value
     */
    public static void addToDoubleInMap(Map theMap, Object key, Double value) {
        Double curValue = (Double) theMap.get(key);
        if (curValue != null) {
            theMap.put(key, new Double(curValue.doubleValue() + value.doubleValue()));
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
        if (localeString == null || localeString.length() == 0) {
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

    public static List availableLocaleList = null;
    /** Returns a List of available locales sorted by display name */
    public static List availableLocales() {
        if (availableLocaleList == null) {
            synchronized(UtilMisc.class) {
                if (availableLocaleList == null) {
                    TreeMap localeMap = new TreeMap();
                    String localesString = UtilProperties.getPropertyValue("general", "locales.available");
                    if (localesString != null && localesString.length() > 0) { // check if available locales need to be limited according general.properties file
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
                    availableLocaleList = new LinkedList(localeMap.values());
                }
            }
        }
        return availableLocaleList;
    }

    /** This is meant to be very quick to create and use for small sized maps, perfect for how we usually use UtilMisc.toMap */
    protected static class SimpleMap implements Map, java.io.Serializable {
        protected Map realMapIfNeeded = null;

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

        protected void makeRealMap() {
            realMapIfNeeded = FastMap.newInstance();
            for (int i = 0; i < names.length; i++) {
                realMapIfNeeded.put(names[i], values[i]);
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
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && names[i] == null) return true;
                    if (names[i] != null && names[i].equals(obj)) return true;
                }
                return false;
            }
        }

        public boolean containsValue(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.containsValue(obj);
            } else {
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && values[i] == null) return true;
                    if (values[i] != null && values[i].equals(obj)) return true;
                }
                return false;
            }
        }

        public java.util.Set entrySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.entrySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.entrySet();
            }
        }

        public Object get(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.get(obj);
            } else {
                for (int i = 0; i < names.length; i++) {
                    if (obj == null && names[i] == null) return values[i];
                    if (names[i] != null && names[i].equals(obj)) return values[i];
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

        public java.util.Set keySet() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.keySet();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.keySet();
            }
        }

        public Object put(Object obj, Object obj1) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.put(obj, obj1);
            } else {
                this.makeRealMap();
                return realMapIfNeeded.put(obj, obj1);
            }
        }

        public void putAll(java.util.Map map) {
            if (realMapIfNeeded != null) {
                realMapIfNeeded.putAll(map);
            } else {
                this.makeRealMap();
                realMapIfNeeded.putAll(map);
            }
        }

        public Object remove(Object obj) {
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

        public java.util.Collection values() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.values();
            } else {
                this.makeRealMap();
                return realMapIfNeeded.values();
            }
        }

        public String toString() {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.toString();
            } else {
                StringBuffer outString = new StringBuffer("{");
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

        public boolean equals(Object obj) {
            if (realMapIfNeeded != null) {
                return realMapIfNeeded.equals(obj);
            } else {
                Map mapObj = (Map) obj;

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
}
