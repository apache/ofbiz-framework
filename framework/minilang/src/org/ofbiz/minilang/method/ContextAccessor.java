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

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 */
public class ContextAccessor<T> {

    protected String name;
    protected FlexibleMapAccessor<T> fma;

    public ContextAccessor(String name) {
        init(name);
    }

    public ContextAccessor(String name, String defaultName) {
        if (UtilValidate.isEmpty(name)) {
            init(defaultName);
        } else {
            init(name);
        }
    }

    protected void init(String name) {
        this.name = name;
        this.fma = FlexibleMapAccessor.getInstance(name);
    }

    public boolean isEmpty() {
        return this.fma.isEmpty();
    }

    /** Based on name get from Map or from List in Map */
    public T get(MethodContext methodContext) {
        return UtilGenerics.<T>cast(methodContext.getEnv(fma));
    }

    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(MethodContext methodContext, T value) {
        methodContext.putEnv(fma, value);
    }

    /** Based on name remove from Map or from List in Map */
    public T remove(MethodContext methodContext) {
        return UtilGenerics.<T>cast(methodContext.removeEnv(fma));
    }

    /** Based on name get from Map or from List in Map */
    public T get(Map<String, ? extends Object> theMap, MethodContext methodContext) {
        return fma.get(theMap);
    }

    /** Based on name put in Map or from List in Map;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that point instead of set at the point.
     */
    public void put(Map<String, Object> theMap, T value, MethodContext methodContext) {
        fma.put(theMap, value);
    }

    /** Based on name remove from Map or from List in Map */
    public T remove(Map<String, ? extends Object> theMap, MethodContext methodContext) {
        return fma.remove(theMap);
    }

    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContextAccessor<?>) {
            ContextAccessor<?> contextAccessor = (ContextAccessor<?>) obj;
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
    @Override
    public String toString() {
        return this.name;
    }
}
