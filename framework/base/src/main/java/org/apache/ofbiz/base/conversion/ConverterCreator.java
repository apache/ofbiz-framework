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

/** ConverterCreator interface. Classes implement this interface to create a
 * converter that can convert one Java object type to another.
 */
public interface ConverterCreator {
    /** Creates a Converter that can convert the <code>sourceClass</code> to
     * the <code>targetClass</code>. Returns <code>null</code> if this creater
     * doesn't support the class pair.
     *
     * @param sourceClass The source <code>Class</code> to convert
     * @param targetClass The target <code>Class</code> to convert to
     * @return a converter that can convert <code>Object</code>s
     */
    public <S, T> Converter<S, T> createConverter(Class<S> sourceClass, Class<T> targetClass);
}
