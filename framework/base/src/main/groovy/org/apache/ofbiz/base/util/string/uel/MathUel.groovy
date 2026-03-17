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


import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelMapping

import java.lang.reflect.Method

/**
 * Class for importing the Math Uel
 */
class MathUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('math:absDouble', Math.getMethod('abs', double)),
                new UelMapping('math:absFloat', Math.getMethod('abs', float)),
                new UelMapping('math:absInt', Math.getMethod('abs', int)),
                new UelMapping('math:absLong', Math.getMethod('abs', long)),
                new UelMapping('math:acos', Math.getMethod('acos', double)),
                new UelMapping('math:asin', Math.getMethod('asin', double)),
                new UelMapping('math:atan', Math.getMethod('atan', double)),
                new UelMapping('math:atan2', Math.getMethod('atan2', double, double)),
                new UelMapping('math:cbrt', Math.getMethod('cbrt', double)),
                new UelMapping('math:ceil', Math.getMethod('ceil', double)),
                new UelMapping('math:cos', Math.getMethod('cos', double)),
                new UelMapping('math:cosh', Math.getMethod('cosh', double)),
                new UelMapping('math:exp', Math.getMethod('exp', double)),
                new UelMapping('math:expm1', Math.getMethod('expm1', double)),
                new UelMapping('math:floor', Math.getMethod('floor', double)),
                new UelMapping('math:hypot', Math.getMethod('hypot', double, double)),
                new UelMapping('math:IEEEremainder', Math.getMethod('IEEEremainder', double, double)),
                new UelMapping('math:log', Math.getMethod('log', double)),
                new UelMapping('math:log10', Math.getMethod('log10', double)),
                new UelMapping('math:log1p', Math.getMethod('log1p', double)),
                new UelMapping('math:maxDouble', Math.getMethod('max', double, double)),
                new UelMapping('math:maxFloat', Math.getMethod('max', float, float)),
                new UelMapping('math:maxInt', Math.getMethod('max', int, int)),
                new UelMapping('math:maxLong', Math.getMethod('max', long, long)),
                new UelMapping('math:minDouble', Math.getMethod('min', double, double)),
                new UelMapping('math:minFloat', Math.getMethod('min', float, float)),
                new UelMapping('math:minInt', Math.getMethod('min', int, int)),
                new UelMapping('math:minLong', Math.getMethod('min', long, long)),
                new UelMapping('math:pow', Math.getMethod('pow', double, double)),
                new UelMapping('math:random', Math.getMethod('random')),
                new UelMapping('math:rint', Math.getMethod('rint', double)),
                new UelMapping('math:roundDouble', Math.getMethod('round', double)),
                new UelMapping('math:roundFloat', Math.getMethod('round', float)),
                new UelMapping('math:signumDouble', Math.getMethod('signum', double)),
                new UelMapping('math:signumFloat', Math.getMethod('signum', float)),
                new UelMapping('math:sin', Math.getMethod('sin', double)),
                new UelMapping('math:sinh', Math.getMethod('sinh', double)),
                new UelMapping('math:sqrt', Math.getMethod('sqrt', double)),
                new UelMapping('math:tan', Math.getMethod('tan', double)),
                new UelMapping('math:tanh', Math.getMethod('tanh', double)),
                new UelMapping('math:toDegrees', Math.getMethod('toDegrees', double)),
                new UelMapping('math:toRadians', Math.getMethod('toRadians', double)),
                new UelMapping('math:ulpDouble', Math.getMethod('ulp', double)),
                new UelMapping('math:ulpFloat', Math.getMethod('ulp', float)),
        ]
    }

}
