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

import org.apache.ofbiz.base.util.ObjectType;

/** Abstract Converter class. This class handles converter registration
 * and it implements the <code>canConvert</code>, <code>getSourceClass</code>,
 * and <code>getTargetClass</code> methods.
 */
public abstract class AbstractConverter<S, T> implements Converter<S, T>, ConverterLoader {
    private final Class<? super S> sourceClass;
    private final Class<? super T> targetClass;

    protected AbstractConverter(Class<? super S> sourceClass, Class<? super T> targetClass) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
    }

    public void loadConverters() {
        Converters.registerConverter(this);
    }

    public T convert(Class<? extends T> targetClass, S obj) throws ConversionException {
        return convert(obj);
    }

    public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
        return ObjectType.instanceOf(sourceClass, this.getSourceClass()) && ObjectType.instanceOf(targetClass, this.getTargetClass());
    }

    public Class<? super S> getSourceClass() {
        return sourceClass;
    }

    public Class<? super T> getTargetClass() {
        return targetClass;
    }
}
