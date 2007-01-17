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
package org.ofbiz.base.util.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;

/**
 * MapComparator.java
 * 
 */
public class MapComparator implements Comparator {
    
    public static final String module = MapComparator.class.getName();
    
    private List keys;

    /**
     * Method MapComparator.
     * @param keys List of Map keys to sort on
     */
    public MapComparator(List keys) {
        this.keys = keys;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return obj.equals(this);
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object obj1, Object obj2) {
        Map map1, map2;
        try {
            map1 = (Map) obj1;
            map2 = (Map) obj2;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Objects not from the Map interface");
        }

        if (keys == null || keys.size() < 1) {
            throw new IllegalArgumentException("No sort fields defined");
        }

        Iterator i = keys.iterator();
        while (i.hasNext()) {
            Object key = i.next();
            // if false will be descending, ie reverse order
            boolean ascending = true;

            Object o1 = null;
            Object o2 = null;

            if (key instanceof FlexibleMapAccessor) {
                FlexibleMapAccessor fmaKey = (FlexibleMapAccessor) key;
                ascending = fmaKey.getIsAscending();
                
                //Debug.logInfo("Doing compare with a FlexibleMapAccessor [" + fmaKey.getOriginalName() + "] ascending [" + ascending + "]", module);
                
                o1 = fmaKey.get(map1);
                o2 = fmaKey.get(map2);
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
                    Comparable comp1 = (Comparable) o1;
                    Comparable comp2 = (Comparable) o2;
                    compareResult = comp1.compareTo(comp2);
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
                } else {
                    return -compareResult;
                }
            }
        }
        
        // none of them were different, so they are equal
        return 0;
    }
}
