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

/**
 * Class for importing the Math Uel
 */
class MathUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('math:absDouble', Math.getMethod('abs', double),
                        'Returns the absolute value of a double value'),
                new UelMapping('math:absFloat', Math.getMethod('abs', float),
                        'Returns the absolute value of a float value.'),
                new UelMapping('math:absInt', Math.getMethod('abs', int),
                        'Returns the absolute value of an int value.'),
                new UelMapping('math:absLong', Math.getMethod('abs', long),
                        'Returns the absolute value of a long value.'),
                new UelMapping('math:acos', Math.getMethod('acos', double),
                        'Returns the arc cosine of an angle, in the range of 0.0 through pi.'),
                new UelMapping('math:asin', Math.getMethod('asin', double),
                        'Returns the arc sine of an angle, in the range of -pi/2 through pi/2.'),
                new UelMapping('math:atan', Math.getMethod('atan', double),
                        'Returns the arc tangent of an angle, in the range of -pi/2 through pi/2.'),
                new UelMapping('math:atan2', Math.getMethod('atan2', double, double),
                        'Converts rectangular coordinates (x, y) to polar (r,  theta).'),
                new UelMapping('math:cbrt', Math.getMethod('cbrt', double),
                        'Returns the cube root of a double value.'),
                new UelMapping('math:ceil', Math.getMethod('ceil', double),
                        'Returns the smallest (closest to negative infinity) double value that is greater than or' +
                                ' equal to the argument and is equal to a mathematical integer.'),
                new UelMapping('math:cos', Math.getMethod('cos', double),
                        'Returns the trigonometric cosine of an angle.'),
                new UelMapping('math:cosh', Math.getMethod('cosh', double),
                        'Returns the hyperbolic cosine of a double value.'),
                new UelMapping('math:exp', Math.getMethod('exp', double),
                        "Returns Euler's number e raised to the power of a double value."),
                new UelMapping('math:expm1', Math.getMethod('expm1', double),
                        'Returns ex -1.'),
                new UelMapping('math:floor', Math.getMethod('floor', double),
                        'Returns the largest (closest to positive infinity) double value that is less than or equal ' +
                                'to the argument and is equal to a mathematical integer.'),
                new UelMapping('math:hypot', Math.getMethod('hypot', double, double),
                        'Returns sqrt(x2 +y2) without intermediate overflow or underflow.'),
                new UelMapping('math:IEEEremainder', Math.getMethod('IEEEremainder', double, double),
                        'Computes the remainder operation on two arguments as prescribed by the IEEE 754 standard.'),
                new UelMapping('math:log', Math.getMethod('log', double),
                        'Returns the natural logarithm (base e) of a double value.'),
                new UelMapping('math:log10', Math.getMethod('log10', double),
                        'Returns the base 10 logarithm of a double value.'),
                new UelMapping('math:log1p', Math.getMethod('log1p', double),
                        'Returns the natural logarithm of the sum of the argument and 1.'),
                new UelMapping('math:maxDouble', Math.getMethod('max', double, double),
                        'Returns the greater of two double values.'),
                new UelMapping('math:maxFloat', Math.getMethod('max', float, float),
                        'Returns the greater of two float values.'),
                new UelMapping('math:maxInt', Math.getMethod('max', int, int),
                        'Returns the greater of two int values.'),
                new UelMapping('math:maxLong', Math.getMethod('max', long, long),
                        'Returns the greater of two long values.'),
                new UelMapping('math:minDouble', Math.getMethod('min', double, double),
                        'Returns the smaller of two double values.'),
                new UelMapping('math:minFloat', Math.getMethod('min', float, float),
                        'Returns the smaller of two float values.'),
                new UelMapping('math:minInt', Math.getMethod('min', int, int),
                        'Returns the smaller of two int values.'),
                new UelMapping('math:minLong', Math.getMethod('min', long, long),
                        'Returns the smaller of two long values.'),
                new UelMapping('math:pow', Math.getMethod('pow', double, double),
                        'Returns the value of the first argument raised to the power of the second argument.'),
                new UelMapping('math:random', Math.getMethod('random'),
                        'Returns a double value with a positive sign, greater than or equal to 0.0 and less than 1.0.'),
                new UelMapping('math:rint', Math.getMethod('rint', double),
                        'Returns the double value that is closest in value to the argument and is equal to a' +
                                ' mathematical integer.'),
                new UelMapping('math:roundDouble', Math.getMethod('round', double),
                        'Returns the closest long to the argument.'),
                new UelMapping('math:roundFloat', Math.getMethod('round', float),
                        'Returns the closest int to the argument.'),
                new UelMapping('math:signumDouble', Math.getMethod('signum', double),
                        'Returns the signum function of the argument; zero if the argument is zero, 1.0 if the ' +
                                'argument is greater than zero, -1.0 if the argument is less than zero.'),
                new UelMapping('math:signumFloat', Math.getMethod('signum', float),
                        'Returns the signum function of the argument; zero if the argument is zero, 1.0f if the ' +
                                'argument is greater than zero, -1.0f if the argument is less than zero.'),
                new UelMapping('math:sin', Math.getMethod('sin', double),
                        'Returns the trigonometric sine of an angle.'),
                new UelMapping('math:sinh', Math.getMethod('sinh', double),
                        'Returns the hyperbolic sine of a double value.'),
                new UelMapping('math:sqrt', Math.getMethod('sqrt', double),
                        'Returns the correctly rounded positive square root of a double value.'),
                new UelMapping('math:tan', Math.getMethod('tan', double),
                        'Returns the trigonometric tangent of an angle.'),
                new UelMapping('math:tanh', Math.getMethod('tanh', double),
                        'Returns the hyperbolic tangent of a double value.'),
                new UelMapping('math:toDegrees', Math.getMethod('toDegrees', double),
                        'Converts an angle measured in radians to an approximately equivalent angle measured in degrees.'),
                new UelMapping('math:toRadians', Math.getMethod('toRadians', double),
                        'Converts an angle measured in degrees to an approximately equivalent angle measured in radians.'),
                new UelMapping('math:ulpDouble', Math.getMethod('ulp', double),
                        'Returns the size of an ulp (units in the last place) of the argument.'),
                new UelMapping('math:ulpFloat', Math.getMethod('ulp', float),
                        'Returns the size of an ulp (units in the last place) of the argument.')
        ]
    }

}
