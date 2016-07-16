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
package org.apache.ofbiz.base.conversion;

/** Converter interface. Classes implement this interface to convert one
 * Java object type to another.
 *
 * @param <S> The source object type
 * @param <T> The target object type
 */
public interface Converter<S, T> {
    /** Returns <code>true</code> if this object can convert
     * <code>sourceClass</code> to <code>targetClass</code>.
     * <p>Implementations can accomodate class hierarchy ranges
     * by converting super classes or interfaces.</p>
     *
     * @param sourceClass The source <code>Class</code>
     * @param targetClass The target <code>Class</code>
     * @return <code>true</code> if this object can convert
     * <code>sourceClass</code> to <code>targetClass</code>.
     */
    public boolean canConvert(Class<?> sourceClass, Class<?> targetClass);

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param obj The source <code>Object</code> to convert
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(S obj) throws ConversionException;

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param targetClass The <code>Class</code> to convert to
     * @param obj The source <code>Object</code> to convert
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    public T convert(Class<? extends T> targetClass, S obj) throws ConversionException;

    /** Returns the source <code>Class</code> for this converter.
     *
     * @return The source <code>Class</code> for this converter
     */
    public Class<?> getSourceClass();

    /** Returns the target <code>Class</code> for this converter.
     *
     * @return The target <code>Class</code> for this converter
     */
    public Class<?> getTargetClass();
}
