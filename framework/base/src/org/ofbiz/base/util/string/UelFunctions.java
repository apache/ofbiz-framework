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
package org.ofbiz.base.util.string;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import javax.el.*;

import javolution.util.FastMap;

/** Implements Unified Expression Language functions.
 * <p>Built-in functions are divided into two prefixes - <code>math</code>
 *  and <code>util</code>.</p><p>The <code>math</code> prefix maps to
 *  the <code>java.lang.Math</code> class. Overloaded <code>java.lang.Math</code>
 *  methods have their parameter data types appended to the UEL function name
 *  - so <code>java.lang.Math.max(double a, double b)</code>
 *  becomes <code>${math:maxDouble(a, b)}</code>.</p><p>The
 *  <code>util</code> prefix contains miscellaneous utility functions:<br/>
 *  <ul>
 *    <li><code>${util:size(Object)}</code> returns the size of Maps,
 *    Collections, and Strings. Invalid Object types return -1.</li>
 *  </ul>
 *  </p>
 */
public class UelFunctions {

    protected static final FunctionMapper functionMapper = new Functions();

    /** Returns a <code>FunctionMapper</code> instance.
     * @return <code>FunctionMapper</code> instance
     */
    public static FunctionMapper getFunctionMapper() {
        return functionMapper;
    }

    protected static class Functions extends FunctionMapper {
        protected final Map<String, Method> functionMap = FastMap.newInstance();
        public Functions() {
            try {
                setFunction("math", "absDouble", Math.class.getMethod("abs", double.class));
                setFunction("math", "absFloat", Math.class.getMethod("abs", float.class));
                setFunction("math", "absInt", Math.class.getMethod("abs", int.class));
                setFunction("math", "absLong", Math.class.getMethod("abs", long.class));
                setFunction("math", "acos", Math.class.getMethod("abs", double.class));
                setFunction("math", "asin", Math.class.getMethod("asin", double.class));
                setFunction("math", "atan", Math.class.getMethod("atan", double.class));
                setFunction("math", "atan2", Math.class.getMethod("max", double.class, double.class));
                setFunction("math", "cbrt", Math.class.getMethod("cbrt", double.class));
                setFunction("math", "ceil", Math.class.getMethod("ceil", double.class));
                setFunction("math", "cos", Math.class.getMethod("cos", double.class));
                setFunction("math", "cosh", Math.class.getMethod("cosh", double.class));
                setFunction("math", "exp", Math.class.getMethod("exp", double.class));
                setFunction("math", "expm1", Math.class.getMethod("expm1", double.class));
                setFunction("math", "floor", Math.class.getMethod("floor", double.class));
                setFunction("math", "hypot", Math.class.getMethod("hypot", double.class, double.class));
                setFunction("math", "IEEEremainder", Math.class.getMethod("IEEEremainder", double.class, double.class));
                setFunction("math", "log", Math.class.getMethod("log", double.class));
                setFunction("math", "log10", Math.class.getMethod("log10", double.class));
                setFunction("math", "log1p", Math.class.getMethod("log1p", double.class));
                setFunction("math", "maxDouble", Math.class.getMethod("max", double.class, double.class));
                setFunction("math", "maxFloat", Math.class.getMethod("max", float.class, float.class));
                setFunction("math", "maxInt", Math.class.getMethod("max", int.class, int.class));
                setFunction("math", "maxLong", Math.class.getMethod("max", long.class, long.class));
                setFunction("math", "minDouble", Math.class.getMethod("min", double.class, double.class));
                setFunction("math", "minFloat", Math.class.getMethod("min", float.class, float.class));
                setFunction("math", "minInt", Math.class.getMethod("min", int.class, int.class));
                setFunction("math", "minLong", Math.class.getMethod("min", long.class, long.class));
                setFunction("math", "pow", Math.class.getMethod("pow", double.class, double.class));
                setFunction("math", "random", Math.class.getMethod("random"));
                setFunction("math", "rint", Math.class.getMethod("rint", double.class));
                setFunction("math", "roundDouble", Math.class.getMethod("round", double.class));
                setFunction("math", "roundFloat", Math.class.getMethod("round", float.class));
                setFunction("math", "signumDouble", Math.class.getMethod("signum", double.class));
                setFunction("math", "signumFloat", Math.class.getMethod("signum", float.class));
                setFunction("math", "sin", Math.class.getMethod("sin", double.class));
                setFunction("math", "sinh", Math.class.getMethod("sinh", double.class));
                setFunction("math", "sqrt", Math.class.getMethod("sqrt", double.class));
                setFunction("math", "tan", Math.class.getMethod("tan", double.class));
                setFunction("math", "tanh", Math.class.getMethod("tanh", double.class));
                setFunction("math", "toDegrees", Math.class.getMethod("toDegrees", double.class));
                setFunction("math", "toRadians", Math.class.getMethod("toRadians", double.class));
                setFunction("math", "ulpDouble", Math.class.getMethod("ulp", double.class));
                setFunction("math", "ulpFloat", Math.class.getMethod("ulp", float.class));
                setFunction("util", "size", UelFunctions.class.getMethod("getSize", Object.class));
            } catch (Exception e) {
                
            }
        }
        public void setFunction(String prefix, String localName, Method method) {
            synchronized(this) {
                functionMap.put(prefix + ":" + localName, method);
            }
        }
        public Method resolveFunction(String prefix, String localName) {
            return functionMap.get(prefix + ":" + localName);
        }
    }

    @SuppressWarnings("unchecked")
    public static int getSize(Object obj) {
        try {
            Map map = (Map) obj;
            return map.size();
        } catch (Exception e) {}
        try {
            Collection coll = (Collection) obj;
            return coll.size();
        } catch (Exception e) {}
        try {
            String str = (String) obj;
            return str.length();
        } catch (Exception e) {}
        return -1;
    }
}
