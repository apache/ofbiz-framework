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
package org.ofbiz.minilang.method;

import java.util.Map;

import org.ofbiz.base.util.collections.FlexibleMapAccessor;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 */
public class ContextAccessor {

    protected String name;
    protected FlexibleMapAccessor fma;
    protected boolean needsExpand;
    protected boolean empty;

    public ContextAccessor(String name) {
        init(name);
    }
    
    public ContextAccessor(String name, String defaultName) {
        if (name == null || name.length() == 0) {
            init(defaultName);
        } else {
            init(name);
        }
    }
    
    protected void init(String name) {
        this.name = name;
        if (name == null || name.length() == 0) {
            empty = true;
            needsExpand = false;
            fma = new FlexibleMapAccessor(name);
        } else {
            empty = false;
            int openPos = name.indexOf("${");
            if (openPos != -1 && name.indexOf("}", openPos) != -1) {
                fma = null;
                needsExpand = true;
            } else {
                fma = new FlexibleMapAccessor(name);
                needsExpand = false;
            }
        }
    }
    
    public boolean isEmpty() {
        return this.empty;
    }
    
    /** Based on name get from Map or from List in Map */
    public Object get(MethodContext methodContext) {
        if (this.needsExpand) {
            return methodContext.getEnv(name);
        } else {
            return methodContext.getEnv(fma);
        }
    }
    
    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(MethodContext methodContext, Object value) {
        if (this.needsExpand) {
            methodContext.putEnv(name, value);
        } else {
            methodContext.putEnv(fma, value);
        }
    }
    
    /** Based on name remove from Map or from List in Map */
    public Object remove(MethodContext methodContext) {
        if (this.needsExpand) {
            return methodContext.removeEnv(name);
        } else {
            return methodContext.removeEnv(fma);
        }
    }
    
    /** Based on name get from Map or from List in Map */
    public Object get(Map theMap, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            return fma.get(theMap);
        } else {
            return fma.get(theMap);
        }
    }
    
    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(Map theMap, Object value, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            fma.put(theMap, value);
        } else {
            fma.put(theMap, value);
        }
    }
    
    /** Based on name remove from Map or from List in Map */
    public Object remove(Map theMap, MethodContext methodContext) {
        if (this.needsExpand) {
            FlexibleMapAccessor fma = new FlexibleMapAccessor(methodContext.expandString(name));
            return fma.remove(theMap);
        } else {
            return fma.remove(theMap);
        }
    }
    
    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */    
    public int hashCode() {
        return this.name.hashCode();
    }

    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */    
    public boolean equals(Object obj) {
        if (obj instanceof ContextAccessor) {
            ContextAccessor contextAccessor = (ContextAccessor) obj;
            if (this.name == null) {
                return contextAccessor.name == null;
            }
            return this.name.equals(contextAccessor.name);
        } else {
            String str = (String) obj;
            if (this.name == null) {
                return str == null;
            }
            return this.name.equals(str);
        }
    }

    /** To be used for a string representation of the accessor, returns the original name. */    
    public String toString() {
        return this.name;
    }
}
