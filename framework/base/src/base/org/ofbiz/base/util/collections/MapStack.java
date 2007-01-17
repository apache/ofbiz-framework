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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.lang.Reusable;
import javolution.realtime.ObjectFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;


/**
 * Map Stack
 * 
 */
public class MapStack implements Map, Reusable, LocalizedMap {

    public static final String module = MapStack.class.getName();

    protected static final ObjectFactory mapStackFactory = new ObjectFactory() {
        protected Object create() {
            return new MapStack();
        }
    };
    
    public static MapStack create() {
        MapStack newValue = (MapStack) mapStackFactory.object();
        // initialize with a single entry
        newValue.push();
        return newValue;
    }

    public static MapStack create(Map baseMap) {
        MapStack newValue = (MapStack) mapStackFactory.object();
        newValue.stackList.add(0, baseMap);
        return newValue;
    }

    /** Does a shallow copy of the internal stack of the passed MapStack; enables simultaneous stacks that share common parent Maps */
    public static MapStack create(MapStack source) {
        MapStack newValue = (MapStack) mapStackFactory.object();
        newValue.stackList.addAll(source.stackList);
        return newValue;
    }

    protected MapStack() {
        super();
    }
    
    protected List stackList = FastList.newInstance();
    
    public void reset() {
        stackList = FastList.newInstance();
    }
    
    /** Puts a new Map on the top of the stack */
    public void push() {
        this.stackList.add(0, FastMap.newInstance());
    }
    
    /** Puts an existing Map on the top of the stack (top meaning will override lower layers on the stack) */
    public void push(Map existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot push null existing Map onto a MapStack");
        }
        this.stackList.add(0, existingMap);
    }
    
    /** Puts an existing Map on the BOTTOM of the stack (bottom meaning will be overriden by lower layers on the stack, ie everything else already there) */
    public void addToBottom(Map existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot add null existing Map to bottom of a MapStack");
        }
        this.stackList.add(existingMap);
    }
    
    /** Remove and returns the Map from the top of the stack; if there is only one Map on the stack it returns null and does not remove it */
    public Map pop() {
        // always leave at least one Map in the List, ie never pop off the last Map
        if (this.stackList.size() > 1) {
            return (Map) stackList.remove(0);
        } else {
            return null;
        }
    }
    
    /** 
     * Creates a MapStack object that has the same Map objects on its stack; 
     * meant to be used to enable a 
     * situation where a parent and child context are operating simultaneously 
     * using two different MapStack objects, but sharing the Maps in common  
     */
    public MapStack standAloneStack() {
        MapStack standAlone = MapStack.create(this);
        return standAlone;
    }

    /** 
     * Creates a MapStack object that has the same Map objects on its stack, 
     * but with a new Map pushed on the top; meant to be used to enable a 
     * situation where a parent and child context are operating simultaneously 
     * using two different MapStack objects, but sharing the Maps in common  
     */
    public MapStack standAloneChildStack() {
        MapStack standAloneChild = MapStack.create(this);
        standAloneChild.push();
        return standAloneChild;
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        // a little bit tricky; to represent the apparent size we need to aggregate all keys and get a count of unique keys
        // this is a bit of a slow way, but gets the best number possible
        Set keys = this.keySet();
        return keys.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        // walk the stackList and if any is not empty, return false; otherwise return true
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            if (!curMap.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            if (curMap.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, consider it an option, otherwise ignore
        Set resultKeySet = FastSet.newInstance();
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            Iterator curEntrySetIter = curMap.entrySet().iterator();
            while (curEntrySetIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) curEntrySetIter.next();
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    if (value == null) {
                        if (curEntry.getValue() == null) {
                            return true;
                        }
                    } else {
                        if (value.equals(curEntry.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        if ("context".equals(key)) {
            return this;
        }
        
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(key)) {
                return curMap.get(key);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.ofbiz.base.util.collections.LocalizedMap#get(java.lang.String, java.util.Locale)
     */
    public Object get(String name, Locale locale) {
        if ("context".equals(name)) {
            return this;
        }
        
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(name)) {
                if (curMap instanceof LocalizedMap) {
                    LocalizedMap lmap = (LocalizedMap) curMap;
                    return lmap.get(name, locale);
                } else {
                    return curMap.get(name);
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value) {
        if ("context".equals(key)) {
            if (value == null || this != value) {
                Debug.logWarning("WARNING: Putting a value in a MapStack with key [context] that is not this MapStack, will be hidden by the current MapStack self-reference: " + value, module);
            }
        }
            
        // all write operations are local: only put in the Map on the top of the stack
        Map currentMap = (Map) this.stackList.get(0);
        return currentMap.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        // all write operations are local: only remove from the Map on the top of the stack
        Map currentMap = (Map) this.stackList.get(0);
        return currentMap.remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map arg0) {
        // all write operations are local: only put in the Map on the top of the stack
        Map currentMap = (Map) this.stackList.get(0);
        currentMap.putAll(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        // all write operations are local: only clear the Map on the top of the stack
        Map currentMap = (Map) this.stackList.get(0);
        currentMap.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        // walk the stackList and aggregate all keys
        Set resultSet = FastSet.newInstance();
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            resultSet.addAll(curMap.keySet());
        }
        return Collections.unmodifiableSet(resultSet);
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set resultKeySet = FastSet.newInstance();
        List resultValues = FastList.newInstance();
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            Iterator curEntrySetIter = curMap.entrySet().iterator();
            while (curEntrySetIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) curEntrySetIter.next();
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    resultValues.add(curEntry.getValue());
                }
            }
        }
        return Collections.unmodifiableCollection(resultValues);
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set resultKeySet = FastSet.newInstance();
        Set resultEntrySet = FastSet.newInstance();
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            Iterator curEntrySetIter = curMap.entrySet().iterator();
            while (curEntrySetIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) curEntrySetIter.next();
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    resultEntrySet.add(curEntry);
                }
            }
        }
        return Collections.unmodifiableSet(resultEntrySet);
    }
    
    public String toString() {
        StringBuffer fullMapString = new StringBuffer();
        int curLevel = 0;
        Iterator stackIter = this.stackList.iterator();
        while (stackIter.hasNext()) {
            Map curMap = (Map) stackIter.next();
            fullMapString.append("============================== Start stack level " + curLevel + "\n");
            Iterator curEntrySetIter = curMap.entrySet().iterator();
            while (curEntrySetIter.hasNext()) {
                Map.Entry curEntry = (Map.Entry) curEntrySetIter.next();
                
                fullMapString.append("==>[");
                fullMapString.append(curEntry.getKey());
                fullMapString.append("]:");
                // skip the instances of MapStack to avoid infinite loop
                if (curEntry.getValue() instanceof MapStack) {
                    fullMapString.append("<Instance of MapStack, not printing to avoid infinite recursion>");
                } else {
                    fullMapString.append(curEntry.getValue());
                }
                fullMapString.append("\n");
            }
            fullMapString.append("============================== End stack level " + curLevel + "\n");
            curLevel++;
        }
        return fullMapString.toString();
    }
}
