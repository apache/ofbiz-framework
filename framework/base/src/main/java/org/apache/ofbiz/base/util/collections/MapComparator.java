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
package org.apache.ofbiz.base.util.collections;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;

/**
 * MapComparator.java
 *
 */
public class MapComparator implements Comparator<Map<Object, Object>> {

    public static final String module = MapComparator.class.getName();

    private List<? extends Object> keys;

    /**
     * Method MapComparator.
     * @param keys List of Map keys to sort on
     */
    public MapComparator(List<? extends Object> keys) {
        this.keys = keys;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj==null) {
            return false;
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int compare(Map<Object, Object> map1, Map<Object, Object> map2) {

        if (keys == null || keys.size() < 1) {
            throw new IllegalArgumentException("No sort fields defined");
        }

        for (Object key: keys) {
            // if false will be descending, ie reverse order
            boolean ascending = true;

            Object o1 = null;
            Object o2 = null;

            if (key instanceof FlexibleMapAccessor<?>) {
                FlexibleMapAccessor<Object> fmaKey = UtilGenerics.cast(key);
                ascending = fmaKey.getIsAscending();

                o1 = fmaKey.get(UtilGenerics.<String, Object>checkMap(map1));
                o2 = fmaKey.get(UtilGenerics.<String, Object>checkMap(map2));
            } else {
                if (key instanceof String) {
                    String keyStr = (String) key;
                    if (keyStr.charAt(0) == '-') {
                        ascending = false;
                        key = keyStr.substring(1);
                    } else if (keyStr.charAt(0) == '+') {
                        ascending = true;
                        key = keyStr.substring(1);
                    }
                }

                o1 = map1.get(key);
                o2 = map2.get(key);
            }

            if (o1 == null && o2 == null) {
                continue;
            }

            int compareResult = 0;
            if (o1 != null && o2 == null) {
                compareResult = -1;
            }
            if (o1 == null && o2 != null) {
                compareResult = 1;
            }

            if (compareResult == 0) {
                try {
                    // the map values in question MUST implement the Comparable interface, if not we'll throw an exception
                    Comparable<Object> comp1 = UtilGenerics.cast(o1);
                    compareResult = comp1.compareTo(o2);
                } catch (Exception e) {
                    String errorMessage = "Error sorting list of Maps: " + e.toString();
                    Debug.logError(e, errorMessage, module);
                    throw new RuntimeException(errorMessage);
                }
            }

            // if zero they are equal, so we carry on to the next key to try and find a difference
            if (compareResult != 0) {
                if (ascending) {
                    return compareResult;
                }
                return -compareResult;
            }
        }

        // none of them were different, so they are equal
        return 0;
    }
}
