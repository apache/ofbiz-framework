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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 *
 */
public class FlexibleMapAccessor implements Serializable {
    public static final String module = FlexibleMapAccessor.class.getName();

    protected String original;
    protected String extName;
    protected boolean isListReference = false;
    protected boolean isAddAtIndex = false;
    protected boolean isAddAtEnd = false;
    protected boolean isAscending = true;
    protected int listIndex = -1;
    protected SubMapAccessor subMapAccessor = null;

    public FlexibleMapAccessor(String name) {
        this.original = name;
        
        // do one quick thing before getting into the Map/List stuff: if it starts with a + or - set the isAscending variable
        if (name != null && name.length() > 0) {
            if (name.charAt(0) == '-') {
                this.isAscending = false;
                name = name.substring(1);
            } else if (name.charAt(0) == '+') {
                this.isAscending = true;
                name = name.substring(1);
            }
        }

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            this.extName = name.substring(dotIndex+1);
            String subName = name.substring(0, dotIndex);
            this.subMapAccessor = new SubMapAccessor(subName);
        } else {
            this.extName = name;
        }
        int openBrace = this.extName.indexOf('[');
        int closeBrace = (openBrace == -1 ? -1 : this.extName.indexOf(']', openBrace));
        if (openBrace != -1 && closeBrace != -1) {
            String liStr = this.extName.substring(openBrace+1, closeBrace);
            //if brackets are empty, append to list
            if (liStr.length() == 0) {
                this.isAddAtEnd = true;
            } else {
                if (liStr.charAt(0) == '+') {
                    liStr = liStr.substring(1);
                    this.listIndex = Integer.parseInt(liStr);
                    this.isAddAtIndex = true;
                } else {
                    this.listIndex = Integer.parseInt(liStr);
                }
            }
            this.extName = this.extName.substring(0, openBrace);
            this.isListReference = true;
        }
    }
    
    public String getOriginalName() {
        return this.original;
    }
    
    public boolean getIsAscending() {
        return this.isAscending;
    }
    
    public boolean isEmpty() {
        if (this.original == null || this.original.length() == 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /** Given the name based information in this accessor, get the value from the passed in Map. 
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     * @param base
     * @return
     */
    public Object get(Map base) {
        return get(base, null);
    }
    
    /** Given the name based information in this accessor, get the value from the passed in Map. 
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     *  Note that the localization functionality is only used when the lowest level sub-map implements the LocalizedMap interface
     * @param base Map to get value from
     * @param locale Optional locale parameter, if null will see if the base Map contains a "locale" key
     * @return
     */
    public Object get(Map base, Locale locale) {
        if (base == null) {
            return null;
        }
        
        // so we can keep the passed context
        Map newBase = null;
        if (this.subMapAccessor != null) {
            try {
                newBase = this.subMapAccessor.getSubMap(base, false);
            } catch (Exception e) {
                String errMsg = "Error getting map accessor sub-map [" + this.subMapAccessor.extName + "] as part of [" + this.original + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new RuntimeException(errMsg);
            }
        } else {
            // DEJ 20041221 was like this, any reason to create a new Map?: newBase = new HashMap(base);
            newBase = base;
        }
        
        try {
            Object ret = null;
            if (this.isListReference) {
                List lst = (List) newBase.get(this.extName);
                if (lst == null) {
                    return null;
                }
                if (lst.size() == 0) {
                    return null;
                }
                ret = lst.get(this.isAddAtEnd ? lst.size() -1 : this.listIndex);
            } else {
                ret = getByLocale(this.extName, base, newBase, locale);
            }
            
            // in case the name has a dot like system env values
            if (ret == null) {
                ret = getByLocale(this.original, base, base, locale);
            }        
            
            return ret;
        } catch (Exception e) {
            String errMsg = "Error getting map accessor entry with name [" + this.extName + "] or [" + this.original + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new RuntimeException(errMsg);
        }
    }
    
    protected Object getByLocale(String name, Map base, Map sub, Locale locale) {
        if (sub == null) {
            return null;
        }
        if (sub instanceof LocalizedMap) {
            LocalizedMap locMap = (LocalizedMap) sub;
            if (locale != null) {
                return locMap.get(name, locale);
            } else if (base.containsKey("locale")) {
                return locMap.get(name, UtilMisc.ensureLocale(base.get("locale")));
            } else {
                return locMap.get(name, Locale.getDefault());
            }
        } else {
            Object getReturn = sub.get(name);
            return getReturn;
        }
    }
    
    /** Given the name based information in this accessor, put the value in the passed in Map. 
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     * @param base
     * @param value
     */
    public void put(Map base, Object value) {
        if (base == null) {
            throw new IllegalArgumentException("Cannot put a value in a null base Map");
        }
        if (this.subMapAccessor != null) {
            Map subBase = this.subMapAccessor.getSubMap(base, true);
            if (subBase == null) {
                return;
            }
            base = subBase;
        }
        if (this.isListReference) {
            List lst = (List) base.get(this.extName);
            if (lst == null) {
                lst = FastList.newInstance();
                base.put(this.extName, lst);
            }
            //if brackets are empty, append to list
            if (this.isAddAtEnd) {
                lst.add(value);
            } else {
                if (this.isAddAtIndex) {
                    lst.add(this.listIndex, value);
                } else {
                    lst.set(this.listIndex, value);
                }
            }
        } else {
            base.put(this.extName, value);
        }
    }
    
    /** Given the name based information in this accessor, remove the value from the passed in Map. * @param base
     * @param base the Map to remove from
     * @return the object removed
     */
    public Object remove(Map base) {
        if (this.subMapAccessor != null) {
            base = this.subMapAccessor.getSubMap(base, false);
        }
        if (this.isListReference) {
            List lst = (List) base.get(this.extName);
            return lst.remove(this.listIndex);
        } else {
            return base.remove(this.extName);
        }
    }
    
    public String toString() {
        return this.original;
    }
    
    public boolean equals(Object that) {
        FlexibleMapAccessor thatAcsr = (FlexibleMapAccessor) that;
        if (this.original == null) {
            if (thatAcsr.original == null) {
                return true;
            } else {
                return false;
            }
        } else {
            return this.original.equals(thatAcsr.original);
        }
    }
    
    public int hashCode() {
        return this.original.hashCode();
    }
    
    public class SubMapAccessor implements Serializable {
        protected String extName;
        protected boolean isListReference = false;
        protected int listIndex = -1;
        protected SubMapAccessor subMapAccessor = null;

        public SubMapAccessor(String name) {
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex != -1) {
                this.extName = name.substring(dotIndex+1);
                String subName = name.substring(0, dotIndex);
                this.subMapAccessor = new SubMapAccessor(subName);
            } else {
                this.extName = name;
            }
            int openBrace = this.extName.indexOf('[');
            int closeBrace = (openBrace == -1 ? -1 : this.extName.indexOf(']', openBrace));
            if (openBrace != -1 && closeBrace != -1) {
                String liStr = this.extName.substring(openBrace+1, closeBrace);
                this.listIndex = Integer.parseInt(liStr);
                this.extName = this.extName.substring(0, openBrace);
                this.isListReference = true;
            }
        }
        
        public Map getSubMap(Map base, boolean forPut) {
            if (base == null) return null;
            if (this.extName == null) return null;
            if (this.subMapAccessor != null) {
                base = this.subMapAccessor.getSubMap(base, forPut);
            }
            Object namedObj = base.get(this.extName);
            if (this.isListReference && (namedObj == null || namedObj instanceof List)) {
                List lst = (List) base.get(this.extName);
                if (lst == null) {
                    lst = FastList.newInstance();
                    base.put(this.extName, lst);
                }
                
                Map extMap = null;
                if (lst.size() > this.listIndex) {
                    extMap = (Map) lst.get(this.listIndex);
                }
                if (forPut && extMap == null) {
                    extMap = FastMap.newInstance();
                    lst.add(this.listIndex, extMap);
                }
                
                return extMap;
            } else if (namedObj == null || namedObj instanceof Map) {
                Map extMap = (Map) namedObj;
                
                // this code creates a new Map if none is missing, but for a get or remove this is a bad idea...
                if (forPut && extMap == null) {
                    extMap = FastMap.newInstance();
                    base.put(this.extName, extMap);
                }
                return extMap;
            } else {
                return null;
            }
        }
    }
}
