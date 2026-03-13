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
package org.apache.ofbiz.base.util.string.uel

import org.apache.ofbiz.base.util.string.UelMappingInterface

import java.lang.reflect.Method

/**
 * Class for importing the Math Uel
 */
class MathUel implements UelMappingInterface {

    @Override
    Map<String, Method> getMapping() {
        return [
                'math:absDouble': Math.getMethod('abs', double),
                'math:absFloat': Math.getMethod('abs', float),
                'math:absInt': Math.getMethod('abs', int),
                'math:absLong': Math.getMethod('abs', long),
                'math:acos': Math.getMethod('acos', double),
                'math:asin': Math.getMethod('asin', double),
                'math:atan': Math.getMethod('atan', double),
                'math:atan2': Math.getMethod('atan2', double, double),
                'math:cbrt': Math.getMethod('cbrt', double),
                'math:ceil': Math.getMethod('ceil', double),
                'math:cos': Math.getMethod('cos', double),
                'math:cosh': Math.getMethod('cosh', double),
                'math:exp': Math.getMethod('exp', double),
                'math:expm1': Math.getMethod('expm1', double),
                'math:floor': Math.getMethod('floor', double),
                'math:hypot': Math.getMethod('hypot', double, double),
                'math:IEEEremainder': Math.getMethod('IEEEremainder', double, double),
                'math:log': Math.getMethod('log', double),
                'math:log10': Math.getMethod('log10', double),
                'math:log1p': Math.getMethod('log1p', double),
                'math:maxDouble': Math.getMethod('max', double, double),
                'math:maxFloat': Math.getMethod('max', float, float),
                'math:maxInt': Math.getMethod('max', int, int),
                'math:maxLong': Math.getMethod('max', long, long),
                'math:minDouble': Math.getMethod('min', double, double),
                'math:minFloat': Math.getMethod('min', float, float),
                'math:minInt': Math.getMethod('min', int, int),
                'math:minLong': Math.getMethod('min', long, long),
                'math:pow': Math.getMethod('pow', double, double),
                'math:random': Math.getMethod('random'),
                'math:rint': Math.getMethod('rint', double),
                'math:roundDouble': Math.getMethod('round', double),
                'math:roundFloat': Math.getMethod('round', float),
                'math:signumDouble': Math.getMethod('signum', double),
                'math:signumFloat': Math.getMethod('signum', float),
                'math:sin': Math.getMethod('sin', double),
                'math:sinh': Math.getMethod('sinh', double),
                'math:sqrt': Math.getMethod('sqrt', double),
                'math:tan': Math.getMethod('tan', double),
                'math:tanh': Math.getMethod('tanh', double),
                'math:toDegrees': Math.getMethod('toDegrees', double),
                'math:toRadians': Math.getMethod('toRadians', double),
                'math:ulpDouble': Math.getMethod('ulp', double),
                'math:ulpFloat': Math.getMethod('ulp', float),
        ]
    }

}
