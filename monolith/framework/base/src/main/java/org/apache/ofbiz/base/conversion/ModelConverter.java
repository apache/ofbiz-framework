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

/** Model converter interface. Classes implement this interface
 * to convert one object type to another.
 */
public interface ModelConverter<S, T> extends Converter<S, T> {

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param obj The source <code>Object</code> to convert
     * @param targetClassName The target class name for conversion - must not be <code>null</code>
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    T convert(S obj, String targetClassName) throws ConversionException;

    /** Converts <code>obj</code> to <code>T</code>.
     *
     * @param targetClass The <code>Class</code> to convert to
     * @param obj The source <code>Object</code> to convert
     * @param targetClassName The target class name for conversion - must not be <code>null</code>
     * @return The converted <code>Object</code>
     * @throws ConversionException
     */
    T convert(Class<? extends T> targetClass, S obj, String targetClassName) throws ConversionException;

}
